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

package tech.qiantong.qdata.mdm.dist.dal.dataobject.task;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 分发任务 DO 对象 DIST_TASK
 *
 * @author qdata
 * @date 2026-10-13
 */
@Data
@TableName(value = "DIST_TASK")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DIST_TASK_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistTaskDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分发任务编码 */
    private String code;

    /** 分发任务名称 */
    private String name;

    /** 主数据模型ID */
    private Long modelId;

    /** 发布模型版本ID */
    private Long modelVersionId;

    /** 分发目标系统ID */
    private Long targetSystemId;

    /** 本任务指定的负责人平台用户 ID，独立于目标系统负责人。 */
    @TableField("RESP_USER_ID")
    private Long responsibleUserId;

    /** 分发方式 */
    private String type;

    /** 分发数据模式：FULL全量、INCREMENTAL增量 */
    private String dataMode;

    /** 执行方式：MANUAL人工执行、SCHEDULED定时执行 */
    private String triggerType;

    /** 重试执行方式：MANUAL 人工执行、SCHEDULED 定时执行。 */
    private String retryTriggerType;

    /** 失败自动重试：0否、1是 */
    private String failRetryFlag;

    /** 失败重试次数 */
    private Long failRetryTimes;

    /** 重试间隔（分钟） */
    private Long failRetryInterval;

    /** 超出次数处理：1停止任务、2转人工处理 */
    private String failRetryOutType;

    /** 定时执行的 Cron 表达式；人工执行时为空。 */
    private String cronExpression;

    /** 首次增量起点；全量任务为空。 */
    private Date incrementalStartTime;

    /** 分发数据范围：ALL全部数据、CONDITION条件筛选 */
    private String dataScopeType;

    /** 目标系统API地址 */
    private String apiUrl;

    /** HTTP请求方法；本期固定为POST */
    private String httpMethod;

    /** API认证方式：NONE无认证、BEARER Token、API_KEY密钥 */
    private String authType;

    /** API认证配置JSON */
    private String authConfig;

    /** 固定请求头配置JSON */
    private String headerConfig;

    /** 请求体组织方式 */
    private String requestBodyMode;

    /** 业务成功判定条件JSON */
    private String successCondition;

    /** 任务状态：ENABLED启用、DISABLED停用 */
    private String status;

    /** 数据分发成功时间 */
    private Date lastSuccessTime;

    /** 是否有效；0：无效，1：有效 */
    private String validFlag;

    /** 逻辑删除标志：0未删除，1已删除 */
    // 明确使用未删除 0、已删除 1，避免全局带引号的配置对字符串字段重复加引号。
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}
