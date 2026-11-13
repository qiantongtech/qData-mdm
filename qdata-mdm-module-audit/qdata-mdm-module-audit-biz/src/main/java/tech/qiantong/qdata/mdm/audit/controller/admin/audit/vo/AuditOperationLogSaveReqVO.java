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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 业务操作审计 创建/修改 Request VO AUDIT_OPERATION_LOG
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "业务操作审计 Response VO")
@Data
public class AuditOperationLogSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;
    /** 关联模型ID。 */
    private Long modelId;
    /** 本次操作涉及的模型版本ID。 */
    private Long modelVersionId;
    /** 与模型ID共同定位权威主数据。 */
    private Long masterRecordId;
    /** 关联本次变更，非请求追踪ID。 */
    private Long changeId;

    @Schema(description = "业务模块编码", example = "")
    @Size(max = 1, message = "业务模块编码长度不能超过1个字符")
    private String moduleCode;

    @Schema(description = "功能编码", example = "")
    @Size(max = 64, message = "功能编码长度不能超过64个字符")
    private String functionCode;

    @Schema(description = "业务对象类型", example = "")
    @Size(max = 1, message = "业务对象类型长度不能超过1个字符")
    private String objectType;

    @Schema(description = "业务对象ID", example = "")
    @Size(max = 64, message = "业务对象ID长度不能超过64个字符")
    private String objectId;

    @Schema(description = "业务对象名称", example = "")
    @Size(max = 128, message = "业务对象名称长度不能超过128个字符")
    private String objectName;

    @Schema(description = "操作类型", example = "")
    @Size(max = 1, message = "操作类型长度不能超过1个字符")
    private String type;

    @Schema(description = "操作人ID", example = "")
    private Long operatorId;

    @Schema(description = "操作人名称", example = "")
    @Size(max = 64, message = "操作人名称长度不能超过64个字符")
    private String operatorName;

    @Schema(description = "操作结果", example = "")
    @Size(max = 1, message = "操作结果长度不能超过1个字符")
    private String operateResult;

    @Schema(description = "操作时间", example = "")
    private Date operateTime;

    @Schema(description = "请求IP", example = "")
    @Size(max = 512, message = "请求IP长度不能超过512个字符")
    private String requestIp;

    @Schema(description = "请求ID", example = "")
    @Size(max = 512, message = "请求ID长度不能超过512个字符")
    private String requestId;

    @Schema(description = "链路追踪ID", example = "")
    @Size(max = 512, message = "链路追踪ID长度不能超过512个字符")
    private String traceId;

    @Schema(description = "错误信息", example = "")
    @Size(max = 512, message = "错误信息长度不能超过512个字符")
    private String errorMessage;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}

