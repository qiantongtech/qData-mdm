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

package tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 质量规则 DO 对象 RULE_QUALITY
 *
 * @author qdata
 * @date 2026-09-18
 */
@Data
@TableName(value = "RULE_QUALITY")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("RULE_QUALITY_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RuleQualityDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 主数据模型ID */
    private Long modelId;

    @TableField(exist = false)
    private String modelName;
    @TableField(exist = false)
    private String modelCode;
    @TableField(exist = false)
    private String modelType;
    @TableField(exist = false)
    private String modelIcon;
    @TableField(exist = false)
    private String modelDescription;

    /** 负责人用户 ID。 */
    private Long responsibleUserId;

    @TableField(exist = false)
    private String responsibleUserName;

    @TableField(exist = false)
    private String responsibleUserPhone;

    /** 主数据域名称，仅用于展示。 */
    @TableField(exist = false)
    private String projectName;

    /** 模型目录名称，仅用于展示。 */
    @TableField(exist = false)
    private String layerName;

    @TableField(exist = false)
    private Long qualityTaskCount;

    @TableField(exist = false)
    private Long mergeSnapshotCount;

    /** 主数据模型版本号 */
    private String modelVersion;

    /** 模型字段 */
    private String modelFieldId;

    /** 配置异常标志;0正常，1配置异常 */
    private String abnormalFlag;

    /** 质量规则类型 */
    private String ruleType;

    /** 校验规则JSON配置 */
    private String ruleConfig;

    /** 规则启用状态;0：无效停用，1：有效启用 */
    private Boolean validFlag;

    /** 规则说明 */
    private String description;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
