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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;

/**
 * 问题数据明细 Request VO 对象 QUALITY_ISSUE_INFO
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题数据明细 Request VO")
@Data
public class QualityIssueInfoPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "质量问题ID", example = "")
    private Long issueId;

    @Schema(description = "质量规则ID", example = "")
    private Long qualityRuleId;

    @Schema(description = "模型字段ID", example = "")
    private Long modelColumnId;

    @Schema(description = "质量任务执行实例ID", example = "")
    private Long lastCheckInstanceId;

    @Schema(description = "问题项状态：1未解决、2已解决", example = "")
    private String status;

    @Schema(description = "规则检测失败原因", example = "")
    private String failReason;

    @Schema(description = "最近一次检测失败时间", example = "")
    private Date lastFoundTime;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;


}
