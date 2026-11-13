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

package tech.qiantong.qdata.mdm.master.api.instance.dto;

import lombok.Data;

/**
 * 当前模型字段的只读配置。
 */
@Data
public class MasterFieldRespDTO {
    /** 主键 ID。 */
    private Long id;
    /** 字段编码。 */
    private String code;
    /** 名称。 */
    private String name;
    /** 字段注释，可与展示名称分别提供。 */
    private String comment;
    /** 字段类别。 */
    private String category;
    private String dataType;
    /** 是否为模型名称字段。 */
    private Boolean nameField;
    /** 是否必填。 */
    private Boolean required;
    /** 是否要求唯一。 */
    private Boolean unique;
    private Long dataLength;
    private Long dataScale;
    /** 字段默认值。 */
    private String defaultValue;
    private Integer sortOrder;
    /** 成功发布配置中的参考数据类型ID。 */
    private Long dictTypeId;
    /** 成功发布配置中的编码方案版本ID。 */
    private Long encodingVersionId;
    /** 成功发布的分类/关联配置。 */
    private Long relatedModelId;
    private String relatedCode;
    private String displayCode;
    private String nodeRule;
    private String displayMode;
}
