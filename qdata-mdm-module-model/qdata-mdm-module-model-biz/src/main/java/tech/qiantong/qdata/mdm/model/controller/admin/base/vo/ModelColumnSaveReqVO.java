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
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 主数据模型字段定义 创建/修改 Request VO MODEL_COLUMN
 *
 * @author qdata
 * @date 2026-09-20
 */
@Schema(description = "主数据模型字段定义 Response VO")
@Data
public class ModelColumnSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "所属主数据模型ID，关联MODEL_BASE.ID", example = "")
    private Long modelId;

    @Schema(description = "字段业务名称", example = "")
    @Size(max = 512, message = "字段业务名称长度不能超过512个字符")
    private String name;

    @Schema(description = "字段编码", example = "")
    @Size(max = 512, message = "字段编码长度不能超过512个字符")
    private String code;

    @Schema(description = "字段分类；SYSTEM：系统预置字段，BUSINESS：普通业务字段，RELATION：普通关联字段，CLASS：分类模型关联字段", example = "")
    @Size(max = 512, message = "字段分类；SYSTEM：系统预置字段，BUSINESS：普通业务字段，RELATION：普通关联字段，CLASS：分类模型关联字段长度不能超过512个字符")
    private String columnCategory;

    @Schema(description = "平台逻辑数据类型；STRING、TEXT、INTEGER、DECIMAL、BOOLEAN、DATE、DATETIME", example = "")
    @Size(max = 512, message = "平台逻辑数据类型；STRING、TEXT、INTEGER、DECIMAL、BOOLEAN、DATE、DATETIME长度不能超过512个字符")
    private String dataType;

    @Schema(description = "字段长度；STRING表示字符长度，DECIMAL表示总有效数字位数，其他类型为空", example = "")
    private Long dataLength;

    @Schema(description = "小数位数，仅DECIMAL类型使用，不能大于DATA_LENGTH", example = "")
    private Long dataScale;

    @Schema(description = "是否为系统主键字段；0：否，1：是；仅系统预置的主数据ID允许为1", example = "")
    private Boolean primaryKeyFlag;

    @Schema(description = "是否必填；0：否，1：是", example = "")
    private Boolean requiredFlag;

    @Schema(description = "是否要求业务值唯一；0：否，1：是", example = "")
    private Boolean uniqueFlag;

    @Schema(description = "是否为主数据名称字段；0：否，1：是；每个模型必须且只能有一个", example = "")
    private Boolean nameFlag;

    @Schema(description = "字段默认值文本；保存和发布前必须校验其与DATA_TYPE兼容", example = "")
    @Size(max = 512, message = "字段默认值文本；保存和发布前必须校验其与DATA_TYPE兼容长度不能超过512个字符")
    private String defaultValue;

    @Schema(description = "关联的参考数据集合ID，关联MODEL_DICT_DATA.ID；未关联参考数据时为空，不指向具体字典数据项", example = "")
    private Long dictTypeId;

    @Schema(description = "当前绑定的编码方案版本ID，关联MODEL_ENCODING_VERSION.ID；未配置自动编码时为空", example = "")
    private Long encodingVersionId;

    @Schema(description = "编码方式；MANUAL：手工录入，AUTOMATIC：自动编码", example = "MANUAL")
    private String encodingMode;

    /** 自动编码参数绑定配置 JSON；手工录入时应为空。 */
    private String encodingRuleConfig;

    @Schema(description = "字段业务含义或使用说明", example = "")
    @Size(max = 512, message = "字段业务含义或使用说明长度不能超过512个字符")
    private String description;

    @Schema(description = "关联模型ID", example = "")
    private Long relatedModelId;

    @Schema(description = "关联字段", example = "")
    @Size(max = 512, message = "关联字段长度不能超过512个字符")
    private String relatedCode;

    @Schema(description = "展示字段", example = "")
    @Size(max = 512, message = "展示字段长度不能超过512个字符")
    private String displayCode;

    @Schema(description = "节点选择规则；ANY：任意节点，LEAF：仅叶子节点", example = "ANY")
    @Size(max = 32, message = "节点选择规则长度不能超过32个字符")
    private String nodeRule;

    @Schema(description = "展示方式；NAME：节点名称，FULL_PATH：完整路径", example = "NAME")
    @Size(max = 32, message = "展示方式长度不能超过32个字符")
    private String displayMode;


}
