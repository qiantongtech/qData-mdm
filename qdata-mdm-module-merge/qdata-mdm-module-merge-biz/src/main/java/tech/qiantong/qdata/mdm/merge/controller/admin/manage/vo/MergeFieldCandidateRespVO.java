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

package tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo;

import lombok.Data;

/**
 * 字段的来源候选值
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeFieldCandidateRespVO {
    private Long sourceRecordId;
    private Long sourceVersionId;
    private String stableRecordId;
    private String systemCode;
    private String systemName;
    private String businessName;
    private String value;
}
