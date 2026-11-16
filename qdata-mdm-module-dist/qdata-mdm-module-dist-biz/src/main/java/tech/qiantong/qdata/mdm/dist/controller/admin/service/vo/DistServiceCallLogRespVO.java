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

package tech.qiantong.qdata.mdm.dist.controller.admin.service.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 数据服务调用日志 Response VO 对象 DIST_SERVICE_CALL_LOG
 *
 * @author chentangwen
 * @date 2026-10-21
 */
@Schema(description = "数据服务调用日志 Response VO")
@Data
public class DistServiceCallLogRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "数据服务ID")
    @Schema(description = "数据服务ID", example = "")
    private Long serviceId;

    @Excel(name = "数据服务名称快照")
    @Schema(description = "数据服务名称快照", example = "")
    private String serviceName;

    @Excel(name = "服务授权记录ID")
    @Schema(description = "服务授权记录ID", example = "")
    private Long authorizationId;

    @Excel(name = "授权单号快照")
    @Schema(description = "授权单号快照", example = "")
    private String authorizationNo;

    @Excel(name = "授权主体类型")
    @Schema(description = "授权主体类型", example = "")
    private String subjectType;

    @Excel(name = "授权主体ID")
    @Schema(description = "授权主体ID", example = "")
    private Long subjectId;

    @Excel(name = "调用系统名称")
    @Schema(description = "调用系统名称", example = "")
    private String callerSystem;

    @Excel(name = "调用方IP地址")
    @Schema(description = "调用方IP地址", example = "")
    private String callerIp;

    @Excel(name = "调用接口地址")
    @Schema(description = "调用接口地址", example = "")
    private String requestPath;

    @Excel(name = "HTTP请求方式")
    @Schema(description = "HTTP请求方式", example = "")
    private String requestMethod;

    @Excel(name = "脱敏后的调用参数摘要")
    @Schema(description = "脱敏后的调用参数摘要", example = "")
    private String requestSummary;

    @Excel(name = "HTTP响应状态码")
    @Schema(description = "HTTP响应状态码", example = "")
    private Long httpStatusCode;

    @Excel(name = "调用结果：SUCCESS成功、FAILURE失败")
    @Schema(description = "调用结果：SUCCESS成功、FAILURE失败", example = "")
    private String callResult;

    @Excel(name = "本次响应数据条数")
    @Schema(description = "本次响应数据条数", example = "")
    private Long responseDataCount;

    @Excel(name = "脱敏后的响应内容摘要")
    @Schema(description = "脱敏后的响应内容摘要", example = "")
    private String responseSummary;

    @Excel(name = "异常信息")
    @Schema(description = "异常信息", example = "")
    private String errorMessage;

    @Excel(name = "调用耗时，单位毫秒")
    @Schema(description = "调用耗时，单位毫秒", example = "")
    private Long durationMs;

    @Excel(name = "调用时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "调用时间", example = "")
    private Date callTime;

    @Excel(name = "请求ID")
    @Schema(description = "请求ID", example = "")
    private String requestId;

    @Excel(name = "链路追踪ID")
    @Schema(description = "链路追踪ID", example = "")
    private String traceId;

    @Excel(name = "是否有效：0无效，1有效")
    @Schema(description = "是否有效：0无效，1有效", example = "")
    private String validFlag;

    @Excel(name = "删除标志：0未删除，1已删除")
    @Schema(description = "删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Excel(name = "创建人名称或账号")
    @Schema(description = "创建人名称或账号", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "最后更新人名称或账号")
    @Schema(description = "最后更新人名称或账号", example = "")
    private String updateBy;

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "最后更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
