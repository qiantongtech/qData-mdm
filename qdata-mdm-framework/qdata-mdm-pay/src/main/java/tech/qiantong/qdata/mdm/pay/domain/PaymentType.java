/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.pay.domain;

/**
 * 定义了系统中支持的支付方式。
 *
 * @author qdata
 */
public enum PaymentType {

    /**
     * 支付宝支付
     */
    ALIPAY,

    /**
     * 微信支付
     */
    WECHAT;

    /**
     * 根据输入的字符串值获取对应的支付类型。
     *
     * @param type 输入的字符串，如 "ALIPAY" 或 "WECHAT"
     * @return 返回对应的 PaymentType 枚举值，如果没有匹配则返回 null
     */
    public static PaymentType fromString(String type) {
        for (PaymentType paymentType : PaymentType.values()) {
            if (paymentType.name().equalsIgnoreCase(type)) {
                return paymentType;
            }
        }
        return null;
    }
}
