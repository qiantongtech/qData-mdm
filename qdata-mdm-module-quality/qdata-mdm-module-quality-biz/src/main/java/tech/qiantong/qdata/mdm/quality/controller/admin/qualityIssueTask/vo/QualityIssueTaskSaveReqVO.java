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

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.Date;
import java.util.List;

/**
 * 整改任务下发请求。
 */
@Schema(description = "整改任务下发请求")
@Data
public class QualityIssueTaskSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public interface Create {}

    @NotBlank(groups = Create.class, message = "整改任务名称不能为空")
    @Size(groups = Create.class, max = 128, message = "整改任务名称长度不能超过128个字符")
    @Schema(description = "整改任务名称")
    private String taskName;

    @Size(groups = Create.class, max = 1000, message = "任务说明长度不能超过1000个字符")
    @Schema(description = "任务说明，保存到整改任务的整改说明字段")
    private String taskDescription;

    @Size(groups = Create.class, min = 1, max = 500, message = "一次请选择1至500个问题项")
    @Schema(description = "本次下发选择的问题项 ID 集合，与旧版单项参数互斥")
    private List<@NotNull(groups = Create.class) @Positive(groups = Create.class) Long> issueInfoIds;

    @Positive(groups = Create.class, message = "质量问题编号必须为正数")
    @Schema(description = "旧版单项下发的质量问题 ID")
    private Long issueId;

    @Positive(groups = Create.class, message = "质量规则编号必须为正数")
    @Schema(description = "质量规则 ID，与质量问题 ID 共同定位一个问题项")
    private Long qualityRuleId;

    @NotNull(groups = Create.class, message = "整改负责人不能为空")
    @Positive(groups = Create.class, message = "负责人编号必须为正数")
    @Schema(description = "整改负责人用户 ID")
    private Long assigneeId;

    @Min(groups = Create.class, value = 1, message = "优先级必须为高、中或低")
    @Max(groups = Create.class, value = 3, message = "优先级必须为高、中或低")
    @Schema(description = "优先级：1高、2中、3低")
    private Long priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "整改期限")
    private Date rectificationTime;
}
