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
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * 发起重复检测
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeDetectStartReqVO {
    private String taskName;
    private Long responsibleUserId;
    private String enableStatus;
    private String remark;

    @NotNull(message = "主数据模型不能为空")
    private Long modelId;

    private String triggerType;
    private String cronExpression;

    /** 1全部待治理数据，2指定来源系统和接收时间 */
    private String scopeType;

    private List<Long> sourceSystemIds;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date receiveStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date receiveEndTime;

    /** 预览页码，仅用于重复检测范围预览。 */
    private Integer previewPage;

    /** 预览分页大小，仅用于重复检测范围预览。 */
    private Integer previewPageSize;
}
