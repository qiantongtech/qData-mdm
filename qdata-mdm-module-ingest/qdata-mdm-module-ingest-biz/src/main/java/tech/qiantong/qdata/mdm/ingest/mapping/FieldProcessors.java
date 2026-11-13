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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.*;

/**
 * 按处理类型注册工厂，规则及日期格式在执行前编译一次。
 */
public final class FieldProcessors {
    private static final Map<String, BiFunction<Map<String, Object>, IngestModelFieldRespVO, FieldProcessor>> FACTORIES;
    static {
        Map<String, BiFunction<Map<String, Object>, IngestModelFieldRespVO, FieldProcessor>> factories = new HashMap<>();
        factories.put(TRANSFORM_NONE, (config, target) -> value -> value);
        factories.put(TRANSFORM_TYPE, (config, target) -> {
            Object configuredType = config.get("targetType");
            // 旧配置未指定转换类型时沿用模型类型；已选择的类型必须实际参与处理。
            String type = configuredType == null ? target.getDataType() : String.valueOf(configuredType).trim();
            if (type.isEmpty()) type = target.getDataType();
            IngestModelFieldRespVO conversionField = new IngestModelFieldRespVO();
            conversionField.setDataType(type);
            TypeConverter converter = TypeConverters.compile(conversionField, config);
            return converter::convert;
        });
        factories.put(TRANSFORM_TRIM, (config, target) -> value -> value instanceof String ? ((String) value).trim() : value);
        factories.put(TRANSFORM_NULL, (config, target) -> {
            require(config, "defaultValue");
            Object replacement = TypeConverters.compile(target, config).convert(config.get("defaultValue"));
            return value -> value == null || value instanceof String && ((String) value).trim().isEmpty() ? replacement : value;
        });
        factories.put(TRANSFORM_FIXED, (config, target) -> {
            require(config, "value");
            Object fixed = TypeConverters.compile(target, config).convert(config.get("value"));
            return value -> fixed;
        });
        factories.put(TRANSFORM_VALUE_MAP, (config, target) -> valueMap(config));
        factories.put(TRANSFORM_DATE, (config, target) -> {
            DateTimeFormatter input = FieldDates.formatter((String) config.get("inputFormat"));
            boolean text = Arrays.asList("STRING", "TEXT", "VARCHAR", "VARCHAR2", "CHAR").contains(target.getDataType().toUpperCase(Locale.ROOT));
            DateTimeFormatter output = text ? FieldDates.formatter((String) config.get("outputFormat")) : null;
            return value -> value == null ? null : text
                    ? FieldDates.parse(value, input).format(output) : FieldDates.parse(value, input);
        });
        FACTORIES = Collections.unmodifiableMap(factories);
    }
    private FieldProcessors() { }

    public static FieldProcessor compile(String type, Map<String, Object> config, IngestModelFieldRespVO target) {
        BiFunction<Map<String, Object>, IngestModelFieldRespVO, FieldProcessor> factory = FACTORIES.get(type == null ? TRANSFORM_NONE : type);
        if (factory == null) throw new ServiceException("字段处理类型不正确");
        return factory.apply(config, target);
    }

    private static void require(Map<String, Object> config, String key) {
        if (!config.containsKey(key)) throw new ServiceException("请配置" + ("value".equals(key) ? "固定值" : "空值替换值"));
    }

    /**
     * 条目数组严格区分类型；页面文本规则兼容数字、布尔值与对应文本键匹配。
     */
    private static FieldProcessor valueMap(Map<String, Object> config) {
        Map<Object, Object> rules = new LinkedHashMap<>();
        boolean typedRules = config.get("entries") instanceof List;
        if (typedRules) {
            for (Object item : (List<?>) config.get("entries")) {
                if (!(item instanceof Map)) throw new ServiceException("值映射规则格式不正确");
                Map<?, ?> entry = (Map<?, ?>) item;
                if (!entry.containsKey("source") || !entry.containsKey("target")) throw new ServiceException("请完整配置值映射规则");
                Object key = key(entry.get("source"));
                key(entry.get("target"));
                if (rules.containsKey(key)) throw new ServiceException("值映射来源值重复");
                rules.put(key, entry.get("target"));
            }
        } else if (config.get("values") instanceof Map) {
            ((Map<?, ?>) config.get("values")).forEach((source, target) -> {
                key(target);
                rules.put(key(source), target);
            });
        } else throw new ServiceException("请配置值映射规则");
        if (rules.isEmpty()) throw new ServiceException("请配置值映射规则");
        return value -> {
            Object key = key(value);
            if (rules.containsKey(key)) return rules.get(key);
            if (value instanceof String && rules.containsKey(((String) value).trim())) return rules.get(((String) value).trim());
            if (!typedRules && (value instanceof Number || value instanceof Boolean)) {
                String text = value.toString();
                if (rules.containsKey(text)) return rules.get(text);
                // JDBC 小数 1.00 与整数 1 均可匹配页面的 1=...，不改写字符串中的前导零。
                if (key instanceof BigDecimal) {
                    String number = ((BigDecimal) key).toPlainString();
                    if (rules.containsKey(number)) return rules.get(number);
                }
            }
            return value;
        };
    }

    private static Object key(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Number) return new BigDecimal(value.toString()).stripTrailingZeros();
        throw new ServiceException("值映射仅支持字符串、数字、布尔和空值");
    }
}
