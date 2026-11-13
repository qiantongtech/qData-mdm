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

package tech.qiantong.qdata.mdm.master.dal.dataobject.change;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 主数据变更执行任务，对应 MASTER_EXECUTE_TASK。
 *
 * <p>一条已通过的 MASTER_CHANGE_REQUEST 只创建一条任务；执行重试复用
 * executionId，不重新生成申请或执行标识。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("MASTER_EXECUTE_TASK")
public class MasterExecuteTaskDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 MASTER_CHANGE_REQUEST.ID。 */
    private Long requestId;

    /** 与申请表一致的执行幂等标识。 */
    private String executionId;

    /** 提交时锁定的物化实例 ID。 */
    private Long instanceId;

    /** 0 待执行、1 执行中、2 成功、3 部分成功、4 失败、5 待确认。 */
    private String taskStatus;

    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Date nextExecuteTime;
    private Date startTime;
    private Date finishTime;
    private String workerId;
    private Date leaseUntil;
    private String errorMessage;

    /** 数据库字符值 0/1。 */
    private Boolean validFlag;

    /** 数据库字符值 0/1。 */
    @TableLogic
    private Boolean delFlag;
}
