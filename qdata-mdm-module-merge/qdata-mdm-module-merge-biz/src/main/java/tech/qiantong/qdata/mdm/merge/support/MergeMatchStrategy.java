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

import java.util.Map;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeCompiledRuleBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchEdgeBO;

/**
 * 一种完整匹配规则的执行策略。
 *
 * @author chentangwen
 */
public interface MergeMatchStrategy {

    /**
     * 比较两条记录，不满足当前策略命中条件时返回 {@code null}。
     * @param left 左侧记录，键为模型字段标识
     * @param right 右侧记录，键与左侧一致
     * @param rule 已通过配置校验的编译规则
     * @param registry 当前规则及字段算法使用的注册表
     * @return 命中分数和规则证据，未命中返回 null
     */
    MergeMatchEdgeBO match(Map<String, String> left, Map<String, String> right,
            MergeCompiledRuleBO rule, MergeMatchStrategyRegistry registry);
}
