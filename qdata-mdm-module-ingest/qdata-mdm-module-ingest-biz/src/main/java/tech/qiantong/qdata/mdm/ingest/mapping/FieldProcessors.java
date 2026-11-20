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

package tech.qiantong.qdata.mdm.ingest.mapping;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_DATE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_FIXED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_NONE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_NULL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_TRIM;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_TYPE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_VALUE_MAP;

/**
 * 数据接入的字段处理器工厂。
 *
 * <p>根据处理类型创建单项规则，在编译阶段完成参数校验、日期格式解析和固定值计算，
 * 逐行执行时复用已准备的规则。最终模型类型及约束由映射计划中的转换器校验。</p>
 *
 * @author chentangwen
 */
public final class FieldProcessors {

    /**
     * 日期处理时需要按输出格式生成字符串的目标类型。
     */
    private static final Set<String> TEXT_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("STRING", "TEXT", "VARCHAR", "VARCHAR2", "CHAR")));

    /**
     * 处理类型与规则工厂的只读注册表。
     */
    private static final Map<String, BiFunction<Map<String, Object>, IngestModelFieldRespVO, FieldProcessor>> FACTORIES;

    static {
        Map<String, BiFunction<Map<String, Object>, IngestModelFieldRespVO, FieldProcessor>> factories = new HashMap<>();
        // 注册阶段只关联创建方法；具体规则在编译映射配置时准备，避免逐行解析参数。
        factories.put(TRANSFORM_NONE, (config, target) -> value -> value);
        factories.put(TRANSFORM_TYPE, FieldProcessors::compileType);
        factories.put(TRANSFORM_TRIM,
                (config, target) -> value -> value instanceof String ? ((String) value).trim() : value);
        factories.put(TRANSFORM_NULL, FieldProcessors::compileNullReplacement);
        factories.put(TRANSFORM_FIXED, FieldProcessors::compileFixed);
        factories.put(TRANSFORM_VALUE_MAP, (config, target) -> compileValueMap(config));
        factories.put(TRANSFORM_DATE, FieldProcessors::compileDate);
        FACTORIES = Collections.unmodifiableMap(factories);
    }

    private FieldProcessors() {
    }

    /**
     * 校验参数并创建可复用的字段处理器。
     *
     * @param type 处理类型编码，为 null 时按不处理规则创建
     * @param config 处理参数，为 null 时视为空配置；具体规则校验其必需参数
     * @param target 目标模型字段，不能为空且必须包含数据类型
     * @return 当前处理规则对应的字段处理器
     * @throws ServiceException 类型不支持、目标字段不完整或处理参数不合法时抛出
     */
    public static FieldProcessor compile(String type, Map<String, Object> config, IngestModelFieldRespVO target) {
        BiFunction<Map<String, Object>, IngestModelFieldRespVO, FieldProcessor> factory =
                FACTORIES.get(type == null ? TRANSFORM_NONE : type);
        if (factory == null) {
            throw new ServiceException("字段处理类型不正确");
        }
        if (target == null || target.getDataType() == null || target.getDataType().trim().isEmpty()) {
            throw new ServiceException("目标模型字段及数据类型不能为空");
        }
        // 统一空配置，缺失参数由具体规则给出明确提示。
        return factory.apply(config == null ? Collections.emptyMap() : config, target);
    }

    /**
     * 编译配置指定的类型转换，兼容旧配置未指定转换类型的情况。
     */
    private static FieldProcessor compileType(Map<String, Object> config, IngestModelFieldRespVO target) {
        Object configuredType = config.get("targetType");
        if (configuredType != null && !(configuredType instanceof String)) {
            throw new ServiceException("类型转换的目标类型必须为字符串");
        }
        String type = configuredType == null ? "" : ((String) configuredType).trim();
        if (type.isEmpty()) {
            type = target.getDataType();
        }
        // 此处仅执行配置指定的类型转换，模型长度、精度等最终约束仍由后续转换器校验。
        IngestModelFieldRespVO conversionField = new IngestModelFieldRespVO();
        conversionField.setDataType(type);
        TypeConverter converter = TypeConverters.compileType(conversionField, config);
        return converter::convert;
    }

    /**
     * 提前转换替换值；运行时仅替换 null 或空白字符串，保留其他原值。
     */
    private static FieldProcessor compileNullReplacement(Map<String, Object> config, IngestModelFieldRespVO target) {
        require(config, "defaultValue", "空值替换值");
        Object replacement = TypeConverters.compile(target, config).convert(config.get("defaultValue"));
        return value -> value == null || value instanceof String && ((String) value).trim().isEmpty()
                ? replacement : value;
    }

    /**
     * 提前校验并转换固定值，创建忽略来源输入的处理器。
     */
    private static FieldProcessor compileFixed(Map<String, Object> config, IngestModelFieldRespVO target) {
        require(config, "value", "固定值");
        Object fixed = TypeConverters.compile(target, config).convert(config.get("value"));
        return value -> fixed;
    }

    /**
     * 编译日期输入格式；文本目标额外编译输出格式，日期目标保留日期时间对象。
     */
    private static FieldProcessor compileDate(Map<String, Object> config, IngestModelFieldRespVO target) {
        DateTimeFormatter input = FieldDates.formatter(requireString(config, "inputFormat", "输入日期格式"));
        boolean text = TEXT_TYPES.contains(target.getDataType().toUpperCase(Locale.ROOT));
        // 非文本目标交由后续模型转换器处理，无需配置文本输出格式。
        DateTimeFormatter output = text
                ? FieldDates.formatter(requireString(config, "outputFormat", "输出日期格式")) : null;
        return value -> {
            if (value == null) {
                return null;
            }
            return text ? FieldDates.parse(value, input).format(output) : FieldDates.parse(value, input);
        };
    }

    /**
     * 检查参数键是否存在，允许显式配置 null，具体值的合法性由对应规则决定。
     */
    private static void require(Map<String, Object> config, String key, String name) {
        if (!config.containsKey(key)) {
            throw new ServiceException("请配置" + name);
        }
    }

    /**
     * 读取非空字符串参数，保留格式表达式中的空格及字面量。
     */
    private static String requireString(Map<String, Object> config, String key, String name) {
        Object value = config.get(key);
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            throw new ServiceException("请配置" + name);
        }
        if (!(value instanceof String)) {
            throw new ServiceException(name + "必须为字符串");
        }
        return (String) value;
    }

    /**
     * 编译值映射；entries 保留类型区分，values 兼容页面文本键与数字、布尔值匹配。
     */
    private static FieldProcessor compileValueMap(Map<String, Object> config) {
        Object entries = config.get("entries");
        boolean typedRules = entries instanceof List;
        Map<Object, Object> rules = new LinkedHashMap<>();
        // 显式配置的条目数组优先；错误类型直接报错，避免静默采用另一组规则。
        if (entries != null && !typedRules) {
            throw new ServiceException("值映射条目必须为数组");
        }
        if (typedRules) {
            readEntryRules((List<?>) entries, rules);
        } else if (config.get("values") instanceof Map) {
            readValueRules((Map<?, ?>) config.get("values"), rules);
        } else {
            throw new ServiceException("请配置值映射规则");
        }
        if (rules.isEmpty()) {
            throw new ServiceException("请配置值映射规则");
        }
        // 复制并冻结规则表，使创建后的处理器不受原配置容器修改影响。
        Map<Object, Object> compiledRules = Collections.unmodifiableMap(new LinkedHashMap<>(rules));
        return value -> matchValue(value, compiledRules, typedRules);
    }

    /**
     * 解析带类型的条目数组，要求每项都包含 source 和 target 键。
     */
    private static void readEntryRules(List<?> entries, Map<Object, Object> rules) {
        for (Object item : entries) {
            if (!(item instanceof Map)) {
                throw new ServiceException("值映射规则格式不正确");
            }
            Map<?, ?> entry = (Map<?, ?>) item;
            if (!entry.containsKey("source") || !entry.containsKey("target")) {
                throw new ServiceException("请完整配置值映射规则");
            }
            addValueRule(rules, entry.get("source"), entry.get("target"));
        }
    }

    /**
     * 解析页面使用的键值映射表，保留字符串键的原始内容。
     */
    private static void readValueRules(Map<?, ?> values, Map<Object, Object> rules) {
        values.forEach((source, target) -> addValueRule(rules, source, target));
    }

    /**
     * 校验单条规则并检查规范化后的来源键重复，允许将命中值映射为 null。
     */
    private static void addValueRule(Map<Object, Object> rules, Object source, Object target) {
        Object key = normalizeMappingKey(source);
        normalizeMappingKey(target);
        // 使用 containsKey 区分“没有规则”与“已有规则的目标值为 null”。
        if (rules.containsKey(key)) {
            throw new ServiceException("值映射来源值重复");
        }
        rules.put(key, target);
    }

    /**
     * 按精确键、去空格字符串、兼容文本键的顺序匹配，未命中时返回原值。
     */
    private static Object matchValue(Object value, Map<Object, Object> rules, boolean typedRules) {
        Object key = normalizeMappingKey(value);
        if (rules.containsKey(key)) {
            return rules.get(key);
        }
        if (value instanceof String && rules.containsKey(((String) value).trim())) {
            return rules.get(((String) value).trim());
        }
        // entries 不跨数字、布尔值和字符串类型匹配；values 兼容页面的字符串键。
        if (!typedRules && (value instanceof Number || value instanceof Boolean)) {
            String text = value.toString();
            if (rules.containsKey(text)) {
                return rules.get(text);
            }
            // JDBC 小数 1.00 可匹配文本键 1，字符串 001 则保留前导零，不转为数字。
            if (key instanceof BigDecimal) {
                String number = ((BigDecimal) key).toPlainString();
                if (rules.containsKey(number)) {
                    return rules.get(number);
                }
            }
        }
        return value;
    }

    /**
     * 校验值映射允许的标量类型，并将数值规范化为忽略无效小数零的比较键。
     */
    private static Object normalizeMappingKey(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number) {
            try {
                return new BigDecimal(value.toString()).stripTrailingZeros();
            } catch (NumberFormatException ex) {
                // 不回显原值，避免将来源字段内容带入异常提示。
                throw new ServiceException("值映射不支持非有限或无效数值");
            }
        }
        throw new ServiceException("值映射仅支持字符串、数字、布尔和空值");
    }
}
