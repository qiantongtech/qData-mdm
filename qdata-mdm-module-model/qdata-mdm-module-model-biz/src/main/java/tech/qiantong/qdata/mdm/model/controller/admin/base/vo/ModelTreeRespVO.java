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
import java.util.ArrayList;
import java.util.List;

/** 模型树固定三层：主数据域、模型目录、已发布主模型。 */
@Data
@Schema(description = "已发布模型树节点")
public class ModelTreeRespVO {
    @Schema(description = "当前域、目录或模型的原始ID")
    private Long id;
    @Schema(description = "父节点的原始ID，主数据域为空")
    private Long parentId;
    @Schema(description = "节点类型：project、layer、model")
    private String nodeType;
    private String name;
    @Schema(description = "主数据域图标路径，未上传时为空")
    private String icon;
    private String code;
    private Long projectId;
    private Long layerId;
    @Schema(description = "模型节点的真实模型ID")
    private Long modelId;
    private Long instanceId;
    private Long versionId;
    private String versionNo;
    private List<ModelTreeRespVO> children = new ArrayList<>();
}
