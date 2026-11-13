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

package tech.qiantong.qdata.mdm.master.change.dto;

import lombok.Data;

import java.io.Serializable;

/** 申请提交结果；提交成功不代表正式主数据已执行成功。 */
@Data
public class MasterChangeRequestSubmitRespDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String requestNo;
    private String requestName;
    /** 提交时的审批状态，当前为1已通过。 */
    private String approvalStatus;
    private Integer itemCount;
    /** 是否启动人工审批流程，当前为false。 */
    private boolean flowStarted;
}
