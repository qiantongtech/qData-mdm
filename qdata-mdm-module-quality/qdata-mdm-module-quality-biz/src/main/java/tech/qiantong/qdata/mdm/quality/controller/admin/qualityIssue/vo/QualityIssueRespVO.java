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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 问题数据 Response VO 对象 QUALITY_ISSUE
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题数据 Response VO")
@Data
public class QualityIssueRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "编号")
    @Schema(description = "编号", example = "")
    private String issueCode;

    @Excel(name = "主数据模型ID")
    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Excel(name = "主数据记录ID")
    @Schema(description = "主数据记录ID", example = "")
    private Long masterRecordId;

    @Schema(description = "质量任务名称")
    private String qualityTaskName;

    @Schema(description = "主数据模型版本")
    private String modelVersion;

    @Excel(name = "问题状态：1待处理、2整改中、3已关闭")
    @Schema(description = "问题状态：1待处理、2整改中、3已关闭", example = "")
    private String status;

    @Excel(name = "首次发现时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "首次发现时间", example = "")
    private Date firstFoundTime;

    @Excel(name = "最近一次发现时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近一次发现时间", example = "")
    private Date lastFoundTime;

    @Excel(name = "人工关闭问题时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "人工关闭问题时间", example = "")
    private Date closeTime;

    @Excel(name = "人工关闭用户ID")
    @Schema(description = "人工关闭用户ID", example = "")
    private Long closerId;

    @Excel(name = "整改情况")
    @Schema(description = "整改情况", example = "")
    private String rectificationStatus;

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

    @Excel(name = "问题数据明细列表")
    @Schema(description = "问题数据明细列表")
    private List<QualityIssueInfoDO> qualityIssueInfoList;

}
