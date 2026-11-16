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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 分发任务执行实例 Request VO 对象 DIST_TASK_INSTANCE
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务执行实例 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DistTaskInstancePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "分发任务ID", example = "")
    private Long taskId;

    @Schema(description = "运行批次号", example = "")
    private String batchNo;

    @Schema(description = "触发类型：MANUAL手动、SCHEDULED定时", example = "")
    private String triggerType;

    @Schema(description = "运行状态", example = "")
    private String runStatus;

    @Schema(description = "开始时间", example = "")
    private Date startTime;

    @Schema(description = "结束时间", example = "")
    private Date endTime;

    @Schema(description = "总记录数", example = "")
    private Long totalCount;

    @Schema(description = "成功记录数", example = "")
    private Long successCount;

    @Schema(description = "失败记录数", example = "")
    private Long failureCount;

    @Schema(description = "运行异常摘要", example = "")
    private String errorMessage;




}
