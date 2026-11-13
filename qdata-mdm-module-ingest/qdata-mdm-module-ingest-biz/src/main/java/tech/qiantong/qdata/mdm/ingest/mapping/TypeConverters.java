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

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * 目标约束只取自模型，转换失败不回显原始字段值。
 */
public final class TypeConverters {
    private TypeConverters() { }

    public static TypeConverter compile(IngestModelFieldRespVO field, Map<String, Object> config) {
        String type = field.getDataType() == null ? "" : field.getDataType().toUpperCase(Locale.ROOT);
        Integer length = field.getDataLength();
        Integer scale = field.getDataScale();
        TypeConverter converter;
        switch (type) {
            case "STRING": case "TEXT": case "VARCHAR": case "VARCHAR2": case "CHAR":
                converter = value -> {
                    String text = scalarText(value);
                    if (length != null && length > 0 && text.codePointCount(0, text.length()) > length)
                        throw new ServiceException("超过模型字段长度");
                    return text;
                };
                break;
            case "INTEGER": case "LONG": case "BIGINT": case "INT":
                converter = value -> {
                    if (value instanceof String && !((String) value).matches("[+-]?[0-9]+"))
                        throw new ServiceException("必须为完整整数格式");
                    return decimal(value).longValueExact();
                };
                break;
            case "DECIMAL": case "NUMBER": case "NUMERIC":
                if (length != null && (length <= 0 || scale == null || scale < 0 || scale > length))
                    throw new ServiceException("模型小数精度配置不正确");
                converter = value -> {
                    BigDecimal number = decimal(value);
                    if (scale != null) number = number.setScale(scale, RoundingMode.UNNECESSARY);
                    if (length != null && number.precision() > length)
                        throw new ServiceException("超过模型小数精度");
                    return number;
                };
                break;
            case "BOOLEAN": case "BOOL":
                converter = value -> {
                    if (value instanceof Boolean) return value;
                    if (value instanceof Number) {
                        BigDecimal number = decimal(value);
                        if (number.compareTo(BigDecimal.ONE) == 0) return true;
                        if (number.compareTo(BigDecimal.ZERO) == 0) return false;
                    } else if (value instanceof String) {
                        String text = (String) value;
                        if ("true".equalsIgnoreCase(text) || "1".equals(text)) return true;
                        if ("false".equalsIgnoreCase(text) || "0".equals(text)) return false;
                    }
                    throw new ServiceException("布尔值仅支持 true、false、1、0");
                };
                break;
            case "DATE": case "DATETIME": case "TIMESTAMP":
                Object pattern = config.get("inputFormat");
                DateTimeFormatter input = FieldDates.formatter(pattern == null
                        ? ("DATE".equals(type) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss") : String.valueOf(pattern));
                converter = "DATE".equals(type)
                        ? value -> java.sql.Date.valueOf(FieldDates.parse(value, input).toLocalDate())
                        : value -> Timestamp.from(FieldDates.parse(value, input).toInstant());
                break;
            default: throw new ServiceException("不支持的模型字段类型");
        }
        return value -> {
            if (value == null) return null;
            try {
                return converter.convert(value);
            } catch (ServiceException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new ServiceException("字段类型转换失败，数值溢出或精度不符合模型要求");
            }
        };
    }

    private static BigDecimal decimal(Object value) {
        if (!(value instanceof Number) && !(value instanceof String))
            throw new ServiceException("无法转换为数值");
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }

    private static String scalarText(Object value) {
        if (value instanceof String || value instanceof Character || value instanceof Boolean || value instanceof Number)
            return value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value.toString();
        if (value instanceof java.time.temporal.TemporalAccessor || value instanceof java.util.Date)
            return value.toString();
        throw new ServiceException("对象或数组不能转换为字符串");
    }
}
