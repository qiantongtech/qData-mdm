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

import java.io.Serializable;

@Data
@Schema(description = "模型关系图谱查询 Request VO")
public class ModelRelationGraphReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "起始模型 ID；为空时按其他查询条件选择起始模型")
    private Long id;

    @Schema(description = "模型名称，支持模糊查询")
    private String name;

    @Schema(description = "模型编码")
    private String code;

    @Schema(description = "数据域 ID")
    private Long projectId;

    @Schema(description = "模型目录 ID，包含后代目录")
    private Long layerId;

    @Schema(description = "关系类型；CHILD：主子关系，REFERENCE：引用关系；为空：全部关系")
    private String relationType;
}
