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

package tech.qiantong.qdata.mdm.master.dal.dataobject.change;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 主数据变更申请 DO，对应 MASTER_CHANGE_REQUEST。
 */
@Data
@TableName(value = "MASTER_CHANGE_REQUEST")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MASTER_CHANGE_REQUEST_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterChangeRequestDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 自增主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请编号。 */
    private String requestNo;

    /** 主数据模型 ID。 */
    private Long modelId;

    /** 模型版本 ID。 */
    private Long modelVersionId;

    /** 提交申请时使用的物化实例 ID。 */
    private Long instanceId;

    /** 本次正式执行的唯一标识，重试时保持不变。 */
    private String executionId;

    /** 来源申请 ID，用于申请派生关系。 */
    private Long sourceRequestId;

    /** 申请类型：CREATE、UPDATE、BATCH_UPDATE、BATCH_IMPORT、STATUS_CHANGE 或 VERSION_RESTORE。 */
    private String changeType;

    /** 变更来源：MANUAL、REQUEST、MERGE、RESTORE 或 SYSTEM。 */
    private String changeSource;

    /** 变更申请名称。 */
    private String requestName;

    /** 申请人 ID。 */
    private Long applicantId;

    /** 申请人名称历史快照。 */
    private String applicantName;

    /** 申请原因。 */
    private String applyReason;

    /** 审批流程实例 ID。 */
    private Long flowInstanceId;

    /** 审批状态：0 审批中、1 已通过、2 已驳回、3 已撤回、4 已终止。 */
    private String status;

    /** 执行状态：0 未执行、1 执行中、2 成功、3 部分成功、4 失败。 */
    private String executeStatus;

    /** 申请明细总数。 */
    private Integer itemCount;

    /** 执行成功明细数。 */
    private Integer successItemCount;

    /** 执行失败明细数。 */
    private Integer failItemCount;

    /** 提交时间。 */
    private Date submitTime;

    /** 申请完成时间。 */
    private Date finishTime;

    /** 申请终止原因。 */
    private String terminateReason;

    /** 执行结果说明。 */
    private String executeMessage;

    /** 是否有效：数据库字符值 0 无效、1 有效。 */
    private Boolean validFlag;

    /** 逻辑删除：数据库字符值 0 未删除、1 已删除。 */
    @TableLogic
    private Boolean delFlag;
}
