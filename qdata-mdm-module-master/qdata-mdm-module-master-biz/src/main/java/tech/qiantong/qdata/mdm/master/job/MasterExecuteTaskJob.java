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

package tech.qiantong.qdata.mdm.master.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.master.service.change.IMasterExecuteTaskService;

/**
 * Quartz 补扫入口，调用目标为 masterExecuteTaskJob.execute()。
 * 执行周期、启停和禁止并发由 SYSTEM_JOB 配置，扫描异常交给统一调度日志记录。
 */
@Slf4j
@Component("masterExecuteTaskJob")
@RequiredArgsConstructor
public class MasterExecuteTaskJob {
    private final IMasterExecuteTaskService executeTaskService;

    public void execute() {
        int completed = executeTaskService.executePendingTasks();
        if (completed > 0) {
            log.info("主数据任务补扫完成，成功处理 {} 个任务", completed);
        }
    }
}

