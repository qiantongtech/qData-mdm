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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskScheduleMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskDbConfigMapper;
import tech.qiantong.qdata.mdm.ingest.mapping.FieldMappingPlan;
import tech.qiantong.qdata.mdm.ingest.reader.task.IngestReadContext;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestRunLogService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskExecuteService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskReadService;
import tech.qiantong.qdata.mdm.ingest.utils.task.IngestTaskExecutionUtils;
import tech.qiantong.qdata.mdm.ingest.utils.task.IngestRunPolicy;
import tech.qiantong.qdata.mdm.ingest.writer.task.IngestSourceTableSession;
import tech.qiantong.qdata.mdm.ingest.writer.task.IngestSourceTableWriter;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.READ_ALL_ROWS;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.DATABASE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.INCREMENTAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.RUN_FAILED;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.RUN_SUCCESS;

/**
 * 接入任务执行服务，负责行处理、实体来源表写入和运行统计。
 * 提交入口负责互斥和配置校验，每条来源数据独立事务，避免读取期间长期持有数据库事务。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class IngestTaskExecuteServiceImpl implements IIngestTaskExecuteService {

    /**
     * 运行概览错误摘要的最大字符数，不限制独立错误事件的条数。
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;
    /**
     * 正常处理期间发布统计进度的记录数间隔；错误事件即时发布。
     */
    private static final long PROGRESS_RECORD_INTERVAL = 100L;

    @Resource
    private IIngestRunLogService runLogService;

    @Resource
    private IIngestTaskReadService taskReadService;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Resource
    private IngestTaskScheduleMapper scheduleMapper;

    @Resource
    private IngestSourceTableWriter sourceTableWriter;

    @Resource
    private IngestTaskDbConfigMapper taskDbConfigMapper;

    /**
     * 执行已提交实例；每行事务校验占用，防止失去心跳的旧线程继续写入。
     *
     * @param task     已完成配置校验并锁定的接入任务
     * @param fields   模型业务字段
     * @param operator 操作人
     * @param instance 已受理的运行实例
     * @return 本次运行结果
     */
    @Override
    public IngestTaskRunRespVO execute(IngestTaskRespVO task, List<IngestModelFieldRespVO> fields, String operator,
                                       IngestTaskInstanceDO instance) {
        instance.setStartTime(new Date());
        boolean incremental = DATABASE.equals(task.getType()) && INCREMENTAL.equals(task.getIngestMode());
        if (incremental) {
            instance.setCursorBefore(task.getCurrentIncrementValue() == null
                    ? task.getDbConfig().getInitialIncrementValue() : task.getCurrentIncrementValue());
            instance.setCursorAfter(instance.getCursorBefore());
        }
        publishOwned(instance, "INFO", "START", "开始执行接入任务，来源类型：" + task.getType());
        TransactionTemplate rowTransaction = new TransactionTemplate(transactionManager);
        // 逐行累计统计，失败重试耗尽后按异常策略停止或继续。
        StringBuilder errors = new StringBuilder();
        IngestReadContext readContext = new IngestReadContext(task, READ_ALL_ROWS,
                incremental ? task.getCurrentIncrementValue() : null);
        String stage = "MAPPING";
        try {
            publishOwned(instance, "INFO", stage, "开始准备字段映射");
            FieldMappingPlan plan = FieldMappingPlan.compile(task.getMappings(), fields, task.getType());
            publishOwned(instance, "INFO", stage, "字段映射准备完成，映射字段数："
                    + task.getMappings().stream().filter(mapping -> mapping.getTargetFieldId() != null).count());
            stage = "WRITE_PREPARE";
            List<String> targetColumns = fields.stream().map(IngestModelFieldRespVO::getColumnCode)
                    .sorted().collect(Collectors.toList());
            publishOwned(instance, "INFO", stage, "开始连接并校验实体来源表");
            try (IngestSourceTableSession writer = sourceTableWriter.open(task.getModelId(), targetColumns)) {
                publishOwned(instance, "INFO", stage, "实体来源表已就绪，映射数据及采集扩展字段将写入目标表");
                stage = "READ";
                publishOwned(instance, "INFO", stage, "开始读取来源数据，读取、转换和写入按记录依次执行");
                IngestRunPolicy.retry(() -> taskReadService.readRows(readContext, row -> {
                            instance.setTotalCount(instance.getTotalCount() + 1);
                            String rowError = null;
                            try {
                                if (incremental && row.getIncrementValue() == null) {
                                    throw new ServiceException("来源记录缺少增量字段值");
                                }
                                AtomicBoolean commitStarted = new AtomicBoolean(false);
                                IngestRunPolicy.retry(() -> {
                                            try {
                                                processInTransaction(rowTransaction, task, instance, row.getValues(), plan, fields, writer, commitStarted);
                                            } catch (RuntimeException failure) {
                                                writer.rollbackRow();
                                                throw failure;
                                            }
                                        }, task.getRetryCount(), task.getRetryIntervalSeconds(), () -> !commitStarted.get(),
                                        attempt -> publishOwned(instance, "WARN", "RETRY", row.getLocation()
                                                + "：第" + attempt + "次重试，等待" + task.getRetryIntervalSeconds() + "秒"));
                                instance.setSuccessCount(instance.getSuccessCount() + 1);
                            } catch (Exception ex) {
                                writer.rollbackRow();
                                if (ex instanceof CancellationException) {
                                    throw ex;
                                }
                                log.error("接入记录处理失败，taskId={}, runId={}, location={}",
                                        task.getId(), instance.getId(), row.getLocation(), ex);
                                instance.setFailureCount(instance.getFailureCount() + 1);
                                appendExecutionError(errors, row.getLocation(), ex);
                                rowError = row.getLocation() + "：" + failureMessage(ex);
                            }
                            instance.setErrorMessage(errors.length() == 0 ? null : errors.toString());
                            if (rowError != null) {
                                publishOwned(instance, "ERROR", "ROW", rowError);
                                if ("STOP".equals(task.getErrorStrategy())) {
                                    throw new ServiceException("异常处理策略要求停止任务：" + rowError);
                                }
                            }
                            if (instance.getTotalCount() % PROGRESS_RECORD_INTERVAL == 0) {
                                publishOwned(instance, "INFO", "PROGRESS", progressMessage(instance));
                            }
                        }), task.getRetryCount(), task.getRetryIntervalSeconds(),
                        () -> instance.getTotalCount() == 0 && readContext.getSkipCount() == 0,
                        attempt -> publishOwned(instance, "WARN", "READ_RETRY", "来源读取失败，第" + attempt
                                + "次重试，等待" + task.getRetryIntervalSeconds() + "秒"));
                publishOwned(instance, "INFO", "READ_COMPLETE", "来源读取完成，读取阶段跳过空记录 "
                        + readContext.getSkipCount() + " 条；" + progressMessage(instance));
            }
            instance.setStatus(resolveRunStatus(instance));
            if (incremental && RUN_SUCCESS.equals(instance.getStatus()) && readContext.getLastIncrementValue() != null) {
                stage = "CHECKPOINT";
                saveIncrementValue(task, instance, readContext.getLastIncrementValue().toString());
            }
        } catch (Exception ex) {
            log.error("接入任务处理异常，taskId={}, runId={}, stage={}",
                    task.getId(), instance.getId(), stage, ex);
            instance.setStatus(RUN_FAILED);
            String location = "MAPPING".equals(stage) ? "字段映射准备失败"
                    : "WRITE_PREPARE".equals(stage) ? "来源表写入准备失败"
                    : "CHECKPOINT".equals(stage) ? "增量位置保存失败" : "来源读取失败";
            appendExecutionError(errors, location, ex);
            // 独立保留致命错误，避免被前面的行错误挤出长度受限的错误汇总。
            publishOwned(instance, "ERROR", stage, location + "：" + failureMessage(ex));
        } finally {
            instance.setTotalCount(instance.getTotalCount() + readContext.getSkipCount());
            instance.setSkipCount(instance.getSkipCount() + readContext.getSkipCount());
            // 最终统计和过程日志统一先写 Redis，归档成功后再移除待入库事件。
            instance.setErrorMessage(errors.length() == 0 ? null : errors.toString());
            instance.setEndTime(new Date());
            String level = RUN_SUCCESS.equals(instance.getStatus()) ? "INFO" : "ERROR";
            publishOwned(instance, level, "FINISH", "执行结束，" + progressMessage(instance)
                    + "；耗时 " + (instance.getEndTime().getTime() - instance.getStartTime().getTime()) + " 毫秒");
            log.info("接入任务执行结束，taskId={}, runId={}, status={}, total={}, success={}, failure={}, skip={}",
                    task.getId(), instance.getId(), instance.getStatus(), instance.getTotalCount(),
                    instance.getSuccessCount(), instance.getFailureCount(), instance.getSkipCount());
        }
        IngestTaskRunRespVO result = BeanUtils.toBean(instance, IngestTaskRunRespVO.class);
        result.setDurationMillis(instance.getEndTime().getTime() - instance.getStartTime().getTime());
        return result;
    }

    /**
     * 完整读取且全部写入成功后才推进位置，失败或空结果保持原位置。
     */
    private void saveIncrementValue(IngestTaskRespVO task, IngestTaskInstanceDO instance, String cursor) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            IngestTaskDO current = scheduleMapper.lockScheduleTask(task.getId());
            if (current == null || !instance.getId().equals(current.getActiveRunId())) {
                throw new CancellationException("任务执行占用已失效");
            }
            if (taskDbConfigMapper.updateIncrementValue(task.getDbConfigId(), task.getId(), cursor) != 1) {
                throw new ServiceException("增量配置已变更，无法保存本次采集位置");
            }
        });
        instance.setCursorAfter(cursor);
    }

    /**
     * 持有任务行锁时写入阶段日志，确保失效的执行线程不能覆盖终态。
     * Redis 操作失败会向外传播，由执行收尾和超时恢复流程处理。
     *
     * @param instance 当前运行实例及进度
     * @param level    日志级别
     * @param stage    执行阶段
     * @param message  阶段说明或可定位的错误信息
     * @throws CancellationException 任务已不再归属于当前实例
     */
    private void publishOwned(IngestTaskInstanceDO instance, String level, String stage, String message) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            IngestTaskDO current = scheduleMapper.lockScheduleTask(instance.getTaskId());
            if (current == null || !instance.getId().equals(current.getActiveRunId())) {
                throw new CancellationException("任务执行占用已失效");
            }
            runLogService.publish(instance, level, stage, message);
        });
    }

    /**
     * 生成各执行阶段共用的累计处理摘要。
     *
     * @param instance 持有累计总数、成功数、失败数和跳过数的实例
     * @return 供运行日志展示的统计文本
     */
    private String progressMessage(IngestTaskInstanceDO instance) {
        return "已处理 " + instance.getTotalCount() + " 条，成功 " + instance.getSuccessCount()
                + " 条，失败 " + instance.getFailureCount() + " 条，跳过 " + instance.getSkipCount() + " 条";
    }

    /**
     * 平台事务只锁定任务以校验执行占用；实体来源行在目标业务库事务内提交。
     * 持有任务锁直至业务库提交完成，防止旧执行线程失去占用后继续写入。
     */
    private void processInTransaction(TransactionTemplate transaction, IngestTaskRespVO task,
                                      IngestTaskInstanceDO instance, Map<String, Object> row,
                                      FieldMappingPlan plan, List<IngestModelFieldRespVO> fields,
                                      IngestSourceTableSession writer, AtomicBoolean commitStarted) {
        transaction.executeWithoutResult(status -> {
            IngestTaskDO current = scheduleMapper.lockScheduleTask(task.getId());
            if (current == null || !instance.getId().equals(current.getActiveRunId())) {
                throw new CancellationException("任务执行占用已失效");
            }
            Map<String, Object> identityValues = buildIdentityValues(task.getUniqueFields(), row, plan);
            Map<String, Object> mappedValues = plan.apply(row);
            boolean hasNull = IngestRunPolicy.checkNulls(mappedValues, task.getNullStrategy());
            if (hasNull && "WARN".equals(task.getNullStrategy())) {
                runLogService.publish(instance, "WARN", "NULL_VALUE", "映射字段存在空值，按空值策略继续执行");
            }
            Map<String, Object> modelData = IngestTaskExecutionUtils.normalizeModelData(mappedValues, fields);
            writer.writeRecord(task, instance, identityValues, modelData);
            // 提交异常可能是结果未知，禁止重试，避免实际已提交的数据被再次追加。
            commitStarted.set(true);
            writer.commitRow();
        });
    }

    /**
     * 按配置顺序提取来源唯一字段的原始值，不使用转换后的模型值。
     *
     * @param uniqueFields 已配置的来源唯一字段
     * @param row          原始来源记录
     * @param plan         已编译的字段映射计划
     * @return 用于计算来源稳定记录 ID 的有序标识值
     */
    private Map<String, Object> buildIdentityValues(List<String> uniqueFields, Map<String, Object> row, FieldMappingPlan plan) {
        if (uniqueFields == null || uniqueFields.isEmpty()) {
            throw new ServiceException("请先配置来源记录唯一标识后再执行");
        }
        LinkedHashMap<String, Object> uniqueValues = new LinkedHashMap<>();
        for (String field : uniqueFields) {
            Object value = plan.readIdentity(field, row);
            uniqueValues.put(field, value);
        }
        return uniqueValues;
    }

    /**
     * 已知业务异常直接展示原因，未知异常提示查看服务日志。
     * 底层异常堆栈由捕获边界记录，此方法不重复输出日志。
     *
     * @param ex 本次处理异常
     * @return 运行日志和错误摘要使用的文本
     */
    private String failureMessage(Exception ex) {
        return ex instanceof ServiceException && ex.getMessage() != null
                ? ex.getMessage() : "数据处理失败，请查看服务日志";
    }

    /**
     * 追加带位置的运行错误摘要，并限制字符长度。
     * 达到上限只停止追加概览摘要，各条错误日志仍独立发布。
     *
     * @param errors   本次运行的累计错误摘要
     * @param location 来源位置或失败阶段
     * @param ex       处理异常
     */
    private void appendExecutionError(StringBuilder errors, String location, Exception ex) {
        if (errors.length() >= MAX_ERROR_MESSAGE_LENGTH) {
            return;
        }
        if (errors.length() > 0) {
            errors.append("；");
        }
        String message = failureMessage(ex);
        errors.append(location).append("：").append(message);
        if (errors.length() > MAX_ERROR_MESSAGE_LENGTH) {
            errors.setLength(MAX_ERROR_MESSAGE_LENGTH);
        }
    }

    /**
     * 有记录失败即判定失败；全部跳过或无数据且无异常时仍视为成功。
     *
     * @param instance 运行实例
     * @return 数据库定义的运行状态码
     */
    private String resolveRunStatus(IngestTaskInstanceDO instance) {
        if (instance.getFailureCount() == 0) {
            return RUN_SUCCESS;
        }
        return RUN_FAILED;
    }
}
