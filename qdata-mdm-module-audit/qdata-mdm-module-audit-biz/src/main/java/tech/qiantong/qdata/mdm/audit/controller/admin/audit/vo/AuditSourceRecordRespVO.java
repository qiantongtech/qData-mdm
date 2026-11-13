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

import java.util.List;
import lombok.Data;

/** 参与形成权威主数据的一条来源记录。 */
@Data
public class AuditSourceRecordRespVO {
    private String id;
    private Long relationId;
    private String system;
    private String sourceSystemId;
    private String sourceSystemCode;
    private String sourceSystemDescription;
    private Integer sourceSystemTrustScore;
    private String status;
    private String established;
    private String changed;
    private List<AuditSourceRecordFieldRespVO> fields;
}
