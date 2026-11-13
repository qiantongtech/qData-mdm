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

package tech.qiantong.qdata.mdm.master.dal.dataobject.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 一条权威主数据记录的正式版本快照 DO，对应 MASTER_DATA_VERSION。
 */
@Data
@TableName(value = "MASTER_DATA_VERSION")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MASTER_DATA_VERSION_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterDataVersionDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 自增主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主数据模型 ID，逻辑关联 MODEL_BASE.ID。 */
    private Long modelId;

    /** 模型版本 ID，逻辑关联 MODEL_VERSION.ID。 */
    private Long modelVersionId;

    /** 权威主数据记录 ID */
    private Long masterRecordId;

    /** 主数据版本序号，INTEGER；版本列表默认按此序号倒序。 */
    private Integer versionSeq;

    /** 数据版本展示编号 */
    private String versionNo;

    /** 正式版本业务快照，TEXT */
    private String snapshotData;

    /** 版本生成类型：CREATE 新增、UPDATE 修改、MERGE 融合、VERSION_RESTORE 历史版本恢复。 */
    private String generateType;

    /** 版本变更历史 ID，关联 MASTER_CHANGE.ID */
    private Long changeId;

    /** 历史恢复时选择的来源版本 ID，逻辑关联 MASTER_DATA_VERSION.ID。 */
    private Long restoreVersionId;

    /** 正式版本生效时间 */
    private Date generateTime;

    /** 是否当前正式版本：数据库字符值 0 否、1 是。 */
    private String currentFlag;

    /** 是否有效：数据库字符值 0 无效、1 有效。 */
    private Boolean validFlag;

    /** 逻辑删除：数据库字符值 0 未删除、1 已删除。 */
    @TableLogic
    private Boolean delFlag;

    /** 版本列表关联的变更类型，来自 MASTER_CHANGE.TYPE，非本表字段。 */
    @TableField(exist = false)
    private String changeType;

    /** 版本列表关联的操作人，来自 MASTER_CHANGE.OPERATOR_NAME，非本表字段。 */
    @TableField(exist = false)
    private String operatorName;

    /** 版本列表关联的申请编号，来自 MASTER_CHANGE_REQUEST.REQUEST_NO，非本表字段。 */
    @TableField(exist = false)
    private String requestNo;
}
