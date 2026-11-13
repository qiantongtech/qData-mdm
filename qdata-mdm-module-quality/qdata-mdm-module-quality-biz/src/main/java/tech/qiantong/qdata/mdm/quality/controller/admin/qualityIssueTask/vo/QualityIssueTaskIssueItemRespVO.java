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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 整改任务下发的问题项及对应任务行。
 */
@Data
@Schema(description = "整改任务问题项")
public class QualityIssueTaskIssueItemRespVO {

    @Schema(description = "问题项 ID")
    private Long id;

    @Schema(description = "整改任务明细 ID")
    private Long taskInfoId;

    @Schema(description = "质量问题 ID")
    private Long issueId;

    @Schema(description = "质量问题编号")
    private String issueCode;

    @Schema(description = "质量规则 ID")
    private Long qualityRuleId;

    @Schema(description = "主数据记录 ID")
    private Long masterRecordId;

    @Schema(description = "模型字段 ID")
    private Long modelColumnId;

    @Schema(description = "该问题项的整改状态")
    private String taskStatus;

    @Schema(description = "整改时间")
    private Date rectificationTime;

    @Schema(description = "整改说明")
    private String requirement;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新人用户 ID")
    private Long updaterId;

    @Schema(description = "更新人手机号")
    private String updaterPhone;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "质量规则名称")
    private String qualityRuleName;

    @Schema(description = "质量规则类型")
    private String ruleType;

    @Schema(description = "问题字段名称")
    private String modelColumnName;

    @Schema(description = "主数据记录编码")
    private String masterCode;

    @Schema(description = "主数据记录名称")
    private String masterName;
}
