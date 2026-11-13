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

/** 权威主数据生命周期状态的统一展示名称。未知值由调用方决定如何降级。 */
public final class MasterStatusLabels {
    private MasterStatusLabels() {
    }

    public static String lifecycle(String status) {
        if ("1".equals(status)) return "生效";
        if ("2".equals(status)) return "冻结";
        if ("3".equals(status)) return "失效";
        if ("4".equals(status)) return "归档";
        return null;
    }
}
