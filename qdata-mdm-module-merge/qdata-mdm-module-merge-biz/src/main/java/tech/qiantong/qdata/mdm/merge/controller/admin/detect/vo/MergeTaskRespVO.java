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

package tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 重复检测任务
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeTaskRespVO {
    private Long id;
    private String taskNo;
    private String taskName;
    private Long responsibleUserId;
    private String enableStatus;
    private String schedulerStatus;
    private String remark;
    private String triggerType;
    private String cronExpression;
    private Long modelId;
    private String scopeType;
    private String sourceSystemIds;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date receiveStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date receiveEndTime;
    private String scopeLabel;
    private String status;
    private Date lastExecuteTime;
    private Long lastExecuteDurationMillis;
    private String validFlag;
    /** 创建任务时保存的姓名。 */
    private String createBy;
    /** 创建任务的账号 ID，用于查询其联系电话。 */
    private Long creatorId;
    /** 创建账号登记的联系电话。 */
    private String createPhoneNumber;
    private Date createTime;
    private Long sourceCount;
    private Integer ruleCount;
    private Integer executeCount;
    private Long lastGroupCount;
}
