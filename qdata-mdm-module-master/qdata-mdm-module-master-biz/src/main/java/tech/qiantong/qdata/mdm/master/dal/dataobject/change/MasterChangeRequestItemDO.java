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
 * 主数据变更申请明细 DO，对应 MASTER_CHANGE_REQUEST_ITEM。
 */
@Data
@TableName(value = "MASTER_CHANGE_REQUEST_ITEM")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MASTER_CHANGE_REQUEST_ITEM_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterChangeRequestItemDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 自增主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请 ID，逻辑关联 MASTER_CHANGE_REQUEST.ID。 */
    private Long requestId;

    /** 申请明细序号。 */
    private Integer itemNo;

    /** 申请涉及的原主数据记录 ID。 */
    private Long masterRecordId;

    /** 申请生效后的主数据记录 ID。 */
    private Long resultMasterRecordId;

    /** 正式生效后形成的数据版本 ID。 */
    private Long resultVersionId;

    /** 明细变更类型。 */
    private String changeType;

    /** 提交前业务快照。 */
    private String beforeData;

    /** 申请业务快照。 */
    private String proposedData;

    /** 变更前状态：1 有效、2 冻结、3 失效、4 归档。 */
    private String beforeStatus;

    /** 目标状态：1 有效、2 冻结、3 失效、4 归档。 */
    private String targetStatus;

    /** 提交申请时的正式版本 ID。 */
    private Long currentVersionId;

    /** 提交申请时的正式版本号。 */
    private String currentVersionNo;

    /** 申请生效后的版本号。 */
    private String targetVersionNo;

    /** 执行状态：0 待执行、1 执行中、2 成功、3 失败、4 跳过。 */
    private String executeStatus;

    /** 执行结果说明。 */
    private String executeMessage;

    /** 执行完成时间。 */
    private Date executeTime;

    /** 是否有效：数据库字符值 0 无效、1 有效。 */
    private Boolean validFlag;

    /** 逻辑删除：数据库字符值 0 未删除、1 已删除。 */
    @TableLogic
    private Boolean delFlag;
}
