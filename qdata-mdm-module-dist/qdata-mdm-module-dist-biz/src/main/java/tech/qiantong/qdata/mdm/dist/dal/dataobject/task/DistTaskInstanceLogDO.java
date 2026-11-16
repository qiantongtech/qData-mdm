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
 * 分发任务执行日志 DO 对象 DIST_TASK_INSTANCE_LOG
 *
 * @author qdata
 * @date 2026-10-13
 */
@Data
@TableName(value = "DIST_TASK_INSTANCE_LOG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DIST_TASK_INSTANCE_LOG_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistTaskInstanceLogDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分发任务运行实例ID */
    private Long instanceId;

    /** 权威主数据记录ID */
    private Long masterRecordId;

    /** 主数据编码快照 */
    private String masterCode;

    /** 主数据名称快照 */
    private String masterName;

    /** 分发状态：SUCCESS成功、FAILURE失败 */
    private String distributeStatus;

    /** 本条数据分发时间 */
    private Date distributeTime;

    /** 分发请求数据JSON */
    private String requestData;

    /** 目标系统响应编码 */
    private String responseCode;

    /** 目标系统响应信息 */
    private String responseMessage;

    /** 失败原因 */
    private String failReason;

    /** 重试次数 */
    private Long retryCount;

    /** 是否有效；0：无效，1：有效 */
    private String validFlag;

    /** 删除标志：0未删除，1已删除 */
    // 明确使用未删除 0、已删除 1，避免全局带引号的配置对字符串字段重复加引号。
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}
