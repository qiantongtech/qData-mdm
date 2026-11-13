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
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSchemaObjectType;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;

/**
 * 差异及步骤的稳定对象定位；编码和物理名称只是属性，不作为身份。
 */
@Data
@NoArgsConstructor
public class ModelSchemaObjectRef {

    /**
     * 对象类型
     */
    private ModelSchemaObjectType objectType;

    /**
     * 当前模型节点ID
     */
    private Long modelId;

    /**
     * 主模型为0，子模型为所属主模型ID
     */
    private Long parentId;

    /**
     * 来源或权威；纯模型配置变化可为空
     */
    private ModelTableRole tableRole;

    /**
     * 模型/表使用固定用途，业务列FIELD:id、系统列SYSTEM:key，索引用规则ID或用途
     */
    private String objectKey;

    /**
     * 对应物理表名
     */
    private String tableName;

    /**
     * 涉及列时的物理列名
     */
    private String columnName;

    /**
     * 涉及索引时的物理索引名
     */
    private String indexName;
}

