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

package tech.qiantong.qdata.mdm.common.enums;

public enum RequestMethodEnum {

    GET("1", "get"),
    POST("2", "post");

    private final String key;

    private final String val;

    //Get key based on value

    RequestMethodEnum(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }

    /**
     * Find the corresponding val based on the given key.
     * If no enumeration instance matching the given key is found, null is returned.
     */
    public static String getValByKey(String key) {
        for (RequestMethodEnum method : RequestMethodEnum.values()) {
            if (method.getKey().equals(key)) {
                return method.getVal();
            }
        }
        // Returns null if no matching key is found
        return null;
    }

    //Find value based on key
    public static String getKeyByVal(String val) {
        for (RequestMethodEnum method : RequestMethodEnum.values()) {
            if (method.getVal().equals(val)) {
                return method.getKey();
            }
        }
        // Returns null if no matching key is found
        return null;
    }
}
