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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 匹配规则 创建/修改 Request VO RULE_MATCH
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "匹配规则 Response VO")
@Data
public class RuleMatchSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @JsonIgnore
    private boolean responsibleUserSpecified;

    @JsonSetter("responsibleUserId")
    public void setResponsibleUserId(Long responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
        this.responsibleUserSpecified = true;
    }

    @Schema(description = "规则编码", example = "")
    @Size(max = 512, message = "规则编码长度不能超过512个字符")
    private String ruleCode;

    @Schema(description = "规则名称", example = "")
    @Size(max = 512, message = "规则名称长度不能超过512个字符")
    private String ruleName;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型版本号", example = "")
    @Size(max = 512, message = "主数据模型版本号长度不能超过512个字符")
    private String modelVersion;

    @Schema(description = "优先级", example = "")
    private Long priority;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "配置异常标志", example = "")
    @Size(max = 512, message = "配置异常标志长度不能超过512个字符")
    private String abnormalFlag;

    @Schema(description = "模型字段ID", example = "")
    @Size(max = 512, message = "模型字段ID长度不能超过512个字符")
    private String modelFieldId;

    @Schema(description = "规则类型", example = "")
    @Size(max = 512, message = "规则类型长度不能超过512个字符")
    private String ruleType;

    @Schema(description = "匹配参数", example = "")
    @Size(max = 512, message = "匹配参数长度不能超过512个字符")
    private String ruleConfig;

    @Schema(description = "匹配分值", example = "")
    private Long ruleScore;

    @Schema(description = "描述", example = "")
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
