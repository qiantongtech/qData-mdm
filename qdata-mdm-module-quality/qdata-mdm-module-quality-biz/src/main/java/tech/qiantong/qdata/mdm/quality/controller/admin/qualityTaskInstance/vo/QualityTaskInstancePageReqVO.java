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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;

/**
 * 主数据质量任务执行实例 Request VO 对象 QUALITY_TASK_INSTANCE
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "主数据质量任务执行实例 Request VO")
@Data
public class QualityTaskInstancePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "质量任务ID", example = "")
    private Long qualityTaskId;

    @Schema(description = "质量任务编码", example = "")
    private String taskCode;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型版本", example = "")
    private String modelVersion;

    @Schema(description = "实例编号", example = "")
    private String executionNo;

    @Schema(description = "负责人ID", example = "")
    private String personCharge;

    @Schema(description = "负责人电话", example = "")
    private String contactNumber;

    @Schema(description = "触发方式：1人工执行、2定时执行", example = "")
    private String triggerType;

    @Schema(description = "执行结果：1执行中、2失败、3部分成功、4成功", example = "")
    private String status;

    @Schema(description = "本次执行开始时间", example = "")
    private Date startTime;

    @Schema(description = "本次执行结束时间", example = "")
    private Date endTime;

    @Schema(description = "检查数据记录数", example = "")
    private Long totalCount;

    @Schema(description = "通过数据记录数", example = "")
    private Long passCount;

    @Schema(description = "问题数据记录数", example = "")
    private Long failCount;

    @Schema(description = "执行失败说明", example = "")
    private String errorMessage;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;


}
