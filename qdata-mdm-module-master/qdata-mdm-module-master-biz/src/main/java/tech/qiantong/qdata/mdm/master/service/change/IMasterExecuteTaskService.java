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

package tech.qiantong.qdata.mdm.master.service.change;

import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterExecuteTaskDO;

/** 主数据变更执行任务服务。 */
public interface IMasterExecuteTaskService {

    /**
     * 为一条已通过申请创建待执行任务。重复调用具有幂等性。
     *
     * @param requestId 申请 ID
     * @return 已存在或新建的任务 ID
     */
    Long createPendingTask(Long requestId);

    MasterExecuteTaskDO getByRequestId(Long requestId);

    MasterExecuteTaskDO getByExecutionId(String executionId);

    /** 供 Quartz 补扫调用，执行当前可领取的单条新增、修改任务及到期的结果补写任务。 */
    int executePendingTasks();

    /** 事件触发与 Quartz 补扫共用的执行入口，支持普通模型单条 CREATE/UPDATE 及其子模型变更。 */
    boolean executeTask(Long taskId);
}
