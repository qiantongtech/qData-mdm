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

package tech.qiantong.qdata.mdm.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述数据库里的一列，例如列名、类型、长度和是否可空。来源和权威各用自己的对象，停用列仍要保留。
 */
@Data
@NoArgsConstructor
public class ModelMaterializedColumnDefinition {

    /**
     * 业务列使用字段稳定ID匹配；系统列为空
     */
    private Long modelFieldId;

    /**
     * 系统列固定标识；不得用可改的列名代替稳定标识
     */
    private String systemFieldKey;

    /**
     * 物理列名，作为待比较属性
     */
    private String columnName;

    /**
     * 平台七类逻辑类型标识
     */
    private String logicalType;

    /**
     * 由common方言生成或从目标读取的数据库类型
     */
    private String physicalType;

    /**
     * 字符串长度，非字符串按适用情况为空
     */
    private Long characterLength;

    /**
     * CHAR或BYTE长度语义
     */
    private String lengthUnit;

    /**
     * 数字总精度
     */
    private Integer numericPrecision;

    /**
     * 数字小数位
     */
    private Integer numericScale;

    /**
     * 实际可空约束；来源业务列必须为true
     */
    private Boolean nullable;

    /**
     * 规范化后的静态默认值
     */
    private String defaultValue;

    /**
     * 是否主键列
     */
    private Boolean primaryKeyFlag;

    /**
     * 是否数据库自增；本期来源和权威id都由应用生成，此值为false
     */
    private Boolean identityFlag;

    /**
     * 列顺序
     */
    private Integer columnPosition;

    /**
     * 真实物理注释；业务中文修改不自动变更此值
     */
    private String columnComment;

    /**
     * 是否系统预制列
     */
    private Boolean systemFlag;

    /**
     * 这一列是否还参与正常业务；停用只是不再使用，数据库中的列和数据仍保留
     */
    private Boolean enabledFlag = Boolean.TRUE;
}

