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
import tech.qiantong.qdata.mdm.merge.domain.review.MergeValueDecisionBO;
import tech.qiantong.qdata.mdm.merge.domain.review.MergeValueInputBO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 没有规则能唯一确定，或多个不同非空值无法区分时，记为字段冲突，不按频次或时间取值。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public final class MergeValueDecider {
    private MergeValueDecider() {
    }

    /** 按非空文本分组；不同值必须由生存规则唯一确定，否则保留冲突，不按频次或时间选值。 */
    public static MergeValueDecisionBO decide(List<MergeValueInputBO> inputs, List<RuleSurvivorDO> rules,
                String columnCode, String columnId) {
        Map<String, List<MergeValueInputBO>> grouped = new LinkedHashMap<>();
        for (MergeValueInputBO input : inputs) {
            String value = input.getValue() == null ? "" : input.getValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            List<MergeValueInputBO> bucket = grouped.get(value);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(value, bucket);
            }
            bucket.add(input);
        }
        MergeValueDecisionBO decision = new MergeValueDecisionBO();
        if (grouped.isEmpty()) {
            return decision;
        }
        if (rules != null) {
            for (RuleSurvivorDO rule : rules) {
                if (!matchField(rule.getModelFieldId(), columnCode, columnId)) {
                    continue;
                }
                MergeValueDecisionBO ruled = applyRule(rule, grouped);
                if (ruled != null) {
                    return ruled;
                }
            }
        }
        if (grouped.size() == 1) {
            return fromBucket(grouped.values().iterator().next(), null);
        }
        MergeValueDecisionBO conflict = new MergeValueDecisionBO();
        conflict.setConflict(true);
        return conflict;
    }

    /** 按既有规则类型选择来源优先或可信度优先，未知类型不决定推荐值。 */
    private static MergeValueDecisionBO applyRule(RuleSurvivorDO rule, Map<String, List<MergeValueInputBO>> grouped) {
        String type = rule.getRuleType() == null ? "" : rule.getRuleType().toUpperCase(Locale.ROOT);
        if ("SOURCE_PRIORITY".equals(type)) {
            return sourcePriority(rule, grouped);
        }
        if ("CREDIBILITY_PRIORITY".equals(type)) {
            return credibility(rule, grouped);
        }
        if ("LATEST_VALUE".equals(type)) {
            return latestValue(rule, grouped);
        }
        if ("NON_EMPTY_PRIORITY".equals(type)) {
            return grouped.size() == 1
                    ? fromBucket(grouped.values().iterator().next(), rule.getId()) : null;
        }
        return null;
    }

    /** 按平台接收时间选择最新值；最新时间有多个不同值时保留冲突。 */
    private static MergeValueDecisionBO latestValue(RuleSurvivorDO rule,
                Map<String, List<MergeValueInputBO>> grouped) {
        String winner = null;
        long latest = Long.MIN_VALUE;
        boolean tie = false;
        for (Map.Entry<String, List<MergeValueInputBO>> entry : grouped.entrySet()) {
            long bucketTime = Long.MIN_VALUE;
            for (MergeValueInputBO input : entry.getValue()) {
                bucketTime = Math.max(bucketTime, input.getReceiveTime());
            }
            if (bucketTime > latest) {
                winner = entry.getKey();
                latest = bucketTime;
                tie = false;
            } else if (bucketTime == latest) {
                tie = true;
            }
        }
        return winner == null || tie ? null : fromBucket(grouped.get(winner), rule.getId());
    }

    /** 依次尝试配置的来源名称或编码，只有该优先级能唯一确定一个值时采用。 */
    private static MergeValueDecisionBO sourcePriority(RuleSurvivorDO rule,
                Map<String, List<MergeValueInputBO>> grouped) {
        JsonNode root = MergeJson.read(rule.getRuleConfig());
        JsonNode list = root.get("sourcePriorityList");
        if (list == null || !list.isArray()) {
            return null;
        }
        for (JsonNode item : list) {
            String token = item.asText("").trim();
            if (token.isEmpty()) {
                continue;
            }
            List<MergeValueInputBO> matched = new ArrayList<>();
            for (List<MergeValueInputBO> bucket : grouped.values()) {
                for (MergeValueInputBO input : bucket) {
                    if (token.equalsIgnoreCase(nullToEmpty(input.getSystemName()))
                            || token.equalsIgnoreCase(nullToEmpty(input.getSystemCode()))) {
                        matched.add(input);
                    }
                }
            }
            Map<String, List<MergeValueInputBO>> byValue = group(matched);
            if (byValue.size() == 1) {
                return fromBucket(byValue.values().iterator().next(), rule.getId());
            }
        }
        return null;
    }

    /** 不同值比较各自最高可信度，最高可信度并列时继续保留冲突。 */
    private static MergeValueDecisionBO credibility(RuleSurvivorDO rule, Map<String, List<MergeValueInputBO>> grouped) {
        String winner = null;
        int best = Integer.MIN_VALUE;
        boolean tie = false;
        for (Map.Entry<String, List<MergeValueInputBO>> entry : grouped.entrySet()) {
            int trust = Integer.MIN_VALUE;
            for (MergeValueInputBO input : entry.getValue()) {
                trust = Math.max(trust, input.getTrust());
            }
            if (winner == null || trust > best) {
                winner = entry.getKey();
                best = trust;
                tie = false;
            } else if (trust == best) {
                tie = true;
            }
        }
        if (winner == null || tie) {
            return null;
        }
        return fromBucket(grouped.get(winner), rule.getId());
    }

    /** 同一文本值仅选较新的接收版本作为追溯依据；时间相同时沿用最后出现的版本。 */
    private static MergeValueDecisionBO fromBucket(List<MergeValueInputBO> bucket, Long ruleId) {
        MergeValueInputBO picked = bucket.get(0);
        for (MergeValueInputBO input : bucket) {
            if (input.getReceiveTime() >= picked.getReceiveTime()) {
                picked = input;
            }
        }
        MergeValueDecisionBO decision = new MergeValueDecisionBO();
        decision.setValue(picked.getValue() == null ? "" : picked.getValue().trim());
        decision.setVersionId(picked.getVersionId());
        decision.setRuleId(ruleId);
        return decision;
    }

    /** 兼容逗号分隔的字段编码与字段编号。 */
    private static boolean matchField(String modelFieldId, String columnCode, String columnId) {
        if (modelFieldId == null || modelFieldId.trim().isEmpty()) {
            return false;
        }
        String[] parts = modelFieldId.split(",");
        for (String part : parts) {
            String token = part.trim();
            if (token.equals(columnCode) || token.equals(columnId)) {
                return true;
            }
        }
        return false;
    }

    /** 按去空格后的非空文本分组，保留来源出现顺序。 */
    private static Map<String, List<MergeValueInputBO>> group(List<MergeValueInputBO> inputs) {
        Map<String, List<MergeValueInputBO>> grouped = new LinkedHashMap<>();
        for (MergeValueInputBO input : inputs) {
            String value = input.getValue() == null ? "" : input.getValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            List<MergeValueInputBO> bucket = grouped.get(value);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(value, bucket);
            }
            bucket.add(input);
        }
        return grouped;
    }

    /** 缺少来源名称或编码时使用空文本进行匹配。 */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
