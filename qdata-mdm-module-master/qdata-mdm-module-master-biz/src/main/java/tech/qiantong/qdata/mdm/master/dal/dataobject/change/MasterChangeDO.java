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
 * 权威主数据变更记录 DO，对应 MASTER_CHANGE。
 */
@Data
@TableName(value = "MASTER_CHANGE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MASTER_CHANGE_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterChangeDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 自增主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 变更申请 ID，逻辑关联 MASTER_CHANGE_REQUEST.ID。 */
    private Long requestId;

    /** 变更申请明细 ID，逻辑关联 MASTER_CHANGE_REQUEST_ITEM.ID。 */
    private Long requestItemId;

    /** 主数据模型 ID。 */
    private Long modelId;

    /** 模型版本 ID。 */
    private Long modelVersionId;

    /** 权威主数据记录 ID。 */
    private Long masterRecordId;

    /** 变更类型：CREATE、UPDATE、RESTORE 或 MERGE。 */
    private String type;

    /** 变更来源：MANUAL、REQUEST、MERGE、RESTORE 或 SYSTEM。 */
    private String source;

    /** 变更前版本 ID，逻辑关联 MASTER_DATA_VERSION.ID。 */
    private Long beforeVersionId;

    /** 变更后版本 ID，逻辑关联 MASTER_DATA_VERSION.ID。 */
    private Long afterVersionId;

    /** 变更前版本号。 */
    private String beforeVersionNo;

    /** 变更后版本号。 */
    private String afterVersionNo;

    /** 变更前状态：1 有效、2 冻结、3 失效、4 归档。 */
    private String beforeStatus;

    /** 变更后状态：1 有效、2 冻结、3 失效、4 归档。 */
    private String afterStatus;

    /** 变更前业务快照。 */
    private String beforeData;

    /** 拟变更业务快照。 */
    private String proposedData;

    /** 最终业务快照。 */
    private String afterData;

    /** 变更原因。 */
    private String changeReason;

    /** 操作人 ID。 */
    private Long operatorId;

    /** 操作人名称。 */
    private String operatorName;

    /** 变更时间。 */
    private Date changeTime;

    /** 处理结果：0 处理中、1 已生效、2 已驳回、3 已撤回、4 执行失败。 */
    private String processResult;

    /** 是否有效：数据库字符值 0 无效、1 有效。 */
    private Boolean validFlag;

    /** 逻辑删除：数据库字符值 0 未删除、1 已删除。 */
    @TableLogic
    private Boolean delFlag;

}
