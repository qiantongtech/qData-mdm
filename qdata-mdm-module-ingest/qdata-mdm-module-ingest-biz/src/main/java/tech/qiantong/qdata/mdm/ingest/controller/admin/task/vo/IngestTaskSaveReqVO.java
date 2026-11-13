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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import javax.validation.Valid;
import javax.validation.constraints.*;

import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

@Data
public class IngestTaskSaveReqVO extends BaseEntity {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Size(max = 64)
    private String code;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 64, message = "任务名称不能超过64个字符")
    @Pattern(regexp = "[\\u4e00-\\u9fa5A-Za-z0-9_-]+", message = "任务名称支持中文、英文字符、数字、下划线和短横线")
    private String name;

    @NotNull(message = "请选择来源系统")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceId;

    @NotNull(message = "请选择主数据模型")
    private Long modelId;

    private Long responsibleUserId;

    @NotBlank
    @Pattern(regexp = "DATABASE|FILE|API", message = "接入类型必须为 DATABASE、FILE 或 API")
    private String type;

    @Pattern(regexp = "FULL|INCREMENTAL")
    private String ingestMode;

    @NotBlank
    @Pattern(regexp = "MANUAL|SCHEDULED")
    private String triggerType;

    @Size(max = 120)
    private String cronExpression;

    @NotEmpty(message = "请配置来源记录唯一标识")
    @Size(max = 32)
    private List<@NotBlank String> uniqueFields;

    @Valid
    @NotEmpty(message = "请配置字段映射")
    @Size(max = 500)
    private List<IngestTaskMappingVO> mappings;

    @Pattern(regexp = "0|1")
    private String status;
    @Pattern(regexp = "STOP|CONTINUE")
    private String errorStrategy = "CONTINUE";
    @Pattern(regexp = "IGNORE|WARN|FAIL")
    private String nullStrategy = "IGNORE";
    @NotNull
    @Min(0)
    @Max(10)
    private Integer retryCount = 0;
    @NotNull
    @Min(0)
    @Max(3600)
    private Integer retryIntervalSeconds = 0;

    @Valid
    private IngestTaskDbConfigVO dbConfig;
    @Valid
    private IngestTaskFileConfigVO fileConfig;
    @Valid
    private IngestTaskApiConfigVO apiConfig;
}
