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

package tech.qiantong.qdata.mdm.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型关系图的查询类型枚举。
 *
 * @author qdata
 */
@Getter
@AllArgsConstructor
public enum ModelRelationTypeEnum {

    /** 主模型和子模型之间的关系。 */
    CHILD("CHILD", "主子关系"),
    /** 通过关联字段或分类字段引用其他模型的关系。 */
    REFERENCE("REFERENCE", "引用关系");

    /** 接口传递的字符串值。 */
    private final String value;

    /** 中文名称。 */
    private final String name;

    /** 根据字符串找对应的类型，忽略大小写；没传值或找不到时返回 null。 */
    public static ModelRelationTypeEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ModelRelationTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
