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

package tech.qiantong.qdata.mdm.ingest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 执行使用有界线程池；日志归档、调度校正和运行续期独立调度。
 *
 * @author chentangwen
 */
@Slf4j
@Configuration
public class IngestTaskScheduleConfig {

    /**
     * 日志归档两轮之间的等待时间，单位毫秒。
     */
    private static final long LOG_FLUSH_INTERVAL_MILLIS = 2000L;
    /**
     * 调度配置校正两轮之间的等待时间，单位毫秒。
     */
    private static final long RECONCILE_INTERVAL_MILLIS = 60000L;
    /**
     * 本机实例续期及超时扫描的间隔，单位毫秒。
     */
    private static final long HEARTBEAT_INTERVAL_MILLIS = 30000L;

    @Resource
    private IIngestTaskScheduleService scheduleService;

    @Autowired
    private ObjectProvider<IIngestTaskSubmitService> submitService;

    @Resource
    private IIngestRunLogService runLogService;

    /**
     * 创建专用日志归档调度器，避免归档耗时阻塞心跳和调度校正。
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
     * 创建有界执行线程池；饱和时拒绝提交，交由提交服务记录失败。
     * 关闭时最多等待六十秒，未正常结束的实例由心跳恢复机制处理。
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
     * 创建维护调度器，分别承担配置校正和运行实例续期。
     *
     * @return 使用两个线程的维护调度器
     */
    @Bean
    public ThreadPoolTaskScheduler ingestScheduleReconciler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ingest-schedule-");
        return scheduler;
    }

    /**
     * 应用就绪后启动日志归档、调度校正及心跳维护。
     * 采用固定延迟，在本次调用结束后等待指定间隔再执行。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void restoreSchedules() {
        ingestLogFlusher().scheduleWithFixedDelay(this::flushLogs, LOG_FLUSH_INTERVAL_MILLIS);
        // 应用就绪后，以接入任务配置恢复和校正 Quartz 调度。
        ingestScheduleReconciler().scheduleWithFixedDelay(this::reconcileSchedules, RECONCILE_INTERVAL_MILLIS);
        ingestScheduleReconciler().scheduleWithFixedDelay(this::maintainRuns, HEARTBEAT_INTERVAL_MILLIS);
        log.info("接入后台调度已启动，logFlushInterval={}ms, reconcileInterval={}ms, heartbeatInterval={}ms",
                LOG_FLUSH_INTERVAL_MILLIS, RECONCILE_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS);
    }

    /**
     * 执行一轮日志归档；异常留在服务日志中，待下一轮重试。
     */
    private void flushLogs() {
        try {
            runLogService.flush();
        } catch (Exception ex) {
            log.error("接入日志归档暂不可用，稍后重试", ex);
        }
    }

    /**
     * 执行一轮配置校正；兜底捕获扫描异常，保证后续轮次仍可执行。
     */
    private void reconcileSchedules() {
        try {
            scheduleService.reconcile();
        } catch (Exception ex) {
            log.error("接入调度校正失败，稍后重试", ex);
        }
    }

    /**
     * 执行一轮续期和超时恢复；捕获异常以保留周期维护能力。
     */
    private void maintainRuns() {
        try {
            submitService.getObject().maintainRuns();
        } catch (Exception ex) {
            log.error("接入执行续期及恢复失败，稍后重试", ex);
        }
    }
}
