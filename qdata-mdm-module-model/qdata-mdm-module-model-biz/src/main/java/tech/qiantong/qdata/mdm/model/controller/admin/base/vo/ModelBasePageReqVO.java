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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.List;

/**
 * 主数据模型基本信息 Request VO 对象 MODEL_BASE
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "主数据模型基本信息 Request VO")
@Data
public class ModelBasePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "模型编码，例如MDM_SUPPLIER；业务上要求系统内唯一", example = "")
    private String code;

    @Schema(description = "模型名称", example = "")
    private String name;

    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "所属主数据域ID，关联MODEL_PROJECT.ID", example = "")
    private Long projectId;

    @Schema(description = "所属模型目录ID，关联MODEL_LAYER.ID；目录必须属于PROJECT_ID对应的主数据域", example = "")
    private Long layerId;

    @Schema(description = "是否包含当前模型目录的全部后代目录，默认否")
    private Boolean includeDescendantLayers;

    /**
     * 服务端按当前目录展开的查询范围，每次查询重新计算。
     */
    @Schema(hidden = true)
    private List<Long> scopeLayerIds;

    @Schema(description = "模型类型；0：普通模型，1：分类模型", example = "")
    private String type;

    @Schema(description = "版本id", example = "")
    private Long versionId;

    @Schema(description = "模型业务说明", example = "")
    private String description;

    @Schema(description = "上级模型ID；0表示主模型", example = "")
    private Long parentId;

    @Schema(description = "逻辑删除标志；0：未删除，1：已删除", example = "")
    private Boolean delFlag;

    @Schema(description = "模型状态；0：草稿，1：已发布，2：修订中，3：已封存，4：已删除", example = "")
    private Long status;

}
