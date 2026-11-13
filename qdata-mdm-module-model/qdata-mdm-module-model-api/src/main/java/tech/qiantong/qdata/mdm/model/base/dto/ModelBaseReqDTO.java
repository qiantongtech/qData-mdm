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

import lombok.*;

/**
 * 主数据模型基本信息 DTO 对象 MODEL_BASE
 *
 * @author qdata
 * @date 2026-09-18
 */
@Data
public class ModelBaseReqDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 主数据模型主键ID，数据库自增生成
     */
    private Long id;

    /**
     * 模型编码，例如MDM_SUPPLIER；业务上要求系统内唯一
     */
    private String code;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型图标路径，空字符串表示使用对应类型的默认图标。
     */
    private String icon;

    /**
     * 负责人用户ID，名称与联系方式由前端公共用户列表匹配。
     */
    private Long responsibleUserId;

    /**
     * 所属主数据域ID，关联MODEL_PROJECT.ID
     */
    private Long projectId;

    /**
     * 所属模型目录ID，关联MODEL_LAYER.ID；目录必须属于PROJECT_ID对应的主数据域
     */
    private Long layerId;

    /**
     * 模型类型；0：普通模型，1：分类模型
     */
    private String type;

    /**
     * 当前正式生效版本号，草稿模型尚未发布时为空，修订中仍保存当前正式版本号
     */
    private Long versionId;

    /**
     * 模型业务说明
     */
    private String description;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    private Boolean delFlag;

}
