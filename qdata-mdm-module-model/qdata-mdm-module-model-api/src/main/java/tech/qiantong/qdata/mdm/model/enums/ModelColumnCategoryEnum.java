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
 * 模型字段分类枚举。
 *
 * @author qdata
 */
@Getter
@AllArgsConstructor
public enum ModelColumnCategoryEnum {

    /** 平台预置的字段，例如主数据 ID、创建时间。 */
    SYSTEM("SYSTEM", "系统预置字段"),
    /** 用户添加的普通字段，例如客户名称、信用等级。 */
    BUSINESS("BUSINESS", "普通业务字段"),
    /** 用来关联其他模型的字段。 */
    RELATION("RELATION", "普通关联字段"),
    /** 用来关联分类模型的字段。 */
    CLASS("CLASS", "分类模型关联字段");

    /** 数据库存储值。 */
    private final String value;

    /** 中文名称。 */
    private final String name;

    /** 根据字符串找对应的分类，忽略大小写；没传值或找不到时返回 null。 */
    public static ModelColumnCategoryEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ModelColumnCategoryEnum type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
