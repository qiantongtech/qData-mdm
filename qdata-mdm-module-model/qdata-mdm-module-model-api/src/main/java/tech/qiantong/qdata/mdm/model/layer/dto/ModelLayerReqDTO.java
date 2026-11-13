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

package tech.qiantong.qdata.mdm.model.layer.dto;

import lombok.*;

/**
 * 模型目录层级 DTO 对象 MODEL_LAYER
 *
 * @author qdata
 * @date 2026-09-15
 */
@Data
public class ModelLayerReqDTO {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 主数据域ID，MODEL_PROJECT.ID
     */
    private Long projectId;

    /**
     * 上级模型目录ID；0表示根目录
     */
    private Long parentId;

    /**
     * 模型目录编码
     */
    private String code;

    /**
     * 模型目录名称
     */
    private String name;

    /**
     * 负责人用户ID，关联系统用户。
     */
    private Long responsibleUserId;

    /**
     * 同级目录排序号，按数值从小到大排列，默认0
     */
    private Long sortOrder;

    /**
     * 模型目录的业务分类范围说明
     */
    private String description;

    /**
     * 是否有效；0：无效，1：有效
     */
    private Boolean validFlag;

    /**
     * 删除标志；1：已删除，0：未删除
     */
    private Boolean delFlag;


}
