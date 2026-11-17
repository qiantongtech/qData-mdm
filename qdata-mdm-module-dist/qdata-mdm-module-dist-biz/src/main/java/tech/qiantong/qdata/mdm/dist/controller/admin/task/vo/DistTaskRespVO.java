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

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 分发任务 Response VO 对象 DIST_TASK
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务 Response VO")
@Data
public class DistTaskRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 详情页回显本任务的条件筛选列表，列表分页响应不填充此字段。 */
    @Schema(description = "条件筛选列表")
    private List<DistTaskFilterRespVO> filters;

    /** 详情按需返回已选择的分发字段及其下游 JSON 属性。 */
    @Schema(description = "分发字段映射列表")
    private List<DistTaskFieldMappingRespVO> fieldMappings;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "分发任务编码")
    @Schema(description = "分发任务编码", example = "")
    private String code;

    @Excel(name = "分发任务名称")
    @Schema(description = "分发任务名称", example = "")
    private String name;

    @Excel(name = "主数据模型ID")
    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    /** 列表中展示的主数据模型名称。 */
    @Schema(description = "主数据模型名称")
    private String modelName;

    @Schema(description = "主数据模型所属主数据域名称")
    private String projectName;

    @Schema(description = "主数据域图标")
    private String projectIcon;

    @Schema(description = "主数据模型所属目录名称")
    private String layerName;

    @Excel(name = "发布模型版本ID")
    @Schema(description = "发布模型版本ID", example = "")
    private Long modelVersionId;

    @Excel(name = "分发目标系统ID")
    @Schema(description = "分发目标系统ID", example = "")
    private Long targetSystemId;

    @Schema(description = "任务负责人平台用户 ID")
    private Long responsibleUserId;

    /** 姓名和电话取自任务负责人账号。 */
    @Schema(description = "任务负责人姓名")
    private String responsibleUserName;

    @Schema(description = "任务负责人联系电话")
    private String responsiblePhone;

    @Schema(description = "目标系统编码")
    private String targetSystemCode;

    /** 列表中展示的目标系统名称。 */
    @Schema(description = "目标系统名称")
    private String targetSystemName;

    @Excel(name = "分发方式")
    @Schema(description = "分发方式", example = "")
    private String type;

    @Excel(name = "分发数据模式：FULL全量、INCREMENTAL增量")
    @Schema(description = "分发数据模式：FULL全量、INCREMENTAL增量", example = "")
    private String dataMode;

    @Excel(name = "执行方式：MANUAL人工执行、SCHEDULED定时执行")
    @Schema(description = "执行方式：MANUAL人工执行、SCHEDULED定时执行", example = "")
    private String triggerType;

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

    @Schema(description = "定时执行的 Cron 表达式")
    private String cronExpression;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "首次增量开始时间")
    private Date incrementalStartTime;

    @Excel(name = "分发数据范围：ALL全部数据、CONDITION条件筛选")
    @Schema(description = "分发数据范围：ALL全部数据、CONDITION条件筛选", example = "")
    private String dataScopeType;

    @Excel(name = "目标系统API地址")
    @Schema(description = "目标系统API地址", example = "")
    private String apiUrl;

    @Excel(name = "HTTP请求方法；本期固定为POST")
    @Schema(description = "HTTP请求方法；本期固定为POST", example = "")
    private String httpMethod;

    @Excel(name = "API认证方式：NONE无认证、BEARER Token、API_KEY密钥")
    @Schema(description = "API认证方式：NONE无认证、BEARER Token、API_KEY密钥", example = "")
    private String authType;

    /** 认证值可能包含凭据，接口响应和 Excel 导出均不得回显。 */
    @Schema(description = "API认证配置JSON", example = "")
    private String authConfig;

    /** 固定请求头可能包含敏感值，不导出原始配置。 */
    @Schema(description = "固定请求头配置JSON", example = "")
    private String headerConfig;

    /** 详情页只返回已保存的 API Key 与固定请求头名称、遮蔽值，不包含原始配置。 */
    @Schema(description = "脱敏后的已配置请求头列表")
    private List<Map<String, String>> headers;

    /** 编辑页回显 API Key 的请求头名称，密钥值仍不下发。 */
    @Schema(description = "API Key 请求头名称")
    private String authHeaderName;

    @Excel(name = "请求体组织方式")
    @Schema(description = "请求体组织方式", example = "")
    private String requestBodyMode;

    @Excel(name = "业务成功判定条件JSON")
    @Schema(description = "业务成功判定条件JSON", example = "")
    private String successCondition;

    @Excel(name = "任务状态：ENABLED启用、DISABLED停用")
    @Schema(description = "任务状态：ENABLED启用、DISABLED停用", example = "")
    private String status;

    @Excel(name = "数据分发成功时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "数据分发成功时间", example = "")
    private Date lastSuccessTime;

    /** 最近一次执行的开始时间；与最近一次成功时间分别返回。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近执行时间")
    private Date lastRunTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近执行结束时间")
    private Date lastRunEndTime;

    @Schema(description = "最近执行耗时，单位毫秒")
    private Long lastRunDurationMillis;

    /** 最近一次执行的结果，取自任务执行实例。 */
    @Schema(description = "最近执行结果")
    private String lastRunStatus;

    /** 最近一次执行实例的成功和失败记录数，供关联任务列表展示。 */
    @Schema(description = "最近执行成功数")
    private Long lastSuccessCount;

    @Schema(description = "最近执行失败数")
    private Long lastFailureCount;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Excel(name = "逻辑删除标志：0未删除，1已删除")
    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    /** 创建人的平台用户电话，不使用目标系统联系人电话。 */
    @Schema(description = "创建人电话")
    private String creatorPhone;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
