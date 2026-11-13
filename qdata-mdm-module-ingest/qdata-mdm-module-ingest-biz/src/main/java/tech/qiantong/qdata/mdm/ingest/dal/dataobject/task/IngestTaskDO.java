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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import lombok.Data;

@Data
@TableName(value = "INGEST_TASK")
public class IngestTaskDO {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField(value = "CODE")
    private String code;

    @TableField(value = "NAME", keepGlobalFormat = true)
    private String name;

    @TableField(value = "SOURCE_SYSTEM_ID")
    private Long sourceId;

    @TableField(value = "MODEL_ID")
    private Long modelId;

    @TableField(value = "TYPE")
    private String type;

    @TableField(value = "INGEST_MODE", updateStrategy = FieldStrategy.IGNORED)
    private String ingestMode;

    @TableField(value = "TRIGGER_TYPE")
    private String triggerType;

    @TableField(value = "CRON_EXPRESSION", updateStrategy = FieldStrategy.IGNORED)
    private String cronExpression;

    /**
     * 当前占用任务的实例 ID；排队和执行期间均保留，跨节点互斥以此为准。
     */
    @TableField(value = "ACTIVE_RUN_ID")
    private Long activeRunId;

    /**
     * 执行节点最近续期的数据库时间，超时恢复前需加锁复核。
     */
    @TableField(value = "RUN_HEARTBEAT_TIME")
    private Date runHeartbeatTime;

    @TableField(value = "STATUS")
    private String status;

    /**
     * 独立调度开关：1启用、0暂停；人工执行只检查任务状态。
     */
    @TableField(value = "SCHEDULER_STATUS")
    private String schedulerStatus;

    @TableField(value = "ERROR_STRATEGY")
    private String errorStrategy;
    @TableField(value = "NULL_STRATEGY")
    private String nullStrategy;
    @TableField(value = "RETRY_COUNT")
    private Integer retryCount;
    @TableField(value = "RETRY_INTERVAL_SECONDS")
    private Integer retryIntervalSeconds;

    @TableField(value = "RESPONSIBLE_USER_ID")
    private Long responsibleUserId;

    @TableField(value = "VALID_FLAG")
    private String validFlag;

    @TableField(value = "CREATE_BY")
    private String createBy;

    @TableField(value = "CREATOR_ID")
    private Long creatorId;

    @TableField(value = "CREATE_TIME")
    private Date createTime;

    @TableField(value = "UPDATE_BY")
    private String updateBy;

    @TableField(value = "UPDATER_ID")
    private Long updaterId;

    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    @TableLogic(value = "0", delval = "1")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    @TableField(value = "REMARK")
    private String remark;
}
