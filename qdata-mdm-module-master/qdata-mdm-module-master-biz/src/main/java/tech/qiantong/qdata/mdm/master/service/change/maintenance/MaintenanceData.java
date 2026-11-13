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

package tech.qiantong.qdata.mdm.master.service.change.maintenance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 申请快照解析和 JDBC 值转换；执行时不重新生成 ID、默认值或编码。 */
public final class MaintenanceData {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern IMPORT_DATE = Pattern.compile(
            "^(\\d{4})([-/.])(\\d{1,2})\\2(\\d{1,2})(?:[T ](\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?$");
    private static final DateTimeFormatter IMPORT_DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private MaintenanceData() { }

    public static JsonNode payload(String json) {
        try {
            JsonNode node = JSON.readTree(json);
            if (node == null || !node.isObject()) throw new IllegalArgumentException();
            return node;
        } catch (Exception ex) {
            throw new ServiceException("申请变更内容格式无效");
        }
    }

    public static Map<String, String> values(String json) {
        JsonNode node = payload(json);
        JsonNode values = node.has("values") ? node.get("values") : node;
        if (!values.isObject()) throw new ServiceException("申请字段内容格式无效");
        try {
            return JSON.convertValue(values, new TypeReference<LinkedHashMap<String, String>>() { });
        } catch (Exception ex) {
            throw new ServiceException("申请字段内容格式无效");
        }
    }

    public static Map<String, String> snapshot(String json) {
        return json == null || json.trim().isEmpty() ? new LinkedHashMap<>() : values(json);
    }

    public static Map<String, Object> resultSnapshot(String json) {
        try {
            return JSON.convertValue(payload(json), new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (Exception ex) {
            throw new ServiceException("执行结果快照格式无效");
        }
    }

    public static Map<String, Object> beforeSnapshot(Map<String, ?> main, String proposedData) {
        Map<String, Object> result = new LinkedHashMap<>(main);
        JsonNode children = payload(proposedData).get("submodelChanges");
        if (children == null || !children.isArray() || children.isEmpty()) return result;
        List<Map<String, Object>> records = new ArrayList<>();
        for (JsonNode child : children) {
            if ("CREATE".equals(child.path("operation").asText())) continue;
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("submodelId", child.path("submodelId").asText());
            record.put("recordId", child.path("recordId").asText());
            record.put("values", snapshot(child.path("beforeData").toString()));
            records.add(record);
        }
        result.put("submodelChanges", records);
        return result;
    }

    public static Long targetRecordId(String json) {
        try {
            JsonNode target = payload(json).get("targetRecordId");
            if (target == null || target.isNull() || target.asText().trim().isEmpty()) return null;
            String value = target.asText();
            if (!value.matches("[1-9]\\d{0,18}")) throw new IllegalArgumentException();
            return Long.valueOf(value);
        } catch (Exception ex) {
            throw new ServiceException("历史新增申请的预分配主数据ID无效");
        }
    }

    public static Object convert(String value, String dataType) {
        if (value == null || value.trim().isEmpty()) return null;
        String text = value.trim();
        String type = dataType == null ? "STRING" : dataType.toUpperCase(Locale.ROOT);
        try {
            if (type.matches("BIGINT|INTEGER|SMALLINT|INT|TINYINT")) return Long.valueOf(text);
            if (type.matches("DECIMAL|NUMBER|NUMERIC|FLOAT|DOUBLE|REAL")) return new BigDecimal(text);
            if ("DATE".equals(type)) return java.sql.Date.valueOf(LocalDate.parse(text));
            if ("DATETIME".equals(type) || "TIMESTAMP".equals(type))
                return Timestamp.valueOf(text.replace('T', ' '));
            if ("BOOLEAN".equals(type) || "BOOL".equals(type)) {
                if ("true".equalsIgnoreCase(text) || "1".equals(text)) return true;
                if ("false".equalsIgnoreCase(text) || "0".equals(text)) return false;
                throw new IllegalArgumentException();
            }
            return value;
        } catch (Exception ex) {
            throw new ServiceException("字段值不符合模型类型：" + dataType);
        }
    }

    /** 仅在导入时归一化明确的年月日；无效值保留原样，继续交给严格校验。 */
    public static String normalizeImportDate(String dataType, String value) {
        if (value == null || dataType == null) return value;
        String type = dataType.trim().toUpperCase(Locale.ROOT);
        boolean dateOnly = "DATE".equals(type);
        if (!dateOnly && !"DATETIME".equals(type) && !"TIMESTAMP".equals(type)) return value;
        Matcher match = IMPORT_DATE.matcher(value.trim());
        if (!match.matches() || (dateOnly && match.group(5) != null)) return value;
        try {
            int year = Integer.parseInt(match.group(1));
            if (year == 0) return value;
            LocalDate date = LocalDate.of(year, Integer.parseInt(match.group(3)), Integer.parseInt(match.group(4)));
            if (dateOnly) return date.toString();
            // 日期时间字段只填写日期时按当天零点导入，不使用当前时间。
            LocalTime time = match.group(5) == null ? LocalTime.MIDNIGHT : LocalTime.of(
                    Integer.parseInt(match.group(5)), Integer.parseInt(match.group(6)),
                    match.group(7) == null ? 0 : Integer.parseInt(match.group(7)));
            return date.atTime(time).format(IMPORT_DATE_TIME);
        } catch (DateTimeException | NumberFormatException ex) {
            return value;
        }
    }

    public static String table(MasterModelRespDTO model) {
        return MasterPublishedModelUtils.qualifiedTable(model.getSchemaName(), model.getPhysicalTableName());
    }

    public static String json(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (Exception ex) { throw new ServiceException("生成主数据快照失败"); }
    }

    public static Object value(Map<String, Object> row, String key) {
        if (row == null || key == null) return null;
        for (Map.Entry<String, Object> entry : row.entrySet())
            if (key.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        return null;
    }

    public static String text(Object value) { return value == null ? null : String.valueOf(value); }

    public static String fieldText(Object value, String dataType) {
        if (value == null) {
            return null;
        }
        boolean date = "DATE".equalsIgnoreCase(dataType);
        if (value instanceof java.util.Date) {
            return new SimpleDateFormat(date ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss")
                    .format((java.util.Date) value);
        }
        if (value instanceof LocalDateTime) {
            LocalDateTime time = (LocalDateTime) value;
            return date ? time.toLocalDate().toString()
                    : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (date && value instanceof CharSequence) {
            // 公共查询组件可能已将 JDBC 日期格式化为时间字符串；快照仍按模型 DATE 保存。
            String source = value.toString();
            try {
                return LocalDateTime.parse(source.replace(' ', 'T')).toLocalDate().toString();
            } catch (DateTimeException ex) {
                // 纯日期或无效内容保持原样，不能通过截断掩盖格式错误。
                return source;
            }
        }
        return text(value);
    }

    public static boolean sameValue(String left, String right, String dataType) {
        Object first = convert(left, dataType);
        Object second = convert(right, dataType);
        if (first instanceof BigDecimal && second instanceof BigDecimal) {
            return ((BigDecimal) first).compareTo((BigDecimal) second) == 0;
        }
        return Objects.equals(first, second);
    }
}
