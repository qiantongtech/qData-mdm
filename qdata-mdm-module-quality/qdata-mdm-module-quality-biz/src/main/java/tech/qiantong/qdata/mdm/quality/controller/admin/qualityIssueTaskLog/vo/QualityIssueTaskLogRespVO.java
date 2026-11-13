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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 问题整改记录日志 Response VO 对象 QUALITY_ISSUE_TASK_LOG
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题整改记录日志 Response VO")
@Data
public class QualityIssueTaskLogRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "整改任务ID")
    @Schema(description = "整改任务ID", example = "")
    private Long qualityIssueTaskId;

    @Excel(name = "问题数据ID")
    @Schema(description = "问题数据ID", example = "")
    private Long issueId;

    @Excel(name = "整改任务明细ID")
    @Schema(description = "整改任务明细ID，关联QUALITY_ISSUE_TASK_INFO.ID", example = "")
    private Long taskInfoId;

    @Excel(name = "整改前值")
    @Schema(description = "整改前值", example = "")
    private String beforeValue;

    @Excel(name = "整改后值")
    @Schema(description = "整改后值", example = "")
    private String afterValue;

    @Excel(name = "整改字段")
    @Schema(description = "整改字段", example = "")
    private String rectificationField;

    @Excel(name = "主数据对象")
    @Schema(description = "主数据对象", example = "")
    private String masterDataObject;

    @Excel(name = "任务下发人ID")
    @Schema(description = "任务下发人ID", example = "")
    private Long taskSendId;

    @Excel(name = "任务处理人ID")
    @Schema(description = "任务处理人ID", example = "")
    private Long taskHandlerId;

    @Schema(description = "整改人手机号")
    private String handlerPhone;

    @Excel(name = "下发时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下发时间", example = "")
    private Date sendTime;

    @Excel(name = "处理时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间", example = "")
    private Date handlerTime;

    @Excel(name = "整改说明")
    @Schema(description = "整改说明", example = "")
    private String description;

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

}
