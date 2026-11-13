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

package tech.qiantong.qdata.mdm.ingest.service.task;

import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;

/**
 * 人工和定时执行的统一提交入口。
 *
 * @author chentangwen
 */
public interface IIngestTaskSubmitService {

    /**
     * 创建运行实例并占用任务后异步执行。
     *
     * @param taskId 接入任务 ID
     * @param triggerType 实际触发方式，人工或定时
     * @param operator 操作人
     * @return 已受理的实例，不代表执行已完成
     */
    IngestTaskRunRespVO submit(Long taskId, String triggerType, String operator);

    /**
     * 续租本进程实例，并回收失去心跳的异常退出实例。
     */
    void maintainRuns();
}
