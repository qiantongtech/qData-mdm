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

package tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 变更历史列表补充的模型和归属信息。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditChangeTraceListRespVO extends AuditChangeTraceRespVO {
    private String modelCode;
    private String modelType;
    private String modelIcon;
    private String modelDescription;
    private String projectName;
    private String projectIcon;
    private String layerName;
    private String modelVersionNo;
}
