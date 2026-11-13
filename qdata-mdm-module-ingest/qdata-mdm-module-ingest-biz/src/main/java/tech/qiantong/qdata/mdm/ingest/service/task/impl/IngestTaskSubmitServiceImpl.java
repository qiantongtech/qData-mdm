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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.ingest.IngestTaskInstanceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskScheduleMapper;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestRunLogService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskExecuteService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskSubmitService;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.ENABLED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.MANUAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.NOT_DELETED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.RUNNING;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.RUN_FAILED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.SCHEDULED;

/**
 * 先提交实例和数据库占用，再交给专用线程池，互斥覆盖等待和执行全过程。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class IngestTaskSubmitServiceImpl implements IIngestTaskSubmitService {

    /**
     * 运行概览错误摘要的最大字符数。
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;
    /**
     * 超过五分钟未续期才允许恢复流程回收实例。
     */
    private static final long RUN_LEASE_MILLIS = TimeUnit.MINUTES.toMillis(5);
    /**
     * 本机负责续期的任务与实例；不承担跨节点互斥，互斥由数据库占用保证。
     */
    private final ConcurrentMap<Long, Long> localRuns = new ConcurrentHashMap<>();

    @Resource
    private IngestTaskScheduleMapper scheduleMapper;

    @Resource
    private IngestTaskInstanceMapper instanceMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 延迟取得任务服务，避免任务服务与提交服务的直接循环依赖。
     */
    @Autowired
    private ObjectProvider<IIngestTaskService> taskService;

    @Resource
    private IIngestTaskExecuteService executeService;

    @Resource
    private IIngestRunLogService runLogService;

    @Resource(name = "ingestTaskExecutor")
    private Executor executor;

    /**
     * 在独立事务内创建实例并占用任务，事务提交后再提交执行线程。
     * 数据库占用覆盖排队与执行，保证人工和定时入口共用同一互斥规则。
     *
     * @param taskId 接入任务 ID
     * @param triggerType 实际触发方式：人工或定时
     * @param operator 执行操作人
     * @return 已受理的实例快照，不代表执行已经完成
     * @throws ServiceException 任务不可执行、已有运行实例、队列拒绝或日志暂存失败
     */
    @Override
    public IngestTaskRunRespVO submit(Long taskId, String triggerType, String operator) {
        if (!MANUAL.equals(triggerType) && !SCHEDULED.equals(triggerType)) {
            throw new ServiceException("执行方式不正确");
        }
        IngestTaskInstanceDO instance = transaction().execute(status -> {
            IngestTaskDO task = scheduleMapper.lockScheduleTask(taskId);
            checkRunnable(task, triggerType);
            if (task.getActiveRunId() != null) {
                log.warn("拒绝重复提交接入任务，taskId={}, activeRunId={}, triggerType={}, operator={}",
                        taskId, task.getActiveRunId(), triggerType, operator);
                throw new ServiceException("任务正在执行，请勿重复提交");
            }
            IngestTaskInstanceDO run = new IngestTaskInstanceDO();
            run.setTaskId(taskId);
            run.setTriggerType(triggerType);
            run.setBatchNo("BATCH-" + UUID.randomUUID().toString().replace("-", ""));
            // 沿用现有运行中状态，包含等待执行的已受理实例。
            run.setStatus(RUNNING);
            run.setStartTime(new Date());
            run.setDelFlag(NOT_DELETED);
            run.setTotalCount(0L);
            run.setSuccessCount(0L);
            run.setFailureCount(0L);
            run.setSkipCount(0L);
            instanceMapper.insert(run);
            if (run.getId() == null) {
                throw new ServiceException("创建任务运行记录失败");
            }
            if (scheduleMapper.claimRun(taskId, run.getId()) != 1) {
                throw new ServiceException("任务正在执行或已停用");
            }
            return run;
        });
        IngestTaskRunRespVO accepted = BeanUtils.toBean(instance, IngestTaskRunRespVO.class);
        localRuns.put(taskId, instance.getId());
        try {
            runLogService.publish(instance, "INFO", "QUEUED", "任务已提交，等待执行；触发方式：" + triggerType);
            log.info("接入任务已受理，taskId={}, runId={}, triggerType={}, operator={}",
                    taskId, instance.getId(), triggerType, operator);
            // 此处已离开提交事务，工作线程只能读取已提交的实例。
            executor.execute(() -> run(instance, operator));
        } catch (RuntimeException ex) {
            localRuns.remove(taskId, instance.getId());
            String reason = ex instanceof RejectedExecutionException
                    ? "接入执行队列已满，请稍后重试" : "执行日志服务不可用，任务未开始执行";
            log.error("接入任务提交失败，taskId={}, runId={}, triggerType={}",
                    taskId, instance.getId(), triggerType, ex);
            try {
                fail(instance, reason);
            } catch (Exception failure) {
                log.error("接入提交失败待恢复，taskId={}, runId={}", taskId, instance.getId(), failure);
            }
            throw new ServiceException(reason);
        }
        return accepted;
    }

    /**
     * 在线程池中重新校验任务归属、准备配置并执行，结束后释放本实例占用。
     * 若异常收尾也失败，则停止本机续期，由超时恢复流程后续处理。
     *
     * @param instance 已提交数据库并取得占用的实例
     * @param operator 写入来源记录的操作人
     */
    private void run(IngestTaskInstanceDO instance, String operator) {
        try {
            IngestTaskRespVO task = transaction().execute(status -> {
                IngestTaskDO current = scheduleMapper.lockScheduleTask(instance.getTaskId());
                checkRunnable(current, instance.getTriggerType());
                if (!instance.getId().equals(current.getActiveRunId())) {
                    throw new ServiceException("任务执行占用已失效");
                }
                runLogService.publish(instance, "INFO", "PREPARE", "开始校验任务配置和来源连接");
                return taskService.getObject().prepareExecution(instance.getTaskId());
            });
            runLogService.publish(instance, "INFO", "PREPARE", "任务配置校验完成，开始加载模型字段");
            executeService.execute(task,
                    taskService.getObject().modelFields(task.getModelId()), operator, instance);
            transaction().executeWithoutResult(status -> releaseRun(instance.getTaskId(), instance.getId()));
        } catch (Exception ex) {
            log.error("接入任务执行失败，taskId={}, runId={}", instance.getTaskId(), instance.getId(), ex);
            try {
                fail(instance, ex instanceof ServiceException ? ex.getMessage() : "任务执行异常，请查看服务日志");
            } catch (Exception finalizationError) {
                log.error("接入任务收尾失败，runId={}", instance.getId(), finalizationError);
            }
        } finally {
            localRuns.remove(instance.getTaskId(), instance.getId());
        }
    }

    /**
     * 为本机排队和执行中的实例续期，并回收超过租期的实例。
     * 过期判断使用数据库时间；加锁后再次检查心跳，避免回收刚被续期的实例。
     * 已在 Redis 结束的实例仅释放占用，不覆盖其最终结果。
     */
    @Override
    public void maintainRuns() {
        localRuns.forEach((taskId, runId) -> {
            try {
                int updated = scheduleMapper.heartbeat(taskId, runId);
                log.debug("接入任务续期，taskId={}, runId={}, updated={}", taskId, runId, updated);
            } catch (Exception ex) {
                log.error("接入任务续期失败，taskId={}, runId={}", taskId, runId, ex);
            }
        });
        Date databaseTime = scheduleMapper.databaseTime();
        if (databaseTime == null) {
            return;
        }
        Date expiredBefore = new Date(databaseTime.getTime() - RUN_LEASE_MILLIS);
        for (Long taskId : scheduleMapper.selectExpiredRuns(expiredBefore)) {
            try {
                transaction().executeWithoutResult(status -> {
                    IngestTaskDO task = scheduleMapper.lockScheduleTask(taskId);
                    if (task == null || task.getActiveRunId() == null || (task.getRunHeartbeatTime() != null
                            && !task.getRunHeartbeatTime().before(expiredBefore))) {
                        return;
                    }
                    IngestTaskInstanceDO run = runLogService.latest(instanceMapper.selectById(task.getActiveRunId()));
                    if (run != null && RUNNING.equals(run.getStatus())) {
                        run.setStatus(RUN_FAILED);
                        run.setEndTime(new Date());
                        run.setErrorMessage("执行节点超过5分钟未续期，本次运行已终止");
                        runLogService.publish(run, "ERROR", "RECOVERY", run.getErrorMessage());
                    }
                    log.warn("回收心跳超时的接入实例，taskId={}, runId={}, heartbeatTime={}",
                            taskId, task.getActiveRunId(), task.getRunHeartbeatTime());
                    releaseRun(taskId, task.getActiveRunId());
                });
            } catch (Exception ex) {
                log.error("回收接入实例失败，taskId={}", taskId, ex);
            }
        }
    }

    /**
     * 校验任务是否存在、启用，以及实际触发方式是否仍与配置一致。
     *
     * @param task 加锁读取的任务，可能为空
     * @param triggerType 本次实际触发方式
     * @throws ServiceException 任务已删除、停用或不再允许定时触发
     */
    private void checkRunnable(IngestTaskDO task, String triggerType) {
        if (task == null || !NOT_DELETED.equals(task.getDelFlag())) {
            throw new ServiceException("接入任务不存在");
        }
        if (!ENABLED.equals(task.getStatus())) {
            log.warn("接入任务未执行，taskId={}, triggerType={}, reason=任务已停用", task.getId(), triggerType);
            throw new ServiceException("停用任务不能执行");
        }
        if (SCHEDULED.equals(triggerType) && !SCHEDULED.equals(task.getTriggerType())) {
            throw new ServiceException("任务已改为人工执行");
        }
        // 拦截暂停后尚未清除的触发及已排队的定时执行，人工执行不受影响。
        if (SCHEDULED.equals(triggerType) && !ENABLED.equals(task.getSchedulerStatus())) {
            throw new ServiceException("任务调度已暂停");
        }
    }

    /**
     * 持有任务行锁时发布失败终态，再释放本实例占用。
     * Redis 写入失败会使数据库事务回滚，保留占用等待超时恢复。
     *
     * @param instance 需要终止的运行实例
     * @param message 对外展示的失败原因，截断到运行概览允许的长度
     */
    private void fail(IngestTaskInstanceDO instance, String message) {
        transaction().executeWithoutResult(status -> {
            IngestTaskDO current = scheduleMapper.lockScheduleTask(instance.getTaskId());
            if (current == null || !instance.getId().equals(current.getActiveRunId())) {
                log.warn("跳过失效实例的失败收尾，taskId={}, runId={}", instance.getTaskId(), instance.getId());
                return;
            }
            instance.setStatus(RUN_FAILED);
            instance.setEndTime(new Date());
            instance.setErrorMessage(message == null ? "任务执行失败"
                    : message.substring(0, Math.min(MAX_ERROR_MESSAGE_LENGTH, message.length())));
            runLogService.publish(instance, "ERROR", "FAILED", instance.getErrorMessage());
            releaseRun(instance.getTaskId(), instance.getId());
        });
    }

    /**
     * 仅释放仍属于指定实例的执行占用，避免旧实例释放新实例的锁。
     *
     * @param taskId 接入任务 ID
     * @param runId 持有占用的运行实例 ID
     */
    private void releaseRun(Long taskId, Long runId) {
        int updated = scheduleMapper.releaseRun(taskId, runId);
        log.debug("释放接入任务执行占用，taskId={}, runId={}, updated={}", taskId, runId, updated);
    }

    /**
     * 构建独立事务，保证提交、收尾和恢复不依赖调用方事务的提交时机。
     *
     * @return 使用 REQUIRES_NEW 传播行为的事务模板
     */
    private TransactionTemplate transaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }
}
