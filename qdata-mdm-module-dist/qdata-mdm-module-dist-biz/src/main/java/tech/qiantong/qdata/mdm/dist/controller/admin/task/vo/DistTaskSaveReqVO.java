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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 分发任务 创建/修改 Request VO DIST_TASK
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DistTaskSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分发任务编码", example = "")
    @Size(max = 64, message = "分发任务编码长度不能超过64个字符")
    private String code;

    @Schema(description = "分发任务名称", example = "")
    @Size(max = 512, message = "分发任务名称长度不能超过512个字符")
    private String name;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "发布模型版本ID", example = "")
    private Long modelVersionId;

    @Schema(description = "分发目标系统ID", example = "")
    private Long targetSystemId;

    /** 从现有平台用户中选择，保存到 DIST_TASK.RESP_USER_ID。 */
    @Schema(description = "任务负责人平台用户 ID")
    private Long responsibleUserId;

    @Schema(description = "分发方式", example = "")
    @Size(max = 512, message = "分发方式长度不能超过512个字符")
    private String type;

    @Schema(description = "分发数据模式：FULL全量、INCREMENTAL增量", example = "")
    @Size(max = 512, message = "分发数据模式：FULL全量、INCREMENTAL增量长度不能超过512个字符")
    private String dataMode;

    @Schema(description = "执行方式：MANUAL人工执行、SCHEDULED定时执行", example = "")
    @Size(max = 512, message = "执行方式：MANUAL人工执行、SCHEDULED定时执行长度不能超过512个字符")
    private String triggerType;

    @Schema(description = "定时执行的 Cron 表达式")
    @Size(max = 120, message = "调度周期不能超过120个字符")
    private String cronExpression;

    @Schema(description = "重试执行方式：MANUAL人工执行、SCHEDULED定时执行")
    private String retryTriggerType;

    @Schema(description = "失败自动重试：0否、1是")
    private String failRetryFlag;

    @Schema(description = "失败重试次数")
    private Long failRetryTimes;

    @Schema(description = "重试间隔（分钟）")
    private Long failRetryInterval;

    @Schema(description = "超出次数处理：1停止任务、2转人工处理")
    private String failRetryOutType;

    @Schema(description = "首次增量开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date incrementalStartTime;

    @Schema(description = "分发数据范围：ALL全部数据、CONDITION条件筛选", example = "")
    @Size(max = 512, message = "分发数据范围：ALL全部数据、CONDITION条件筛选长度不能超过512个字符")
    private String dataScopeType;

    /** 条件筛选的完整条件列表；创建任务时与任务一起保存，修改时传入表示整体替换。 */
    @Schema(description = "条件筛选列表；全部数据时为空")
    private List<DistTaskFilterSaveReqVO> filters;

    /** 已选择的分发字段及其下游 JSON 属性；修改时 null 表示保持原映射。 */
    @Schema(description = "分发字段映射列表；仅包含已选择的模型字段")
    private List<DistTaskFieldMappingSaveReqVO> fieldMappings;

    @Schema(description = "目标系统API地址", example = "")
    @Size(max = 512, message = "目标系统API地址长度不能超过512个字符")
    private String apiUrl;

    @Schema(description = "HTTP请求方法；本期固定为POST", example = "")
    @Size(max = 512, message = "HTTP请求方法；本期固定为POST长度不能超过512个字符")
    private String httpMethod;

    @Schema(description = "API认证方式：NONE无认证、BEARER Token、API_KEY密钥", example = "")
    @Size(max = 512, message = "API认证方式：NONE无认证、BEARER Token、API_KEY密钥长度不能超过512个字符")
    private String authType;

    @Schema(description = "API认证配置JSON", example = "")
    @Size(max = 512, message = "API认证配置JSON长度不能超过512个字符")
    private String authConfig;

    @Schema(description = "固定请求头配置JSON", example = "")
    @Size(max = 512, message = "固定请求头配置JSON长度不能超过512个字符")
    private String headerConfig;

    @Schema(description = "请求体组织方式", example = "")
    @Size(max = 512, message = "请求体组织方式长度不能超过512个字符")
    private String requestBodyMode;

    @Schema(description = "业务成功判定条件JSON", example = "")
    @Size(max = 512, message = "业务成功判定条件JSON长度不能超过512个字符")
    private String successCondition;

    @Schema(description = "任务状态：ENABLED启用、DISABLED停用", example = "")
    @Size(max = 512, message = "任务状态：ENABLED启用、DISABLED停用长度不能超过512个字符")
    private String status;

    @Schema(description = "数据分发成功时间", example = "")
    private Date lastSuccessTime;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
