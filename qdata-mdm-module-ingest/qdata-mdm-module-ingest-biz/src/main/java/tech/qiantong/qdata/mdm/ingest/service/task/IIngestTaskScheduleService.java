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

/**
 * 接入配置与 Quartz 调度同步。
 *
 * @author chentangwen
 */
public interface IIngestTaskScheduleService {

    /**
     * 等待当前配置事务提交后同步，避免 Quartz 读取到尚未提交的任务配置。
     *
     * @param taskId 需要同步的接入任务 ID
     */
    void syncAfterCommit(Long taskId);

    /**
     * 在独立事务内锁定任务，创建、更新、暂停或移除关联调度。
     *
     * @param taskId 接入任务 ID，包含需要清理调度的已删除任务
     */
    void synchronize(Long taskId);

    /**
     * 校正所有任务的调度状态，单个任务失败不阻断其他任务。
     */
    void reconcile();
}
