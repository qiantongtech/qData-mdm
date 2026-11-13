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

package tech.qiantong.qdata.mdm.model.controller.admin.layer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 模型目录层级 Request VO 对象 MODEL_LAYER
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "模型目录层级 Request VO")
@Data
public class ModelLayerPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "主数据域ID，MODEL_PROJECT.ID", example = "")
    private Long projectId;

    @Schema(description = "上级模型目录ID；0表示根目录", example = "")
    private Long parentId;

    @Schema(description = "模型目录编码", example = "")
    private String code;

    @Schema(description = "模型目录名称", example = "")
    private String name;

    /**
     * 负责人用户ID，关联系统用户。
     */
    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "同级目录排序号，按数值从小到大排列，默认0", example = "")
    private Long sortOrder;

    @Schema(description = "模型目录的业务分类范围说明", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;
}
