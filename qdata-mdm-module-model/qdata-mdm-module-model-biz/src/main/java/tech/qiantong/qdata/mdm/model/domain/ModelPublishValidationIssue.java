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

package tech.qiantong.qdata.mdm.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelValidationStage;

/**
 * 按顺序执行的校验规则（规则链）的单项错误，定位到阶段、规则及对象，不增加数据库ERROR_CODE。
 */
@Data
@NoArgsConstructor
public class ModelPublishValidationIssue {

    /**
     * CONFIG、CHANGE或TARGET
     */
    private ModelValidationStage stage;

    /**
     * 产生错误的规则名称
     */
    private String ruleName;

    /**
     * 模型/字段/索引的可读定位路径
     */
    private String objectPath;

    /**
     * 可以直接记录日志的错误原因
     */
    private String message;
}

