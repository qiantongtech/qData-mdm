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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import lombok.Data;

/**
 * 接入任务实体，对应表 {@code INGEST_TASK}。
 *
 * @author chentangwen
 */
@Data
@TableName(value = "INGEST_TASK")
public class IngestTaskDO {

    /**
     * 接入任务 ID。
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 接入任务编码。
     */
    @TableField(value = "CODE")
    private String code;

    /**
     * 接入任务名称。
     */
    @TableField(value = "NAME", keepGlobalFormat = true)
    private String name;

    /**
     * 来源系统 ID。
     */
    @TableField(value = "SOURCE_SYSTEM_ID")
    private Long sourceId;

    /**
     * 目标主数据模型 ID。
     */
    @TableField(value = "MODEL_ID")
    private Long modelId;

    /**
     * 接入类型：DATABASE 表示数据库，FILE 表示文件，API 表示接口。
     */
    @TableField(value = "TYPE")
    private String type;

    /**
     * 数据库接入模式：FULL 表示全量读取，INCREMENTAL 表示增量读取。
     */
    @TableField(value = "INGEST_MODE", updateStrategy = FieldStrategy.ALWAYS)
    private String ingestMode;

    /**
     * 触发方式：MANUAL 表示人工执行，SCHEDULED 表示定时执行。
     */
    @TableField(value = "TRIGGER_TYPE")
    private String triggerType;

    /**
     * 定时执行的 Cron 表达式。
     */
    @TableField(value = "CRON_EXPRESSION", updateStrategy = FieldStrategy.ALWAYS)
    private String cronExpression;

    /**
     * 当前占用任务的运行实例 ID，用于排队及执行期间的跨节点互斥。
     */
    @TableField(value = "ACTIVE_RUN_ID")
    private Long activeRunId;

    /**
     * 执行节点最近一次续期的数据库时间。
     */
    @TableField(value = "RUN_HEARTBEAT_TIME")
    private Date runHeartbeatTime;

    /**
     * 任务启停状态：0 表示停用，1 表示启用。
     */
    @TableField(value = "STATUS")
    private String status;

    /**
     * 独立调度开关：0 表示暂停，1 表示启用。
     */
    @TableField(value = "SCHEDULER_STATUS")
    private String schedulerStatus;

    /**
     * 行处理失败策略：STOP 表示停止任务，CONTINUE 表示继续处理。
     */
    @TableField(value = "ERROR_STRATEGY")
    private String errorStrategy;

    /**
     * 映射字段空值策略：IGNORE 表示忽略，WARN 表示记录警告，FAIL 表示按失败处理。
     */
    @TableField(value = "NULL_STRATEGY")
    private String nullStrategy;

    /**
     * 允许的重试次数，取值范围为 0～10。
     */
    @TableField(value = "RETRY_COUNT")
    private Integer retryCount;

    /**
     * 重试间隔，单位：秒；启用重试时取值范围为 1～3600。
     */
    @TableField(value = "RETRY_INTERVAL_SECONDS")
    private Integer retryIntervalSeconds;

    /**
     * 任务责任人用户 ID。
     */
    @TableField(value = "RESPONSIBLE_USER_ID")
    private Long responsibleUserId;

    /**
     * 有效标志，1 表示有效。
     */
    @TableField(value = "VALID_FLAG")
    private String validFlag;

    /**
     * 创建人名称。
     */
    @TableField(value = "CREATE_BY")
    private String createBy;

    /**
     * 创建人用户 ID。
     */
    @TableField(value = "CREATOR_ID")
    private Long creatorId;

    /**
     * 创建时间。
     */
    @TableField(value = "CREATE_TIME")
    private Date createTime;

    /**
     * 更新人名称。
     */
    @TableField(value = "UPDATE_BY")
    private String updateBy;

    /**
     * 更新人用户 ID。
     */
    @TableField(value = "UPDATER_ID")
    private Long updaterId;

    /**
     * 最近更新时间。
     */
    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    /**
     * 逻辑删除标志：0 表示未删除，1 表示已删除。
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    /**
     * 备注信息。
     */
    @TableField(value = "REMARK")
    private String remark;
}
