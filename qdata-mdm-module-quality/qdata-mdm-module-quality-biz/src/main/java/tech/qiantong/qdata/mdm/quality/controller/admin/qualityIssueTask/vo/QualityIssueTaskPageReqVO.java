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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.Pattern;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;
import java.util.List;

/**
 * 问题整改任务 Request VO 对象 QUALITY_ISSUE_TASK
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题整改任务 Request VO")
@Data
public class QualityIssueTaskPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "整改任务名称或任务编号关键词")
    private String keyword;

    @Schema(description = "整改任务名称，模糊查询")
    private String taskName;

    @Schema(description = "问题项关联的质量规则 ID")
    private Long qualityRuleId;

    @Schema(description = "ID", example = "")
    private Long id;

    @Schema(description = "勾选导出的整改任务 ID 列表")
    private List<Long> ids;

    @Schema(description = "整改任务编号", example = "")
    private String taskNo;

    @Schema(description = "质量问题ID", example = "")
    private Long issueId;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型版本", example = "")
    private String modelVersion;

    @Schema(description = "数据记录ID", example = "")
    private Long masterRecordId;

    @Schema(description = "整改负责人用户ID", example = "")
    private Long assigneeId;

    @Schema(description = "优先级：1高、2中、3低")
    private Long priority;

    @Schema(description = "整改任务状态：1待审批、2审批中、3已完成、4已取消、5处理中；", example = "")
    @Pattern(regexp = "[1-5]", message = "任务状态不正确")
    private String status;

    @Schema(description = "整改说明", example = "")
    private String requirement;

    @Schema(description = "整改时间", example = "")
    private Date rectificationTime;

    @Schema(description = "是否逾期：1逾期、0未逾期", example = "")
    private Integer overdue;

    @Schema(description = "下发时间起（创建时间范围）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    @Schema(description = "下发时间止（创建时间范围）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;


}
