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

package tech.qiantong.qdata.mdm.ingest.utils.task;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.mapping.FieldDates;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * 接入任务的来源标识及业务值规范化工具，不访问数据库。
 *
 * @author chentangwen
 */
public final class IngestTaskExecutionUtils {

    private IngestTaskExecutionUtils() {
    }

    /**
     * 单字段直接使用原值，多字段按字段名排序后转义拼接。
     * 字符串保留大小写、空格和前导零，不截断超长标识。
     *
     * @param uniqueValues 按配置顺序排列的唯一字段和值
     * @return 最多 64 字符的来源记录标识
     */
    public static String stableRecordId(Map<String, Object> uniqueValues) {
        if (uniqueValues == null || uniqueValues.isEmpty()) {
            throw new ServiceException("请先配置来源记录唯一标识后再执行");
        }
        StringJoiner parts = new StringJoiner("|");
        for (Object value : new TreeMap<>(uniqueValues).values()) {
            String text = identityText(value);
            if (text.trim().isEmpty()) {
                throw new ServiceException("来源记录唯一标识字段值不能为空");
            }
            parts.add(uniqueValues.size() == 1 ? text : text.replace("\\", "\\\\").replace("|", "\\|"));
        }
        String recordId = parts.toString();
        if (recordId.codePointCount(0, recordId.length()) > 64) {
            throw new ServiceException("来源记录唯一标识超过 source_record_id 的 64 字符限制");
        }
        return recordId;
    }

    /**
     * 保留映射阶段转换后的业务值，未映射字段按 null 写入。
     * 动态表写入时按 JDBC 元数据还原日期类型。
     */
    public static Map<String, Object> normalizeModelData(Map<String, Object> values,
                                                       List<IngestModelFieldRespVO> fields) {
        Map<String, Object> normalized = new TreeMap<>();
        for (IngestModelFieldRespVO field : fields) {
            Object value = values.get(field.getColumnCode());
            if (value != null) {
                String type = field.getDataType() == null ? "" : field.getDataType().toUpperCase(Locale.ROOT);
                if ("DATE".equals(type)) {
                    value = dateTime(value).toLocalDate().toString();
                } else if ("DATETIME".equals(type) || "TIMESTAMP".equals(type)) {
                    value = dateTime(value).toLocalDateTime().toString();
                }
            }
            normalized.put(field.getColumnCode(), value);
        }
        return normalized;
    }

    private static String identityText(Object value) {
        if (value == null) throw new ServiceException("来源记录唯一标识字段值不能为空");
        if (value instanceof Number) return normalizedNumber((Number) value).toPlainString();
        if (value instanceof Boolean || value instanceof String || value instanceof Character) return value.toString();
        if (value instanceof java.sql.Date || value instanceof LocalDate) {
            return dateTime(value).toLocalDate().toString();
        }
        if (value instanceof Date || value instanceof java.time.temporal.TemporalAccessor) {
            return dateTime(value).toInstant().toString();
        }
        throw new ServiceException("来源标识包含不支持的值类型");
    }

    private static BigDecimal normalizedNumber(Number value) {
        try {
            BigDecimal number = new BigDecimal(value.toString()).stripTrailingZeros();
            return number.signum() == 0 ? BigDecimal.ZERO : number;
        } catch (NumberFormatException ex) {
            throw new ServiceException("来源数据包含非有限数值");
        }
    }

    /**
     * 日期规范化采用固定业务时区，兼容 JSON 中的 ISO 日期文本及 JDBC 日期对象。
     */
    private static ZonedDateTime dateTime(Object value) {
        if (value instanceof String) {
            String text = ((String) value).replace(' ', 'T');
            try {
                if (text.length() == 10) return LocalDate.parse(text).atStartOfDay(FieldDates.ZONE);
                try {
                    return OffsetDateTime.parse(text).atZoneSameInstant(FieldDates.ZONE);
                } catch (java.time.format.DateTimeParseException ignored) {
                    return LocalDateTime.parse(text).atZone(FieldDates.ZONE);
                }
            } catch (DateTimeException ex) {
                throw new ServiceException("模型日期格式不正确");
            }
        }
        return FieldDates.parse(value, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
    }

}
