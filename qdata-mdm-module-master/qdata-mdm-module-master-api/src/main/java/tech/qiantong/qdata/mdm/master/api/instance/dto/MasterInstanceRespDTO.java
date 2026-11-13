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

package tech.qiantong.qdata.mdm.master.api.instance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 权威主数据入口卡片所需的物化实例元数据。
 */
@Data
public class MasterInstanceRespDTO {

    /** 物化实例 ID */
    private Long instanceId;

    /** 模型 ID */
    private Long modelId;

    /** 卡片对应的成功发布版本 ID。 */
    private Long modelVersionId;

    /** 卡片对应的成功发布版本号。 */
    private String versionNo;

    /** 模型编码 */
    private String modelCode;

    /** 模型名称 */
    private String modelName;

    /** 模型业务说明 */
    private String modelDescription;

    /** 模型类型：NORMAL、CATEGORY */
    private String modelType;

    /** 成功发布时所属主数据域ID。 */
    private Long projectId;

    /** 成功发布时所属模型目录ID。 */
    private Long layerId;

    /** 模型目录名称 */
    private String layerName;

    /** 主数据域名称 */
    private String domainName;

    /** 物理表数据量 */
    private Long dataCount;

    /** 物理表数据量是否统计成功 */
    private Boolean dataCountAvailable;

    /** 生命周期状态分布：1 有效、2 冻结、3 失效、4 归档 */
    private Long activeCount;
    private Long frozenCount;
    private Long invalidCount;
    private Long archivedCount;

    /** 物理表最近业务更新时间；读取已发布系统字段 UPDATED_TIME */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestDataUpdateTime;

    /** 当前模型从权威主数据直接发起、已提交且审批中的维护单数；来源不可用时为空 */
    private Long pendingMaintenanceApprovalCount;
}
