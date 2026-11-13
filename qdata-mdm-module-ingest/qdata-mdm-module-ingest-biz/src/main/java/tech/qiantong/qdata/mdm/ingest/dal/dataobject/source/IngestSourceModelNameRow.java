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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.source;

import lombok.Data;

import java.util.Date;

/**
 * 来源系统关联主数据模型名称
 *
 * @author chentangwen
 */
@Data
public class IngestSourceModelNameRow {
    private Long sourceId;
    private String modelName;
    private String modelCode;
    private String description;
    private String versionNo;
    private Long versionStatus;
    private Long responsibleUserId;
    private Long creatorId;
    private String createBy;
    private Date createTime;
    private Date updateTime;
    private Long modelId;
    private String icon;
    private String type;
    private Long parentId;
    private Long duplicateTaskCount;
    private Long qualityTaskCount;
}
