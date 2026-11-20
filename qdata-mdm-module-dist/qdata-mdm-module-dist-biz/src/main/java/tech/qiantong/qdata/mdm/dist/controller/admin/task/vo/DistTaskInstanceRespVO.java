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

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 分发任务执行实例 Response VO 对象 DIST_TASK_INSTANCE
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务执行实例 Response VO")
@Data
public class DistTaskInstanceRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "分发任务ID")
    @Schema(description = "分发任务ID", example = "")
    private Long taskId;

    @Excel(name = "运行批次号")
    @Schema(description = "运行批次号", example = "")
    private String batchNo;

    @Excel(name = "触发类型：MANUAL手动、SCHEDULED定时")
    @Schema(description = "触发类型：MANUAL手动、SCHEDULED定时", example = "")
    private String triggerType;

    @Excel(name = "运行状态")
    @Schema(description = "运行状态", example = "")
    private String runStatus;

    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间", example = "")
    private Date startTime;

    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间", example = "")
    private Date endTime;

    @Excel(name = "总记录数")
    @Schema(description = "总记录数", example = "")
    private Long totalCount;

    @Excel(name = "成功记录数")
    @Schema(description = "成功记录数", example = "")
    private Long successCount;

    @Excel(name = "失败记录数")
    @Schema(description = "失败记录数", example = "")
    private Long failureCount;

    @Excel(name = "运行异常摘要")
    @Schema(description = "运行异常摘要", example = "")
    private String errorMessage;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Excel(name = "逻辑删除标志：0未删除，1已删除")
    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人ID")
    @Schema(description = "更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
