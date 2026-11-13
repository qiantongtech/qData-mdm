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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

/** 来源追溯详情；来源字段随模型变化。 */
@Data
public class AuditSourceTraceRespVO {
    private String id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelVersionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long masterRecordId;
    private String model;
    private String modelCode;
    private String modelType;
    private String modelIcon;
    private String modelDescription;
    private String modelVersionNo;
    private String projectName;
    private String projectIcon;
    private String layerName;
    private String name;
    private String code;
    private String mode;
    private String systems;
    private String sourceRecordId;
    private List<String> sourceRecordIds;
    private String updated;
    private String lifecycle;
    private List<AuditSourceAuthorityFieldRespVO> fields;
    private List<AuditSourceRecordRespVO> records;
}
