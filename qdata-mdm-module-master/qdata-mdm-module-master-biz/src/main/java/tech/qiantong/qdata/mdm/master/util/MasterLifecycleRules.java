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

package tech.qiantong.qdata.mdm.master.util;

/** 权威主数据的单条生命周期转换规则。 */
public final class MasterLifecycleRules {
    private MasterLifecycleRules() {
    }

    public static boolean isAction(String action) {
        return "FREEZE".equals(action) || "UNFREEZE".equals(action)
                || "INVALIDATE".equals(action) || "RECOVER".equals(action)
                || "ARCHIVE".equals(action);
    }

    public static String target(String action, String currentStatus) {
        if ("1".equals(currentStatus)) {
            if ("FREEZE".equals(action)) return "2";
            if ("INVALIDATE".equals(action)) return "3";
        }
        if ("2".equals(currentStatus)) {
            if ("UNFREEZE".equals(action)) return "1";
            if ("INVALIDATE".equals(action)) return "3";
        }
        if ("3".equals(currentStatus)) {
            if ("RECOVER".equals(action)) return "1";
            if ("ARCHIVE".equals(action)) return "4";
        }
        return null;
    }

    public static String actionLabel(String action) {
        if ("FREEZE".equals(action)) return "冻结";
        if ("UNFREEZE".equals(action)) return "解冻";
        if ("INVALIDATE".equals(action)) return "失效";
        if ("RECOVER".equals(action)) return "恢复";
        if ("ARCHIVE".equals(action)) return "归档";
        return null;
    }
}
