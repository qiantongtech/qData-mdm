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

package tech.qiantong.qdata.mdm.auth.api.enums;

/** 数据库持久化的授权状态。 */
public enum AuthorizationStatus {
    ENABLED("1"),
    REVOKED("0");

    private final String code;

    AuthorizationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 兼容迁移前数据库中的英文状态。 */
    public boolean matches(String value) {
        return code.equals(value) || name().equals(value);
    }
}
