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
 * 分发任务执行实例 DO 对象 DIST_TASK_INSTANCE
 *
 * @author qdata
 * @date 2026-10-13
 */
@Data
@TableName(value = "DIST_TASK_INSTANCE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DIST_TASK_INSTANCE_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistTaskInstanceDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分发任务ID */
    private Long taskId;

    /** 运行批次号 */
    private String batchNo;

    /** 触发类型：MANUAL手动、SCHEDULED定时 */
    private String triggerType;

    /** 运行状态 */
    private String runStatus;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 总记录数 */
    private Long totalCount;

    /** 成功记录数 */
    private Long successCount;

    /** 失败记录数 */
    private Long failureCount;

    /** 运行异常摘要 */
    private String errorMessage;

    /** 是否有效；0：无效，1：有效 */
    private String validFlag;

    /** 逻辑删除标志：0未删除，1已删除 */
    // 明确使用未删除 0、已删除 1，避免全局带引号的配置对字符串字段重复加引号。
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}
