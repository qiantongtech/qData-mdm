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
 * 来源数据详情。
 */
@Data
public class SourceDataDetailRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long sourceSystemId;
    private String sourceSystemName;
    private Long modelId;
    private String modelCode;
    private String modelName;
    private String modelIcon;
    private String modelType;
    private String modelVersionNo;
    private Long modelVersionStatus;
    private String sourceRecordKey;
    private String stableRecordId;
    private Long currentVersionId;
    private String governanceStatus;
    private Date firstReceiveTime;
    private Date lastReceiveTime;
    private Integer versionNo;
    private Long taskId;
    private String taskName;
    private Long taskRunId;
    private String batchNo;
    private String delFlag;
    private String triggerType;
    private String taskRunStatus;
    private Date taskStartTime;
    private Date taskEndTime;
    private Long totalCount;
    private Long successCount;
    private Long failureCount;
    private Long skipCount;
    private Date sourceUpdateTime;
    private String receiveTime;
    private String rawData;
    private String modelData;
}
