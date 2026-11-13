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

package tech.qiantong.qdata.mdm.master.service.change.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tech.qiantong.qdata.mdm.master.service.change.IMasterExecuteTaskService;

/** 事务提交后通过业务线程池立即触发任务执行。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterExecuteTaskEventListener {
    private final IMasterExecuteTaskService executeTaskService;

    @Async("threadPoolTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(MasterExecuteTaskCreatedEvent event) {
        try {
            executeTaskService.executeTask(event.getTaskId());
        } catch (Exception ex) {
            log.error("异步触发主数据执行任务异常，taskId={}", event.getTaskId(), ex);
        }
    }
}

