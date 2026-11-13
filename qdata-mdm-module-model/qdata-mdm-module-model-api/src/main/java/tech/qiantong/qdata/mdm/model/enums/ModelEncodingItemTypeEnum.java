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
 * 编码方案规则段类型枚举。
 *
 * @author qdata
 */
@Getter
@AllArgsConstructor
public enum ModelEncodingItemTypeEnum {

    FIXED_TEXT("FIXED_TEXT", "固定内容"),
    BUSINESS_FIELD("BUSINESS_FIELD", "业务字段"),
    REFERENCE_MAPPING("REFERENCE_MAPPING", "参照映射"),
    DATETIME("DATETIME", "日期时间"),
    SERIAL_NUMBER("SERIAL_NUMBER", "流水号");

    /**
     * 数据库存储值。
     */
    private final String value;

    /**
     * 中文名称。
     */
    private final String name;

    public static ModelEncodingItemTypeEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ModelEncodingItemTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
