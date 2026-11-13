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

import java.util.Date;

/**
 * 成功物化字段定义返回信息。
 */
@Data
public class ModelMaterializedColumnRespDTO {

    /**
     * 成功物化字段定义ID
     */
    private Long id;

    /**
     * 所属成功物化表记录
     */
    private Long materializedTableId;

    /**
     * 业务字段稳定ID，业务字段必须填
     */
    private Long modelFieldId;

    /**
     * 系统列固定标识，如ID/PARENT_ID/CATEGORY_ID
     */
    private String systemFieldKey;

    /**
     * 数据库真实列名
     */
    private String columnName;

    /**
     * 平台统一类型STRING/TEXT/INTEGER/DECIMAL/DATE/DATETIME/BOOLEAN，用于模型比较
     */
    private String logicalType;

    /**
     * 真实数据库类型，如VARCHAR2/NUMBER/TIMESTAMP，用于结构核验
     */
    private String physicalType;

    /**
     * 实际字符串长度，其他类型按适用情况为空
     */
    private Long characterLength;

    /**
     * CHAR/BYTE长度语义，不适用为空
     */
    private String lengthUnit;

    /**
     * 实际数字精度
     */
    private Integer numericPrecision;

    /**
     * 实际数字小数位
     */
    private Integer numericScale;

    /**
     * 1允许空、0非空，保存真实约束
     */
    private Boolean nullable;

    /**
     * 实际默认值，无默认值为SQL NULL；不与文本NULL混淆
     */
    private String defaultValue;

    /**
     * 是否主键成员，顺序见表的索引JSON
     */
    private Boolean primaryKeyFlag;

    /**
     * 是否数据库自增/identity，按元数据核验
     */
    private Boolean identityFlag;

    /**
     * 数据库实际列序号，不是页面顺序
     */
    private Integer columnPosition;

    /**
     * 数据库真实列注释
     */
    private String columnComment;

    /**
     * 1系统列、0业务列
     */
    private Boolean systemFlag;

    /**
     * 本次成功版本启用状态，停用列继续保留
     */
    private Boolean enabledFlag;

    /**
     * 系统有效标志，1有效、0无效
     */
    private Boolean validFlag;

    /**
     * 系统逻辑删除标志，0未删除、1已删除；不删除实际物理表或字段
     */
    private Boolean delFlag;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新人ID
     */
    private Long updaterId;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}
