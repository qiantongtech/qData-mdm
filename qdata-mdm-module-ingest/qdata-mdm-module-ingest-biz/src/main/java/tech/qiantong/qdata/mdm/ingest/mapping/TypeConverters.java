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

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据接入字段的类型转换器工厂。
 *
 * <p>根据目标模型字段预先创建转换规则，供逐行处理复用。类型、长度和小数精度取自模型，
 * 映射配置仅提供日期输入格式。转换不静默截断字符串或舍入数值，失败提示不包含原始字段值。
 * 非空、唯一性等其他模型约束不在本类处理。</p>
 *
 * @author chentangwen
 */
public final class TypeConverters {

    /**
     * 与模型建表规则一致的 STRING 默认字符长度。
     */
    private static final int DEFAULT_STRING_LENGTH = 255;

    /**
     * 整数字符串不接受小数点、科学计数法或空白。
     */
    private static final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?[0-9]+");

    private TypeConverters() {
    }

    /**
     * 创建目标模型字段的转换器，提前校验类型及长度、精度配置。
     *
     * <p>STRING 未配置长度时按 255 个 Unicode 码点校验，TEXT 不设置默认长度。
     * 整数返回 Long，小数返回 BigDecimal，日期返回 JDBC 日期或时间戳。
     * 未指定日期输入格式时，DATE 使用 yyyy-MM-dd，DATETIME/TIMESTAMP 使用 yyyy-MM-dd HH:mm:ss。
     * 所有转换器均保留 null，非字符串日期值由 {@link FieldDates} 处理。</p>
     *
     * @param field  目标模型字段，不能为空且必须包含受支持的数据类型
     * @param config 转换参数，null 视为空配置；非空的 inputFormat 必须为字符串
     * @return 可逐行复用的目标类型转换器
     * @throws ServiceException 字段类型、长度、精度或日期格式配置不合法时抛出；
     *                          字段值不合法时在调用 {@link TypeConverter#convert(Object)} 时抛出
     */
    public static TypeConverter compile(IngestModelFieldRespVO field, Map<String, Object> config) {
        return compile(field, config, true);
    }

    /**
     * 创建字段处理中间步骤的转换器，不套用模型的默认字符串长度。
     *
     * <p>调用方传入仅含类型的字段定义；最终长度、精度仍由后续目标模型转换器校验。</p>
     *
     * @param field  中间转换使用的字段类型定义，不能为空
     * @param config 转换参数，null 视为空配置
     * @return 不套用默认字符串长度的类型转换器
     * @throws ServiceException 字段类型或转换配置不合法时抛出
     */
    static TypeConverter compileType(IngestModelFieldRespVO field, Map<String, Object> config) {
        return compile(field, config, false);
    }

