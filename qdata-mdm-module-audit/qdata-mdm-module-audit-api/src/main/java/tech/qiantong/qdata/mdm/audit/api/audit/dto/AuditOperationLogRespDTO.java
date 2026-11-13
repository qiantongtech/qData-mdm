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

package tech.qiantong.qdata.mdm.audit.api.audit.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

/**
 * 业务操作审计 DTO 对象 AUDIT_OPERATION_LOG
 *
 * @author qdata
 * @date 2026-09-22
 */
@Data
public class AuditOperationLogRespDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 关联模型ID。 */
    private Long modelId;
    /** 本次操作涉及的模型版本ID。 */
    private Long modelVersionId;
    /** 与模型ID共同定位权威主数据。 */
    private Long masterRecordId;
    /** 关联本次变更，非请求追踪ID。 */
    private Long changeId;

    /** 业务模块编码 */
    private String moduleCode;

    /** 功能编码 */
    private String functionCode;

    /** 业务对象类型 */
    private String objectType;

    /** 业务对象ID */
    private String objectId;

    /** 业务对象名称 */
    private String objectName;

    /** 操作类型 */
    private String type;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作结果 */
    private String operateResult;

    /** 操作时间 */
    private Date operateTime;

    /** 请求IP */
    private String requestIp;

    /** 请求ID */
    private String requestId;

    /** 链路追踪ID */
    private String traceId;

    /** 错误信息 */
    private String errorMessage;

    /** 是否有效：0：无效，1：有效 */
    private String validFlag;

    /** 删除标志：0：未删除，1：已删除 */
    private String delFlag;


}

