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

package tech.qiantong.qdata.mdm.dist.controller.admin.service.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 数据服务调用日志 Request VO 对象 DIST_SERVICE_CALL_LOG
 *
 * @author chentangwen
 * @date 2026-10-21
 */
@Schema(description = "数据服务调用日志 Request VO")
@Data
public class DistServiceCallLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "数据服务ID", example = "")
    private Long serviceId;

    @Schema(description = "数据服务名称快照", example = "")
    private String serviceName;

    @Schema(description = "服务授权记录ID", example = "")
    private Long authorizationId;

    @Schema(description = "授权单号快照", example = "")
    private String authorizationNo;

    @Schema(description = "授权主体类型", example = "")
    private String subjectType;

    @Schema(description = "授权主体ID", example = "")
    private Long subjectId;

    @Schema(description = "调用系统名称", example = "")
    private String callerSystem;

    @Schema(description = "调用方IP地址", example = "")
    private String callerIp;

    @Schema(description = "调用接口地址", example = "")
    private String requestPath;

    @Schema(description = "HTTP请求方式", example = "")
    private String requestMethod;

    @Schema(description = "脱敏后的调用参数摘要", example = "")
    private String requestSummary;

    @Schema(description = "HTTP响应状态码", example = "")
    private Long httpStatusCode;

    @Schema(description = "调用结果：SUCCESS成功、FAILURE失败", example = "")
    private String callResult;

    @Schema(description = "本次响应数据条数", example = "")
    private Long responseDataCount;

    @Schema(description = "脱敏后的响应内容摘要", example = "")
    private String responseSummary;

    @Schema(description = "异常信息", example = "")
    private String errorMessage;

    @Schema(description = "调用耗时，单位毫秒", example = "")
    private Long durationMs;

    @Schema(description = "调用时间", example = "")
    private Date callTime;

    @Schema(description = "请求ID", example = "")
    private String requestId;

    @Schema(description = "链路追踪ID", example = "")
    private String traceId;




}
