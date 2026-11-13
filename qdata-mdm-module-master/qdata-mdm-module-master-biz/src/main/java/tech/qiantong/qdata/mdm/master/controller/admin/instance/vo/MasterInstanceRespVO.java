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

package tech.qiantong.qdata.mdm.master.controller.admin.instance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 权威主数据入口卡片 Response VO。
 */
@Data
@Schema(description = "权威主数据入口 Response VO")
public class MasterInstanceRespVO {

    @Schema(description = "物化实例 ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long instanceId;

    @Schema(description = "模型 ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long modelId;

    @Schema(description = "成功发布版本 ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long modelVersionId;

    @Schema(description = "成功发布版本号")
    private String versionNo;

    @Schema(description = "模型编码")
    private String modelCode;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型业务说明")
    private String modelDescription;

    @Schema(description = "模型类型：NORMAL、CATEGORY")
    private String modelType;

    @Schema(description = "主数据域ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    @Schema(description = "模型目录ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long layerId;

    @Schema(description = "模型目录名称")
    private String layerName;

    @Schema(description = "主数据域名称")
    private String domainName;

    @Schema(description = "物理表数据量")
    private Long dataCount;

    @Schema(description = "物理表数据量是否统计成功")
    private Boolean dataCountAvailable;

    @Schema(description = "有效数据量")
    private Long activeCount;

    @Schema(description = "冻结数据量")
    private Long frozenCount;

    @Schema(description = "失效数据量")
    private Long invalidCount;

    @Schema(description = "归档数据量")
    private Long archivedCount;

    @Schema(description = "最近业务数据更新时间；读取已发布系统字段 UPDATED_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestDataUpdateTime;

    @Schema(description = "直接维护且审批中的申请单数；统计来源不可用时为空")
    private Long pendingMaintenanceApprovalCount;
}
