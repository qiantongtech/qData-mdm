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

package tech.qiantong.qdata.mdm.master.dal.dto.source;

import lombok.Data;

/**
 * MASTER_FIELD_SOURCE_REL 固定字段来源查询结果。
 */
@Data
public class MasterFieldSourceDTO {

    /** 权威主数据字段路径。 */
    private String fieldPath;

    /** 来源记录字段路径。 */
    private String sourceFieldPath;

    /** 来源系统名称。 */
    private String sourceSystemName;

    /** 来源记录 ID。 */
    private Long sourceRecordId;

    /** 来源记录版本 ID。 */
    private Long sourceVersionId;
}
