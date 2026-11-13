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

package tech.qiantong.qdata.mdm.master.service.change.maintenance;

import lombok.Value;
import java.util.LinkedHashMap;
import java.util.Map;

/** 权威库已确认成功的结果，供统一保存变更、版本及申请结果。 */
@Value
public class MaintenanceResult {
    Long recordId;
    Map<String, Object> before;
    Map<String, Object> after;
    String beforeStatus;
    String afterStatus;

    public MaintenanceResult(Long recordId, Map<String, ?> before, Map<String, ?> after,
            String beforeStatus, String afterStatus) {
        this.recordId = recordId;
        this.before = new LinkedHashMap<>(before);
        this.after = new LinkedHashMap<>(after);
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
    }

    public MaintenanceResult withRecordId(Long generatedRecordId) {
        return new MaintenanceResult(generatedRecordId, before, after, beforeStatus, afterStatus);
    }
}
