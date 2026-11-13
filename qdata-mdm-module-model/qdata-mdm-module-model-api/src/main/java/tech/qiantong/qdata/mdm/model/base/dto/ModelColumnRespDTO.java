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
 * 主数据模型字段定义 DTO 对象 MODEL_COLUMN
 *
 * @author qdata
 * @date 2026-09-20
 */
@Data
public class ModelColumnRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 所属主数据模型ID，关联MODEL_BASE.ID */
    private Long modelId;

    /** 字段业务名称 */
    private String name;

    /** 字段编码 */
    private String code;

    /** 字段分类；SYSTEM：系统预置字段，BUSINESS：普通业务字段，RELATION：普通关联字段，CLASS：分类模型关联字段 */
    private String columnCategory;

    /** 平台逻辑数据类型；STRING、TEXT、INTEGER、DECIMAL、BOOLEAN、DATE、DATETIME */
    private String dataType;

    /** 字段长度；STRING表示字符长度，DECIMAL表示总有效数字位数，其他类型为空 */
    private Long dataLength;

    /** 小数位数，仅DECIMAL类型使用，不能大于DATA_LENGTH */
    private Long dataScale;

    /** 是否为系统主键字段；0：否，1：是；仅系统预置的主数据ID允许为1 */
    private Boolean primaryKeyFlag;

    /** 是否必填；0：否，1：是 */
    private Boolean requiredFlag;

    /** 是否要求业务值唯一；0：否，1：是 */
    private Boolean uniqueFlag;

    /** 是否为主数据名称字段；0：否，1：是；每个模型必须且只能有一个 */
    private Boolean nameFlag;

    /** 字段默认值文本；保存和发布前必须校验其与DATA_TYPE兼容 */
    private String defaultValue;

    /** 关联的参考数据集合ID，关联MODEL_DICT_DATA.ID；未关联参考数据时为空，不指向具体字典数据项 */
    private Long dictTypeId;

    /** 当前绑定的编码方案版本ID，关联MODEL_ENCODING_VERSION.ID；未配置自动编码时为空 */
    private Long encodingVersionId;

    /** 编码方式；MANUAL：手工录入，AUTOMATIC：自动编码 */
    private String encodingMode;

    /** 自动编码参数绑定配置 JSON。 */
    private String encodingRuleConfig;

    /** 字段业务含义或使用说明 */
    private String description;

    /** 字段状态；0：正常使用，1：停用/逻辑删除；保留物理字段及历史数据 */
    private Boolean delFlag;

    /** 关联模型ID */
    private Long relatedModelId;

    /** 关联模型名称 */
    private String relatedModelName;

    /** 关联模型所属主数据域ID */
    private Long relatedProjectId;

    /** 关联模型所属目录ID */
    private Long relatedLayerId;

    /** 关联字段 */
    private String relatedCode;

    /** 展示字段 */
    private String displayCode;

    /** 节点选择规则；ANY：任意节点，LEAF：仅叶子节点 */
    private String nodeRule;

    /** 展示方式；NAME：节点名称，FULL_PATH：完整路径 */
    private String displayMode;


}
