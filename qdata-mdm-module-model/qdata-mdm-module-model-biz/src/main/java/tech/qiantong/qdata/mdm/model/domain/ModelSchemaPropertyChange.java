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

/**
 * 对象单项属性变化，完整记录类型、约束及元数据，DDL资格由后续规则判断。
 */
@Data
@NoArgsConstructor
public class ModelSchemaPropertyChange {

    /**
     * 属性名
     */
    private String property;

    /**
     * 旧值，以JSON兼容值保存
     */
    private Object beforeValue;

    /**
     * 新值，以JSON兼容值保存
     */
    private Object afterValue;
}