    /**
     * 校验公共参数并创建规则，统一保留空值及转换业务异常。
     */
    private static TypeConverter compile(IngestModelFieldRespVO field, Map<String, Object> config,
                                         boolean modelDefaults) {
        if (field == null || field.getDataType() == null || field.getDataType().trim().isEmpty()) {
            throw new ServiceException("目标模型字段及数据类型不能为空");
        }
        String type = field.getDataType().trim().toUpperCase(Locale.ROOT);
        Map<String, Object> params = config == null ? Collections.emptyMap() : config;
        Integer length = modelDefaults && "STRING".equals(type) && field.getDataLength() == null
                ? Integer.valueOf(DEFAULT_STRING_LENGTH) : field.getDataLength();
        Integer scale = field.getDataScale();
        TypeConverter converter;
        switch (type) {
            case "STRING":
            case "TEXT":
            case "VARCHAR":
            case "VARCHAR2":
            case "CHAR":
                if (length != null && length <= 0) {
                    throw new ServiceException("模型字符串长度必须大于0");
                }
                converter = value -> {
                    String text = scalarText(value);
                    if (length != null && text.codePointCount(0, text.length()) > length) {
                        throw new ServiceException("超过模型字段长度");
                    }
                    return text;
                };
                break;
            case "INTEGER":
            case "LONG":
            case "BIGINT":
            case "INT":
                converter = TypeConverters::integer;
                break;
            case "DECIMAL":
            case "NUMBER":
            case "NUMERIC":
                if ((scale != null && scale < 0)
                        || (length != null && (length <= 0 || scale == null || scale > length))) {
                    throw new ServiceException("模型小数精度配置不正确");
                }
                converter = value -> {
                    BigDecimal number = decimal(value);
                    if (scale != null) {
                        try {
                            // 仅允许去除多余的零，不通过舍入改变来源数值。
                            number = number.setScale(scale, RoundingMode.UNNECESSARY);
                        } catch (ArithmeticException ex) {
                            throw new ServiceException("超过模型小数位数");
                        }
                    }
                    if (length != null && number.precision() > length) {
                        throw new ServiceException("超过模型小数精度");
                    }
                    return number;
                };
                break;
            case "BOOLEAN":
            case "BOOL":
                converter = value -> {
                    if (value instanceof Boolean) {
                        return value;
                    }
                    if (value instanceof Number) {
                        BigDecimal number = decimal(value);
                        if (number.compareTo(BigDecimal.ONE) == 0) {
                            return true;
                        }
                        if (number.compareTo(BigDecimal.ZERO) == 0) {
                            return false;
                        }
                    } else if (value instanceof String) {
                        String text = (String) value;
                        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                            return true;
                        }
                        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                            return false;
                        }
                    }
                    throw new ServiceException("布尔值仅支持 true、false、1、0");
                };
                break;
            case "DATE":
            case "DATETIME":
            case "TIMESTAMP":
                Object pattern = params.get("inputFormat");
                if (pattern != null && !(pattern instanceof String)) {
                    throw new ServiceException("输入日期格式必须为字符串");
                }
                DateTimeFormatter input = FieldDates.formatter(pattern == null
                        ? ("DATE".equals(type) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss") : (String) pattern);
                converter = "DATE".equals(type)
                        ? value -> java.sql.Date.valueOf(FieldDates.parse(value, input).toLocalDate())
                        : value -> Timestamp.from(FieldDates.parse(value, input).toInstant());
                break;
            default:
                throw new ServiceException("不支持的模型字段类型");
        }
        return value -> {
            if (value == null) {
                return null;
            }
            try {
                return converter.convert(value);
            } catch (ServiceException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                // 不透传底层异常消息，避免暴露解析失败的原始字段内容。
                throw new ServiceException("字段类型转换失败");
            }
        };
    }

    /**
     * 平台 INTEGER 使用 BIGINT 范围，拒绝小数截断和整数溢出。
     */
    private static Long integer(Object value) {
        if (value instanceof String && !INTEGER_PATTERN.matcher((String) value).matches()) {
            throw new ServiceException("必须为完整整数格式");
        }
        try {
            return decimal(value).longValueExact();
        } catch (ArithmeticException ex) {
            throw new ServiceException("整数超出BIGINT范围或包含非零小数位");
        }
    }

    /**
     * 将数值或数字字符串转为精确小数，不回显非法文本及非有限数值。
     */
    private static BigDecimal decimal(Object value) {
        if (!(value instanceof Number) && !(value instanceof String)) {
            throw new ServiceException("无法转换为数值");
        }
        try {
            return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new ServiceException("数值格式不正确或不是有限数值");
        }
    }

    /**
     * 将标量转为文本，小数使用普通十进制形式，日期沿用原类型的文本表示。
     */
    private static String scalarText(Object value) {
        if (value instanceof String || value instanceof Character
                || value instanceof Boolean || value instanceof Number) {
            return value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value.toString();
        }
        if (value instanceof java.time.temporal.TemporalAccessor || value instanceof java.util.Date) {
            return value.toString();
        }
        throw new ServiceException("对象或数组不能转换为字符串");
    }
}
