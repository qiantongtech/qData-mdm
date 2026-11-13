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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import javax.validation.constraints.Positive;
import java.util.Date;

/**
 * 主数据质量任务 创建/修改 Request VO QUALITY_TASK
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "主数据质量任务 Response VO")
@Data
public class QualityTaskSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "质量任务编码", example = "")
    @Size(max = 512, message = "质量任务编码长度不能超过512个字符")
    private String taskCode;

    @Schema(description = "质量任务名称", example = "")
    @Size(max = 512, message = "质量任务名称长度不能超过512个字符")
    private String taskName;

    @Schema(description = "负责人用户ID")
    @Positive(message = "负责人用户ID必须为正数")
    private Long responsibleUserId;

    /** 区分主动清空和局部更新未传负责人，避免切换状态或调度时误清空。 */
    @JsonIgnore
    @Schema(hidden = true)
    private boolean responsibleUserIdSpecified;

    public void setResponsibleUserId(Long responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
        this.responsibleUserIdSpecified = true;
    }

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型版本", example = "")
    @Size(max = 512, message = "主数据模型版本长度不能超过512个字符")
    private String modelVersion;

    @Schema(description = "数据范围类型：1全部权威主数据、2按条件筛选", example = "")
    @Size(max = 512, message = "数据范围类型：1全部权威主数据、2按条件筛选长度不能超过512个字符")
    private String dataScopeType;

    @Schema(description = "数据范围条件配置JSON", example = "")
    @Size(max = 512, message = "数据范围条件配置JSON长度不能超过512个字符")
    private String dataScopeConfig;

    @Schema(description = "是否启用定时执行：0否、1是", example = "")
    @Size(max = 512, message = "是否启用定时执行：0否、1是长度不能超过512个字符")
    private String scheduleFlag;

    @Schema(description = "定时执行配置JSON", example = "")
    @Size(max = 512, message = "定时执行配置JSON长度不能超过512个字符")
    private String scheduleConfig;

    @Schema(description = "任务状态：1启用、2停用", example = "")
    @Size(max = 512, message = "任务状态：1启用、2停用长度不能超过512个字符")
    private String status;

    @Schema(description = "最近一次实际启动执行的时间", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastRunTime;

    @Schema(description = "最近一次运行状态：1执行中、2失败、3部分成功、4成功", example = "")
    @Size(max = 512, message = "最近一次运行状态：1执行中、2失败、3部分成功、4成功长度不能超过512个字符")
    private String lastRunStatus;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
