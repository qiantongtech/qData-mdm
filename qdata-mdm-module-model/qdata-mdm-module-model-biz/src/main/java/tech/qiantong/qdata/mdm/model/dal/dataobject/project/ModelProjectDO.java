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

package tech.qiantong.qdata.mdm.model.dal.dataobject.project;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 主数据域 DO 对象 MODEL_PROJECT
 *
 * @author qdata
 * @date 2026-09-15
 */
@Data
@TableName(value = "MODEL_PROJECT")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MODEL_PROJECT_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelProjectDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 主数据域编码
     */
    private String code;

    /**
     * 主数据域名称
     */
    private String name;

    /**
     * 主数据域图标路径，未配置时前端显示默认图标。
     */
    private String icon;

    /**
     * 负责人用户ID，关联系统用户。
     */
    private Long responsibleUserId;

    /**
     * 描述
     */
    private String description;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    @TableLogic
    private Boolean delFlag;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    /**
     * 关联的主数据模型总数
     */
    @TableField(exist = false)
    private String modelCount;

    /**
     * 关联的模型目录总数
     */
    @TableField(exist = false)
    private String layerCount;

    /**
     * 域下全部模型关联的未删除规则总数（匹配、生存、质量）。
     */
    @TableField(exist = false)
    private Long ruleCount;

    /**
     * 域下未删除的匹配规则数。
     */
    @TableField(exist = false)
    private Long matchRuleCount;

    /**
     * 域下未删除的生存规则数。
     */
    @TableField(exist = false)
    private Long survivorRuleCount;

    /**
     * 域下未删除的质量规则数。
     */
    @TableField(exist = false)
    private Long qualityRuleCount;

}
