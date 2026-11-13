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

package tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskLog;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 问题整改记录日志 DO 对象 QUALITY_ISSUE_TASK_LOG
 *
 * @author qdata
 * @date 2026-09-23
 */
@Data
@TableName(value = "QUALITY_ISSUE_TASK_LOG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("QUALITY_ISSUE_TASK_LOG_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QualityIssueTaskLogDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整改任务ID */
    private Long qualityIssueTaskId;

    /** 问题数据ID */
    private Long issueId;

    /** 整改任务明细ID，关联QUALITY_ISSUE_TASK_INFO.ID */
    private Long taskInfoId;

    /** 整改前值 */
    private String beforeValue;

    /** 整改后值 */
    private String afterValue;

    /** 整改字段 */
    private String rectificationField;

    /** 主数据对象 */
    private String masterDataObject;

    /** 任务下发人ID */
    private Long taskSendId;

    /** 任务处理人ID */
    private Long taskHandlerId;

    /** 整改人手机号 */
    @TableField(exist = false)
    private String handlerPhone;

    /** 下发时间 */
    private Date sendTime;

    /** 处理时间 */
    private Date handlerTime;

    /** 整改说明 */
    private String description;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    @TableLogic
    private Boolean delFlag;


}
