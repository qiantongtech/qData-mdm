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

package tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 业务操作审计 Request VO 对象 AUDIT_OPERATION_LOG
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "业务操作审计 Request VO")
@Data
public class AuditOperationLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "业务模块编码", example = "")
    private String moduleCode;

    @Schema(description = "功能编码", example = "")
    private String functionCode;

    @Schema(description = "业务对象类型", example = "")
    private String objectType;

    @Schema(description = "业务对象ID", example = "")
    private String objectId;

    @Schema(description = "业务对象名称", example = "")
    private String objectName;

    @Schema(description = "操作类型", example = "")
    private String type;

    @Schema(description = "操作人ID", example = "")
    private Long operatorId;

    @Schema(description = "操作人名称", example = "")
    private String operatorName;

    @Schema(description = "操作结果", example = "")
    private String operateResult;

    @Schema(description = "操作时间", example = "")
    private Date operateTime;

    @Schema(description = "请求IP", example = "")
    private String requestIp;

    @Schema(description = "请求ID", example = "")
    private String requestId;

    @Schema(description = "链路追踪ID", example = "")
    private String traceId;

    @Schema(description = "错误信息", example = "")
    private String errorMessage;




}

