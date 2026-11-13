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
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.reader.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONPath;
import org.springframework.stereotype.Component;
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
 * API 读取策略，按响应路径提取记录，支持单次、页码、偏移量和游标读取。
 * 每页独立释放 HTTP 连接，校验分页状态后同步回调该页记录。
 *
 * @author chentangwen
 */
@Component
public class IngestApiReader implements IngestReader {
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 60000;
    private static final int MAX_RESPONSE_BYTES = 20 * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;

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
     */
    public List<String> read(IngestTaskApiConfigVO config, int limit, Consumer<Map<String, Object>> consumer) {
        IngestTaskSaveReqVO task = new IngestTaskSaveReqVO();
        task.setApiConfig(config);
        return read(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 按页拉取并消费记录，预览达到条数限制后不再请求下一页。
     * 单页响应仍完整读取并受大小上限约束；重复页、游标循环及分页状态矛盾均报错。
     *
     * @param context 本次读取配置与进度
     * @param consumer 带页内位置的来源记录回调
     * @return 已回调记录的字段并集
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> read(IngestReadContext context, Consumer<IngestRow> consumer) {
        IngestTaskApiConfigVO config = context.getTask().getApiConfig();
        validateConfig(config);
        IngestApiReadConfigVO paging = config.getReadConfig() == null
                ? new IngestApiReadConfigVO() : config.getReadConfig();
        Set<String> fields = new LinkedHashSet<>();
        // 仅保存页摘要和已见游标，防止接口忽略分页参数或返回循环游标。
        Set<String> pageHashes = new HashSet<>();
        Set<String> cursors = new HashSet<>();
        String cursor = paging.getInitialCursor();
        if (cursor != null) {
            cursors.add(cursor);
        }
        long position = paging.getStartPosition();
        long received = 0;
        for (int page = 1; page <= paging.getMaxPages(); page++) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ServiceException("API读取已中断");
                }
                Object response = request(config, parameters(config, paging, position, cursor));
                Object records = JSONPath.eval(response, paging.getRecordsPath());
                if (!(records instanceof List)) {
                    String recordsPath = paging.getRecordsPath();
                    if ("$".equals(recordsPath)) {
                        throw new ServiceException("接口响应不是数据数组，请填写响应数据路径");
                    }
                    String displayPath = recordsPath.startsWith("$.")
                            ? recordsPath.substring(2) : recordsPath.substring(1);
                    if (!JSONPath.contains(response, recordsPath)) {
                        throw new ServiceException("响应数据路径 " + displayPath + " 不存在，请检查配置");
                    }
                    throw new ServiceException("响应数据路径 " + displayPath + " 对应的数据不是数组，请检查配置");
                }
                List<?> entries = (List<?>) records;
                if (!entries.isEmpty() && !"NONE".equals(paging.getPagingMode())
                        && !pageHashes.add(hash(entries))) {
                    throw new ServiceException("API返回重复页，第" + page + "页");
                }
                // 先校验整页结构，避免半页写入后才发现响应结构错误。
                for (Object entry : entries) {
                    if (!(entry instanceof Map)) {
                        throw new ServiceException("API记录必须是JSON对象，第" + page + "页");
                    }
                }
                Boolean hasMore = hasMore(response, paging);
                Long total = total(response, paging);
                String next = nextCursor(response, paging);
                received += entries.size();
                boolean done = finished(paging, entries.size(), received, hasMore, total, next);
                if (!done && "CURSOR".equals(paging.getPagingMode())
                        && (next == null || !cursors.add(next))) {
                    throw new ServiceException("API游标未前进，第" + page + "页");
                }
                int index = 0;
                for (Object entry : entries) {
                    if (context.reachedLimit()) {
                        break;
                    }
                    Map<String, Object> values = new LinkedHashMap<>((Map<String, Object>) entry);
                    fields.addAll(values.keySet());
                    context.emit(values, "API第" + page + "页第" + (++index) + "条", consumer);
                }
                if (context.reachedLimit() || done) {
                    return new ArrayList<>(fields);
                }
                if ("CURSOR".equals(paging.getPagingMode())) {
                    cursor = next;
                } else {
                    position = Math.addExact(position, "OFFSET".equals(paging.getPagingMode())
                            ? paging.getPageSize() : 1);
                }
            } catch (ServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                ServiceException failure = new ServiceException("API第" + page + "页拉取或解析失败，请检查请求与数据路径");
                failure.initCause(ex);
                throw failure;
            }
        }
        throw new ServiceException("API达到最大页数仍未结束：" + paging.getMaxPages());
    }

    /**
     * 校验请求地址、方法、响应路径及分页参数，由任务保存和实际读取共用。
     *
     * @param config 待校验的 API 配置
     */
    public static void validateConfig(IngestTaskApiConfigVO config) {
        if (config == null || config.getUrl() == null
                || !Arrays.asList("GET", "POST").contains(config.getHttpMethod())) {
            throw new ServiceException("API配置不完整");
        }
        URI uri;
        try {
            uri = URI.create(config.getUrl());
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("API地址无效");
        }
        if (!Arrays.asList("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new ServiceException("API地址无效");
        }
        IngestApiReadConfigVO read = config.getReadConfig();
        if (read == null) {
            return;
        }
        if (!Arrays.asList("NONE", "PAGE", "OFFSET", "CURSOR").contains(read.getPagingMode())
                || read.getPageSize() <= 0 || read.getPageSize() > 10000
                || read.getStartPosition() < 0 || read.getMaxPages() <= 0 || read.getMaxPages() > 100000) {
            throw new ServiceException("API分页类型、页大小、起始位置或最大页数无效");
        }
        if ("PAGE".equals(read.getPagingMode()) && read.getStartPosition() < 1) {
            throw new ServiceException("页码分页的起始页码必须大于或等于1");
        }
        validatePath(read.getRecordsPath(), true);
        validatePath(read.getHasMorePath(), false);
        validatePath(read.getTotalPath(), false);
        validatePath(read.getNextCursorPath(), false);
        if (!"NONE".equals(read.getPagingMode())) {
            if (blank(read.getPositionParam()) || blank(read.getPageSizeParam())
                    || read.getPositionParam().equals(read.getPageSizeParam())) {
                throw new ServiceException("API分页参数名称不能为空或相同");
            }
            if ("CURSOR".equals(read.getPagingMode()) && blank(read.getNextCursorPath())) {
                throw new ServiceException("游标分页必须配置下一游标路径");
            }
        }
    }

    /**
     * 限定为根节点、字段访问和数组下标，不接受过滤或脚本类路径表达式。
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

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 复制固定请求参数后覆盖本页分页参数，避免修改原始任务配置。
     */
    private Map<String, Object> parameters(IngestTaskApiConfigVO config, IngestApiReadConfigVO read,
                                            long position, String cursor) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (config.getParamConfig() != null) {
            params.putAll(config.getParamConfig());
        }
        if (!"NONE".equals(read.getPagingMode())) {
            params.put(read.getPageSizeParam(), read.getPageSize());
            if ("CURSOR".equals(read.getPagingMode())) {
                params.remove(read.getPositionParam());
                if (!blank(cursor)) {
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
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 未配置时返回 null，配置后必须为布尔值，防止错误路径被当作结束标志。
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
     * 将字符串或数字游标统一为字符串，缺失或空白值表示没有下一游标。
     */
    private String nextCursor(Object response, IngestApiReadConfigVO read) {
        if (blank(read.getNextCursorPath())) {
            return null;
        }
        Object value = JSONPath.eval(response, read.getNextCursorPath());
        if (value == null) {
            return null;
        }
        if (!(value instanceof String) && !(value instanceof Number)) {
            throw new ServiceException("API下一游标必须是字符串或数字");
        }
        return blank(value.toString()) ? null : value.toString();
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
     */
    private boolean finished(IngestApiReadConfigVO read, int size, long received,
                              Boolean hasMore, Long total, String next) {
        if ("NONE".equals(read.getPagingMode())) {
            return true;
        }
        // 从中间位置开始时，将起始位置之前的记录计入总数比较，避免多请求一页。
        long alreadySkipped = "OFFSET".equals(read.getPagingMode()) ? read.getStartPosition()
                : "PAGE".equals(read.getPagingMode())
                ? Math.multiplyExact(read.getStartPosition() - 1, read.getPageSize()) : 0;
        long consumed = Math.addExact(alreadySkipped, received);
        if (Boolean.FALSE.equals(hasMore)) {
            if (total != null && consumed < total) {
                throw new ServiceException("API hasMore与总数矛盾");
            }
            return true;
        }
        if (total != null && consumed >= total) {
            if (Boolean.TRUE.equals(hasMore)) {
                throw new ServiceException("API hasMore与总数矛盾");
            }
            return true;
        }
        if (size == 0) {
            if (Boolean.TRUE.equals(hasMore) || (total != null && consumed < total)) {
                throw new ServiceException("API返回空页但仍声明有数据");
            }
            // 未配置 hasMore/总数时，空页携带下一游标，继续请求以免漏读。
            return !"CURSOR".equals(read.getPagingMode()) || next == null;
        }
        if (Boolean.TRUE.equals(hasMore)) {
            return false;
        }
        if ("CURSOR".equals(read.getPagingMode())) {
            if (next == null && total != null && consumed < total) {
                throw new ServiceException("API总数未读取完成但没有下一游标");
            }
            return next == null;
        }
        if (total != null) {
            return false;
        }
        return read.isShortPageEnds() && size < read.getPageSize();
    }

    /**
     * 对页内记录计算摘要，用于在重复页回调前阻止重复消费。
     */
    private String hash(List<?> records) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(JSON.toJSONString(records).getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(digest);
    }
    /**
     * 保留普通查询参数，分页参数统一由读取配置生成。
     */
    private String buildRequestUrl(IngestTaskApiConfigVO config) throws IOException {
        URI uri = URI.create(config.getUrl());
        if (!Arrays.asList("http", "https").contains(uri.getScheme())
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new ServiceException("API地址无效");
        }
        String requestUrl = config.getUrl();
        IngestApiReadConfigVO read = config.getReadConfig();
        if ("GET".equals(config.getHttpMethod()) && read != null
                && !"NONE".equals(read.getPagingMode()) && uri.getRawQuery() != null) {
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
            if (!retained.isEmpty()) {
                requestUrl += "?" + String.join("&", retained);
            }
        }
        StringBuilder url = new StringBuilder(requestUrl);
        if ("GET".equals(config.getHttpMethod()) && config.getParamConfig() != null) {
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
     */
    private void configureConnection(HttpURLConnection connection, IngestTaskApiConfigVO config) throws IOException {
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(config.getHttpMethod());
        connection.setRequestProperty("Accept", "application/json");
        if (config.getHeaderConfig() != null) {
            for (Map.Entry<String, String> header : config.getHeaderConfig().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
    }

    /**
     * POST 参数作为 JSON 请求体发送；未配置参数时发送空对象。
     */
    private void writeRequestBody(HttpURLConnection connection, IngestTaskApiConfigVO config) throws IOException {
        if (!"POST".equals(config.getHttpMethod())) {
            return;
        }
        connection.setDoOutput(true);
        if (connection.getRequestProperty("Content-Type") == null) {
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        }
        Map<String, Object> parameters = config.getParamConfig() == null
                ? Collections.emptyMap() : config.getParamConfig();
        try (OutputStream output = connection.getOutputStream()) {
            output.write(JSON.toJSONString(parameters).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 只接收成功状态码，并在解析前限制响应字节数。
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
                if (bytes.size() + bytesRead > MAX_RESPONSE_BYTES) {
                    throw new ServiceException("API响应超过20MB，请缩小请求数据范围");
                }
                bytes.write(buffer, 0, bytesRead);
            }
            return JSON.parse(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        }
    }

}
