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

import java.util.Date;

/**
 * 合并结果审批与流转记录响应对象，字段来自待合并结果操作日志。
 * 操作前后数据快照用于查看该次操作对应的结果变化。
 */
@Data
public class MergePendingApprovalRecordRespVO {
    /** 操作日志编号。 */
    private Long id;
    /** 执行本次操作的用户名称。 */
    private String operatorName;
    /** 操作类型编码，对应合并管理日志类型。 */
    private String actionType;
    /** 操作发生时间。 */
    private Date operateTime;
    /** 操作说明或处理意见。 */
    private String reason;
    /** 操作前的结果版本号。 */
    private Integer beforeVersionNo;
    /** 操作后的结果版本号。 */
    private Integer afterVersionNo;
    /** 工作流实例编号，未进入工作流时为空。 */
    private String flowInstanceId;
    /** 关联的业务申请编号。 */
    private Long requestId;
    /** 操作前的结果数据快照。 */
    private String beforeData;
    /** 操作后的结果数据快照。 */
    private String afterData;
}
