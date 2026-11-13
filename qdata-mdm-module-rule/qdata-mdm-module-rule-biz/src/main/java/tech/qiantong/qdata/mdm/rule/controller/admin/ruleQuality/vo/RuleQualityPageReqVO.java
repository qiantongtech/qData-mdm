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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 质量规则 Request VO 对象 RULE_QUALITY
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "质量规则 Request VO")
@Data
public class RuleQualityPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "规则编码", example = "")
    private String ruleCode;

    @Schema(description = "规则名称", example = "")
    private String ruleName;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型版本号", example = "")
    private String modelVersion;

    @Schema(description = "模型字段", example = "")
    private String modelFieldId;

    @Schema(description = "配置异常标志;0正常，1配置异常", example = "")
    private String abnormalFlag;

    @Schema(description = "质量规则类型", example = "")
    private String ruleType;

    @Schema(description = "校验规则JSON配置", example = "")
    private String ruleConfig;

    @Schema(description = "规则启用状态;0：无效停用，1：有效启用", example = "")
    private Boolean validFlag;

    @Schema(description = "规则说明", example = "")
    private String description;



}
