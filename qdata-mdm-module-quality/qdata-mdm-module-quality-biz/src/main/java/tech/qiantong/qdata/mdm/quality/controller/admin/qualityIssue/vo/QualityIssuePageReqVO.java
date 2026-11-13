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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;
import java.util.List;

/**
 * 问题数据 Request VO 对象 QUALITY_ISSUE
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题数据 Request VO")
@Data
public class QualityIssuePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "编号", example = "")
    private String issueCode;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型ID集合，用于按域或目录筛选")
    private List<Long> modelIds;

    @Schema(description = "主数据记录ID", example = "")
    private Long masterRecordId;

    @Schema(description = "问题状态：1待处理、2整改中、3已关闭", example = "")
    private String status;

    @Schema(description = "首次发现时间", example = "")
    private Date firstFoundTime;

    @Schema(description = "最近一次发现时间", example = "")
    private Date lastFoundTime;

    @Schema(description = "人工关闭问题时间", example = "")
    private Date closeTime;

    @Schema(description = "人工关闭用户ID", example = "")
    private Long closerId;

    @Schema(description = "整改情况", example = "")
    private String rectificationStatus;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;
}
