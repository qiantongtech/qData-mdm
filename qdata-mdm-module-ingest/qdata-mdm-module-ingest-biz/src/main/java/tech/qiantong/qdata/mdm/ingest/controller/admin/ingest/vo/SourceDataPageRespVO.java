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

package tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 来源数据列表响应。
 * <p>
 * 列表业务字段由 {@code modelData} 承载，前端依据 {@code modelCode}
 * 按当前主数据模型动态组织展示，不在接口层固化供应商、客户或物料字段。
 */
@Data
public class SourceDataPageRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long sourceSystemId;
    private String sourceSystemName;
    private String sourceSystemCode;
    private String sourceSystemIcon;
    private Long modelId;
    private String modelCode;
    private String modelName;
    private String modelIcon;
    private String modelType;
    private String modelDescription;
    private String modelVersionNo;
    private Long modelVersionStatus;
    private String sourceRecordKey;
    private String stableRecordId;
    private String batchNo;
    private Long currentVersionId;
    private Long taskId;
    private String taskName;
    private String taskCode;
    private String taskType;
    private String taskDescription;
    private String governanceStatus;
    private Date firstReceiveTime;
    private Date lastReceiveTime;
    private String modelData;
}
