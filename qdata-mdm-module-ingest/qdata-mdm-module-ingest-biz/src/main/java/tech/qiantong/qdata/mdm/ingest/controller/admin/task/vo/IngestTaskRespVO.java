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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class IngestTaskRespVO extends IngestTaskSaveReqVO {
    /**
     * 服务端执行快照，不作为页面可编辑配置。
     */
    @JsonIgnore
    private Long dbConfigId;
    @JsonIgnore
    private String currentIncrementValue;
    private String sourceName;
    private String sourceCode;
    private String sourceIcon;
    private String modelIcon;
    private String modelType;
    private String modelDescription;
    private String modelVersionNo;
    private Long modelVersionStatus;
    private Date lastRunEndTime;
    private Long lastRunDurationMillis;
    private Integer sourceTrustScore;
    private String modelName;
    /**
     * 持久化调度开关，供列表回显；调度同步异常通过服务日志报告。
     */
    private String schedulerStatus;
    private String responsiblePersonName;
    private String responsiblePersonPhone;
    private String createPhoneNumber;
    private String lastRunStatus;
    private Date lastRunTime;
    private String modelCode;
    private String dataFetch;
    @JsonIgnore
    private String datasourceName;
    @JsonIgnore
    private String fetchSourceObject;
    @JsonIgnore
    private String fetchFileName;
    @JsonIgnore
    private String fetchApiUrl;
    @JsonIgnore
    private String fetchHttpMethod;
}
