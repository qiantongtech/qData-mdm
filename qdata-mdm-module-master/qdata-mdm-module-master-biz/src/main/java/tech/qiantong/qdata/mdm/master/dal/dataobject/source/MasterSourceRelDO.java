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

import java.sql.Timestamp;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 权威主数据来源映射 DO 对象 MASTER_SOURCE_REL
 *
 * @author qdata
 * @date 2026-10-09
 */
@Data
@TableName(value = "MASTER_SOURCE_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MASTER_SOURCE_REL_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterSourceRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主数据模型 ID；查询时用于校验来源记录所属模型。 */
    private Long modelId;

    /** 权威主数据记录ID */
    private Long masterRecordId;

    /** 来源系统或来源数据源ID */
    private Long sourceId;

    /** 来源记录 ID，关联 INGEST_SOURCE_DATA.ID。 */
    private Long sourceRecordId;

    /** 来源记录版本 ID，关联 INGEST_SOURCE_DATA_VERSION.ID。 */
    private Long sourceVersionId;

    /** 映射形成类型：MANUAL人工维护、MERGE多源融合、IMPORT批量导入 */
    private String type;

    /** 来源映射状态：1有效、2失效 */
    private String status;

    /** 映射正式生效时间 */
    private Date effectiveTime;

    /** 映射失效时间 */
    private Date invalidTime;

    /** 失效原因 */
    private String invalidReason;

    /** 变更申请ID */
    private Long requestId;

    /** 变更申请明细ID */
    private Long requestItemId;

    /** 变更历史ID */
    private Long changeId;

    /** 融合记录ID */
    private Long mergeRecordId;

    /** 是否有效：数据库字符值 0 无效、1 有效。 */
    private Boolean validFlag;

    /** 逻辑删除标志：数据库字符值 0 未删除、1 已删除；表模型说明历史映射原则上不删除。 */
    @TableLogic
    private Boolean delFlag;

    /** 关联采集记录所属的来源系统名称。 */
    @TableField(exist = false)
    private String sourceSystemName;

    /** 关联采集记录的业务主键。 */
    @TableField(exist = false)
    private String sourceRecordKey;

    /** 关联采集记录的稳定记录 ID，按字符串传递以保留原始表示。 */
    @TableField(exist = false)
    private String stableRecordId;

    /** 关联采集记录最近接收时间，保留 JDBC 时间戳精度。 */
    @TableField(exist = false)
    private Timestamp lastReceiveTime;

    /** 关联采集版本的数据快照；详情接口按原有规则截断超长内容。 */
    @TableField(exist = false)
    private String sourceModelData;

}