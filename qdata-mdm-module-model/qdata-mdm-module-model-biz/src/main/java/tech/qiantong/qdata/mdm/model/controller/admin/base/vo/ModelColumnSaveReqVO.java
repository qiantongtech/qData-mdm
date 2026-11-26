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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

/**
 * 主数据模型字段定义 创建/修改 Request VO MODEL_COLUMN
 *
 * @author qdata
 * @date 2026-09-20
 */
@Schema(description = "主数据模型字段定义 Response VO")
@Data
public class ModelColumnSaveReqVO extends BaseEntity {

    /** 模型草稿保存时，各类字段都必须提供的基本信息。 */
    public interface Required {}

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "所属主数据模型ID，关联MODEL_BASE.ID", example = "")
    private Long modelId;

    @Schema(description = "字段业务名称", example = "")
    @NotBlank(message = "字段名称不能为空", groups = {Default.class, Required.class})
    @Size(max = 512, message = "字段业务名称长度不能超过512个字符")
    private String name;

    @Schema(description = "字段编码", example = "")
    @NotBlank(message = "字段编码不能为空", groups = {Default.class, Required.class})
    @Size(max = 512, message = "字段编码长度不能超过512个字符")
    private String code;

    @Schema(description = "字段分类", example = "")
    @NotBlank(message = "字段类别不能为空", groups = {Default.class, Required.class})
    @Size(max = 512, message = "字段分类模型关联字段长度不能超过512个字符")
    private String columnCategory;

    @Schema(description = "平台逻辑数据类型；", example = "")
    @NotBlank(message = "数据类型不能为空", groups = {Default.class, Required.class})
    @Size(max = 512, message = "平台逻辑数据类型长度不能超过512个字符")
    private String dataType;

    @Schema(description = "字段长度", example = "")
    private Long dataLength;

    @Schema(description = "小数位数", example = "")
    private Long dataScale;

    @Schema(description = "是否为系统主键字段", example = "")
    private Boolean primaryKeyFlag;

    @Schema(description = "是否必填", example = "")
    private Boolean requiredFlag;

    @Schema(description = "是否要求业务值唯一", example = "")
    private Boolean uniqueFlag;

    @Schema(description = "是否为主数据名称字段", example = "")
    private Boolean nameFlag;

    @Schema(description = "字段默认值文本", example = "")
    @Size(max = 512, message = "字段默认值文本兼容长度不能超过512个字符")
    private String defaultValue;

    @Schema(description = "关联的参考数据项", example = "")
    private Long dictTypeId;

    @Schema(description = "当前绑定的编码方案", example = "")
    private Long encodingVersionId;

    @Schema(description = "编码方式", example = "MANUAL")
    private String encodingMode;

    /** 自动编码参数绑定配置 JSON；手工录入时应为空。 */
    private String encodingRuleConfig;

    @Schema(description = "字段说明", example = "")
    @Size(max = 512, message = "字段使用说明长度不能超过512个字符")
    private String description;

    @Schema(description = "关联模型ID", example = "")
    private Long relatedModelId;

    @Schema(description = "关联字段", example = "")
    @Size(max = 512, message = "关联字段长度不能超过512个字符")
    private String relatedCode;

    @Schema(description = "展示字段", example = "")
    @Size(max = 512, message = "展示字段长度不能超过512个字符")
    private String displayCode;

    @Schema(description = "节点选择规则", example = "ANY")
    @Size(max = 32, message = "节点选择规则长度不能超过32个字符")
    private String nodeRule;

    @Schema(description = "展示方式", example = "NAME")
    @Size(max = 32, message = "展示方式长度不能超过32个字符")
    private String displayMode;


}
