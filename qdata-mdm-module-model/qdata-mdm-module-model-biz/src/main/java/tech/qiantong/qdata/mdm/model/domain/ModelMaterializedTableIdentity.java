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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;

/**
 * 用“模型ID＋来源/权威角色”识别同一张表，可作为Map的键；表名变化后仍能找到旧表。
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelMaterializedTableIdentity {
    /**
     * 当前节点模型ID，不是统一写根模型ID。
     */
    Long modelId;
    /**
     * 来源和权威具有不同表身份。
     */
    ModelTableRole tableRole;

    /**
     * 使用节点模型ID及角色建立稳定身份，不包含可修改的物理表名。
     *
     * @param modelId   当前节点模型ID，必须为正数
     * @param tableRole 来源或权威角色，不得为空
     * @return 可作为Map键的不可变身份
     * @throws IllegalArgumentException 模型ID或角色缺失、不合法时抛出
     */
    public static ModelMaterializedTableIdentity of(Long modelId, ModelTableRole tableRole) {
        // 同一个模型会有来源和权威两张表，因此模型ID和表角色都要有，缺一项就无法准确识别。
        if (modelId == null || modelId <= 0 || tableRole == null) {
            throw new IllegalArgumentException("物化表身份必须包含有效模型ID及表角色");
        }
        return new ModelMaterializedTableIdentity(modelId, tableRole);
    }
}
