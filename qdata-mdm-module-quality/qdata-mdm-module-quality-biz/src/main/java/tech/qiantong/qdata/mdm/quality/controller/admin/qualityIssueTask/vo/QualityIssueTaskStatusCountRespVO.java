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

/** 整改任务独立状态统计。 */
@Data
@Schema(description = "整改任务状态数量")
public class QualityIssueTaskStatusCountRespVO {
    @Schema(description = "全部任务数量，包含历史未知状态")
    private Long allCount = 0L;
    @Schema(description = "待审批任务数量")
    private Long pendingCount = 0L;
    @Schema(description = "审批中任务数量")
    private Long processingCount = 0L;
    @Schema(description = "已完成任务数量")
    private Long completedCount = 0L;
    @Schema(description = "已取消任务数量")
    private Long cancelledCount = 0L;
    @Schema(description = "处理中任务数量")
    private Long rectifyingCount = 0L;
}
