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

package tech.qiantong.qdata.mdm.audit.dal.dataobject.audit;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 业务操作审计 DO 对象 AUDIT_OPERATION_LOG
 *
 * @author zhangyanyan
 * @date 2026-09-22
 */
@Data
@TableName(value = "AUDIT_OPERATION_LOG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("AUDIT_OPERATION_LOG_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditOperationLogDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联模型ID。 */
    private Long modelId;
    /** 本次操作涉及的模型版本ID。 */
    private Long modelVersionId;
    /** 与模型ID共同定位权威主数据。 */
    private Long masterRecordId;
    /** 关联本次变更，非请求追踪ID。 */
    private Long changeId;

    /** 业务模块编码 */
    private String moduleCode;

    /** 功能编码 */
    private String functionCode;

    /** 业务对象类型 */
    private String objectType;

    /** 业务对象ID */
    private String objectId;

    /** 业务对象名称 */
    private String objectName;

    /** 操作类型 */
    private String type;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作结果 */
    private String operateResult;

    /** 操作时间 */
    private Date operateTime;

    /** 请求IP */
    private String requestIp;

    /** 请求ID */
    private String requestId;

    /** 链路追踪ID */
    private String traceId;

    /** 错误信息 */
    private String errorMessage;

    /** 是否有效：0：无效，1：有效 */
    private String validFlag;

    /** 删除标志：0：未删除，1：已删除 */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}

