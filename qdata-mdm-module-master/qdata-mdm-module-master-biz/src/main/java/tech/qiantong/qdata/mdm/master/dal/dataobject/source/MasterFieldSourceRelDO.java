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

package tech.qiantong.qdata.mdm.master.dal.dataobject.source;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 权威主数据某版本的字段取值来源关系 DO，对应 MASTER_FIELD_SOURCE_REL。
 */
@Data
@TableName(value = "MASTER_FIELD_SOURCE_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MASTER_FIELD_SOURCE_REL_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterFieldSourceRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键 ID；表定义未声明自增。 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 主数据模型 ID；查询时用于约束来源记录所属模型。 */
    private Long modelId;

    /** 权威主数据记录 ID */
    private Long masterRecordId;

    /** 权威主数据版本 ID，关联 MASTER_DATA_VERSION.ID。 */
    private Long masterVersionId;

    /** 主模型字段 ID，关联 MODEL_COLUMN.ID。 */
    private Long modelColumnId;

    /** 子模型字段 ID */
    private Long modelSubmodelColumnId;

    /** 权威主数据字段在业务数据 JSON 中的路径 */
    private String fieldPath;

    /** 来源数据源 ID */
    private Long sourceId;

    /** 来源记录 ID，关联 INGEST_SOURCE_DATA.ID。 */
    private Long sourceRecordId;

    /** 来源记录版本 ID，关联 INGEST_SOURCE_DATA_VERSION.ID。 */
    private Long sourceVersionId;

    /** 来源记录中的字段路径 */
    private String sourceFieldPath;

    /** 形成类型：MANUAL 人工维护、MERGE 多源融合、IMPORT 批量导入 */
    private String relationType;

    /** 映射状态：1 有效、2 失效 */
    private String status;

    /** 正式生效时间 */
    private Date effectiveTime;

    /** 失效时间 */
    private Date invalidTime;

    /** 失效原因 */
    private String invalidReason;

    /** 形成该映射的变更申请 ID */
    private Long requestId;

    /** 形成该映射的变更申请明细 ID */
    private Long requestItemId;

    /** 形成该映射的变更历史 ID */
    private Long changeId;

    /** 是否有效：数据库字符值 0 无效、1 有效。 */
    private Boolean validFlag;

    /** 逻辑删除：数据库字符值 0 未删除、1 已删除。 */
    @TableLogic
    private Boolean delFlag;
}
