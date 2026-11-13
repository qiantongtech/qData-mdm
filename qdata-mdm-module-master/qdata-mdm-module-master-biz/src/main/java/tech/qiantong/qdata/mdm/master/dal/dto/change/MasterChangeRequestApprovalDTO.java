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

import java.util.Date;

/**
 * MASTER_CHANGE_REQUEST 审批查询的固定结果。
 */
@Data
public class MasterChangeRequestApprovalDTO {
    private Long id;
    private String requestNo;
    private String requestName;
    private String changeType;
    private Long modelId;
    private Long modelVersionId;
    private String changeSource;
    private Long instanceId;
    private String executionId;
    private String status;
    private String executeStatus;
    private Long applicantId;
    private String applyReason;
    private Long flowInstanceId;
    private Integer itemCount;
    private Integer successItemCount;
    private Integer failItemCount;
    private Date submitTime;
    private Date finishTime;
    private String terminateReason;
    private String executeMessage;
}
