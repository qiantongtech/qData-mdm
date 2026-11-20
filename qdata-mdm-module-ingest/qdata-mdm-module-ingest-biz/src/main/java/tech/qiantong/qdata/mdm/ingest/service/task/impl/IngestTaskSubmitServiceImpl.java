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

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
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
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.ENABLED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.MANUAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.NOT_DELETED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.RUNNING;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.RUN_FAILED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.SCHEDULED;

/**
 * 人工和定时接入任务的统一提交服务，负责异步派发、实例续期和超时恢复。
 *
 * <p>先在独立事务中创建运行实例并取得数据库占用，再交给专用线程池。
 * 占用覆盖排队和执行全过程；本机记录仅用于续期，跨节点互斥以数据库为准。</p>
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
     * @param taskId      接入任务 ID
     * @param triggerType 实际触发方式：人工或定时
     * @param operator    执行操作人
     * @return 已受理的实例快照，不代表执行已经完成
     * @throws ServiceException 任务不可执行、已有运行实例、线程池拒绝或日志暂存失败
     */
    @Override
    public IngestTaskRunRespVO submit(Long taskId, String triggerType, String operator) {
        if (!MANUAL.equals(triggerType) && !SCHEDULED.equals(triggerType)) {
            throw new ServiceException("执行方式不正确", HttpStatus.WARN);
        }
        IngestTaskInstanceDO instance = transaction().execute(status -> {
            IngestTaskDO task = scheduleMapper.lockScheduleTask(taskId);
            checkRunnable(task, triggerType);
            if (ObjectUtil.isNotNull(task.getActiveRunId())) {
                log.warn("拒绝重复提交接入任务，taskId={}, activeRunId={}, triggerType={}, operator={}",
                        taskId, task.getActiveRunId(), triggerType, operator);
                throw new ServiceException("任务正在执行，请勿重复提交", HttpStatus.WARN);
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
            if (ObjectUtil.isNull(run.getId())) {
                throw new ServiceException("创建任务运行记录失败");
            }
            if (scheduleMapper.claimRun(taskId, run.getId()) != 1) {
                throw new ServiceException("任务正在执行或已停用", HttpStatus.WARN);
            }
            return run;
        });
        IngestTaskRunRespVO accepted = BeanUtils.toBean(instance, IngestTaskRunRespVO.class);
        localRuns.put(taskId, instance.getId());
        // 区分日志暂存和线程池提交失败，避免将执行器异常误报为日志服务故障。
        String failureMessage = "执行日志暂存失败，任务未开始执行";
        try {
            runLogService.publish(instance, "INFO", "QUEUED", "任务已提交，等待执行；触发方式：" + triggerType);
            failureMessage = "接入执行线程池暂不可用，请稍后重试";
            // 此处已离开提交事务，工作线程只能读取已提交的实例。
            executor.execute(() -> run(instance));
            log.info("接入任务已受理，taskId={}, runId={}, triggerType={}, operator={}",
                    taskId, instance.getId(), triggerType, operator);
        } catch (RuntimeException ex) {
            localRuns.remove(taskId, instance.getId());
            log.error("接入任务提交失败，taskId={}, runId={}, triggerType={}",
                    taskId, instance.getId(), triggerType, ex);
            try {
                fail(instance, failureMessage);
            } catch (Exception failure) {
                log.error("接入提交失败待恢复，taskId={}, runId={}", taskId, instance.getId(), failure);
            }
            throw new ServiceException(failureMessage);
        }
        return accepted;
    }

    /**
     * 在线程池中重新校验任务归属、准备配置并执行，结束后释放本实例占用。
     * 若异常收尾也失败，则停止本机续期，由超时恢复流程后续处理。
     *
     * @param instance 已提交数据库并取得占用的实例
     */
    private void run(IngestTaskInstanceDO instance) {
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
            executeService.execute(task, taskService.getObject().modelFields(task.getModelId()), instance);
            // Mapper 同时匹配任务和实例编号，旧实例不能释放后续实例的占用。
            transaction().executeWithoutResult(status ->
                    scheduleMapper.releaseRun(instance.getTaskId(), instance.getId()));
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
     * 已在 Redis 结束的实例先补写数据库终态再释放占用，不覆盖其最终结果。
     *
     * <p>续期返回零时移除对应本机记录；续期异常时保留记录供下轮重试。
     * 回收仅标记失败并释放占用，旧执行线程在后续归属校验时停止。</p>
     */
    @Override
    public void maintainRuns() {
        localRuns.forEach((taskId, runId) -> {
            try {
                int updated = scheduleMapper.heartbeat(taskId, runId);
                if (updated == 0) {
                    // 按实例编号移除，避免并发提交的新实例被旧续期结果误删。
                    localRuns.remove(taskId, runId);
                }
                log.debug("接入任务续期，taskId={}, runId={}, updated={}", taskId, runId, updated);
            } catch (Exception ex) {
                log.error("接入任务续期失败，taskId={}, runId={}", taskId, runId, ex);
            }
        });
        Date databaseTime = scheduleMapper.databaseTime();
        if (ObjectUtil.isNull(databaseTime)) {
            return;
        }
        Date expiredBefore = new Date(databaseTime.getTime() - RUN_LEASE_MILLIS);
        for (Long taskId : scheduleMapper.selectExpiredRuns(expiredBefore)) {
            try {
                IngestTaskInstanceDO recovered = transaction().execute(status -> {
                    IngestTaskDO task = scheduleMapper.lockScheduleTask(taskId);
                    if (ObjectUtil.isNull(task) || ObjectUtil.isNull(task.getActiveRunId())
                            || (ObjectUtil.isNotNull(task.getRunHeartbeatTime())
                            && !task.getRunHeartbeatTime().before(expiredBefore))) {
                        return null;
                    }
                    IngestTaskInstanceDO run = runLogService.latest(instanceMapper.lockRun(task.getActiveRunId()));
                    if (ObjectUtil.isNotNull(run) && RUNNING.equals(run.getStatus())) {
                        run.setStatus(RUN_FAILED);
                        run.setEndTime(new Date());
                        run.setErrorMessage("执行节点超过5分钟未续期，本次运行已标记失败");
                    }
                    if (ObjectUtil.isNotNull(run) && run.getEndTime() != null) {
                        instanceMapper.updateById(run);
                    }
                    log.warn("回收心跳超时的接入实例，taskId={}, runId={}, heartbeatTime={}",
                            taskId, task.getActiveRunId(), task.getRunHeartbeatTime());
                    scheduleMapper.releaseRun(taskId, task.getActiveRunId());
                    return run;
                });
                publishFailure(recovered, "RECOVERY");
            } catch (Exception ex) {
                log.error("回收接入实例失败，taskId={}", taskId, ex);
            }
        }
    }

    /**
     * 校验任务是否存在并启用；定时触发还须符合执行方式和调度开关配置。
     *
     * @param task        加锁读取的任务，可能为空
     * @param triggerType 本次实际触发方式
     * @throws ServiceException 任务已删除、停用或不再允许定时触发
     */
    private void checkRunnable(IngestTaskDO task, String triggerType) {
        if (ObjectUtil.isNull(task) || !NOT_DELETED.equals(task.getDelFlag())) {
            throw new ServiceException("接入任务不存在");
        }
        if (!ENABLED.equals(task.getStatus())) {
            log.warn("接入任务未执行，taskId={}, triggerType={}, reason=任务已停用", task.getId(), triggerType);
            throw new ServiceException("停用任务不能执行", HttpStatus.WARN);
        }
        if (SCHEDULED.equals(triggerType) && !SCHEDULED.equals(task.getTriggerType())) {
            throw new ServiceException("任务已改为人工执行", HttpStatus.WARN);
        }
        // 拦截暂停后尚未清除的触发及已排队的定时执行，人工执行不受影响。
        if (SCHEDULED.equals(triggerType) && !ENABLED.equals(task.getSchedulerStatus())) {
            throw new ServiceException("任务调度已暂停", HttpStatus.WARN);
        }
    }

    /**
     * 在数据库事务内保存终态并释放占用，提交后再尝试发布失败日志。
     * 日志服务故障不影响失败收尾，已结束实例的最终结果保持不变。
     *
     * @param instance 需要终止的运行实例
     * @param message  对外展示的失败原因，截断到运行概览允许的长度
     */
    private void fail(IngestTaskInstanceDO instance, String message) {
        IngestTaskInstanceDO failed = transaction().execute(status -> {
            IngestTaskDO current = scheduleMapper.lockScheduleTask(instance.getTaskId());
            if (ObjectUtil.isNull(current) || !instance.getId().equals(current.getActiveRunId())) {
                log.warn("跳过失效实例的失败收尾，taskId={}, runId={}", instance.getTaskId(), instance.getId());
                return null;
            }
            IngestTaskInstanceDO stored = runLogService.latest(instanceMapper.lockRun(instance.getId()));
            if (stored != null && stored.getEndTime() != null) {
                instanceMapper.updateById(stored);
                scheduleMapper.releaseRun(instance.getTaskId(), instance.getId());
                return null;
            }
            instance.setStatus(RUN_FAILED);
            instance.setEndTime(new Date());
            instance.setErrorMessage(ObjectUtil.isNull(message) ? "任务执行失败"
                    : message.substring(0, Math.min(MAX_ERROR_MESSAGE_LENGTH, message.length())));
            instanceMapper.updateById(instance);
            scheduleMapper.releaseRun(instance.getTaskId(), instance.getId());
            return instance;
        });
        publishFailure(failed, "FAILED");
    }

    /**
     * 数据库终态已提交，日志发布失败仅记录服务日志，不再阻塞实例释放。
     */
    private void publishFailure(IngestTaskInstanceDO instance, String stage) {
        if (instance == null || !RUN_FAILED.equals(instance.getStatus())) {
            return;
        }
        try {
            runLogService.publish(instance, "ERROR", stage, instance.getErrorMessage());
        } catch (RuntimeException ex) {
            log.error("接入失败终态已保存，日志暂存失败，taskId={}, runId={}, stage={}",
                    instance.getTaskId(), instance.getId(), stage, ex);
        }
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
