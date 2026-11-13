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
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 业务操作审计 Response VO 对象 AUDIT_OPERATION_LOG
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "业务操作审计 Response VO")
@Data
public class AuditOperationLogRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
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

    @Excel(name = "业务模块编码")
    @Schema(description = "业务模块编码", example = "")
    private String moduleCode;

    @Excel(name = "功能编码")
    @Schema(description = "功能编码", example = "")
    private String functionCode;

    @Excel(name = "业务对象类型")
    @Schema(description = "业务对象类型", example = "")
    private String objectType;

    @Excel(name = "业务对象ID")
    @Schema(description = "业务对象ID", example = "")
    private String objectId;

    @Excel(name = "业务对象名称")
    @Schema(description = "业务对象名称", example = "")
    private String objectName;

    @Excel(name = "操作类型")
    @Schema(description = "操作类型", example = "")
    private String type;

    @Excel(name = "操作人ID")
    @Schema(description = "操作人ID", example = "")
    private Long operatorId;

    @Excel(name = "操作人名称")
    @Schema(description = "操作人名称", example = "")
    private String operatorName;

    @Excel(name = "操作结果")
    @Schema(description = "操作结果", example = "")
    private String operateResult;

    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作时间", example = "")
    private Date operateTime;

    @Excel(name = "请求IP")
    @Schema(description = "请求IP", example = "")
    private String requestIp;

    @Excel(name = "请求ID")
    @Schema(description = "请求ID", example = "")
    private String requestId;

    @Excel(name = "链路追踪ID")
    @Schema(description = "链路追踪ID", example = "")
    private String traceId;

    @Excel(name = "错误信息")
    @Schema(description = "错误信息", example = "")
    private String errorMessage;

    @Excel(name = "是否有效：0：无效，1：有效")
    @Schema(description = "是否有效：0：无效，1：有效", example = "")
    private String validFlag;

    @Excel(name = "删除标志：0：未删除，1：已删除")
    @Schema(description = "删除标志：0：未删除，1：已删除", example = "")
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

