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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.source;

import lombok.Data;

import java.util.Date;

/**
 * 来源系统关联模型的查询结果，包含模型基础信息、版本信息及任务统计。
 *
 * @author chentangwen
 */
@Data
public class IngestSourceModelNameRow {

    /**
     * 来源系统 ID。
     */
    private Long sourceId;

    /**
     * 模型名称。
     */
    private String modelName;

    /**
     * 模型编码。
     */
    private String modelCode;

    /**
     * 模型业务说明。
     */
    private String description;

    /**
     * 模型最近一次成功发布的版本号；子模型取所属主模型的发布版本。
     */
    private String versionNo;

    /**
     * 模型当前版本状态；子模型未单独关联版本时取上级模型的版本状态。
     */
    private Long versionStatus;

    /**
     * 模型负责人用户 ID。
     */
    private Long responsibleUserId;

    /**
     * 模型创建人用户 ID。
     */
    private Long creatorId;

    /**
     * 模型创建人名称。
     */
    private String createBy;

    /**
     * 模型创建时间。
     */
    private Date createTime;

    /**
     * 模型最近更新时间。
     */
    private Date updateTime;

    /**
     * 关联模型 ID。
     */
    private Long modelId;

    /**
     * 模型图标路径。
     */
    private String icon;

    /**
     * 模型类型：0 表示普通模型，1 表示分类模型。
     */
    private String type;

    /**
     * 上级模型 ID。
     */
    private Long parentId;

    /**
     * 该模型下未删除的重复检测任务数量。
     */
    private Long duplicateTaskCount;

    /**
     * 该模型下未删除的质量任务数量。
     */
    private Long qualityTaskCount;
}
