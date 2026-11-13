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

import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;

import java.util.List;

/** 只根据有效申请明细描述拟变更目标，不推断审批或执行结果。 */
public final class MasterTargetSummaryFormatter {
    private MasterTargetSummaryFormatter() {
    }

    public static String format(String changeType, List<MasterChangeRequestItemQueryDTO> items) {
        if (items == null || items.isEmpty()) return "—";
        int count = items.size();
        if ("CREATE".equals(changeType)) return count == 1 ? "新增主数据" : "新增 " + count + " 条主数据";
        if ("UPDATE".equals(changeType) || "BATCH_UPDATE".equals(changeType)) {
            return updateSummary(items);
        }
        if ("BATCH_IMPORT".equals(changeType)) return importSummary(items);
        if ("VERSION_RESTORE".equals(changeType)) return restoreSummary(items);
        if (isStatusChange(changeType)) return statusSummary(items);
        return "—";
    }

    private static String updateSummary(List<MasterChangeRequestItemQueryDTO> items) {
        if (items.size() != 1) return items.size() + " 条业务字段修改";
        String before = items.get(0).getCurrentVersionNo();
        String target = items.get(0).getTargetVersionNo();
        return hasText(before) && hasText(target) && !before.equals(target)
                ? displayVersion(before) + " → " + displayVersion(target) : "修改业务字段";
    }

    private static String importSummary(List<MasterChangeRequestItemQueryDTO> items) {
        int creates = 0;
        int updates = 0;
        for (MasterChangeRequestItemQueryDTO item : items) {
            String type = item.getChangeType();
            if ("CREATE".equals(type)) creates++;
            else if ("UPDATE".equals(type)) updates++;
        }
        String base = "导入 " + items.size() + " 条";
        if (creates + updates != items.size()) return base;
        if (creates == 0) return base + "（更新 " + updates + "）";
        if (updates == 0) return base + "（新增 " + creates + "）";
        return base + "（新增 " + creates + "、更新 " + updates + "）";
    }

    private static String restoreSummary(List<MasterChangeRequestItemQueryDTO> items) {
        if (items.size() != 1) return items.size() + " 条历史内容恢复";
        String target = items.get(0).getTargetVersionNo();
        return hasText(target) ? "恢复历史内容并形成 " + displayVersion(target) : "恢复历史内容";
    }

    private static boolean isStatusChange(String type) {
        return "STATUS_CHANGE".equals(type) || "BATCH_STATUS".equals(type)
                || "FREEZE".equals(type) || "UNFREEZE".equals(type)
                || "INVALIDATE".equals(type) || "RECOVER".equals(type)
                || "ARCHIVE".equals(type);
    }

    private static String statusSummary(List<MasterChangeRequestItemQueryDTO> items) {
        String firstBefore = items.get(0).getBeforeStatus();
        String firstTarget = items.get(0).getTargetStatus();
        if (MasterStatusLabels.lifecycle(firstBefore) == null
                || MasterStatusLabels.lifecycle(firstTarget) == null) return "—";
        boolean sameTransition = true;
        boolean sameTarget = true;
        for (MasterChangeRequestItemQueryDTO item : items) {
            String before = item.getBeforeStatus();
            String target = item.getTargetStatus();
            if (MasterStatusLabels.lifecycle(before) == null
                    || MasterStatusLabels.lifecycle(target) == null || before.equals(target)) return "—";
            if (!firstBefore.equals(before) || !firstTarget.equals(target)) sameTransition = false;
            if (!firstTarget.equals(target)) sameTarget = false;
        }
        if (items.size() == 1) {
            return MasterStatusLabels.lifecycle(firstBefore) + " → "
                    + MasterStatusLabels.lifecycle(firstTarget);
        }
        if (sameTransition) {
            return items.size() + " 条：" + MasterStatusLabels.lifecycle(firstBefore) + " → "
                    + MasterStatusLabels.lifecycle(firstTarget);
        }
        if (sameTarget) {
            return items.size() + " 条：统一转为" + MasterStatusLabels.lifecycle(firstTarget);
        }
        return items.size() + " 条生命周期变更";
    }

    private static String displayVersion(String value) {
        String version = value.trim();
        return "V" + (version.startsWith("V") || version.startsWith("v") ? version.substring(1) : version);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
