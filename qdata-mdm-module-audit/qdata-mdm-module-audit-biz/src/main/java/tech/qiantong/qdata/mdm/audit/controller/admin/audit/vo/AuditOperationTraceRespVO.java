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

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 操作审计列表行和详情。 */
@Data
public class AuditOperationTraceRespVO {
    private Long id;
    /** 来源表类型与来源主键共同标识事件，防止跨表ID相同。 */
    private String sourceType;
    private String eventKey;
    private Long sourceId;
    private Long modelId;
    private Long modelVersionId;
    private Long masterRecordId;
    private Long changeId;
    private String modelDisplayName;
    private String modelBusinessCode;
    private String modelType;
    private String modelIcon;
    private String modelDescription;
    private String modelVersionNo;
    private String projectName;
    private String projectIcon;
    private String layerName;
    private String masterDataName;
    private String masterDataCode;
    private String masterLifecycle;
    private String masterVersion;
    private String scope;
    private String operateId;
    private String objectType;
    private String objectId;
    private String objectName;
    private String modelName;
    private String modelCode;
    private String type;
    private String operatorName;
    private String operatorPhone;
    private String operateResult;
    private String operateTime;
    private String businessScope;
    private String functionName;
    private String moduleCode;
    private Integer affectCount;
    private List<Map<String, Object>> affectedRows;
    private String modelVersion;
    private String operateDesc;
}
