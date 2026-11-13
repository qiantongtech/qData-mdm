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
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskScheduleMapper;
import tech.qiantong.qdata.mdm.ingest.job.IngestTaskJob;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskScheduleService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.ENABLED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.NOT_DELETED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.SCHEDULED;

/**
 * 配置提交后同步调度；失败记录服务日志，由启动校正和自动重试恢复。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class IngestTaskScheduleServiceImpl implements IIngestTaskScheduleService {

    /**
     * 接入调度固定使用的业务时区。
     */
    private static final String TIME_ZONE = "Asia/Shanghai";
    /** 接入模块独立维护的作业分组，避免与系统通用任务混用。 */
    private static final String JOB_GROUP = "INGEST_TASK";
    /** 作业与触发器共用的名称前缀，后接接入任务编号。 */
    private static final String JOB_PREFIX = "INGEST_TASK_";
    /**
     * 接入任务允许的 Cron 表达式最大字符数。
     */
    private static final int MAX_CRON_LENGTH = 128;

    @Resource
    private IngestTaskScheduleMapper scheduleMapper;

    @Resource
    private Scheduler scheduler;

    /**
     * 延迟取得代理对象，避免内部调用绕过 REQUIRES_NEW 事务。
     */
    @Autowired
    private ObjectProvider<IIngestTaskScheduleService> self;

    /**
     * 等待任务配置事务提交后同步调度；未开启事务同步时立即执行。
     * 同步失败不撤销已提交的任务配置，由后台校正重试。
     *
     * @param taskId 需要同步的接入任务 ID
     */
    @Override
    public void syncAfterCommit(Long taskId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            attempt(taskId, true);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                attempt(taskId, true);
            }
        });
    }

    /**
     * 在独立事务中按任务配置直接同步 Quartz，不维护系统任务记录。
     *
     * @param taskId 接入任务 ID
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void synchronize(Long taskId) {
        // 锁定任务配置，使同一任务的保存、启停与后台校正串行执行。
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
            // 固定北京时间；错过的执行时间直接跳过，避免恢复后集中补跑。
            CronTrigger desired = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .inTimeZone(TimeZone.getTimeZone(TIME_ZONE))
                            .withMisfireHandlingInstructionDoNothing()).build();
            // 历史表达式可能已无未来执行时间，保持不再触发。
            if (desired.getFireTimeAfter(new Date()) == null) {
                scheduler.deleteJob(jobKey);
                return;
            }
            JobDetail existing = scheduler.getJobDetail(jobKey);
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
                scheduler.rescheduleJob(triggerKey, desired);
                log.info("更新接入调度，taskId={}, cron={}", taskId, cron);
            }
            if (scheduler.getTriggerState(triggerKey) == Trigger.TriggerState.PAUSED) {
                scheduler.resumeJob(jobKey);
                log.info("恢复接入调度，taskId={}", taskId);
            }
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("调度同步失败");
            failure.initCause(ex);
            throw failure;
        }
    }

    /** 核对触发器的表达式、时区和错过执行策略。 */
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
     * 按业务场景校验 Cron；周期校正允许已无下一次执行时间的历史表达式。
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
            throw new ServiceException("Cron 表达式无效或没有下一次执行时间");
        }
    }

    /**
     * 启动后及周期性扫描全部任务，逐个同步并隔离失败。
     * 扫描保留已删除任务，以清理其尚未移除的调度。
     */
    @Override
    public void reconcile() {
        List<Long> taskIds = scheduleMapper.selectTaskIds();
        log.debug("开始校正接入调度，taskCount={}", taskIds.size());
        for (Long taskId : taskIds) {
            attempt(taskId, false);
        }
    }

    /**
     * 通过 Spring 代理调用同步方法，使独立事务注解生效。
     *
     * @param taskId 待同步任务 ID
     * @param report 是否向配置操作调用方报告失败；后台校正仅记录日志
     */
    private void attempt(Long taskId, boolean report) {
        try {
            self.getObject().synchronize(taskId);
        } catch (Exception ex) {
            log.error("接入任务调度同步失败，taskId={}", taskId, ex);
            if (report) {
                throw new ServiceException("任务配置已保存，调度同步失败，将自动重试");
            }
        }
    }
}
