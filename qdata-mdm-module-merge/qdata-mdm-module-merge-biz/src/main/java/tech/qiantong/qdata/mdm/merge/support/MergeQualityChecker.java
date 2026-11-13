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

package tech.qiantong.qdata.mdm.merge.support;

import com.fasterxml.jackson.databind.JsonNode;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergeQualityCheckItemBO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/**
 * 只执行能够识别的质量规则。没有任何可执行规则时由调用方禁止提交。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public final class MergeQualityChecker {
    private static final Pattern USCC = Pattern.compile("^[0-9A-Z]{18}$");

    private MergeQualityChecker() {
    }

    /** 两种检测入口均使用已发布参考数据快照。 */
    public static Map<String, Set<String>> referenceValues(ModelPublishedStructureRespDTO structure) {
        Map<String, Set<String>> values = new HashMap<>();
        Map<Long, Set<String>> byType = new HashMap<>();
        structure.getReferenceItems().forEach(item -> {
            if (Boolean.FALSE.equals(item.getValidFlag()) || Boolean.TRUE.equals(item.getDelFlag())) return;
            Set<String> allowed = byType.computeIfAbsent(item.getTypeId(), ignored -> new HashSet<>());
            if (item.getCode() != null) allowed.add(item.getCode().trim());
            if (item.getName() != null) allowed.add(item.getName().trim());
        });
        for (ModelPublishedColumnRespDTO field : structure.getFields()) {
            if (field.getDictTypeId() == null) continue;
            Set<String> allowed = byType.get(field.getDictTypeId());
            if (allowed == null) continue;
            values.put(String.valueOf(field.getId()), allowed);
            values.put(field.getCode(), allowed);
        }
        return values;
    }

    /**
     * 执行已支持的配置规则，不支持的规则保留未执行标记供提交校验使用。
     */
    public static List<MergeQualityCheckItemBO> check(Map<String, String> finals, List<RuleQualityDO> rules) {
        return check(finals, rules, java.util.Collections.emptyList(), java.util.Collections.emptySet());
    }

    /** 使用其他有效记录检查唯一性，并对尚未生成的自动字段跳过适用的空值检查。 */
    public static List<MergeQualityCheckItemBO> check(Map<String, String> finals, List<RuleQualityDO> rules,
            List<Map<String, String>> peers, java.util.Set<String> generatedFields) {
        return check(finals, rules, peers, generatedFields, java.util.Collections.emptyMap());
    }

    /** 执行规则并保留逐项结论；参考集合缺失或配置无效时标记为未执行，交由提交校验拦截。 */
    public static List<MergeQualityCheckItemBO> check(Map<String, String> finals, List<RuleQualityDO> rules,
            List<Map<String, String>> peers, java.util.Set<String> generatedFields,
            Map<String, java.util.Set<String>> referenceValues) {
        List<MergeQualityCheckItemBO> items = new ArrayList<>();
        if (rules == null) {
            return items;
        }
        for (RuleQualityDO rule : rules) {
            MergeQualityCheckItemBO item = new MergeQualityCheckItemBO();
            item.setRuleName(rule.getRuleName());
            String fieldKey = fieldKey(rule);
            String value = fieldKey == null ? "" : nullToEmpty(finals.get(fieldKey)).trim();
            String type = rule.getRuleType() == null ? "" : rule.getRuleType().toUpperCase(Locale.ROOT);
            JsonNode config = MergeJson.read(rule.getRuleConfig());
            List<String> keys = fieldKeys(rule);
            if (java.util.Arrays.asList("NOT_EMPTY", "EXACT", "LENGTH", "FORMAT", "UNIQUE").contains(type)
                    && keys.stream().anyMatch(key -> generatedFields.contains(key)
                    && nullToEmpty(finals.get(key)).trim().isEmpty())) {
                item.setExecuted(true);
                item.setPassed(true);
                item.setMessage("自动生成字段在正式生效时生成，本次不检查空值");
            } else if ("UNIQUE".equals(type)) {
                item.setExecuted(!keys.isEmpty());
                boolean empty = keys.stream().anyMatch(key -> nullToEmpty(finals.get(key)).trim().isEmpty());
                boolean duplicate = !empty && peers.stream().anyMatch(peer -> keys.stream().allMatch(key ->
                        nullToEmpty(finals.get(key)).trim().equals(nullToEmpty(peer.get(key)).trim())));
                item.setPassed(!keys.isEmpty() && !duplicate);
                item.setMessage(keys.isEmpty() ? "唯一性规则未配置字段" : empty ? "参与字段为空，跳过唯一性判断"
                        : duplicate ? "字段组合与当前模型其他有效数据重复" : "通过");
            } else if ("NOT_EMPTY".equals(type) || "EXACT".equals(type)) {
                item.setExecuted(true);
                item.setPassed(!value.isEmpty());
                item.setMessage(item.isPassed() ? "通过" : "字段不能为空");
            } else if ("LENGTH".equals(type)) {
                item.setExecuted(true);
                int min = config.path("minLen").asInt(0);
                int max = config.path("maxLen").asInt(Integer.MAX_VALUE);
                int length = value.length();
                item.setPassed(length >= min && length <= max);
                item.setMessage(item.isPassed() ? "通过" : "长度不在允许范围内");
            } else if ("FORMAT".equals(type)) {
                String regex = config.path("regex").asText("");
                String preset = config.path("preset").asText("");
                if (!regex.isEmpty()) {
                    item.setExecuted(true);
                    item.setPassed(value.matches(regex));
                    item.setMessage(item.isPassed() ? "通过" : "不匹配自定义格式");
                } else if (preset.contains("统一社会信用代码")) {
                    item.setExecuted(true);
                    item.setPassed(USCC.matcher(value.toUpperCase(Locale.ROOT)).matches());
                    item.setMessage(item.isPassed() ? "通过" : "不是18位统一社会信用代码");
                } else {
                    item.setExecuted(false);
                    item.setMessage("格式预置暂不支持：" + preset);
                }
            } else if ("NUMBER_RANGE".equals(type) || "FIELD_LOGIC".equals(type)) {
                item.setExecuted(true);
                try {
                    item.setPassed(evaluateStructured(type, config, finals));
                    item.setMessage(item.isPassed() ? "通过" : "不满足规则条件");
                } catch (IllegalArgumentException ex) {
                    item.setExecuted(false);
                    item.setMessage(ex.getMessage());
                }
            } else if ("REF_DATA_VALID".equals(type)) {
                String key = fieldKey(rule);
                java.util.Set<String> allowed = referenceValues.get(key);
                item.setExecuted(allowed != null);
                item.setPassed(allowed != null && (value.isEmpty() || allowed.contains(value)));
                item.setMessage(allowed == null ? "参考数据集合未找到" : item.isPassed() ? "通过" : "不在参考数据集合中");
            } else {
                item.setExecuted(false);
                item.setMessage("规则类型暂不支持：" + type);
            }
            items.add(item);
        }
        return items;
    }

    /** 数值范围及双字段比较共用空值跳过策略，非空约束由独立规则承担。 */
    private static boolean evaluateStructured(String type, JsonNode config, Map<String, String> finals) {
        if ("NUMBER_RANGE".equals(type)) {
            String value = nullToEmpty(finals.get(config.path("modelFieldId").asText(config.path("fieldId").asText("")))).trim();
            if (value.isEmpty()) {
                return true;
            }
            BigDecimal number;
            try { number = new BigDecimal(value); }
            catch (NumberFormatException ex) { return false; }
            if (config.hasNonNull("minVal") && !config.path("minVal").asText("").trim().isEmpty()) {
                int compare = number.compareTo(new BigDecimal(config.path("minVal").asText()));
                if (">".equals(config.path("minOp").asText(">=")) ? compare <= 0 : compare < 0) {
                    return false;
                }
            }
            if (config.hasNonNull("maxVal") && !config.path("maxVal").asText("").trim().isEmpty()) {
                int compare = number.compareTo(new BigDecimal(config.path("maxVal").asText()));
                if ("<".equals(config.path("maxOp").asText("<=")) ? compare >= 0 : compare > 0) {
                    return false;
                }
            }
            return true;
        }
        String leftKey = config.path("modelFieldId1").asText(config.path("fieldId1").asText(""));
        String rightKey = config.path("modelFieldId2").asText(config.path("fieldId2").asText(""));
        String left = nullToEmpty(finals.get(leftKey)).trim();
        String right = nullToEmpty(finals.get(rightKey)).trim();
        if (left.isEmpty() || right.isEmpty()) {
            return true;
        }
        int compare;
        try { compare = new BigDecimal(left).compareTo(new BigDecimal(right)); }
        catch (NumberFormatException ex) { compare = left.compareTo(right); }
        switch (config.path("compareOp").asText("=").toUpperCase(Locale.ROOT)) {
            case "=": case "==": case "EQ": return compare == 0;
            case "!=": case "<>": case "NE": return compare != 0;
            case ">": case "GT": return compare > 0;
            case ">=": case "GE": return compare >= 0;
            case "<": case "LT": return compare < 0;
            case "<=": case "LE": return compare <= 0;
            default: throw new IllegalArgumentException("不支持的字段比较符：" + config.path("compareOp").asText());
        }
    }

    /**
     * 兼容单字段配置及历史 fieldList 配置，保持既有取首字段规则。
     */
    private static String fieldKey(RuleQualityDO rule) {
        List<String> keys = fieldKeys(rule);
        return keys.isEmpty() ? null : keys.get(0);
    }

    /** 依次从 JSON 字段列表、规则表逗号分隔编号及 JSON 单字段配置中解析字段。 */
    public static List<String> fieldKeys(RuleQualityDO rule) {
        List<String> keys = new ArrayList<>();
        JsonNode config = MergeJson.read(rule.getRuleConfig());
        JsonNode list = config.has("fieldIdList") ? config.get("fieldIdList") : config.get("fieldList");
        if (list != null && list.isArray()) {
            for (JsonNode entry : list) {
                String key = entry.isObject() ? entry.path("modelFieldId").asText(entry.path("fieldId").asText(""))
                        : entry.asText("");
                if (!key.trim().isEmpty()) keys.add(key.trim());
            }
        }
        if (keys.isEmpty() && rule.getModelFieldId() != null) {
            for (String key : rule.getModelFieldId().split(",")) if (!key.trim().isEmpty()) keys.add(key.trim());
        }
        if (keys.isEmpty()) {
            String key = config.path("modelFieldId").asText(config.path("fieldId").asText(""));
            if (!key.isEmpty()) keys.add(key);
        }
        return keys;
    }

    /**
     * 未提供字段值时按空值进行质量检查。
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
