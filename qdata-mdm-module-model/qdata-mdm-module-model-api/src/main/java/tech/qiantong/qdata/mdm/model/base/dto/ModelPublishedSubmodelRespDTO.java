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

package tech.qiantong.qdata.mdm.model.base.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 成功发布配置中的一个直接子模型，不提供更深层级或物化信息。
 */
@Data
public class ModelPublishedSubmodelRespDTO {
    /**
     * 子模型自身ID。
     */
    private Long id;
    /**
     * 所属主模型ID。
     */
    private Long parentId;
    /**
     * 成功发布时的子模型编码。
     */
    private String code;
    /**
     * 成功发布时的子模型名称。
     */
    private String name;
    /**
     * 成功发布时的模型类型。
     */
    private String type;
    /**
     * 成功发布时的说明。
     */
    private String description;
    /**
     * 正常使用的子模型字段，逻辑删除字段不返回。
     */
    private List<ModelPublishedColumnRespDTO> fields = new ArrayList<>();
}
