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

package tech.qiantong.qdata.mdm.model.dal.dataobject.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;

import java.util.List;

/**
 * 主数据模型基本信息 DO 对象 MODEL_BASE
 *
 * @author qdata
 * @date 2026-09-18
 */
@Data
@TableName(value = "MODEL_BASE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MODEL_BASE_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelBaseDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主数据模型主键ID，数据库自增生成
     */
    @TableId(type = IdType.AUTO)
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
     * 所属主数据域展示信息，不属于模型表字段。
     */
    @TableField(exist = false)
    private String projectName;

    @TableField(exist = false)
    private String projectIcon;

    /**
     * 所属模型目录ID，关联MODEL_LAYER.ID；目录必须属于PROJECT_ID对应的主数据域
     */
    private Long layerId;

    /**
     * 模型类型；0：普通模型，1：分类模型
     */
    private String type;

    /**
     * 版本id
     */
    private Long versionId;

    /**
     * 模型业务说明
     */
    private String description;

    /**
     * 父级主数据模型id
     */
    private Long parentId;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    @TableLogic
    private Boolean delFlag;

    /**
     * 目录
     */
    @TableField(exist = false)
    private List<ModelLayerDO> layers;

    /**
     * 当前版本
     */
    @TableField(exist = false)
    private ModelVersionDO version;

    /**
     * 发布版本
     */
    @TableField(exist = false)
    private ModelVersionDO releaseVersion;

    /**
     * 模型字段定义；详情查询时由模型字段表补充，非 MODEL_BASE 表字段。
     */
    @TableField(exist = false)
    private List<ModelColumnDO> fields;

    /**
     * 子模型列表；详情查询时根据当前主模型 ID 关联 PARENT_ID 补充。
     */
    @TableField(exist = false)
    private List<ModelBaseDO> subModels;
    /**
     * 最近一次成功发布记录ID，仅由发布成功流程维护
     */
    private Long publishedInstanceId;


}
