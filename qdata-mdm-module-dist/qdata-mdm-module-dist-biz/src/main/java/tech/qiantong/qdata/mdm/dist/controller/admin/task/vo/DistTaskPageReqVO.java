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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 分发任务 Request VO 对象 DIST_TASK
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DistTaskPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分发任务名称", example = "")
    private String name;

    @Schema(description = "分发任务编码")
    private String code;

    @Schema(description = "目标系统名称")
    private String targetSystemName;

    @Schema(description = "目标系统ID；用于查询该系统关联的分发任务")
    private Long targetSystemId;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "分发数据模式：FULL全量、INCREMENTAL增量", example = "")
    private String dataMode;

    @Schema(description = "执行方式：MANUAL人工执行、SCHEDULED定时执行", example = "")
    private String triggerType;

    @Schema(description = "任务状态：ENABLED启用、DISABLED停用")
    private String status;

    @Schema(description = "最近执行结果：RUNNING运行中、SUCCESS成功、FAILED失败")
    private String lastRunStatus;

}
