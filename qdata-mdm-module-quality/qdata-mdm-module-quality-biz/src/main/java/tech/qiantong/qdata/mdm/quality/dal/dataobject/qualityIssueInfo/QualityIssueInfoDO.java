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

package tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 问题数据明细 DO 对象 QUALITY_ISSUE_INFO
 *
 * @author qdata
 * @date 2026-09-23
 */
@Data
@TableName(value = "QUALITY_ISSUE_INFO")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("QUALITY_ISSUE_INFO_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QualityIssueInfoDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质量问题ID */
    private Long issueId;

    /** 质量规则ID */
    private Long qualityRuleId;

    /** 质量规则名称 */
    @TableField(exist = false)
    private String qualityRuleName;

    /** 质量规则类型 */
    @TableField(exist = false)
    private String qualityRuleType;

    /** 模型字段ID */
    private Long modelColumnId;

    /** 模型字段名称 */
    @TableField(exist = false)
    private String modelColumnName;

    /** 质量任务执行实例ID */
    private Long lastCheckInstanceId;

    /** 问题项状态：1未解决、2已解决 */
    private String status;

    /** 规则检测失败原因 */
    private String failReason;

    /** 最近一次检测失败时间 */
    private Date lastFoundTime;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    @TableLogic
    private Boolean delFlag;

    /** 整改情况 */
    private String rectificationDesc;


}
