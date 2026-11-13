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

import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeCompiledRuleBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchFieldBO;

/**
 * 匹配策略注册表，按 Spring Bean 名称选择对应规则类型的策略。
 *
 * @author chentangwen
 */
@Component
public class MergeMatchStrategyRegistry {

    @Autowired
    private Map<String, MergeMatchStrategy> strategies;

    /** 按规则类型获取策略；未知类型必须显式报错，不能退化到其他算法。 */
    public MergeMatchStrategy get(String type) {
        MergeMatchStrategy strategy = strategies.get(normalize(type));
        if (strategy == null) {
            throw new ServiceException("不支持的匹配规则类型: " + type);
        }
        return strategy;
    }

    /** 对规则及字段配置执行统一的前置校验。 */
    public void validate(MergeCompiledRuleBO rule) {
        if (rule == null) {
            throw new ServiceException("匹配规则不能为空");
        }
        get(rule.getType());
        if (rule.getFields() == null || rule.getFields().isEmpty()) {
            throw new ServiceException("匹配规则没有字段配置: " + rule.getName());
        }
        if (rule.getFinalThreshold() < 0 || rule.getFinalThreshold() > 100) {
            throw new ServiceException("匹配规则最终阈值必须在 0 到 100 之间: " + rule.getName());
        }
        int weightSum = 0;
        for (MergeMatchFieldBO field : rule.getFields()) {
            if (field == null || field.getKey() == null || field.getKey().trim().isEmpty()) {
                throw new ServiceException("匹配规则存在缺失字段: " + rule.getName());
            }
            if (field.getMethod() == null || field.getMethod().trim().isEmpty()) {
                throw new ServiceException("匹配字段算法不能为空: " + field.getKey());
            }
            if (field.getWeight() <= 0) {
                throw new ServiceException("匹配字段权重必须大于 0: " + field.getKey());
            }
            if (field.getThreshold() < 0 || field.getThreshold() > 100) {
                throw new ServiceException("匹配字段阈值必须在 0 到 100 之间: " + field.getKey());
            }
            weightSum += field.getWeight();
            get(field.getMethod());
        }
        if ("WEIGHTED".equalsIgnoreCase(rule.getType()) && weightSum != 100) {
            throw new ServiceException("加权匹配规则的字段权重合计必须为 100: " + rule.getName());
        }
        if ("STD_EXACT".equalsIgnoreCase(rule.getType())) {
            // 在没有记录时也提前校验标准化配置，不能等到第一对数据才失败。
            MergeMatchScoring.standardize("", rule.getStandardOptions());
        }
        for (MergeMatchFieldBO field : rule.getFields()) {
            String method = field.getMethod();
            if ("EXACT".equalsIgnoreCase(rule.getType()) && !"EXACT".equalsIgnoreCase(method)) {
                throw new ServiceException("精确匹配规则的字段算法必须为 EXACT: " + field.getKey());
            }
            if ("STD_EXACT".equalsIgnoreCase(rule.getType())
                    && !"EXACT".equalsIgnoreCase(method) && !"STD_EXACT".equalsIgnoreCase(method)) {
                throw new ServiceException("标准化精确匹配规则的字段算法不正确: " + field.getKey());
            }
            if ("FUZZY".equalsIgnoreCase(rule.getType()) && !"FUZZY".equalsIgnoreCase(method)) {
                throw new ServiceException("模糊匹配规则的字段算法必须为 FUZZY: " + field.getKey());
            }
            if ("STD_EXACT".equalsIgnoreCase(method)) {
                MergeMatchScoring.standardize("", rule.getStandardOptions());
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
