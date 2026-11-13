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

package tech.qiantong.qdata.mdm.merge.service.detect.impl;

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
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskScheduleMapper;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.job.MergeDetectTaskJob;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScheduleService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * 重复检测配置提交后同步 Quartz；后台校正会恢复保存的调度。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class MergeDetectScheduleServiceImpl implements IMergeDetectScheduleService {

    private static final String TIME_ZONE = "Asia/Shanghai";
    private static final String JOB_GROUP = "MERGE_DETECT_TASK";
    private static final String JOB_PREFIX = "MERGE_DETECT_TASK_";
    private static final int MAX_CRON_LENGTH = 128;

    @Resource
    private MergeDuplicateTaskScheduleMapper scheduleMapper;

    @Resource
    private Scheduler scheduler;

    @Autowired
    private ObjectProvider<IMergeDetectScheduleService> self;

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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void synchronize(Long taskId) {
        MergeDuplicateTaskDO task = scheduleMapper.lockScheduleTask(taskId);
        JobKey jobKey = new JobKey(JOB_PREFIX + taskId, JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(JOB_PREFIX + taskId, JOB_GROUP);
        try {
            if (task == null || !MergeCodes.NO.equals(task.getDelFlag())
                    || !MergeCodes.YES.equals(task.getValidFlag())
                    || !"ENABLED".equals(task.getEnableStatus())
                    || !MergeCodes.YES.equals(task.getSchedulerStatus())
                    || !MergeCodes.TRIGGER_SCHEDULED.equals(task.getTriggerType())) {
                scheduler.deleteJob(jobKey);
                return;
            }
            validateCron(task.getCronExpression(), false);
            String cron = task.getCronExpression().trim();
            CronTrigger desired = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .inTimeZone(TimeZone.getTimeZone(TIME_ZONE))
                            .withMisfireHandlingInstructionDoNothing()).build();
            if (desired.getFireTimeAfter(new Date()) == null) {
                scheduler.deleteJob(jobKey);
                return;
            }
            JobDetail existing = scheduler.getJobDetail(jobKey);
            if (existing != null && (existing.getJobClass() != MergeDetectTaskJob.class
                    || !taskId.equals(existing.getJobDataMap().get(MergeDetectTaskJob.TASK_ID)))) {
                scheduler.deleteJob(jobKey);
                existing = null;
            }
            if (existing == null) {
                JobDetail job = JobBuilder.newJob(MergeDetectTaskJob.class).withIdentity(jobKey)
                        .usingJobData(MergeDetectTaskJob.TASK_ID, taskId).build();
                scheduler.scheduleJob(job, desired);
                log.info("创建重复检测调度，taskId={}, cron={}", taskId, cron);
                return;
            }
            Trigger current = scheduler.getTrigger(triggerKey);
            if (current == null) {
                scheduler.scheduleJob(desired);
            } else if (!matchesTrigger(current, cron)) {
                scheduler.rescheduleJob(triggerKey, desired);
                log.info("更新重复检测调度，taskId={}, cron={}", taskId, cron);
            }
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("调度同步失败");
            failure.initCause(ex);
            throw failure;
        }
    }

    private boolean matchesTrigger(Trigger trigger, String cron) {
        if (!(trigger instanceof CronTrigger)) {
            return false;
        }
        CronTrigger actual = (CronTrigger) trigger;
        return cron.equals(actual.getCronExpression()) && TIME_ZONE.equals(actual.getTimeZone().getID())
                && actual.getMisfireInstruction() == CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    }

    public static void validateCron(String expression) {
        validateCron(expression, true);
    }

    private static void validateCron(String expression, boolean requireNextFire) {
        try {
            if (expression == null || expression.trim().isEmpty() || expression.trim().length() > MAX_CRON_LENGTH) {
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

    @Override
    public void reconcile() {
        List<Long> taskIds = scheduleMapper.selectTaskIds();
        for (Long taskId : taskIds) {
            attempt(taskId, false);
        }
    }

    private void attempt(Long taskId, boolean report) {
        try {
            self.getObject().synchronize(taskId);
        } catch (Exception ex) {
            log.error("重复检测任务调度同步失败，taskId={}", taskId, ex);
            if (report) {
                throw new ServiceException("任务配置已保存，调度同步失败，将自动重试");
            }
        }
    }
}
