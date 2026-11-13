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
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishChangeStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存本次新结构、上次成功的旧结构及两者的全部差异，供后续校验和生成SQL使用。
 */
@Data
@NoArgsConstructor
public class ModelPublishStructureDiff {

    /**
     * 上次成功发布实例ID，首次为空
     */
    private Long baseInstanceId;

    /**
     * 上次成功完整结构，首次为空结构
     */
    private ModelMaterializedStructure beforeStructure = new ModelMaterializedStructure();

    /**
     * 本次完整新结构，包含需要保留的旧物理对象
     */
    private ModelMaterializedStructure afterStructure = new ModelMaterializedStructure();

    /**
     * 本次发布变化汇总
     */
    private ModelPublishChangeStatus changeStatus;

    /**
     * 完整对象变化列表
     */
    private List<ModelSchemaObjectChange> changes = new ArrayList<>();
}

