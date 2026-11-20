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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.service.task.impl;

import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskScheduleMapper;
import tech.qiantong.qdata.mdm.ingest.job.IngestTaskJob;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskScheduleService;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.ENABLED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.NOT_DELETED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.SCHEDULED;

/**
 * 以接入任务配置为准维护 Quartz 作业和触发器，不写入系统通用任务记录。
 * 配置变更提交后同步调度，失败项由后台校正重试。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class IngestTaskScheduleServiceImpl implements IIngestTaskScheduleService {

    private static final String TIME_ZONE = "Asia/Shanghai";
    /** 独立分组，避免接入调度与系统通用任务混用。 */
    private static final String JOB_GROUP = "INGEST_TASK";
    /** 作业和触发器均以此前缀加任务 ID 命名。 */
    private static final String JOB_PREFIX = "INGEST_TASK_";
    private static final int MAX_CRON_LENGTH = 128;

    @Resource
    private IngestTaskScheduleMapper scheduleMapper;

    @Resource
    private Scheduler scheduler;

    /** 通过 Spring 代理调用同步方法，使每条任务的独立事务生效。 */
    @Autowired
    private ObjectProvider<IIngestTaskScheduleService> self;

    /**
     * 在任务配置提交后同步 Quartz 调度；没有事务同步时立即执行。
     * 同一批任务只注册一次回调，逐条处理后再报告失败，不回滚已提交的配置。
     *
     * @param taskIds 待同步的接入任务 ID
     * @throws ServiceException 一个或多个任务的调度同步失败
     */
    @Override
    public void syncAfterCommit(Long... taskIds) {
        Runnable sync = () -> {
            int failed = syncTasks(Arrays.asList(taskIds));
            if (failed > 0) {
                String message = taskIds.length == 1
                        ? "任务配置已保存，调度同步失败，将自动重试"
                        : "任务变更已保存，" + failed + " 条任务的调度同步失败，将自动重试";
                throw new ServiceException(message);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sync.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sync.run();
            }
        });
    }

    /**
     * 在独立事务中锁定任务记录，并按当前配置同步 Quartz 作业和触发器。
     * 任务已删除或改为人工执行时移除作业；任务停用或调度暂停时暂停已有作业。
     *
     * @param taskId 待同步的接入任务 ID
     * @throws ServiceException Quartz 调度同步失败
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void synchronize(Long taskId) {
        // 配置变更、运行提交和后台校正共用任务行锁，避免按旧配置更新调度。
        IngestTaskDO task = scheduleMapper.lockScheduleTask(taskId);
        JobKey jobKey = new JobKey(JOB_PREFIX + taskId, JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(JOB_PREFIX + taskId, JOB_GROUP);
        try {
            if (task == null || !NOT_DELETED.equals(task.getDelFlag()) || !SCHEDULED.equals(task.getTriggerType())) {
                scheduler.deleteJob(jobKey);
                return;
            }
            if (!ENABLED.equals(task.getStatus()) || !ENABLED.equals(task.getSchedulerStatus())) {
                if (scheduler.checkExists(jobKey)
                        && scheduler.getTriggerState(triggerKey) != Trigger.TriggerState.PAUSED) {
                    scheduler.pauseJob(jobKey);
                    log.info("暂停接入调度，taskId={}", taskId);
                }
                return;
            }
            validateCron(task.getCronExpression(), false);
            String cron = task.getCronExpression().trim();
            // 按北京时间解析 Cron；Quartz 判定错过触发时，不补跑该次任务。
            CronTrigger desired = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .inTimeZone(TimeZone.getTimeZone(TIME_ZONE))
                            .withMisfireHandlingInstructionDoNothing()).build();
            // 已无未来触发时间的计划不再注册，并清理已有调度。
            if (desired.getFireTimeAfter(new Date()) == null) {
                scheduler.deleteJob(jobKey);
                return;
            }
            JobDetail existing = scheduler.getJobDetail(jobKey);
            // 作业类型或关联任务不符时重建，避免沿用错误的执行入口。
            if (existing != null && (existing.getJobClass() != IngestTaskJob.class
                    || !taskId.equals(existing.getJobDataMap().get(IngestTaskJob.TASK_ID)))) {
                scheduler.deleteJob(jobKey);
                existing = null;
            }
            if (existing == null) {
                JobDetail job = JobBuilder.newJob(IngestTaskJob.class).withIdentity(jobKey)
                        .usingJobData(IngestTaskJob.TASK_ID, taskId).build();
                scheduler.scheduleJob(job, desired);
                log.info("创建接入调度，taskId={}, cron={}", taskId, cron);
                return;
            }
            Trigger current = scheduler.getTrigger(triggerKey);
            if (current == null) {
                scheduler.scheduleJob(desired);
            } else if (!matchesTrigger(current, cron)) {
                // 仅在配置不一致时替换触发器，避免每轮校正都重算下次触发时间。
                scheduler.rescheduleJob(triggerKey, desired);
                log.info("更新接入调度，taskId={}, cron={}", taskId, cron);
            }
            Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
            if (state == Trigger.TriggerState.ERROR) {
                // 异常状态不代表 Cron 配置有变，只重置状态，不重建计划。
                scheduler.resetTriggerFromErrorState(triggerKey);
                log.info("重置异常的接入调度触发器，taskId={}", taskId);
            } else if (state == Trigger.TriggerState.PAUSED) {
                scheduler.resumeJob(jobKey);
                log.info("恢复接入调度，taskId={}", taskId);
            }
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("调度同步失败");
            failure.initCause(ex);
            throw failure;
        }
    }

    /**
     * 判断已有触发器是否符合接入任务的定时配置。
     *
     * @param trigger 已有触发器
     * @param cron 任务配置的 Cron 表达式
     * @return 是否为定时触发器且 Cron 表达式、时区及错过触发策略均一致
     */
    private boolean matchesTrigger(Trigger trigger, String cron) {
        if (!(trigger instanceof CronTrigger)) {
            return false;
        }
        CronTrigger actual = (CronTrigger) trigger;
        return cron.equals(actual.getCronExpression()) && TIME_ZONE.equals(actual.getTimeZone().getID())
                && actual.getMisfireInstruction() == CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    }

    /**
     * 保存或启用任务时校验 Cron 语法，并要求存在下一次执行时间。
     *
     * @param expression 用户填写的 Quartz Cron 表达式
     * @throws ServiceException 表达式无效或没有下一次执行时间
     */
    public static void validateCron(String expression) {
        validateCron(expression, true);
    }

    /**
     * 校验 Cron 的长度和语法；调度同步时不在此处拒绝已过期的计划，由同步流程清理。
     *
     * @param expression Quartz Cron 表达式
     * @param requireNextFire 是否要求当前时间之后仍有执行时间
     * @throws ServiceException 表达式不满足当前场景的校验规则
     */
    private static void validateCron(String expression, boolean requireNextFire) {
        try {
            if (expression == null || expression.trim().length() > MAX_CRON_LENGTH) {
                throw new IllegalArgumentException("Cron 长度不合法");
            }
            CronExpression cron = new CronExpression(expression.trim());
            cron.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
            if (requireNextFire && cron.getNextValidTimeAfter(new Date()) == null) {
                throw new IllegalArgumentException("Cron 没有下一次执行时间");
            }
        } catch (Exception ex) {
            throw new ServiceException("Cron 表达式无效或没有下一次执行时间", HttpStatus.WARN);
        }
    }

    /**
     * 应用就绪后及后台周期内逐条校正 Quartz 调度。
     * 扫描包含已删除的任务，用于清理残留作业；单条同步失败仅记录日志。
     */
    @Override
    public void reconcile() {
        List<Long> taskIds = scheduleMapper.selectTaskIds();
        log.debug("开始校正接入调度，taskCount={}", taskIds.size());
        syncTasks(taskIds);
    }

    /**
     * 逐条同步任务并记录失败，不中断后续处理。
     *
     * @param taskIds 待同步的接入任务 ID
     * @return 调度同步失败的任务数量
     */
    private int syncTasks(List<Long> taskIds) {
        int failed = 0;
        for (Long taskId : taskIds) {
            try {
                self.getObject().synchronize(taskId);
            } catch (Exception ex) {
                failed++;
                log.error("接入任务调度同步失败，taskId={}", taskId, ex);
            }
        }
        return failed;
    }
}
