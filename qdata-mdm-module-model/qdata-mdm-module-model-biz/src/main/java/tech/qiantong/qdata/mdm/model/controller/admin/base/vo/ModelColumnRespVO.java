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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 主数据模型字段定义 Response VO 对象 MODEL_COLUMN
 *
 * @author qdata
 * @date 2026-09-20
 */
@Schema(description = "主数据模型字段定义 Response VO")
@Data
public class ModelColumnRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "所属主数据模型ID，关联MODEL_BASE.ID")
    @Schema(description = "所属主数据模型ID，关联MODEL_BASE.ID", example = "")
    private Long modelId;

    @Excel(name = "字段业务名称")
    @Schema(description = "字段业务名称", example = "")
    private String name;

    @Excel(name = "字段编码")
    @Schema(description = "字段编码", example = "")
    private String code;

    @Excel(name = "字段分类；SYSTEM：系统预置字段，BUSINESS：普通业务字段，RELATION：普通关联字段，CLASS：分类模型关联字段")
    @Schema(description = "字段分类；SYSTEM：系统预置字段，BUSINESS：普通业务字段，RELATION：普通关联字段，CLASS：分类模型关联字段", example = "")
    private String columnCategory;

    @Excel(name = "平台逻辑数据类型；STRING、TEXT、INTEGER、DECIMAL、BOOLEAN、DATE、DATETIME")
    @Schema(description = "平台逻辑数据类型；STRING、TEXT、INTEGER、DECIMAL、BOOLEAN、DATE、DATETIME", example = "")
    private String dataType;

    @Excel(name = "字段长度；STRING表示字符长度，DECIMAL表示总有效数字位数，其他类型为空")
    @Schema(description = "字段长度；STRING表示字符长度，DECIMAL表示总有效数字位数，其他类型为空", example = "")
    private Long dataLength;

    @Excel(name = "小数位数，仅DECIMAL类型使用，不能大于DATA_LENGTH")
    @Schema(description = "小数位数，仅DECIMAL类型使用，不能大于DATA_LENGTH", example = "")
    private Long dataScale;

    @Excel(name = "是否为系统主键字段；0：否，1：是；仅系统预置的主数据ID允许为1")
    @Schema(description = "是否为系统主键字段；0：否，1：是；仅系统预置的主数据ID允许为1", example = "")
    private Boolean primaryKeyFlag;

    @Excel(name = "是否必填；0：否，1：是")
    @Schema(description = "是否必填；0：否，1：是", example = "")
    private Boolean requiredFlag;

    @Excel(name = "是否要求业务值唯一；0：否，1：是")
    @Schema(description = "是否要求业务值唯一；0：否，1：是", example = "")
    private Boolean uniqueFlag;

    @Excel(name = "是否为主数据名称字段；0：否，1：是；每个模型必须且只能有一个")
    @Schema(description = "是否为主数据名称字段；0：否，1：是；每个模型必须且只能有一个", example = "")
    private Boolean nameFlag;

    @Excel(name = "字段默认值文本；保存和发布前必须校验其与DATA_TYPE兼容")
    @Schema(description = "字段默认值文本；保存和发布前必须校验其与DATA_TYPE兼容", example = "")
    private String defaultValue;

    @Excel(name = "关联的参考数据集合ID，关联MODEL_DICT_DATA.ID；未关联参考数据时为空，不指向具体字典数据项")
    @Schema(description = "关联的参考数据集合ID，关联MODEL_DICT_DATA.ID；未关联参考数据时为空，不指向具体字典数据项", example = "")
    private Long dictTypeId;

    @Excel(name = "当前绑定的编码方案版本ID，关联MODEL_ENCODING_VERSION.ID；未配置自动编码时为空")
    @Schema(description = "当前绑定的编码方案版本ID，关联MODEL_ENCODING_VERSION.ID；未配置自动编码时为空", example = "")
    private Long encodingVersionId;

    @Excel(name = "编码方式；MANUAL：手工录入，AUTOMATIC：自动编码")
    @Schema(description = "编码方式；MANUAL：手工录入，AUTOMATIC：自动编码", example = "MANUAL")
    private String encodingMode;

    /** 自动编码参数绑定配置 JSON。 */
    private String encodingRuleConfig;

    @Excel(name = "字段业务含义或使用说明")
    @Schema(description = "字段业务含义或使用说明", example = "")
    private String description;

    @Excel(name = "字段状态；0：正常使用，1：停用/逻辑删除；保留物理字段及历史数据")
    @Schema(description = "字段状态；0：正常使用，1：停用/逻辑删除；保留物理字段及历史数据", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人名称或账号")
    @Schema(description = "创建人名称或账号", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "最后更新人名称或账号")
    @Schema(description = "最后更新人名称或账号", example = "")
    private String updateBy;

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "最后更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后更新时间", example = "")
    private Date updateTime;

    @Excel(name = "关联模型ID")
    @Schema(description = "关联模型ID", example = "")
    private Long relatedModelId;

    @Schema(description = "关联模型名称", example = "")
    private String relatedModelName;

    @Schema(description = "关联模型所属主数据域ID", example = "")
    private Long relatedProjectId;

    @Schema(description = "关联模型所属目录ID", example = "")
    private Long relatedLayerId;

    @Excel(name = "关联字段")
    @Schema(description = "关联字段编码", example = "")
    private String relatedCode;

    @Excel(name = "展示字段")
    @Schema(description = "展示字段编码", example = "")
    private String displayCode;

    @Excel(name = "节点选择规则")
    @Schema(description = "节点选择规则；ANY：任意节点，LEAF：仅叶子节点", example = "ANY")
    private String nodeRule;

    @Excel(name = "展示方式")
    @Schema(description = "展示方式；NAME：节点名称，FULL_PATH：完整路径", example = "NAME")
    private String displayMode;

}
