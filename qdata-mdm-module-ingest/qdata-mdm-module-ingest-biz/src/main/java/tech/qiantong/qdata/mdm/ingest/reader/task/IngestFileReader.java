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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskFileConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.FILE;

/**
 * 接入任务的文件读取策略，供上传校验、来源预览和正式采集共用。
 *
 * <p>CSV 按 UTF-8 逐条解析，XLS/XLSX 通过事件回调读取首个工作表。
 * CSV 首条逻辑记录或 Excel 第一物理行作为表头，清除字段名首尾空白及首列 BOM，
 * 拒绝空字段名和重复字段名。
 * 数据按字符串读取，空单元格转为 {@code null}，非空值保留空格，全空记录跳过且不占读取上限。</p>
 *
 * <p>读取时保留物理行号；CSV 引号内换行统一为 LF，并以逻辑记录的起始物理行号定位。
 * 回调同步执行，字段转换、目标入库和失败策略由调用方负责，已完成回调不会因后续解析失败而回滚。</p>
 *
 * @author chentangwen
 */
@Component
public class IngestFileReader implements IngestReader {

    @Value("${qdata.profile:${user.dir}/upload}")
    private String profile;

    @Override
    public String getType() {
        return FILE;
    }

    /**
     * 兼容上传校验等只消费字段值的调用。
     *
     * @param config 文件配置
     * @param limit 最大回调条数，小于或等于 0 时不限制
     * @param consumer 来源字段值回调
     * @return 规范化后的文件表头
     * @throws ServiceException 配置、文件或解析格式无效时抛出
     */
    public List<String> read(IngestTaskFileConfigVO config, int limit, Consumer<Map<String, Object>> consumer) {
        if (ObjectUtil.isNull(consumer)) {
            throw new ServiceException("文件记录回调不能为空");
        }
        IngestTaskSaveReqVO task = new IngestTaskSaveReqVO();
        task.setFileConfig(config);
        return read(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 校验文件令牌与类型后选择解析方式，解析器负责关闭文件资源。
     *
     * <p>达到条数上限后不再解析后续记录，因此预览成功不代表整个文件均已通过校验。
     * 回调的运行时异常（含任务取消）保持原样抛出，避免误报为文件格式错误。</p>
     *
     * @param context 本次读取配置与进度
     * @param consumer 带文件位置的来源记录回调
     * @return 规范化后的文件表头
     * @throws ServiceException 入口配置、文件或解析格式无效，以及读取中断时抛出
     */
    @Override
    public List<String> read(IngestReadContext context, Consumer<IngestRow> consumer) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(consumer)) {
            throw new ServiceException("文件读取上下文和记录回调不能为空");
        }
        checkInterrupted();
        IngestTaskFileConfigVO config = context.getTask().getFileConfig();
        if (ObjectUtil.isNull(config) || ObjectUtil.isNull(config.getFileType())) {
            throw new ServiceException("缺少文件配置");
        }
        Path path = resolveFile(getConfiguredFileToken(config));
        String type = config.getFileType().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("CSV", "XLS", "XLSX").contains(type)
                || !path.getFileName().toString().endsWith("." + type.toLowerCase(Locale.ROOT))) {
            throw new ServiceException("上传文件与配置类型不一致");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ServiceException("来源文件不存在或不可读取，请重新上传");
        }
        // Excel 解析框架可能包装监听器异常，单独记录回调失败以保留其原始类型。
        AtomicReference<RuntimeException> callbackFailure = new AtomicReference<>();
        Consumer<IngestRow> callback = row -> {
            try {
                consumer.accept(row);
            } catch (RuntimeException ex) {
                callbackFailure.set(ex);
                throw ex;
            }
        };
        try {
            List<String> fields = "CSV".equals(type)
                    ? readCsv(path, context, callback) : readExcel(path, context, callback);
            if (ObjectUtil.isNotNull(callbackFailure.get())) {
                // 解析框架可能将特定异常视为正常停止，回调失败仍须向调用方报告。
                throw callbackFailure.get();
            }
            return fields;
        } catch (Exception ex) {
            RuntimeException original = callbackFailure.get();
            if (ObjectUtil.isNotNull(original)) {
                if (original != ex) {
                    for (Throwable suppressed : ex.getSuppressed()) {
                        if (suppressed != original) {
                            original.addSuppressed(suppressed);
                        }
                    }
                }
                throw original;
            }
            if (ex instanceof ServiceException) {
                throw (ServiceException) ex;
            }
            ServiceException failure = new ServiceException("文件无法解析，请检查文件、编码和表头配置");
            failure.initCause(ex);
            throw failure;
        }
    }

    /**
     * 按 UTF-8 读取 CSV；含换行的引号记录以起始物理行号定位，非空记录必须与表头等宽。
     *
     * @param path 已校验的文件路径
     * @param context 读取进度、所需字段和条数限制
     * @param consumer 来源记录回调
     * @return 规范化后的表头
     * @throws IOException 文件打开、字符解码、读取或关闭失败时抛出
     * @throws ServiceException 表头、记录结构无效或读取中断时抛出
     */
    private List<String> readCsv(Path path, IngestReadContext context,
                                 Consumer<IngestRow> consumer) throws IOException {
        try (CsvRecords records = new CsvRecords(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            List<String> header = records.next();
            if (ObjectUtil.isNull(header)) {
                throw new ServiceException("没有找到表头");
            }
            List<String> fields = normalizeHeaders(header);
            context.validateFields(fields);
            List<String> values;
            while (!context.reachedLimit() && ObjectUtil.isNotNull(values = records.next())) {
                String location = path.getFileName() + " 第" + records.startLine + "行";
                if (values.stream().allMatch(value -> value.trim().isEmpty())) {
                    context.skip();
                    continue;
                }
                if (values.size() != fields.size()) {
                    throw new ServiceException(location + "字段数量与表头不一致");
                }
                emit(fields, values, location, context, consumer);
            }
            return fields;
        }
    }

    /**
     * 事件式读取首个工作表，缺失单元格补空值，超出表头范围的非空单元格报错。
     *
     * @param path 已校验的 Excel 文件路径
     * @param context 读取进度、所需字段和条数限制
     * @param consumer 来源记录回调
     * @return 规范化后的表头
     * @throws ServiceException 表头、记录结构无效或读取中断时抛出
     */
    private List<String> readExcel(Path path, IngestReadContext context, Consumer<IngestRow> consumer) {
        List<String> fields = new ArrayList<>();
        EasyExcel.read(path.toFile(), new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invokeHeadMap(Map<Integer, String> head, AnalysisContext analysis) {
                checkInterrupted();
                if (analysis.readRowHolder().getRowIndex() != 0) {
                    throw new ServiceException("第一行表头不存在");
                }
                int size = head.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
                List<String> names = new ArrayList<>();
                for (int index = 0; index < size; index++) {
                    names.add(head.get(index));
                }
                fields.addAll(normalizeHeaders(names));
                context.validateFields(fields);
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext analysis) {
                checkInterrupted();
                if (context.reachedLimit()) {
                    return;
                }
                if (CollectionUtils.isEmpty(fields)) {
                    throw new ServiceException("表头不存在");
                }
                String location = path.getFileName() + " " + analysis.readSheetHolder().getSheetName()
                        + " 第" + (analysis.readRowHolder().getRowIndex() + 1) + "行";
                if (data.values().stream().allMatch(value -> ObjectUtil.isNull(value) || value.trim().isEmpty())) {
                    context.skip();
                    return;
                }
                if (data.entrySet().stream().anyMatch(entry -> entry.getKey() >= fields.size()
                        && ObjectUtil.isNotNull(entry.getValue()) && !entry.getValue().trim().isEmpty())) {
                    throw new ServiceException(location + "字段数量与表头不一致");
                }
                List<String> values = new ArrayList<>();
                for (int index = 0; index < fields.size(); index++) {
                    values.add(data.get(index));
                }
                emit(fields, values, location, context, consumer);
            }

            @Override
            public boolean hasNext(AnalysisContext analysis) {
                checkInterrupted();
                return !context.reachedLimit();
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysis) {
                // 记录已在事件回调中处理，无需在工作表结束时额外汇总。
            }

            @Override
            public void onException(Exception exception, AnalysisContext analysis) throws Exception {
                throw exception;
            }
        }).headRowNumber(1).ignoreEmptyRow(false).autoTrim(false).sheet(0).doRead();
        if (CollectionUtils.isEmpty(fields)) {
            throw new ServiceException("第一行表头不存在");
        }
        return fields;
    }

    /**
     * 按表头组装记录，空单元格转为 null，非空字符串保留原始空格。
     *
     * @param fields 规范化后的表头
     * @param values 与表头等宽的单元格值
     * @param location 文件及物理行号描述
     * @param context 读取进度
     * @param consumer 来源记录回调，异常原样交由读取入口处理
     */
    private void emit(List<String> fields, List<String> values, String location,
                      IngestReadContext context, Consumer<IngestRow> consumer) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < fields.size(); index++) {
            String value = values.get(index);
            row.put(fields.get(index), ObjectUtil.isEmpty(value) ? null : value);
        }
        context.emit(row, location, consumer);
    }

    /**
     * 清理首列表头的 BOM 和字段名前后空白，拒绝空表头或重复字段名。
     *
     * @param names 原始字段名，缺失单元格允许为 {@code null}
     * @return 保持原有列顺序的规范字段名
     * @throws ServiceException 字段名为空或重复时抛出
     */
    private List<String> normalizeHeaders(List<String> names) {
        List<String> fields = new ArrayList<>();
        for (int index = 0; index < names.size(); index++) {
            String name = ObjectUtil.isNull(names.get(index)) ? "" : names.get(index);
            if (index == 0 && name.startsWith("\uFEFF")) {
                name = name.substring(1);
            }
            fields.add(name.trim());
        }
        if (CollectionUtils.isEmpty(fields) || fields.stream().anyMatch(String::isEmpty)
                || new HashSet<>(fields).size() != fields.size()) {
            throw new ServiceException("表头不能为空或重复");
        }
        return fields;
    }

    /**
     * 统一任务保存、预览和执行的文件定位规则，优先使用 filePath，兼容仅传入 fileToken 的配置。
     *
     * @param config 文件配置，允许为 {@code null}
     * @return 待校验的上传令牌；未配置时为 {@code null}
     */
    public static String getConfiguredFileToken(IngestTaskFileConfigVO config) {
        if (ObjectUtil.isNull(config)) {
            return null;
        }
        if (ObjectUtil.isNotNull(config.getFilePath()) && !config.getFilePath().trim().isEmpty()) {
            return config.getFilePath();
        }
        return config.getFileToken();
    }

    /**
     * 检查当前读取线程是否被中断，不清除中断标志。
     *
     * @throws ServiceException 读取线程已被中断时抛出
     */
    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ServiceException("文件读取已中断");
        }
    }

    /**
     * 将上传生成的文件令牌解析为接入目录内的路径，不接受任意客户端路径。
     *
     * @param token 32 位小写十六进制标识加受支持的文件后缀
     * @return 接入文件的绝对路径
     * @throws ServiceException 令牌格式无效或路径超出接入目录时抛出
     */
    public Path resolveFile(String token) {
        if (ObjectUtil.isNull(token) || !token.matches("[a-f0-9]{32}\\.(csv|xls|xlsx)")) {
            throw new ServiceException("文件标识无效");
        }
        Path root = Paths.get(profile).toAbsolutePath().normalize().resolve("ingest");
        Path path = root.resolve(token).normalize();
        if (!path.startsWith(root)) {
            throw new ServiceException("文件标识无效");
        }
        return path;
    }

    /**
     * 保留物理行号并严格校验引号的 CSV 解析器，解析期间仅缓存一条逻辑记录。
     *
     * <p>一条记录最多 4096 列、10 × 1024 × 1024 个 UTF-16 字符单元。
     * 长度包括引号、分隔符、CRLF 及行结束符，不包括文件开头的 BOM。</p>
     */
    private static class CsvRecords implements AutoCloseable {
        private static final int MAX_RECORD_CHARS = 10 * 1024 * 1024;
        private static final int MAX_COLUMNS = 4096;
        private final PushbackReader reader;
        private long line = 1;
        private long startLine;

        /**
         * 接管字符流的关闭责任，允许回退一个用于识别 CRLF 的字符。
         *
         * @param reader UTF-8 解码后的文件字符流
         */
        private CsvRecords(BufferedReader reader) {
            this.reader = new PushbackReader(reader, 1);
        }

        /**
         * 解析一条逻辑记录，支持引号内换行和双引号转义，文件结束时返回 null。
         *
         * @return 一条逻辑记录的原始单元格值，无剩余内容时为 {@code null}
         * @throws IOException 字符读取或回退失败时抛出
         * @throws ServiceException 引号格式错误、记录超限或读取中断时抛出
         */
        private List<String> next() throws IOException {
            checkInterrupted();
            startLine = line;
            List<String> fields = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean closed = false;
            boolean seen = false;
            int length = 0;
            int remainingUntilInterruptCheck = 1024;
            int ch;
            while ((ch = reader.read()) != -1) {
                if (!seen && line == 1 && ch == '\uFEFF') {
                    continue;
                }
                seen = true;
                length = checkLength(length + 1);
                if (--remainingUntilInterruptCheck == 0) {
                    checkInterrupted();
                    remainingUntilInterruptCheck = 1024;
                }
                if (ch == '\r') {
                    // CRLF 和单独 CR 均计为一次物理换行，统一为 LF 交给后续状态处理。
                    int next = reader.read();
                    if (next == '\n') {
                        length = checkLength(length + 1);
                    } else if (next != -1) {
                        reader.unread(next);
                    }
                    ch = '\n';
                }
                if (ch == '\n') {
                    line++;
                    if (quoted) {
                        field.append('\n');
                        continue;
                    }
                    addField(fields, field);
                    return fields;
                }
                if (quoted) {
                    if (ch == '"') {
                        quoted = false;
                        closed = true;
                    } else {
                        field.append((char) ch);
                    }
                } else if (closed && ch == '"') {
                    // 闭合引号紧接另一个引号表示转义，引号本身也计入记录长度。
                    field.append('"');
                    quoted = true;
                    closed = false;
                } else if (ch == ',') {
                    addField(fields, field);
                    field.setLength(0);
                    closed = false;
                } else if (ch == '"' && field.length() == 0 && !closed) {
                    quoted = true;
                } else {
                    if (closed || ch == '"') {
                        throw error("双引号或分隔符格式错误");
                    }
                    field.append((char) ch);
                }
            }
            if (quoted) {
                throw error("双引号未闭合");
            }
            if (!seen) {
                return null;
            }
            addField(fields, field);
            return fields;
        }

        /**
         * 在分隔符、换行和文件结束三个出口统一检查列数。
         *
         * @param fields 已解析的单元格
         * @param field 本次准备追加的单元格
         * @throws ServiceException 追加后超出列数上限时抛出
         */
        private void addField(List<String> fields, StringBuilder field) {
            if (fields.size() >= MAX_COLUMNS) {
                throw error("字段数量超过4096");
            }
            fields.add(field.toString());
        }

        /**
         * 校验已消费字符数，避免转义引号或 CRLF 的额外字符绕过上限。
         *
         * @param length 包括语法字符的记录长度
         * @return 未超限的记录长度
         * @throws ServiceException 记录超过字符数上限时抛出
         */
        private int checkLength(int length) {
            if (length > MAX_RECORD_CHARS) {
                throw error("记录超过10M字符");
            }
            return length;
        }

        /**
         * 构造包含逻辑记录起始物理行号的解析错误。
         *
         * @param reason 不包含来源数据内容的错误原因
         * @return 可定位到来源行的业务异常
         */
        private ServiceException error(String reason) {
            return new ServiceException("CSV第" + startLine + "行：" + reason);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
