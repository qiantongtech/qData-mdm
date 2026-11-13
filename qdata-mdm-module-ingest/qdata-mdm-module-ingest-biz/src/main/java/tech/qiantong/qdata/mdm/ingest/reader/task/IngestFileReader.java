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
import java.util.function.Consumer;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.FILE;

/**
 * 文件读取策略：CSV 逐条解析，XLS/XLSX 通过事件回调读取首个工作表。
 * 首行作为表头，跳过全空记录并保留物理行号，便于定位来源数据错误。
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
     */
    public List<String> read(IngestTaskFileConfigVO config, int limit, Consumer<Map<String, Object>> consumer) {
        IngestTaskSaveReqVO task = new IngestTaskSaveReqVO();
        task.setFileConfig(config);
        return read(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 校验文件令牌与类型后选择解析方式，解析器负责关闭文件资源。
     *
     * @param context 本次读取配置与进度
     * @param consumer 带文件位置的来源记录回调
     * @return 规范化后的文件表头
     */
    @Override
    public List<String> read(IngestReadContext context, Consumer<IngestRow> consumer) {
        IngestTaskFileConfigVO config = context.getTask().getFileConfig();
        if (config == null || config.getFileType() == null) {
            throw new ServiceException("缺少文件配置");
        }
        String token = config.getFileToken() == null || config.getFileToken().trim().isEmpty()
                ? config.getFilePath() : config.getFileToken();
        Path path = resolveFile(token);
        String type = config.getFileType().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("CSV", "XLS", "XLSX").contains(type)
                || !path.getFileName().toString().endsWith("." + type.toLowerCase(Locale.ROOT))) {
            throw new ServiceException("上传文件与配置类型不一致");
        }
        try {
            return "CSV".equals(type) ? readCsv(path, context, consumer) : readExcel(path, context, consumer);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("文件无法解析，请检查文件、编码和表头配置");
            failure.initCause(ex);
            throw failure;
        }
    }

    /**
     * 按 UTF-8 读取 CSV；含换行的引号记录以起始物理行号定位，非空记录必须与表头等宽。
     */
    private List<String> readCsv(Path path, IngestReadContext context, Consumer<IngestRow> consumer) throws IOException {
        try (CsvRecords records = new CsvRecords(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            List<String> header = records.next();
            if (header == null) {
                throw new ServiceException("没有找到表头");
            }
            List<String> fields = normalizeHeaders(header);
            context.validateFields(fields);
            List<String> values;
            while (!context.reachedLimit() && (values = records.next()) != null) {
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
     */
    private List<String> readExcel(Path path, IngestReadContext context, Consumer<IngestRow> consumer) {
        List<String> fields = new ArrayList<>();
        EasyExcel.read(path.toFile(), new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invokeHeadMap(Map<Integer, String> head, AnalysisContext analysis) {
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
                if (fields.isEmpty()) {
                    throw new ServiceException("表头不存在");
                }
                String location = path.getFileName() + " " + analysis.readSheetHolder().getSheetName()
                        + " 第" + (analysis.readRowHolder().getRowIndex() + 1) + "行";
                if (data.values().stream().allMatch(value -> value == null || value.trim().isEmpty())) {
                    context.skip();
                    return;
                }
                if (data.entrySet().stream().anyMatch(entry -> entry.getKey() >= fields.size()
                        && entry.getValue() != null && !entry.getValue().trim().isEmpty())) {
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
                return !context.reachedLimit();
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysis) {
            }

            @Override
            public void onException(Exception exception, AnalysisContext analysis) throws Exception {
                throw exception;
            }
        }).headRowNumber(1).ignoreEmptyRow(false).autoTrim(false).sheet(0).doRead();
        if (fields.isEmpty()) {
            throw new ServiceException("第一行表头不存在");
        }
        return fields;
    }

    /**
     * 按表头组装记录，空单元格转为 null，非空字符串保留原始空格。
     */
    private void emit(List<String> fields, List<String> values, String location,
                      IngestReadContext context, Consumer<IngestRow> consumer) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < fields.size(); index++) {
            String value = values.get(index);
            row.put(fields.get(index), value == null || value.isEmpty() ? null : value);
        }
        context.emit(row, location, consumer);
    }

    /**
     * 清理首列表头的 BOM 和字段名前后空白，拒绝空表头或重复字段名。
     */
    private List<String> normalizeHeaders(List<String> names) {
        List<String> fields = new ArrayList<>();
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index) == null ? "" : names.get(index);
            if (index == 0 && name.startsWith("\uFEFF")) {
                name = name.substring(1);
            }
            fields.add(name.trim());
        }
        if (fields.isEmpty() || fields.stream().anyMatch(String::isEmpty)
                || new HashSet<>(fields).size() != fields.size()) {
            throw new ServiceException("表头不能为空或重复");
        }
        return fields;
    }

    /**
     * 将上传生成的文件令牌解析为接入目录内的路径，不接受任意客户端路径。
     *
     * @param token 32 位小写十六进制标识加受支持的文件后缀
     * @return 接入文件的绝对路径
     */
    public Path resolveFile(String token) {
        if (token == null || !token.matches("[a-f0-9]{32}\\.(csv|xls|xlsx)")) {
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
     * 保留物理行号并严格校验引号，内存最多持有一条 CSV 记录。
     */
    private static class CsvRecords implements AutoCloseable {
        private static final int MAX_RECORD_CHARS = 10 * 1024 * 1024;
        private final PushbackReader reader;
        private long line = 1;
        private long startLine;
        private CsvRecords(BufferedReader reader) {
            this.reader = new PushbackReader(reader, 1);
        }

        /**
         * 解析一条逻辑记录，支持引号内换行和双引号转义，文件结束时返回 null。
         */
        private List<String> next() throws IOException {
            startLine = line;
            List<String> fields = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean closed = false;
            boolean seen = false;
            int length = 0;
            int ch;
            while ((ch = reader.read()) != -1) {
                if (!seen && line == 1 && ch == '\uFEFF') {
                    continue;
                }
                seen = true;
                if (++length > MAX_RECORD_CHARS) {
                    throw error("记录超过10MB");
                }
                if (ch == '\r') {
                    // CRLF 和单独 CR 均计为一次物理换行，统一为 LF 交给后续状态处理。
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
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
                    fields.add(field.toString());
                    return fields;
                }
                if (quoted) {
                    if (ch == '"') {
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                        } else {
                            quoted = false;
                            closed = true;
                            if (next != -1) {
                                reader.unread(next);
                            }
                        }
                    } else {
                        field.append((char) ch);
                    }
                } else if (ch == ',') {
                    fields.add(field.toString());
                    if (fields.size() > 4096) {
                        throw error("字段数量超过4096");
                    }
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
            fields.add(field.toString());
            return fields;
        }

        private ServiceException error(String reason) {
            return new ServiceException("CSV第" + startLine + "行：" + reason);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
