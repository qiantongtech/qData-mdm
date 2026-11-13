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

package tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTask;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskIssueItemRespVO;

import java.util.Date;
import java.util.List;

/**
 * 问题整改任务 DO 对象 QUALITY_ISSUE_TASK
 *
 * @author qdata
 * @date 2026-09-23
 */
@Data
@TableName(value = "QUALITY_ISSUE_TASK")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("QUALITY_ISSUE_TASK_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QualityIssueTaskDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整改任务编号 */
    private String taskNo;

    /** 整改任务名称 */
    private String taskName;

    /** 优先级：1高、2中、3低 */
    private Long priority;

    /** 主数据模型ID */
    private Long modelId;

    /** 主数据模型版本 */
    private String modelVersion;

    /** 整改负责人用户 ID */
    private Long assigneeId;

    /** 整改任务状态：1待审批、2审批中、3已完成、4已取消、5处理中。 */
    private String status;

    /** 整改说明 */
    private String requirement;

    /** 整改时间 */
    private Date rectificationTime;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    @TableLogic
    private Boolean delFlag;

    // 列表和详情使用的非持久化字段。

    /** 涉及主数据数 */
    @TableField(exist = false)
    private Long masterDataCount;

    /** 问题项总数 */
    @TableField(exist = false)
    private Long issueDataCount;

    /** 已整改问题项数 */
    @TableField(exist = false)
    private Long completedIssueCount;

    /** 任务关联的全部问题项，仅详情返回 */
    @TableField(exist = false)
    private List<QualityIssueTaskIssueItemRespVO> issueItems;

    /** 主数据模型名称 */
    @TableField(exist = false)
    private String modelName;

    /** 主数据模型编码 */
    @TableField(exist = false)
    private String modelCode;

    /** 负责人名称 */
    @TableField(exist = false)
    private String assigneeName;

    /** 负责人手机号 */
    @TableField(exist = false)
    private String assigneePhone;

    /** 最近处理时间 */
    @TableField(exist = false)
    private Date lastHandledTime;

}
