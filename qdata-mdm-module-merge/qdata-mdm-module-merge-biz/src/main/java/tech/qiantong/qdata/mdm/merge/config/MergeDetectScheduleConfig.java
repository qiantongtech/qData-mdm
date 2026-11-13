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

package tech.qiantong.qdata.mdm.merge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScheduleService;

import javax.annotation.Resource;

/**
 * 应用启动后恢复重复检测 Quartz 配置，并定期校正。
 *
 * @author chentangwen
 */
@Slf4j
@Configuration
public class MergeDetectScheduleConfig {

    private static final long RECONCILE_INTERVAL_MILLIS = 60000L;

    @Resource
    private IMergeDetectScheduleService scheduleService;

    @Bean
    public ThreadPoolTaskScheduler mergeDetectScheduleReconciler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("merge-detect-schedule-");
        return scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restoreSchedules() {
        reconcileSchedules();
        mergeDetectScheduleReconciler().scheduleWithFixedDelay(this::reconcileSchedules,
                RECONCILE_INTERVAL_MILLIS);
    }

    private void reconcileSchedules() {
        try {
            scheduleService.reconcile();
        } catch (Exception ex) {
            log.error("重复检测调度校正失败，稍后重试", ex);
        }
    }
}
