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

package tech.qiantong.qdata.mdm.ingest.service.task;

/**
 * 根据接入任务配置维护 Quartz 调度。
 *
 * @author chentangwen
 */
public interface IIngestTaskScheduleService {

    /**
     * 在配置事务成功提交后同步调度；未启用事务同步时立即执行。
     * 批量同步时逐条处理，全部尝试后再报告失败，不回滚已提交的配置。
     *
     * @param taskIds 待同步的接入任务 ID
     */
    void syncAfterCommit(Long... taskIds);

    /**
     * 在独立事务中锁定任务记录，根据当前配置同步关联调度。
     *
     * @param taskId 接入任务 ID，可以是需要清理调度的已删除任务 ID
     */
    void synchronize(Long taskId);

    /**
     * 逐条校正任务调度，单条失败仅记录日志。
     * 扫描范围包含已删除的任务，以清理残留调度。
     */
    void reconcile();
}
