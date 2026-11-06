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

package tech.qiantong.qdata.mdm.system.dal.dataobject.message.enums;

import java.util.Objects;

public enum MessageHasReadEnums {
    WD(0, "未读"),
    YD(1, "已读");
    public final Integer code;
    public final String info;

    MessageHasReadEnums(Integer code, String info) {
        this.code = code;
        this.info = info;
    }

    public static MessageHasReadEnums get(Integer code) {
        for (MessageHasReadEnums v : values()) {
            if (v.eq(code)) {
                return v;
            }
        }
        return null;
    }

    // 根据code返回县市名称
    public static MessageHasReadEnums getName(String info) {
        for (MessageHasReadEnums v : values()) {
            if (v.like(info)) {
                return v;
            }
        }
        return null;
    }

    public boolean eq(Integer code) {
        return this.code.equals(code);
    }

    public boolean like(String info) {
        return this.info.equals(info);
    }

    public static String getInfo(Integer code) {
        return Objects.requireNonNull(MessageHasReadEnums.get(code)).getInfo();
    }

    public static Integer getCode(String info) {
        return Objects.requireNonNull(MessageHasReadEnums.getName(info)).getCode();
    }

    public Integer getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }
}
