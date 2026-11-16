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
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 分发任务执行日志 Response VO 对象 DIST_TASK_INSTANCE_LOG
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务执行日志 Response VO")
@Data
public class DistTaskInstanceLogRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "分发任务运行实例ID")
    @Schema(description = "分发任务运行实例ID", example = "")
    private Long instanceId;

    @Excel(name = "权威主数据记录ID")
    @Schema(description = "权威主数据记录ID", example = "")
    private Long masterRecordId;

    @Excel(name = "主数据编码快照")
    @Schema(description = "主数据编码快照", example = "")
    private String masterCode;

    @Excel(name = "主数据名称快照")
    @Schema(description = "主数据名称快照", example = "")
    private String masterName;

    @Excel(name = "分发状态：SUCCESS成功、FAILURE失败")
    @Schema(description = "分发状态：SUCCESS成功、FAILURE失败", example = "")
    private String distributeStatus;

    @Excel(name = "本条数据分发时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "本条数据分发时间", example = "")
    private Date distributeTime;

    @Excel(name = "分发请求数据JSON")
    @Schema(description = "分发请求数据JSON", example = "")
    private String requestData;

    @Excel(name = "目标系统响应编码")
    @Schema(description = "目标系统响应编码", example = "")
    private String responseCode;

    @Excel(name = "目标系统响应信息")
    @Schema(description = "目标系统响应信息", example = "")
    private String responseMessage;

    @Excel(name = "失败原因")
    @Schema(description = "失败原因", example = "")
    private String failReason;

    @Excel(name = "重试次数")
    @Schema(description = "重试次数", example = "")
    private Long retryCount;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Excel(name = "删除标志：0未删除，1已删除")
    @Schema(description = "删除标志：0未删除，1已删除", example = "")
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
