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
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterExecuteTaskDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.data.MasterDataVersionDO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestItemMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterExecuteTaskMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.data.MasterDataVersionMapper;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataWriteOutcomeUnknownException;
import tech.qiantong.qdata.mdm.master.service.change.IMasterExecuteTaskService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceData;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceResult;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceStrategy;
import tech.qiantong.qdata.mdm.master.util.MasterLifecycleRules;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceWritePlan;

import java.util.*;

/**
 * 主数据正式执行器。
 *
 * <p>支持普通及分类模型 CREATE/UPDATE、主子维护与生命周期操作。
 * 批量修改、批量状态采用请求级权威事务；审计结果在项目库事务中整体回写。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MasterExecuteTaskServiceImpl implements IMasterExecuteTaskService {

    private final MasterChangeRequestMapper changeRequestMapper;
    private final MasterChangeRequestItemMapper changeRequestItemMapper;
    private final MasterExecuteTaskMapper executeTaskMapper;
    private final MasterChangeMapper changeMapper;
    private final MasterDataVersionMapper versionMapper;
    private final IMasterDataMetadataService metadataService;
    private final List<MaintenanceStrategy> maintenanceStrategies;
    private final MasterDataQueryRepository dataQueryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public Long createPendingTask(Long requestId) {
        if (requestId == null || requestId <= 0) throw new ServiceException("申请ID无效");
        MasterExecuteTaskDO existing = executeTaskMapper.selectByRequestId(requestId);
        if (existing != null) return existing.getId();
        MasterChangeRequestDO request = changeRequestMapper.selectById(requestId);
        if (request == null || !Boolean.TRUE.equals(request.getValidFlag())
                || Boolean.TRUE.equals(request.getDelFlag())) throw new ServiceException("申请记录不存在或已失效");
        if (!"1".equals(request.getStatus())) throw new ServiceException("申请尚未审核通过，不能创建执行任务");
        if (request.getExecutionId() == null || request.getExecutionId().trim().isEmpty())
            throw new ServiceException("申请缺少执行标识");
        if (request.getInstanceId() == null || request.getInstanceId() <= 0)
            throw new ServiceException("申请缺少物化实例");

        MasterExecuteTaskDO task = MasterExecuteTaskDO.builder()
                .requestId(request.getId()).executionId(request.getExecutionId())
                .instanceId(request.getInstanceId()).taskStatus("0")
                .totalCount(request.getItemCount() == null ? 0 : request.getItemCount())
                .successCount(0).failCount(0).retryCount(0).maxRetryCount(3)
                .validFlag(true).delFlag(false).build();
        task.setCreateBy(request.getCreateBy());
        task.setCreatorId(request.getCreatorId());
        task.setUpdateBy(request.getUpdateBy());
        task.setUpdatorId(request.getUpdatorId());
        task.setRemark("申请审核通过，等待执行器处理");
        if (executeTaskMapper.insert(task) != 1 || task.getId() == null)
            throw new ServiceException("创建主数据执行任务失败");
        if (request.getItemCount() != null && request.getItemCount() > 0 && request.getItemCount() <= 500
                && (request.getItemCount() == 1 || "BATCH_UPDATE".equals(request.getChangeType())
                    || "STATUS_CHANGE".equals(request.getChangeType()) || "BATCH_IMPORT".equals(request.getChangeType()))
                && ("BATCH_IMPORT".equals(request.getChangeType())
                    ? supports("CREATE") && supports("UPDATE") : supports(strategyType(request)))) {
            eventPublisher.publishEvent(new MasterExecuteTaskCreatedEvent(task.getId()));
        }
        return task.getId();
    }

    @Override
    public MasterExecuteTaskDO getByRequestId(Long requestId) {
        return requestId == null ? null : executeTaskMapper.selectByRequestId(requestId);
    }

    @Override
    public MasterExecuteTaskDO getByExecutionId(String executionId) {
        return executionId == null || executionId.trim().isEmpty()
                ? null : executeTaskMapper.selectByExecutionId(executionId);
    }

    @Override
    public int executePendingTasks() {
        int count = 0;
        for (MasterExecuteTaskDO task : executeTaskMapper.selectPendingTasks()) {
            if (executeTask(task.getId())) count++;
        }
        return count;
    }

    @Override
    public boolean executeTask(Long taskId) {
        if (taskId == null || taskId <= 0) return false;
        String workerId = UUID.randomUUID().toString();
        Date leaseUntil = new Date(System.currentTimeMillis() + 300000L);
        if (executeTaskMapper.claimPending(taskId, workerId, leaseUntil) != 1) return false;

        MasterExecuteTaskDO task = executeTaskMapper.selectById(taskId);
        try {
            executeMaintenance(task, workerId);
            return true;
        } catch (Exception ex) {
            String message = safeMessage(ex);
            boolean pendingConfirmation = ex instanceof TargetOutcomePendingException
                    || ex instanceof MasterDataWriteOutcomeUnknownException;
            transactionTemplate.executeWithoutResult(status -> markFailure(task, workerId, message, pendingConfirmation));
            return false;
        }
    }

    private void executeMaintenance(MasterExecuteTaskDO task, String workerId) {
        if (task == null) throw new ServiceException("执行任务不存在");
        MasterChangeRequestDO request = changeRequestMapper.selectById(task.getRequestId());
        if (request == null || !"1".equals(request.getStatus()) || !"0".equals(request.getExecuteStatus()))
            throw new ServiceException("申请状态不允许执行");
        if (!Objects.equals(task.getInstanceId(), request.getInstanceId())
                || task.getExecutionId() == null || !task.getExecutionId().equals(request.getExecutionId()))
            throw new ServiceException("任务物化实例或执行标识与申请不一致");
        boolean batchImport = "BATCH_IMPORT".equalsIgnoreCase(request.getChangeType());
        MaintenanceStrategy strategy = batchImport ? null : maintenanceStrategies.stream()
                .filter(candidate -> candidate.changeType().equalsIgnoreCase(strategyType(request)))
                .findFirst().orElseThrow(() -> new ServiceException("当前执行器不支持此申请类型"));

        List<MasterChangeRequestItemDO> items =
                changeRequestItemMapper.selectByRequestIdForExecute(request.getId());
        if (items.isEmpty() || items.size() > 500 || !Integer.valueOf(items.size()).equals(request.getItemCount()))
            throw new ServiceException("申请明细数量不完整或超过500条");
        boolean lifecycle = "STATUS_CHANGE".equalsIgnoreCase(request.getChangeType());
        boolean batchUpdate = "BATCH_UPDATE".equalsIgnoreCase(request.getChangeType());
        if (items.size() > 1 && !lifecycle && !batchUpdate && !batchImport)
            throw new ServiceException("此操作不支持批量执行");
        Set<Long> records = new HashSet<>();
        Set<Long> itemIds = new HashSet<>();
        for (MasterChangeRequestItemDO item : items) {
            if (item.getId() == null || !itemIds.add(item.getId())) throw new ServiceException("申请明细标识缺失或重复");
            boolean validType = lifecycle ? MasterLifecycleRules.isAction(item.getChangeType())
                    : batchImport ? ("CREATE".equalsIgnoreCase(item.getChangeType())
                            || "UPDATE".equalsIgnoreCase(item.getChangeType()))
                    : strategy.changeType().equalsIgnoreCase(item.getChangeType());
            if (!validType)
                throw new ServiceException("申请主单与明细操作类型不一致");
            if (!"0".equals(item.getExecuteStatus())) throw new ServiceException("申请明细已处理");
            if (!"CREATE".equals(item.getChangeType()) && (item.getMasterRecordId() == null
                    || item.getMasterRecordId() <= 0 || !records.add(item.getMasterRecordId())))
                throw new ServiceException("申请记录标识缺失或重复");
        }

        MasterModelRespDTO model = metadataService.getModel(task.getInstanceId());
        if (model == null || !Objects.equals(model.getInstanceId(), task.getInstanceId())
                || !Objects.equals(model.getModelId(), request.getModelId())
                || !Objects.equals(model.getModelVersionId(), request.getModelVersionId()))
            throw new ServiceException("申请模型已变更，禁止执行");
        if (!"NORMAL".equalsIgnoreCase(model.getModelType())
                && !"CATEGORY".equalsIgnoreCase(model.getModelType()))
            throw new ServiceException("当前模型类型不支持此维护操作");
        if (model.getDatasourceId() == null || model.getPhysicalTableName() == null
                || model.getColumnMappings() == null || model.getColumnMappings().isEmpty())
            throw new ServiceException("模型物化绑定不完整");

        Map<Long, MaintenanceResult> results = new LinkedHashMap<>();
        if (batchUpdate || batchImport || items.size() > 1) {
            results.putAll(executeBatch(task, request, items, model));
        } else {
            MasterChangeRequestItemDO item = items.get(0);
            results.put(item.getId(), strategy.execute(task, request, item, model));
        }
        // 权威库事务已提交；项目库回写失败时转人工确认，避免没有执行凭据时盲目重放。
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (MasterChangeRequestItemDO item : items) saveExecutionResult(request, item, model, results.get(item.getId()));
                String message = items.size() == 1 ? successMessage(items.get(0).getChangeType())
                        : (batchImport ? "批量导入成功，共" : lifecycle ? "批量状态变更成功，共" : "批量修改成功，共")
                                + items.size() + "条";
                request.setExecuteStatus("2"); request.setSuccessItemCount(items.size()); request.setFailItemCount(0);
                request.setFinishTime(new Date()); request.setExecuteMessage(message);
                if (changeRequestMapper.updateById(request) != 1) throw new ServiceException("回写申请结果失败");
                if (executeTaskMapper.markSuccess(task.getId(), workerId, items.size(), new Date()) != 1) {
                    throw new ServiceException("回写执行任务成功状态失败");
                }
            });
        } catch (Exception ex) {
            throw new TargetOutcomePendingException("权威库已提交，项目库执行结果待确认：" + safeMessage(ex), ex);
        }
    }

    private String strategyType(MasterChangeRequestDO request) {
        return "BATCH_UPDATE".equalsIgnoreCase(request.getChangeType()) ? "UPDATE" : request.getChangeType();
    }

    private boolean supports(String type) {
        return maintenanceStrategies.stream().anyMatch(strategy -> strategy.changeType().equalsIgnoreCase(type));
    }

    private Map<Long, MaintenanceResult> executeBatch(MasterExecuteTaskDO task, MasterChangeRequestDO request,
            List<MasterChangeRequestItemDO> items, MasterModelRespDTO model) {
        Map<Long, MaintenanceResult> results = new LinkedHashMap<>();
        List<MasterChangeRequestItemDO> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparing(MasterChangeRequestItemDO::getItemNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<MaintenanceWritePlan> plans = new ArrayList<>();
        for (MasterChangeRequestItemDO item : ordered) {
            try {
                String type = MasterLifecycleRules.isAction(item.getChangeType()) ? "STATUS_CHANGE" : item.getChangeType();
                MaintenanceStrategy strategy = maintenanceStrategies.stream()
                        .filter(candidate -> candidate.changeType().equalsIgnoreCase(type))
                        .findFirst().orElseThrow(() -> new ServiceException("当前执行器不支持明细操作：" + item.getChangeType()));
                MaintenanceWritePlan plan = strategy.plan(task, request, item, model);
                plans.add(plan); results.put(item.getId(), plan.getResult());
            } catch (ServiceException ex) {
                String row = item.getItemNo() == null ? String.valueOf(item.getMasterRecordId()) : item.getItemNo().toString();
                throw new ServiceException("第 " + row + " 行校验失败，整批未执行：" + ex.getMessage());
            }
        }
        Map<Long, Long> generatedIds = dataQueryRepository.writeRecordsAtomically(model.getDatasourceId(), MaintenanceData.table(model), model.getColumnMappings(),
                plans.stream().map(MaintenanceWritePlan::getMutation)
                        .collect(java.util.stream.Collectors.toList()));
        for (Map.Entry<Long, Long> generated : generatedIds.entrySet()) {
            MaintenanceResult result = results.get(generated.getKey());
            if (result != null) results.put(generated.getKey(), result.withRecordId(generated.getValue()));
        }
        return results;
    }

    private String successMessage(String changeType) {
        return MasterLifecycleRules.isAction(changeType)
                ? "主数据" + MasterLifecycleRules.actionLabel(changeType) + "成功"
                : "CREATE".equals(changeType) ? "主数据新增成功" : "主数据修改成功";
    }

    private void saveExecutionResult(MasterChangeRequestDO request, MasterChangeRequestItemDO item,
            MasterModelRespDTO model, MaintenanceResult result) {
        Long recordId = result.getRecordId();
        String changeType = item.getChangeType();
        String successMessage = successMessage(changeType);
        int versionSeq = Optional.ofNullable(versionMapper.selectMaxVersionSeq(
                model.getModelId(), recordId)).orElse(0) + 1;
        String versionNo = String.valueOf(versionSeq);
        List<Long> currentVersions = versionMapper.selectCurrentVersionIds(
                model.getModelId(), recordId);
        if (currentVersions.size() > 1) throw new ServiceException("主数据存在多个当前版本");
        Long beforeVersionId = currentVersions.isEmpty() ? item.getCurrentVersionId() : currentVersions.get(0);
        MasterChangeDO change = new MasterChangeDO();
        change.setRequestId(request.getId());
        change.setRequestItemId(item.getId());
        change.setModelId(model.getModelId());
        change.setModelVersionId(model.getModelVersionId());
        change.setMasterRecordId(recordId);
        change.setType(changeType);
        change.setSource(request.getChangeSource());
        change.setBeforeVersionId(beforeVersionId);
        change.setBeforeVersionNo(item.getCurrentVersionNo());
        change.setBeforeStatus(result.getBeforeStatus());
        change.setAfterStatus(result.getAfterStatus());
        change.setBeforeData(MaintenanceData.json(result.getBefore()));
        change.setProposedData(item.getProposedData());
        change.setAfterData(MaintenanceData.json(result.getAfter()));
        change.setChangeReason(request.getApplyReason());
        change.setOperatorId(request.getApplicantId());
        change.setOperatorName(request.getApplicantName());
        change.setChangeTime(new Date());
        change.setProcessResult("1");
        change.setValidFlag(true);
        change.setDelFlag(false);
        if (changeMapper.insert(change) != 1 || change.getId() == null)
            throw new ServiceException("保存主数据变更记录失败");

        for (Long versionId : currentVersions) {
            MasterDataVersionDO old = new MasterDataVersionDO();
            old.setId(versionId);
            old.setCurrentFlag("0");
            if (versionMapper.updateById(old) != 1) throw new ServiceException("关闭旧数据版本失败");
        }
        MasterDataVersionDO version = new MasterDataVersionDO();
        version.setModelId(model.getModelId());
        version.setModelVersionId(model.getModelVersionId());
        version.setMasterRecordId(recordId);
        version.setVersionSeq(versionSeq);
        version.setVersionNo(versionNo);
        version.setSnapshotData(MaintenanceData.json(result.getAfter()));
        version.setGenerateType(changeType);
        version.setChangeId(change.getId());
        version.setGenerateTime(new Date());
        version.setCurrentFlag("1");
        version.setValidFlag(true);
        version.setDelFlag(false);
        if (versionMapper.insert(version) != 1 || version.getId() == null)
            throw new ServiceException("保存主数据版本失败");

        change.setAfterVersionId(version.getId());
        change.setAfterVersionNo(versionNo);
        if (changeMapper.updateById(change) != 1) throw new ServiceException("回写变更版本失败");

        item.setExecuteStatus("2");
        item.setResultMasterRecordId(recordId);
        item.setResultVersionId(version.getId());
        item.setTargetVersionNo(versionNo);
        item.setExecuteMessage(successMessage);
        item.setExecuteTime(new Date());
        if (changeRequestItemMapper.updateById(item) != 1) throw new ServiceException("回写申请明细失败");

    }

    private void markFailure(MasterExecuteTaskDO task, String workerId, String message, boolean pendingConfirmation) {
        if (task == null) return;
        MasterChangeRequestDO request = changeRequestMapper.selectById(task.getRequestId());
        if (!pendingConfirmation && request != null && "2".equals(request.getExecuteStatus())) return;
        List<MasterChangeRequestItemDO> items = changeRequestItemMapper.selectByRequestIdForExecute(task.getRequestId());
        int count = items.size();
        if (pendingConfirmation) {
            String pending = "权威库执行结果待人工确认：" + message;
            updateFailedRequest(request, items, "1", "1", pending, false);
            executeTaskMapper.markFailure(task.getId(), workerId, 0, pending,
                    task.getRetryCount() == null ? 0 : task.getRetryCount(), null, "5");
            log.error("主数据执行结果待人工确认，taskId={}, executionId={}, reason={}",
                    task.getId(), task.getExecutionId(), message);
            return;
        }
        String failure = count > 1 ? "整批执行失败：" + message : message;
        updateFailedRequest(request, items, "4", "3", failure, true);
        executeTaskMapper.markFailure(task.getId(), workerId, count, failure,
                task.getRetryCount() == null ? 0 : task.getRetryCount(), null, "4");
    }

    private void updateFailedRequest(MasterChangeRequestDO request, List<MasterChangeRequestItemDO> items,
            String requestStatus, String itemStatus, String message, boolean failed) {
        if (request != null) {
            request.setExecuteStatus(requestStatus); request.setSuccessItemCount(0);
            request.setFailItemCount(failed ? items.size() : 0);
            request.setFinishTime(failed ? new Date() : null); request.setExecuteMessage(message);
            if (changeRequestMapper.updateById(request) != 1) throw new ServiceException("回写申请异常结果失败");
        }
        for (MasterChangeRequestItemDO item : items) {
            item.setExecuteStatus(itemStatus); item.setExecuteMessage(message);
            item.setExecuteTime(failed ? new Date() : null);
            if (changeRequestItemMapper.updateById(item) != 1) throw new ServiceException("回写申请明细异常结果失败");
        }
    }

    private String safeMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.trim().isEmpty() ? "主数据执行失败" : message;
    }

    private static final class TargetOutcomePendingException extends RuntimeException {
        private TargetOutcomePendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

