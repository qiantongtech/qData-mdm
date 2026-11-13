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

package tech.qiantong.qdata.mdm.merge.domain.manage;

import lombok.Data;

/**
 * 单项质量规则或模型约束的执行结果。
 *
 * @author chentangwen
 */
@Data
public class MergeQualityCheckItemBO {

    /**
     * 规则或模型约束名称。
     */
    private String ruleName;

    /**
     * 当前检测器是否支持并执行该规则。
     */
    private boolean executed;

    /**
     * 已执行的规则是否通过。
     */
    private boolean passed;

    /**
     * 单项执行说明。
     */
    private String message;

    /**
     * 是否为模型约束；仅模型约束通过不能替代配置质量规则。
     */
    private boolean modelConstraint;
}
