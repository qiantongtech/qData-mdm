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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 整改任务明细响应。
 */
@Schema(description = "质量问题整改任务明细 Response VO")
@Data
public class QualityIssueTaskInfoRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "整改任务ID")
    @Schema(description = "整改任务ID", example = "")
    private Long taskId;

    @Excel(name = "质量问题ID")
    @Schema(description = "质量问题ID", example = "")
    private Long issueId;

    @Excel(name = "数据记录ID")
    @Schema(description = "数据记录ID", example = "")
    private Long masterRecordId;

    @Excel(name = "整改状态")
    @Schema(description = "整改状态：1待整改、2整改中、3已整改", example = "")
    private String status;

    @Excel(name = "整改说明")
    @Schema(description = "整改说明", example = "")
    private String requirement;

    @Excel(name = "整改时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "整改时间", example = "")
    private Date rectificationTime;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志：0未删除，1已删除", example = "")
    private Boolean delFlag;

    @Excel(name = "质量规则ID")
    @Schema(description = "质量规则ID", example = "")
    private Long qualityRuleId;

    @Excel(name = "问题项ID")
    @Schema(description = "下发整改时选中的问题项ID，关联QUALITY_ISSUE_INFO.ID", example = "")
    private Long issueInfoId;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
