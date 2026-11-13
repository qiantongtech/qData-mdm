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
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 复用成功物化表绑定信息，增加同表真实字段；索引沿用indexDefinition。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelPublishedMaterializedTableRespDTO extends ModelMaterializedTableRespDTO {
    /**
     * 成功核验保存的实际字段，包含停用但仍保留在物理表中的列。
     */
    private List<ModelMaterializedColumnRespDTO> columns = new ArrayList<>();
}
