/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This file is part of qData-mdm Master Data Platform (Open Source Edition).
 *
 * qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
 * You may use qData-mdm for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData-mdm logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData-mdm as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONPath;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestApiReadConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskApiConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.API;

/**
 * 接入任务的 API 数据读取策略，供来源预览和正式采集共用。
 *
 * <p>支持 GET 查询参数和 POST JSON 请求体，按简单 JSON 路径提取对象数组，
 * 并按单次、页码、偏移量或游标方式读取。每页完整读取、校验后同步逐条回调，
 * 字段转换和入库由调用方负责；读取进度仅保存在本次调用的上下文及局部变量中。</p>
 *
 * <p>接口须提供稳定的分页结果。分页时拒绝内容完全相同的非空页，防止重复消费；
 * OFFSET 按配置页大小推进，未结束的短页会报错，避免接口限制页大小造成漏读。
 * HTTP 连接逐页释放，响应按 UTF-8 JSON 解析，不自动重定向或重试。</p>
 *
 * @author chentangwen
 */
@Component
public class IngestApiReader implements IngestReader {

    /** 单次连接建立超时，不代表整个读取任务的超时。 */
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    /** 单次阻塞读取超时，不代表整页响应的总耗时上限。 */
    private static final int READ_TIMEOUT_MILLIS = 60000;
    /** 单页响应的字节上限，解析后的对象占用内存可能更大。 */
    private static final int MAX_RESPONSE_BYTES = 20 * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static final String PAGING_NONE = "NONE";
    private static final String PAGING_PAGE = "PAGE";
    private static final String PAGING_OFFSET = "OFFSET";
    private static final String PAGING_CURSOR = "CURSOR";

    @Override
    public String getType() {
        return API;
    }

