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

package tech.qiantong.qdata.mdm.master.dal.mapper.change;

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterExecuteTaskDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.Date;
import java.util.List;

/** MASTER_EXECUTE_TASK 持久化 Mapper。 */
public interface MasterExecuteTaskMapper extends BaseMapperX<MasterExecuteTaskDO> {

    MasterExecuteTaskDO selectByRequestId(@Param("requestId") Long requestId);

    MasterExecuteTaskDO selectByExecutionId(@Param("executionId") String executionId);

    List<MasterExecuteTaskDO> selectPendingTasks();

    /** 抢占待执行任务；返回 1 表示抢占成功。 */
    int claimPending(@Param("id") Long id, @Param("workerId") String workerId,
                     @Param("leaseUntil") Date leaseUntil);

    int markSuccess(@Param("id") Long id, @Param("workerId") String workerId,
                    @Param("successCount") Integer successCount,
                    @Param("finishTime") Date finishTime);

    int markFailure(@Param("id") Long id, @Param("workerId") String workerId,
                    @Param("failCount") Integer failCount,
                    @Param("errorMessage") String errorMessage,
                    @Param("retryCount") Integer retryCount,
                    @Param("nextExecuteTime") Date nextExecuteTime,
                    @Param("taskStatus") String taskStatus);
}
