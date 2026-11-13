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
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * 完整主键及索引定义，用来比较上次成功结构与本次结构、生成SQL并核对实际数据库。
 */
@Data
@NoArgsConstructor
public class ModelMaterializedIndexDefinition {

    /**
     * 按规则稳定ID或固定用途标识匹配，不能只按索引名匹配
     */
    private String stableKey;

    /**
     * 实际索引或约束名称
     */
    private String indexName;

    /**
     * 主键、唯一或普通索引
     */
    private ModelIndexType indexType;

    /**
     * 有序列稳定标识；业务列使用FIELD:id，系统列使用SYSTEM:key
     */
    private List<String> columnKeys = new ArrayList<>();

    /**
     * 对应的物理列名，顺序与columnKeys一致
     */
    private List<String> columnNames = new ArrayList<>();

    /**
     * 主键是否已经包含在建表SQL中，避免重复创建
     */
    private Boolean includedInCreateTable = Boolean.FALSE;
}

