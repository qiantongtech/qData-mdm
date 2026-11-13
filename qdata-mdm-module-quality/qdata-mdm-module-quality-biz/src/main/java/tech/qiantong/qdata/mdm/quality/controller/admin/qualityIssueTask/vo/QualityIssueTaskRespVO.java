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

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 问题整改任务 Response VO 对象 QUALITY_ISSUE_TASK
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题整改任务 Response VO")
@Data
public class QualityIssueTaskRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "整改任务编号")
    @Schema(description = "整改任务编号", example = "")
    private String taskNo;

    @Excel(name = "整改任务名称")
    @Schema(description = "整改任务名称")
    private String taskName;

    @Schema(description = "优先级：1高、2中、3低")
    private Long priority;

    @Excel(name = "涉及主数据数", cellType = Excel.ColumnType.NUMERIC)
    @Schema(description = "下发范围内的主数据记录数，按数据记录 ID 去重")
    private Long masterDataCount;

    @Excel(name = "问题数据数量", cellType = Excel.ColumnType.NUMERIC)
    @Schema(description = "下发范围内的问题项总数，解决后仍计入")
    private Long issueDataCount;

    @Schema(description = "下发范围内已整改的问题项数，明细状态为已整改时计入")
    private Long completedIssueCount;

    @Schema(description = "任务关联的全部问题项，仅详情返回")
    private List<QualityIssueTaskIssueItemRespVO> issueItems;

    @Excel(name = "主数据模型ID")
    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Excel(name = "主数据模型版本")
    @Schema(description = "主数据模型版本", example = "")
    private String modelVersion;

    @Excel(name = "整改负责人用户ID")
    @Schema(description = "整改负责人用户ID", example = "")
    private Long assigneeId;

    @Excel(name = "整改任务状态：1待审批、2审批中、3已完成、4已取消、5处理中；")
    @Schema(description = "整改任务状态：1待审批、2审批中、3已完成、4已取消、5处理中；", example = "")
    private String status;

    @Excel(name = "整改说明")
    @Schema(description = "整改说明", example = "")
    private String requirement;

    @Excel(name = "整改时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "整改时间", example = "")
    private Date rectificationTime;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志：0未删除，1已删除")
    @Schema(description = "删除标志：0未删除，1已删除", example = "")
    private Boolean delFlag;

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

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

    @Schema(description = "主数据模型名称")
    @Excel(name = "主数据模型名称")
    private String modelName;

    @Schema(description = "主数据模型编码")
    private String modelCode;

    @Schema(description = "负责人名称")
    @Excel(name = "负责人名称")
    private String assigneeName;

    @Schema(description = "负责人手机号")
    private String assigneePhone;

    @Schema(description = "最近处理时间，无处理记录时为下发时间")
    @Excel(name = "最近处理时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastHandledTime;
}
