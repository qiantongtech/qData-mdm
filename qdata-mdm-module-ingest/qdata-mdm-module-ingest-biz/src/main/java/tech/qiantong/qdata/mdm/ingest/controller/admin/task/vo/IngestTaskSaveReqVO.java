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
 * Business License: https://docs.qdata.tech/business/policy.html
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

/**
 * 接入任务新增及修改请求对象，包含来源配置、字段映射、触发方式及运行策略。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskSaveReqVO extends BaseEntity {

    /**
     * 接入任务 ID，修改时用于定位任务；新增时由服务端生成。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 接入任务编码，最长 64 个字符；新增时由服务端生成，修改时保留原编码。
     */
    @Size(max = 64)
    private String code;

    /**
     * 接入任务名称，不能为空，最长 64 个字符，支持中文、英文字母、数字、下划线和短横线。
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 64, message = "任务名称不能超过64个字符")
    @Pattern(regexp = "[\\u4e00-\\u9fa5A-Za-z0-9_-]+", message = "任务名称支持中文、英文字符、数字、下划线和短横线")
    private String name;

    /**
     * 来源系统 ID，必填。
     */
    @NotNull(message = "请选择来源系统")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceId;

    /**
     * 目标主数据模型 ID，必填。
     */
    @NotNull(message = "请选择主数据模型")
    private Long modelId;

    /**
     * 任务责任人用户 ID。
     */
    private Long responsibleUserId;

    /**
     * 接入类型：DATABASE 表示数据库，FILE 表示文件，API 表示接口。
     */
    @NotBlank
    @Pattern(regexp = "DATABASE|FILE|API", message = "接入类型必须为 DATABASE、FILE 或 API")
    private String type;

    /**
     * 数据库接入模式：FULL 表示全量读取，INCREMENTAL 表示增量读取。
     */
    @Pattern(regexp = "FULL|INCREMENTAL")
    private String ingestMode;

    /**
     * 触发方式：MANUAL 表示人工执行，SCHEDULED 表示定时执行。
     */
    @NotBlank
    @Pattern(regexp = "MANUAL|SCHEDULED")
    private String triggerType;

    /**
     * 定时执行的 Cron 表达式，最长 120 个字符。
     */
    @Size(max = 120)
    private String cronExpression;

    /**
     * 用于生成来源记录唯一标识的来源字段名称列表，不能为空，最多 32 个字段。
     */
    @NotEmpty(message = "请配置来源记录唯一标识")
    @Size(max = 32)
    private List<@NotBlank String> uniqueFields;

    /**
     * 来源字段到模型字段的映射配置，不能为空，最多 500 项。
     */
    @Valid
    @NotEmpty(message = "请配置字段映射")
    @Size(max = 500)
    private List<IngestTaskMappingVO> mappings;

    /**
     * 任务启停状态：0 表示停用，1 表示启用；新增时未指定则默认停用。
     */
    @Pattern(regexp = "0|1")
    private String status;

    /**
     * 行处理失败策略：STOP 表示停止任务，CONTINUE 表示继续处理，默认 CONTINUE。
     */
    @Pattern(regexp = "STOP|CONTINUE")
    private String errorStrategy = "CONTINUE";

    /**
     * 映射字段空值策略：IGNORE 表示忽略，WARN 表示记录警告，FAIL 表示按失败处理，默认 IGNORE。
     */
    @Pattern(regexp = "IGNORE|WARN|FAIL")
    private String nullStrategy = "IGNORE";

    /**
     * 允许的重试次数，取值范围为 0～10，默认 0 表示不重试。
     */
    @NotNull
    @Min(0)
    @Max(10)
    private Integer retryCount = 0;

    /**
     * 重试间隔，单位：秒；取值范围为 0～3600，启用重试时至少为 1。
     */
    @NotNull
    @Min(0)
    @Max(3600)
    private Integer retryIntervalSeconds = 0;

    /**
     * 数据库来源配置，DATABASE 类型任务使用。
     */
    @Valid
    private IngestTaskDbConfigVO dbConfig;

    /**
     * 文件来源配置，FILE 类型任务使用。
     */
    @Valid
    private IngestTaskFileConfigVO fileConfig;

    /**
     * API 来源配置，API 类型任务使用。
     */
    @Valid
    private IngestTaskApiConfigVO apiConfig;
}
