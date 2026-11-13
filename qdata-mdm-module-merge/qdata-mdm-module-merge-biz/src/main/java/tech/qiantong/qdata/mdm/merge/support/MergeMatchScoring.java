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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeCompiledRuleBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeFieldMatchResultBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchEdgeBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchFieldBO;

/** 匹配策略共享的字段评分与边构造逻辑。 */
final class MergeMatchScoring {

    /** 字段算法适配入口，由规则级策略提供字段比较方式。 */
    interface FieldMatcher {
        MergeFieldMatchResultBO match(String left, String right, MergeMatchFieldBO field,
                MergeCompiledRuleBO rule);
    }

    private MergeMatchScoring() {
    }

    /** 默认按字段权重汇总并应用最终阈值，至少一个字段命中才生成匹配边。 */
    static MergeMatchEdgeBO score(Map<String, String> left, Map<String, String> right,
            MergeCompiledRuleBO rule, FieldMatcher matcher) {
        return score(left, right, rule, matcher, false, true);
    }

    /** 模糊规则要求所有字段命中且不使用总阈值；其他规则按加权总分判断。 */
    static MergeMatchEdgeBO score(Map<String, String> left, Map<String, String> right,
            MergeCompiledRuleBO rule, FieldMatcher matcher, boolean requireAllFields,
            boolean applyFinalThreshold) {
        int totalWeight = 0;
        BigDecimal weightedScore = BigDecimal.ZERO;
        List<String> hitFields = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        for (MergeMatchFieldBO field : rule.getFields()) {
            int weight = field.getWeight();
            totalWeight += weight;
            String leftValue = left == null ? null : left.get(field.getKey());
            String rightValue = right == null ? null : right.get(field.getKey());
            MergeFieldMatchResultBO result = matcher.match(leftValue, rightValue, field, rule);
            weightedScore = weightedScore.add(result.getScore().multiply(BigDecimal.valueOf(weight)));
            if (result.isMatched()) {
                hitFields.add(field.getKey());
                evidence.add(field.getKey() + "=" + result.getScore().stripTrailingZeros().toPlainString());
            }
        }
        if (totalWeight <= 0 || hitFields.isEmpty()
                || (requireAllFields && hitFields.size() != rule.getFields().size())) {
            return null;
        }
        BigDecimal score = weightedScore.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
        if (applyFinalThreshold && score.compareTo(BigDecimal.valueOf(rule.getFinalThreshold())) < 0) {
            return null;
        }
        MergeMatchEdgeBO edge = new MergeMatchEdgeBO();
        edge.setRuleId(rule.getId());
        edge.setScore(score);
        edge.setMatchFields(MergeJson.toJson(hitFields));
        edge.setReason(rule.getName() + " 命中 " + score.stripTrailingZeros().toPlainString()
                + "，字段评分 " + String.join(", ", evidence));
        edge.setSnapshot(rule.getSnapshot());
        return edge;
    }

    /** 原值精确比较，空白值不参与匹配，命中得 100 分。 */
    static MergeFieldMatchResultBO exact(String left, String right) {
        MergeFieldMatchResultBO result = emptyResult(left, right);
        if (blank(left) || blank(right)) {
            result.setReason("空值不参与匹配");
            return result;
        }
        result.setMatched(left.equals(right));
        result.setScore(result.isMatched() ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
        result.setReason(result.isMatched() ? "精确匹配" : "值不一致");
        return result;
    }

    /** 按配置顺序标准化后精确比较，保留标准化后的左右值用于结果追溯。 */
    static MergeFieldMatchResultBO standardizedExact(String left, String right,
            MergeCompiledRuleBO rule) {
        String normalizedLeft = standardize(left, rule.getStandardOptions());
        String normalizedRight = standardize(right, rule.getStandardOptions());
        MergeFieldMatchResultBO result = emptyResult(normalizedLeft, normalizedRight);
        if (blank(normalizedLeft) || blank(normalizedRight)) {
            result.setReason("标准化后为空值，不参与匹配");
            return result;
        }
        result.setMatched(normalizedLeft.equals(normalizedRight));
        result.setScore(result.isMatched() ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
        result.setReason(result.isMatched() ? "标准化后精确匹配" : "标准化后值不一致");
        return result;
    }

    /** 按编辑距离计算相似度，未达到字段阈值时该字段得分为零。 */
    static MergeFieldMatchResultBO fuzzy(String left, String right, MergeMatchFieldBO field) {
        MergeFieldMatchResultBO result = emptyResult(left, right);
        if (blank(left) || blank(right)) {
            result.setReason("空值不参与匹配");
            return result;
        }
        int similarity = MergeMatchEngine.similarity(left, right);
        int threshold = field.getThreshold();
        result.setMatched(similarity >= threshold);
        result.setScore(result.isMatched() ? BigDecimal.valueOf(similarity) : BigDecimal.ZERO);
        result.setReason("编辑距离相似度 " + similarity + "，阈值 " + threshold);
        return result;
    }

    /** 仅执行显式配置的标准化选项，并保持其顺序；未知选项直接报错。 */
    static String standardize(String value, List<String> options) {
        if (value == null) {
            return "";
        }
        String result = value;
        if (options == null) {
            return result;
        }
        for (String option : options) {
            if (option == null || option.trim().isEmpty()) {
                throw new ServiceException("标准化配置不能为空");
            }
            String code = option.trim().toUpperCase(java.util.Locale.ROOT);
            switch (code) {
                case "TRIM":
                    result = trimWhitespace(result);
                    break;
                case "SPACES":
                    result = result.replaceAll("(?U)\\s+", " ");
                    break;
                case "CASE":
                    result = result.toUpperCase(java.util.Locale.ROOT);
                    break;
                case "WIDTH":
                    result = toHalfWidth(result);
                    break;
                case "PUNCT":
                    result = removePunctuation(result);
                    break;
                default:
                    throw new ServiceException("不支持的标准化方式: " + option);
            }
        }
        return result;
    }

    private static MergeFieldMatchResultBO emptyResult(String left, String right) {
        MergeFieldMatchResultBO result = new MergeFieldMatchResultBO();
        result.setLeftValue(left == null ? "" : left);
        result.setRightValue(right == null ? "" : right);
        return result;
    }

    private static boolean blank(String value) {
        return value == null || trimWhitespace(value).isEmpty();
    }

    /** 裁剪首尾空白，兼容全角空格及 Unicode 空白字符。 */
    private static String trimWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean whitespace(char value) {
        return value <= ' ' || Character.isWhitespace(value) || Character.isSpaceChar(value);
    }

    /** 仅转换全角空格与全角 ASCII 字符，其他字符保持原值。 */
    private static String toHalfWidth(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\u3000') {
                result.append(' ');
            } else if (ch >= '\uFF01' && ch <= '\uFF5E') {
                result.append((char) (ch - 0xFEE0));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /** 按 Unicode 标点分类移除标点，保留字母、数字及非标点符号。 */
    private static String removePunctuation(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            int type = Character.getType(ch);
            if (type != Character.CONNECTOR_PUNCTUATION
                    && type != Character.DASH_PUNCTUATION
                    && type != Character.START_PUNCTUATION
                    && type != Character.END_PUNCTUATION
                    && type != Character.INITIAL_QUOTE_PUNCTUATION
                    && type != Character.FINAL_QUOTE_PUNCTUATION
                    && type != Character.OTHER_PUNCTUATION) {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
