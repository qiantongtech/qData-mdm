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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 问题整改记录日志 创建/修改 Request VO QUALITY_ISSUE_TASK_LOG
 *
 * @author qdata
 * @date 2026-09-23
 */
@Schema(description = "问题整改记录日志 Response VO")
@Data
public class QualityIssueTaskLogSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "整改任务ID", example = "")
    private Long qualityIssueTaskId;

    @Schema(description = "问题数据ID", example = "")
    private Long issueId;

    @Schema(description = "任务下发人ID", example = "")
    private Long taskSendId;

    @Schema(description = "任务处理人ID", example = "")
    private Long taskHandlerId;

    @Schema(description = "下发时间", example = "")
    private Date sendTime;

    @Schema(description = "处理时间", example = "")
    private Date handlerTime;

    @Schema(description = "整改说明", example = "")
    @Size(max = 512, message = "整改说明长度不能超过512个字符")
    private String description;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
