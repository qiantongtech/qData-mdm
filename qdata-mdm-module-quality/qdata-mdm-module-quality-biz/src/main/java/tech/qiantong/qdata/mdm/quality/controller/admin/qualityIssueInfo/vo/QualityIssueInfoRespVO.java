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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 问题数据明细 Response VO 对象 QUALITY_ISSUE_INFO
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题数据明细 Response VO")
@Data
public class QualityIssueInfoRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "质量问题ID")
    @Schema(description = "质量问题ID", example = "")
    private Long issueId;

    @Excel(name = "质量规则ID")
    @Schema(description = "质量规则ID", example = "")
    private Long qualityRuleId;

    @Excel(name = "模型字段ID")
    @Schema(description = "模型字段ID", example = "")
    private Long modelColumnId;

    @Excel(name = "质量任务执行实例ID")
    @Schema(description = "质量任务执行实例ID", example = "")
    private Long lastCheckInstanceId;

    @Excel(name = "问题项状态：1未解决、2已解决")
    @Schema(description = "问题项状态：1未解决、2已解决", example = "")
    private String status;

    @Excel(name = "规则检测失败原因")
    @Schema(description = "规则检测失败原因", example = "")
    private String failReason;

    @Excel(name = "最近一次检测失败时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近一次检测失败时间", example = "")
    private Date lastFoundTime;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志：0未删除，1已删除")
    @Schema(description = "删除标志：0未删除，1已删除", example = "")
    private Boolean delFlag;

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

    /** 质量规则名称 */
    @Excel(name = "质量规则名称")
    @Schema(description = "质量规则名称", example = "")
    private String qualityRuleName;

    /** 质量规则类型 */
    @Excel(name = "质量规则类型")
    @Schema(description = "质量规则类型", example = "")
    private String qualityRuleType;

    /** 模型字段名称 */
    @Excel(name = "模型字段名称")
    @Schema(description = "模型字段名称", example = "")
    private String modelColumnName;

    /** 整改情况 */
    @Excel(name = "整改情况")
    @Schema(description = "整改情况", example = "")
    private String rectificationDesc;

}
