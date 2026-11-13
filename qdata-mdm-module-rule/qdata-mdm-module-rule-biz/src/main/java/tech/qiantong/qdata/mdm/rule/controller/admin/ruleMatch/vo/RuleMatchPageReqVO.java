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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 匹配规则 Request VO 对象 RULE_MATCH
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "匹配规则 Request VO")
@Data
public class RuleMatchPageReqVO extends PageParam {

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

    @Schema(description = "优先级", example = "")
    private Long priority;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "配置异常标志", example = "")
    private String abnormalFlag;

    @Schema(description = "模型字段ID", example = "")
    private String modelFieldId;

    @Schema(description = "规则类型", example = "")
    private String ruleType;

    @Schema(description = "匹配参数", example = "")
    private String ruleConfig;

    @Schema(description = "匹配分值", example = "")
    private Long ruleScore;

    @Schema(description = "描述", example = "")
    private String description;



}
