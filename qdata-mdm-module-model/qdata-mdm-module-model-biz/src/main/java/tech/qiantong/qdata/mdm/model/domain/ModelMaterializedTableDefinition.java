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
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型某一角色的完整表定义；表身份由modelId和tableRole共同确定。
 */
@Data
@NoArgsConstructor
public class ModelMaterializedTableDefinition {

    /**
     * 当前节点模型自身ID
     */
    private Long modelId;

    /**
     * 主模型为0，子模型为所属主模型ID
     */
    private Long parentId;

    /**
     * 来源或权威角色
     */
    private ModelTableRole tableRole;

    /**
     * 物理表名，主表及子表分别按已确认命名规则构建
     */
    private String tableName;

    /**
     * 真实物理表注释
     */
    private String tableComment;

    /**
     * 这张表是否还参与正常业务；停用后仍保留实际表及完整结构
     */
    private Boolean enabledFlag = Boolean.TRUE;

    /**
     * 完整列清单，包含停用及业务逻辑删除后的真实列
     */
    private List<ModelMaterializedColumnDefinition> columns = new ArrayList<>();

    /**
     * 完整主键和索引清单，保留已有真实约束
     */
    private List<ModelMaterializedIndexDefinition> indexes = new ArrayList<>();
}

