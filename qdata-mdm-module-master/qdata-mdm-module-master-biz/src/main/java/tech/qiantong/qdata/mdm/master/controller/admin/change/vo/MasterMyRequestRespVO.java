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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import tech.qiantong.qdata.mdm.model.base.dto.ModelBaseRespDTO;

import java.util.Date;

/**
 * 当前登录人的已提交主数据维护申请。
 */
@Data
public class MasterMyRequestRespVO {
    /** 字符串传输申请 ID，避免 JavaScript 丢失 BIGINT 精度。 */
    private String requestId;
    /** 申请编号。 */
    private String requestNo;
    /** 申请名称。 */
    private String requestName;
    /** 申请人名称快照。 */
    private String applicantName;
    /** 当前申请人的手机号码。 */
    private String applicantPhone;
    /** 当前状态是否允许申请人撤回。 */
    private boolean canWithdraw;
    /** 当前状态是否允许申请人重新申请。 */
    private boolean canReapply;
    /** 字符串传输模型 ID，避免 JavaScript 丢失 BIGINT 精度。 */
    private String modelId;
    /** 模型名称。 */
    private String modelName;
    /** 模型编码。 */
    private String modelCode;
    /** 与模型定义列表一致的当前模型信息、版本和归属位置。 */
    private ModelBaseRespDTO model;
    /** 本次申请填写的原因。 */
    private String applyReason;
    /** 申请变更类型。 */
    private String changeType;
    /** 审批状态。 */
    private String approvalStatus;
    /** 申请执行状态。 */
    private String executeStatus;
    /** 申请提交时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 申请完成时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
    /** 执行结果说明；审批中时由业务层隐藏。 */
    private String executeMessage;
    /** 字符串传输审批流程实例 ID，避免 JavaScript 丢失 BIGINT 精度。 */
    private String flowInstanceId;
    /** 申请明细数量。 */
    private Integer itemCount;
}
