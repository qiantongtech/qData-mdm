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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static tech.qiantong.qdata.mdm.quality.enums.task.QualityIssueTaskConstants.NOTE_MAX_LENGTH;

/**
 * 整改任务明细状态操作请求。
 */
@Schema(description = "整改任务明细操作请求")
@Data
public class QualityIssueTaskInfoSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "整改问题项不能为空")
    @Positive(message = "整改问题项编号必须为正数")
    @Schema(description = "整改任务明细 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "目标状态不能为空")
    @Pattern(regexp = "[23]", message = "仅支持开始整改和完成整改")
    @Schema(description = "问题项目标状态：2 整改中，3 已整改")
    private String status;

    @Size(max = NOTE_MAX_LENGTH, message = "整改说明长度不能超过512个字符")
    @Schema(description = "整改说明，选填")
    private String requirement;

}
