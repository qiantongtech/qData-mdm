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

package tech.qiantong.qdata.mdm.ingest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestRunLogService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskScheduleService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskSubmitService;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 接入任务执行线程池与后台维护调度配置。
 * <p>
 * 提供有界任务执行线程池，并在应用就绪后启动日志归档、Quartz 调度校正和运行实例心跳维护。
 * 日志归档使用独立调度器，调度校正与心跳维护共用维护调度器。
 *
 * @author chentangwen
 */
@Slf4j
@Configuration
public class IngestTaskScheduleConfig {

    /**
     * 日志归档完成至下一轮开始的等待时间，单位：毫秒。
     */
    private static final long LOG_FLUSH_INTERVAL_MILLIS = 2000L;

    /**
     * Quartz 调度校正完成至下一轮开始的等待时间，单位：毫秒。
     */
    private static final long RECONCILE_INTERVAL_MILLIS = 60000L;

    /**
     * 运行实例心跳维护完成至下一轮开始的等待时间，单位：毫秒。
     */
    private static final long HEARTBEAT_INTERVAL_MILLIS = 30000L;

    @Resource
    private IIngestTaskScheduleService scheduleService;

    @Resource
    private ObjectProvider<IIngestTaskSubmitService> submitService;

    @Resource
    private IIngestRunLogService runLogService;

    /**
     * 创建独立的单线程日志归档调度器，避免日志归档占用维护调度器的线程。
     *
     * @return 单线程日志归档调度器
     */
    @Bean
    public ThreadPoolTaskScheduler ingestLogFlusher() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ingest-log-");
        return scheduler;
    }

    /**
     * 创建接入任务执行线程池，限制工作线程数及等待队列容量。
     * <p>
     * 线程和队列均满时拒绝新任务并抛出异常；应用关闭时等待已提交任务完成，最多等待 60 秒。
     *
     * @return 执行接入任务的线程池
     */
    @Bean("ingestTaskExecutor")
    public ThreadPoolTaskExecutor ingestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ingest-run-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

    /**
     * 创建双线程维护调度器，供 Quartz 调度校正和运行实例心跳维护使用。
     *
     * @return 任务调度与运行实例维护调度器
     */
    @Bean
    public ThreadPoolTaskScheduler ingestScheduleReconciler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ingest-schedule-");
        return scheduler;
    }

    /**
     * 应用就绪后注册日志归档、Quartz 调度校正和运行实例心跳维护任务。
     * <p>
     * 各任务采用固定延迟调度，在本轮执行完成后等待对应间隔，再开始下一轮。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startBackgroundTasks() {
        // 将待处理日志归档入库，异常时保留后续周期执行。
        ingestLogFlusher().scheduleWithFixedDelay(() -> {
            try {
                runLogService.flush();
            } catch (Exception ex) {
                log.error("接入日志归档暂不可用，稍后重试", ex);
            }
        }, LOG_FLUSH_INTERVAL_MILLIS);

        // 根据接入任务配置恢复和校正 Quartz 调度。
        ingestScheduleReconciler().scheduleWithFixedDelay(() -> {
            try {
                scheduleService.reconcile();
            } catch (Exception ex) {
                log.error("接入调度校正失败，稍后重试", ex);
            }
        }, RECONCILE_INTERVAL_MILLIS);

        // 为本进程运行实例续期，并回收失去心跳的实例。
        ingestScheduleReconciler().scheduleWithFixedDelay(() -> {
            try {
                submitService.getObject().maintainRuns();
            } catch (Exception ex) {
                log.error("接入执行续期及恢复失败，稍后重试", ex);
            }
        }, HEARTBEAT_INTERVAL_MILLIS);
        log.info("接入后台调度已启动，logFlushInterval={}ms, reconcileInterval={}ms, heartbeatInterval={}ms",
                LOG_FLUSH_INTERVAL_MILLIS, RECONCILE_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS);
    }
}
