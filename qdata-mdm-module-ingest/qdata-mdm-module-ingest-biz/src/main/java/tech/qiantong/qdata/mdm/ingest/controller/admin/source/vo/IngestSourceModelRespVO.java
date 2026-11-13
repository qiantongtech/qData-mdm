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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo;

import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** 来源系统关联模型展示信息。@author chentangwen */
@Data
public class IngestSourceModelRespVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;
    private String modelName;
    private String modelCode;
    private String description;
    private String versionNo;
    private Long versionStatus;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long responsibleUserId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;
    private String createBy;
    private Date createTime;
    private Date updateTime;
    private String icon;
    private String type;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
}