    /**
     * 兼容只消费来源字段值的读取入口。
     *
     * @param config 请求及分页配置
     * @param limit 最大回调条数，小于或等于 0 时不限制
     * @param consumer 来源字段值回调
     * @return 已回调记录的字段并集，按首次出现顺序排列
     * @throws ServiceException 请求配置、接口响应或分页状态无效时抛出
     */
    public List<String> read(IngestTaskApiConfigVO config, int limit, Consumer<Map<String, Object>> consumer) {
        if (ObjectUtil.isNull(consumer)) {
            throw new ServiceException("API记录回调不能为空");
        }
        IngestTaskSaveReqVO task = new IngestTaskSaveReqVO();
        task.setType(API);
        task.setApiConfig(config);
        return read(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 按页拉取并消费记录，预览达到条数限制后不再请求下一页。
     *
     * <p>单页响应仍完整读取并受大小上限约束；整页结构及分页状态在回调前检查。
     * 已完成的回调不因后续页失败而回滚，调用方负责事务和失败策略。
     * 回调异常保持原样抛出，不包装为接口拉取或解析错误。</p>
     *
     * @param context 本次读取配置与进度
     * @param consumer 带页内位置的来源记录回调
     * @return 已回调记录的字段并集
     * @throws ServiceException 入口参数、请求配置、接口响应或分页状态无效，以及读取中断时抛出
     */
    @Override
    public List<String> read(IngestReadContext context, Consumer<IngestRow> consumer) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(consumer)) {
            throw new ServiceException("API读取上下文和记录回调不能为空");
        }
        IngestTaskApiConfigVO config = context.getTask().getApiConfig();
        try {
            validateConfig(config);
        } catch (ServiceException ex) {
            if (!context.isPreview() || ex.getCode() != null) {
                throw ex;
            }
            ServiceException warning = new ServiceException(ex.getMessage(), HttpStatus.WARN);
            warning.initCause(ex);
            throw warning;
        }
        IngestApiReadConfigVO paging = config.getReadConfig() == null
                ? new IngestApiReadConfigVO() : config.getReadConfig();
        Set<String> fields = new LinkedHashSet<>();
        // 仅保存页摘要和已见游标，防止接口忽略分页参数或返回循环游标。
        Set<String> pageHashes = new HashSet<>();
        Set<String> cursors = new HashSet<>();
        Object cursor = paging.getInitialCursor();
        if (ObjectUtil.isNotNull(cursor) && !blank(cursor.toString())) {
            cursors.add(cursor.toString());
        }
        long position = paging.getStartPosition();
        long received = 0;
        for (int page = 1; page <= paging.getMaxPages(); page++) {
            if (context.reachedLimit()) {
                return new ArrayList<>(fields);
            }
            List<?> entries;
            Object next;
            boolean done;
            try {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ServiceException("API读取已中断");
                }
                Object response = request(config, parameters(config, paging, position, cursor));
                entries = records(response, paging.getRecordsPath(), page, context.isPreview());
                if (CollectionUtils.isNotEmpty(entries) && !PAGING_NONE.equals(paging.getPagingMode())
                        && !pageHashes.add(hash(entries))) {
                    throw new ServiceException("API返回重复页，第" + page + "页");
                }
                Boolean hasMore = hasMore(response, paging);
                Long total = total(response, paging);
                next = nextCursor(response, paging);
                received = Math.addExact(received, entries.size());
                done = finished(paging, entries.size(), received, hasMore, total, next);
                if (!done && PAGING_CURSOR.equals(paging.getPagingMode())
                        && (ObjectUtil.isNull(next) || !cursors.add(next.toString()))) {
                    throw new ServiceException("API游标未前进，第" + page + "页");
                }
                if (!done && PAGING_OFFSET.equals(paging.getPagingMode()) && entries.size() < paging.getPageSize()) {
                    throw new ServiceException("API偏移量分页返回不足一页但仍需继续读取，请核对接口实际页大小");
                }
            } catch (ServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                ServiceException failure = new ServiceException("API第" + page + "页拉取或解析失败，请检查请求与数据路径");
                failure.initCause(ex);
                throw failure;
            }
            emitPage(entries, page, context, consumer, fields);
            if (context.reachedLimit() || done) {
                return new ArrayList<>(fields);
            }
            if (PAGING_CURSOR.equals(paging.getPagingMode())) {
                cursor = next;
            } else {
                try {
                    position = Math.addExact(position, PAGING_OFFSET.equals(paging.getPagingMode())
                            ? paging.getPageSize() : 1);
                } catch (ArithmeticException ex) {
                    ServiceException failure = new ServiceException("API分页位置超出整数范围");
                    failure.initCause(ex);
                    throw failure;
                }
            }
        }
        throw new ServiceException("API达到最大页数仍未结束：" + paging.getMaxPages());
    }

    /**
     * 提取并校验整页对象数组，避免消费部分记录后才发现同页结构错误。
     *
     * @param response 已解析的 JSON 响应
     * @param path 记录数组路径
     * @param page 本次读取的页序号，从 1 开始
     * @return 通过结构校验的记录列表
     * @throws ServiceException 路径不存在、结果不是数组或数组元素不是对象时抛出
     */
    private List<?> records(Object response, String path, int page, boolean preview) {
        Object value = JSONPath.eval(response, path);
        if (!(value instanceof List)) {
            if ("$".equals(path)) {
                throw new ServiceException("接口响应不是数据数组，请填写响应数据路径", preview ? HttpStatus.WARN : HttpStatus.ERROR);
            }
            // 与页面输入保持一致，不向用户展示内部补齐的根节点前缀。
            String displayPath = path.startsWith("$.") ? path.substring(2) : path.substring(1);
            if (!JSONPath.contains(response, path)) {
                throw new ServiceException("响应数据路径 " + displayPath + " 不存在，请检查配置", preview ? HttpStatus.WARN : HttpStatus.ERROR);
            }
            throw new ServiceException("响应数据路径 " + displayPath + " 对应的数据不是数组，请检查配置", preview ? HttpStatus.WARN : HttpStatus.ERROR);
        }
        List<?> entries = (List<?>) value;
        for (Object entry : entries) {
            if (!(entry instanceof Map)) {
                throw new ServiceException("API记录必须是JSON对象，第" + page + "页");
            }
        }
        return entries;
    }

    /**
     * 同步消费已校验的一页记录，达到上限后停止；调用方异常原样上抛。
     *
     * @param entries 已校验的记录列表
     * @param page 本次读取的页序号
     * @param context 读取进度与条数限制
     * @param consumer 来源记录处理回调
     * @param fields 已成功回调记录的字段并集
     */
    @SuppressWarnings("unchecked")
    private void emitPage(List<?> entries, int page, IngestReadContext context,
                          Consumer<IngestRow> consumer, Set<String> fields) {
        int index = 0;
        for (Object entry : entries) {
            if (context.reachedLimit()) {
                break;
            }
            Map<String, Object> values = new LinkedHashMap<>((Map<String, Object>) entry);
            context.emit(values, "API第" + page + "页第" + (++index) + "条", consumer);
            fields.addAll(values.keySet());
        }
    }

    /**
     * 校验请求地址、方法、响应路径及分页参数，由任务保存和实际读取共用。
     *
     * @param config 待校验的 API 配置
     * @throws ServiceException 地址、请求方法、路径或分页规则无效时抛出
     */
    public static void validateConfig(IngestTaskApiConfigVO config) {
        if (ObjectUtil.isNull(config) || ObjectUtil.isNull(config.getUrl())
                || !Arrays.asList("GET", "POST").contains(config.getHttpMethod())) {
            throw new ServiceException("API配置不完整");
        }
        URI uri;
        try {
            uri = URI.create(config.getUrl());
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("API地址无效");
        }
        if (!Arrays.asList("http", "https").contains(uri.getScheme()) || ObjectUtil.isNull(uri.getHost())
                || ObjectUtil.isNotNull(uri.getUserInfo()) || ObjectUtil.isNotNull(uri.getFragment())) {
            throw new ServiceException("API地址无效");
        }
        IngestApiReadConfigVO read = config.getReadConfig();
        if (ObjectUtil.isNull(read)) {
            return;
        }
        if (!Arrays.asList(PAGING_NONE, PAGING_PAGE, PAGING_OFFSET, PAGING_CURSOR).contains(read.getPagingMode())
                || read.getPageSize() <= 0 || read.getPageSize() > 10000
                || read.getStartPosition() < 0 || read.getMaxPages() <= 0 || read.getMaxPages() > 100000) {
            throw new ServiceException("API分页类型、页大小、起始位置或最大页数无效");
        }
        if (PAGING_PAGE.equals(read.getPagingMode()) && read.getStartPosition() < 1) {
            throw new ServiceException("页码分页的起始页码必须大于或等于1");
        }
        validatePath(read.getRecordsPath(), true);
        validatePath(read.getHasMorePath(), false);
        validatePath(read.getTotalPath(), false);
        validatePath(read.getNextCursorPath(), false);
        if (!PAGING_NONE.equals(read.getPagingMode())) {
            if (blank(read.getPositionParam()) || blank(read.getPageSizeParam())
                    || read.getPositionParam().equals(read.getPageSizeParam())) {
                throw new ServiceException("API分页参数名称不能为空或相同");
            }
            if (PAGING_CURSOR.equals(read.getPagingMode()) && blank(read.getNextCursorPath())) {
                throw new ServiceException("游标分页必须配置下一游标路径");
            }
        }
    }

    /**
     * 限定为根节点、字段访问和数组下标，不接受过滤或脚本类路径表达式。
     *
     * @param path 待校验的简单 JSON 路径
     * @param required 是否必须配置
     * @throws ServiceException 必填路径为空或路径格式不支持时抛出
     */
    private static void validatePath(String path, boolean required) {
        if (blank(path)) {
            if (required) {
                throw new ServiceException("API记录路径不能为空");
            }
            return;
        }
        if (path.length() > 512 || !path.matches("\\$(\\.[\\p{L}_][\\p{L}\\p{N}_-]*|\\[[0-9]+\\])*")) {
            throw new ServiceException("API路径只支持根节点、对象字段和数组下标：" + path);
        }
    }

    /**
     * 判断字符串是否为空或仅含空白字符。
     *
     * @param value 待检查的字符串
     * @return 为空或空白时返回 {@code true}
     */
    private static boolean blank(String value) {
        return ObjectUtil.isNull(value) || value.trim().isEmpty();
    }

    /**
     * 复制固定请求参数后覆盖本页分页参数，避免修改原始任务配置。
     *
     * @param config API 请求配置
     * @param read 分页规则
     * @param position 当前页码或偏移量
     * @param cursor 当前游标，保持响应的字符串或数字类型
     * @return 本页独享的请求参数
     */
    private Map<String, Object> parameters(IngestTaskApiConfigVO config, IngestApiReadConfigVO read,
                                            long position, Object cursor) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(config.getParamConfig())) {
            params.putAll(config.getParamConfig());
        }
        if (!PAGING_NONE.equals(read.getPagingMode())) {
            params.put(read.getPageSizeParam(), read.getPageSize());
            if (PAGING_CURSOR.equals(read.getPagingMode())) {
                params.remove(read.getPositionParam());
                if (ObjectUtil.isNotNull(cursor) && !blank(cursor.toString())) {
                    params.put(read.getPositionParam(), cursor);
                }
            } else {
                params.put(read.getPositionParam(), position);
            }
        }
        return params;
    }

    /**
     * 执行单页请求并解析响应，无论成功或异常均断开该页 HTTP 连接。
     *
     * @param config API 请求配置
     * @param params 本页请求参数
     * @return 已解析的 JSON 响应
     * @throws IOException 连接、请求写入或响应读取失败时抛出
     * @throws ServiceException HTTP 状态码或响应大小不符合要求时抛出
     */
    private Object request(IngestTaskApiConfigVO config, Map<String, Object> params) throws IOException {
        IngestTaskApiConfigVO request = new IngestTaskApiConfigVO();
        request.setUrl(config.getUrl());
        request.setHttpMethod(config.getHttpMethod());
        request.setHeaderConfig(config.getHeaderConfig());
        request.setParamConfig(params);
        request.setReadConfig(config.getReadConfig());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(buildRequestUrl(request)).openConnection();
            configureConnection(connection, request);
            writeRequestBody(connection, request);
            return readResponse(connection);
        } finally {
            if (ObjectUtil.isNotNull(connection)) {
                connection.disconnect();
            }
        }
    }

    /**
     * 未配置时返回 null，配置后必须为布尔值，防止错误路径被当作结束标志。
     *
     * @param response 已解析的 JSON 响应
     * @param read 响应读取规则
     * @return 接口声明的后续页状态，未配置时为 {@code null}
     * @throws ServiceException 配置路径未返回布尔值时抛出
     */
    private Boolean hasMore(Object response, IngestApiReadConfigVO read) {
        if (blank(read.getHasMorePath())) {
            return null;
        }
        Object value = JSONPath.eval(response, read.getHasMorePath());
        if (!(value instanceof Boolean)) {
            throw new ServiceException("API hasMore路径必须返回布尔值");
        }
        return (Boolean) value;
    }

    /**
     * 总数仅接受非负整数，不截断小数或溢出值。
     *
     * @param response 已解析的 JSON 响应
     * @param read 响应读取规则
     * @return 总记录数，未配置时为 {@code null}；兼容可精确转换的数字字符串
     * @throws ServiceException 配置路径未返回有效的非负整数时抛出
     */
    private Long total(Object response, IngestApiReadConfigVO read) {
        if (blank(read.getTotalPath())) {
            return null;
        }
        Object value = JSONPath.eval(response, read.getTotalPath());
        try {
            long total = new java.math.BigDecimal(String.valueOf(value)).longValueExact();
            if (total < 0) {
                throw new ArithmeticException();
            }
            return total;
        } catch (RuntimeException ex) {
            throw new ServiceException("API总数路径必须返回非负整数");
        }
    }

    /**
     * 提取下一游标，保留原始类型，避免 POST 请求将数字游标转换为字符串。
     *
     * @param response 已解析的 JSON 响应
     * @param read 响应读取规则
     * @return 字符串或数字游标，未配置、缺失或空白时为 {@code null}
     * @throws ServiceException 游标类型不是字符串或数字时抛出
     */
    private Object nextCursor(Object response, IngestApiReadConfigVO read) {
        if (blank(read.getNextCursorPath())) {
            return null;
        }
        Object value = JSONPath.eval(response, read.getNextCursorPath());
        if (ObjectUtil.isNull(value)) {
            return null;
        }
        if (!(value instanceof String) && !(value instanceof Number)) {
            throw new ServiceException("API下一游标必须是字符串或数字");
        }
        return blank(value.toString()) ? null : value;
    }

    /**
     * 优先依据 hasMore 和总数判断结束，并拒绝两者矛盾的响应。
     * 游标分页结合下一游标判断；页码和偏移量仅在显式启用时以短页结束。
     *
     * @param read 分页规则
     * @param size 当前页记录数
     * @param received 本次请求累计取得的记录数，包含当前页
     * @param hasMore 接口声明的后续页状态，未配置时为空
     * @param total 接口声明的总记录数，未配置时为空
     * @param next 下一游标，缺失或空白时为空
     * @return 是否已读取结束
     * @throws ServiceException 分页结束标志相互矛盾，或仍有数据却返回空页、缺少游标时抛出
     */
    private boolean finished(IngestApiReadConfigVO read, int size, long received,
                               Boolean hasMore, Long total, Object next) {
        if (PAGING_NONE.equals(read.getPagingMode())) {
            return true;
        }
        // 从中间位置开始时，将起始位置之前的记录计入总数比较，避免多请求一页。
        long alreadySkipped = PAGING_OFFSET.equals(read.getPagingMode()) ? read.getStartPosition()
                : PAGING_PAGE.equals(read.getPagingMode())
                ? Math.multiplyExact(read.getStartPosition() - 1, read.getPageSize()) : 0;
        long consumed = Math.addExact(alreadySkipped, received);
        if (Boolean.FALSE.equals(hasMore)) {
            if (ObjectUtil.isNotNull(total) && consumed < total) {
                throw new ServiceException("API hasMore与总数矛盾");
            }
            return true;
        }
        if (ObjectUtil.isNotNull(total) && consumed >= total) {
            if (Boolean.TRUE.equals(hasMore)) {
                throw new ServiceException("API hasMore与总数矛盾");
            }
            return true;
        }
        if (size == 0) {
            if (Boolean.TRUE.equals(hasMore) || ObjectUtil.isNotNull(total)) {
                throw new ServiceException("API返回空页但仍声明有数据");
            }
            // 未配置 hasMore/总数时，空页仍可能携带下一游标，继续请求以免漏读。
            return !PAGING_CURSOR.equals(read.getPagingMode()) || ObjectUtil.isNull(next);
        }
        if (Boolean.TRUE.equals(hasMore)) {
            return false;
        }
        if (PAGING_CURSOR.equals(read.getPagingMode())) {
            if (ObjectUtil.isNull(next) && ObjectUtil.isNotNull(total)) {
                throw new ServiceException("API总数未读取完成但没有下一游标");
            }
            return ObjectUtil.isNull(next);
        }
        if (ObjectUtil.isNotNull(total)) {
            return false;
        }
        return read.isShortPageEnds() && size < read.getPageSize();
    }

    /**
     * 对页内记录计算摘要，用于在重复页回调前阻止重复消费。
     *
     * @param records 本页记录，按响应顺序序列化，不作业务主键去重
     * @return SHA-256 摘要的 Base64 表示
     * @throws NoSuchAlgorithmException 运行环境不支持 SHA-256 时抛出
     */
    private String hash(List<?> records) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(JSON.toJSONString(records).getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(digest);
    }

    /**
     * 构造请求地址，保留普通查询参数，GET 分页参数统一由读取配置生成。
     *
     * <p>URL 中普通参数保留原始编码；新增 GET 参数按 UTF-8 编码，值沿用字符串表示。
     * POST 的 URL 查询参数保持不变，请求参数在 JSON 请求体中发送。</p>
     *
     * @param config 已校验并追加本页分页参数的请求配置
     * @return 本页实际请求地址
     * @throws IOException 查询参数编解码失败时抛出
     * @throws ServiceException 请求地址无效时抛出
     */
    private String buildRequestUrl(IngestTaskApiConfigVO config) throws IOException {
        URI uri = URI.create(config.getUrl());
        if (!Arrays.asList("http", "https").contains(uri.getScheme())
                || ObjectUtil.isNull(uri.getHost()) || ObjectUtil.isNotNull(uri.getUserInfo())) {
            throw new ServiceException("API地址无效");
        }
        String requestUrl = config.getUrl();
        IngestApiReadConfigVO read = config.getReadConfig();
        if ("GET".equals(config.getHttpMethod()) && ObjectUtil.isNotNull(read)
                && !PAGING_NONE.equals(read.getPagingMode()) && ObjectUtil.isNotNull(uri.getRawQuery())) {
            // 移除 URL 自带的分页参数；其余参数保留原始编码，避免重复编码或语义变化。
            List<String> retained = new ArrayList<>();
            for (String parameter : uri.getRawQuery().split("&")) {
                String key = URLDecoder.decode(parameter.split("=", 2)[0], StandardCharsets.UTF_8.name());
                if (!key.equals(read.getPositionParam()) && !key.equals(read.getPageSizeParam())
                        && !parameter.isEmpty()) {
                    retained.add(parameter);
                }
            }
            requestUrl = requestUrl.substring(0, requestUrl.indexOf('?'));
            if (CollectionUtils.isNotEmpty(retained)) {
                requestUrl += "?" + String.join("&", retained);
            }
        }
        StringBuilder url = new StringBuilder(requestUrl);
        if ("GET".equals(config.getHttpMethod()) && CollectionUtils.isNotEmpty(config.getParamConfig())) {
            boolean hasQuery = requestUrl.contains("?");
            for (Map.Entry<String, Object> parameter : config.getParamConfig().entrySet()) {
                url.append(hasQuery ? '&' : '?')
                        .append(URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8.name()))
                        .append('=')
                        .append(URLEncoder.encode(String.valueOf(parameter.getValue()), StandardCharsets.UTF_8.name()));
                hasQuery = true;
            }
        }
        return url.toString();
    }

    /**
     * 设置超时和请求头，禁止自动重定向以避免请求配置被转发。
     *
     * @param connection 本页 HTTP 连接
     * @param config API 请求配置
     * @throws IOException 请求方法或连接配置失败时抛出
     */
    private void configureConnection(HttpURLConnection connection, IngestTaskApiConfigVO config) throws IOException {
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(config.getHttpMethod());
        connection.setRequestProperty("Accept", "application/json");
        if (CollectionUtils.isNotEmpty(config.getHeaderConfig())) {
            for (Map.Entry<String, String> header : config.getHeaderConfig().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
    }

    /**
     * POST 参数作为 UTF-8 JSON 请求体发送；未配置参数时发送空对象，GET 不写请求体。
     *
     * @param connection 本页 HTTP 连接
     * @param config API 请求配置
     * @throws IOException 请求体写入失败时抛出
     */
    private void writeRequestBody(HttpURLConnection connection, IngestTaskApiConfigVO config) throws IOException {
        if (!"POST".equals(config.getHttpMethod())) {
            return;
        }
        connection.setDoOutput(true);
        if (ObjectUtil.isNull(connection.getRequestProperty("Content-Type"))) {
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        }
        Map<String, Object> parameters = ObjectUtil.isNull(config.getParamConfig())
                ? Collections.emptyMap() : config.getParamConfig();
        try (OutputStream output = connection.getOutputStream()) {
            output.write(JSON.toJSONString(parameters).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 只接收成功状态码，并在解析前限制响应字节数。
     *
     * @param connection 本页 HTTP 连接
     * @return 按 UTF-8 解析的 JSON 响应
     * @throws IOException 响应状态或响应体读取失败时抛出
     * @throws ServiceException 状态码不是 2xx、响应超限或读取中断时抛出
     */
    private Object readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status < HttpURLConnection.HTTP_OK || status >= HttpURLConnection.HTTP_MULT_CHOICE) {
            throw new ServiceException("API返回HTTP状态码：" + status);
        }
        try (InputStream input = connection.getInputStream();
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ServiceException("API读取已中断");
                }
                if (bytes.size() + bytesRead > MAX_RESPONSE_BYTES) {
                    throw new ServiceException("API响应超过20MB，请缩小请求数据范围");
                }
                bytes.write(buffer, 0, bytesRead);
            }
            return JSON.parse(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        }
    }

}
