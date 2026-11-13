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

package tech.qiantong.qdata.mdm.master.dal.dto.change;

import lombok.Data;

/**
 * MASTER_CHANGE_REQUEST_ITEM 固定查询结果；动态业务快照仍以字符串保存。
 */
@Data
public class MasterChangeRequestItemQueryDTO {
    private Long id;
    private Integer itemNo;
    private Long requestId;
    private Long masterRecordId;
    private Long resultMasterRecordId;
    private Long recordId;
    private String requestNo;
    private String status;
    private String changeType;
    private String beforeStatus;
    private String targetStatus;
    private String currentVersionNo;
    private String targetVersionNo;
    private String beforeData;
    private String proposedData;
    private String executeStatus;
    private String executeMessage;
}
