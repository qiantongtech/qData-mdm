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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.Date;
import java.util.List;
import javax.validation.constraints.Positive;

/**
 * 主数据质量任务 Request VO 对象 QUALITY_TASK
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "主数据质量任务 Request VO")
@Data
public class QualityTaskPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "质量任务编码", example = "")
    private String taskCode;

    @Schema(description = "质量任务名称", example = "")
    private String taskName;

    @Schema(description = "负责人用户ID")
    @Positive(message = "负责人用户ID必须为正数")
    private Long responsibleUserId;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "所选主数据域或目录下的模型ID集合")
    private List<Long> modelIds;

    @Schema(description = "主数据模型版本", example = "")
    private String modelVersion;

    @Schema(description = "数据范围类型：1全部权威主数据、2按条件筛选", example = "")
    private String dataScopeType;

    @Schema(description = "数据范围条件配置JSON", example = "")
    private String dataScopeConfig;

    @Schema(description = "是否启用定时执行：0否、1是", example = "")
    private String scheduleFlag;

    @Schema(description = "定时执行配置JSON", example = "")
    private String scheduleConfig;

    @Schema(description = "任务状态：1启用、2停用", example = "")
    private String status;

    @Schema(description = "最近一次实际启动执行的时间", example = "")
    private Date lastRunTime;

    @Schema(description = "最近一次运行状态：1执行中、2失败、3部分成功、4成功", example = "")
    private String lastRunStatus;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;


}
