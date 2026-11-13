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

package tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo;

import java.util.Date;
import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 整改任务明细记录。
 */
@Data
@TableName(value = "QUALITY_ISSUE_TASK_INFO")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("QUALITY_ISSUE_TASK_INFO_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QualityIssueTaskInfoDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整改任务ID */
    private Long taskId;

    /** 质量问题ID */
    private Long issueId;

    /** 数据记录ID */
    private Long masterRecordId;

    /** 整改状态：1待整改、2整改中、3已整改 */
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

    /** 质量规则ID */
    private Long qualityRuleId;

    /** 下发整改时选中的问题项ID，关联QUALITY_ISSUE_INFO.ID */
    private Long issueInfoId;


}
