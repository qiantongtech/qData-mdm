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

package tech.qiantong.qdata.mdm.merge.service.detect.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateMatchDetailDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMemberMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateMatchDetailMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskInstanceMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskMapper;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeCompiledRuleBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectContextBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectResultBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchClusterBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchEdgeBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchFieldBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectExecuteService;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScopeService;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectRunLogService;
import tech.qiantong.qdata.mdm.merge.support.MergeAudit;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.MergeMatchEngine;
import tech.qiantong.qdata.mdm.merge.support.MergeMatchStrategyRegistry;
import tech.qiantong.qdata.mdm.merge.support.MergeResultFactory;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.merge.utils.detect.MergeDetectConfigUtils;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Resource;

/**
 * 重复检测执行服务，先提交运行实例，再异步匹配并保存检测结果。
 *
 * @author chentangwen
 */
@Slf4j
@Service
public class MergeDetectExecuteServiceImpl implements IMergeDetectExecuteService {

    @Resource
    private MergeMatchStrategyRegistry matchStrategyRegistry;

    @Resource
    private MergeDuplicateTaskMapper mergeDuplicateTaskMapper;

    @Resource
    private MergeDuplicateTaskInstanceMapper mergeDuplicateTaskInstanceMapper;

    @Resource
    private MergeCandidateGroupMapper mergeCandidateGroupMapper;

    @Resource
    private MergeCandidateGroupMemberMapper mergeCandidateGroupMemberMapper;

    @Resource
    private MergeCandidateMatchDetailMapper mergeCandidateMatchDetailMapper;

    @Resource
    private IMergeSourceTableService sourceTableService;

    @Resource
    private MergeResultFactory mergeResultFactory;

    @Resource
    private IMergeDetectScopeService scopeService;

    @Resource(name = "mergeDetectTaskExecutor")
    private Executor executor;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Resource
    private IMergeDetectRunLogService runLogService;

    /**
     * 在调用方持有任务行锁的事务内创建实例并固定快照，提交后异步检测。
     *
     * @param task 已锁定的有效任务
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 检测运行实例编号
     */
    @Override
    public Long execute(MergeDuplicateTaskDO task, Long operatorId, String operatorName) {
        MergeDetectContextBO context = scopeService.prepareDetection(MergeDetectConfigUtils.toScopeRequest(task));
        return executePrepared(task, context, operatorId, operatorName);
    }

    /**
     * 使用同一事务内已校验的检测上下文创建实例，事务提交后才派发执行。
     *
     * @param task 新创建或已锁定的任务
     * @param context 本次固定数据
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     * @return 运行实例编号
     */
    @Override
    public Long executePrepared(MergeDuplicateTaskDO task, MergeDetectContextBO context,
            Long operatorId, String operatorName) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("重复检测提交必须在事务内执行");
        }
        MergeDuplicateTaskInstanceDO instance = createInstance(task, context, operatorId, operatorName);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public int getOrder() {
                // 优先派发已提交的实例，避免调度同步失败使本次运行无人执行。
                return org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
            }

            @Override
            public void afterCommit() {
                runLogService.publish(instance, "INFO", "QUEUED", "检测任务已提交，等待执行");
                try {
                    executor.execute(() -> run(task, instance, context, operatorId, operatorName));
                } catch (RuntimeException ex) {
                    fail(task.getId(), instance.getId(), "重复检测线程池暂不可用，请稍后重试", ex);
                    throw new ServiceException("重复检测线程池暂不可用，请稍后重试");
                }
            }
        });
        return instance.getId();
    }

    /** 匹配计算在事务外执行，候选组、来源占用和成功终态在同一事务内保存。 */
    private void run(MergeDuplicateTaskDO task, MergeDuplicateTaskInstanceDO instance,
            MergeDetectContextBO context, Long operatorId, String operatorName) {
        try {
            runLogService.publish(instance, "INFO", "PREPARE", "开始准备本次固定的模型、匹配规则和来源数据");
            List<MergeCompiledRuleBO> rules = compile(context.getRules(), context.getColumns());
            runLogService.publish(instance, "INFO", "PREPARE", "准备完成，匹配规则 " + rules.size()
                    + " 条，待检测数据 " + instance.getSourceCount() + " 条，历史比对数据 "
                    + instance.getHistorySourceCount() + " 条");
            if (context.getSkippedCount() > 0) {
                runLogService.publish(instance, "WARN", "PREPARE", "跳过无法解析的来源数据 "
                        + context.getSkippedCount() + " 条");
            }
            List<Map<String, String>> valueRows = new ArrayList<>();
            for (SourceSnapshot snapshot : context.getSnapshots()) {
                valueRows.add(snapshot.getValues());
            }
            long totalPairs = (long) valueRows.size() * (valueRows.size() - 1) / 2;
            runLogService.publish(instance, "INFO", "MATCH", "开始匹配，参与比对数据 " + valueRows.size()
                    + " 条，共需比较 " + totalPairs + " 对记录");
            long[] lastProgressTime = {System.nanoTime()};
            List<MergeMatchClusterBO> clusters = MergeMatchEngine.cluster(valueRows, rules, matchStrategyRegistry,
                    compared -> {
                        long now = System.nanoTime();
                        if (compared == totalPairs || now - lastProgressTime[0] >= 3000000000L) {
                            runLogService.publish(instance, "INFO", "MATCH_PROGRESS", "已比较 " + compared
                                    + " / " + totalPairs + " 对记录");
                            lastProgressTime[0] = now;
                        }
                    });
            long candidates = clusters.stream().filter(cluster -> cluster.getIndexes().size() > 1
                    && cluster.getIndexes().stream().anyMatch(index -> context.getSnapshots().get(index).isTarget())).count();
            long singles = clusters.stream().filter(cluster -> cluster.getIndexes().size() == 1
                    && context.getSnapshots().get(cluster.getIndexes().get(0)).isTarget()).count();
            runLogService.publish(instance, "INFO", "MATCH_COMPLETE", "匹配完成，识别候选组 " + candidates
                    + " 组，单条组 " + singles + " 组，开始保存结果");
            transaction().executeWithoutResult(status -> {
                mergeDuplicateTaskMapper.selectForUpdate(task.getId());
                MergeDetectResultBO result = saveGroups(instance, context, clusters, operatorId, operatorName);
                instance.setStatus(MergeCodes.TASK_SUCCESS);
                instance.setEndTime(new Date());
                instance.setSourceCount((long) result.getSourceIds().size());
                instance.setGroupCount(result.getGroupCount());
                instance.setSingleCount(result.getSingleCount());
                instance.setHistorySourceCount(result.getHistoryCount());
                runLogService.archive(instance, "INFO", "FINISH", "执行结束，检测数据 " + instance.getSourceCount()
                        + " 条，候选组 " + instance.getGroupCount() + " 组，单条组 " + instance.getSingleCount()
                        + " 组；耗时 " + (instance.getEndTime().getTime() - instance.getStartTime().getTime()) + " 毫秒");
                mergeDuplicateTaskInstanceMapper.updateById(instance);
                task.setStatus(MergeCodes.TASK_SUCCESS);
                task.setLastExecuteTime(instance.getEndTime());
                mergeDuplicateTaskMapper.updateById(task);
                if (!result.getSourceIds().isEmpty()) {
                    sourceTableService.transition(task.getModelId(), result.getSourceIds(),
                            MergeCodes.GOVERNANCE_PENDING, MergeCodes.GOVERNANCE_PROCESSING);
                }
            });
            runLogService.clear(instance.getId());
        } catch (Exception ex) {
            fail(task.getId(), instance.getId(), ex instanceof ServiceException
                    ? ex.getMessage() : "重复检测执行异常，请查看服务日志", ex);
        }
    }

    /** 检测结果事务回滚后单独保存失败终态，保留本次实例及原始快照。 */
    private void fail(Long taskId, Long instanceId, String message, Exception cause) {
        log.error("重复检测执行失败，taskId={}, instanceId={}", taskId, instanceId, cause);
        MergeDuplicateTaskInstanceDO failed = transaction().execute(status -> {
            MergeDuplicateTaskDO task = mergeDuplicateTaskMapper.selectForUpdate(taskId);
            MergeDuplicateTaskInstanceDO stored = mergeDuplicateTaskInstanceMapper.selectById(instanceId);
            if (stored == null || stored.getEndTime() != null) {
                return null;
            }
            stored.setStatus(MergeCodes.TASK_FAILED);
            stored.setEndTime(new Date());
            stored.setErrorMessage(message.substring(0, Math.min(2000, message.length())));
            runLogService.archive(stored, "ERROR", "FAIL", "执行失败，检测结果已回滚；原因：" + message);
            mergeDuplicateTaskInstanceMapper.updateById(stored);
            task.setStatus(MergeCodes.TASK_FAILED);
            task.setLastExecuteTime(stored.getEndTime());
            mergeDuplicateTaskMapper.updateById(task);
            return stored;
        });
        if (failed != null) {
            runLogService.clear(failed.getId());
        }
    }

    /** 提交回调也可能仍绑定原事务资源，收尾必须使用独立事务。 */
    private TransactionTemplate transaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    /** 创建运行实例并保存模型版本、规则、来源与范围快照，保持本次检测可追溯。 */
    private MergeDuplicateTaskInstanceDO createInstance(MergeDuplicateTaskDO task, MergeDetectContextBO context,
            Long operatorId, String operatorName) {
        Date now = new Date();
        task.setStatus(MergeCodes.TASK_RUNNING);
        task.setLastExecuteTime(now);
        mergeDuplicateTaskMapper.updateById(task);

        MergeDuplicateTaskInstanceDO instance = new MergeDuplicateTaskInstanceDO();
        instance.setTaskId(task.getId());
        instance.setInstanceNo(MergeAudit.no("DI", 1));
        instance.setModelId(task.getModelId());
        instance.setModelVersionId(context.getModel().getVersionId());
        instance.setStatus(MergeCodes.TASK_RUNNING);
        instance.setSourceCount(context.getSnapshots().stream().filter(SourceSnapshot::isTarget).count());
        instance.setGroupCount(0L);
        instance.setSingleCount(0L);
        instance.setHistorySourceCount(context.getSnapshots().stream().filter(source -> !source.isTarget()).count());
        instance.setTaskConfigSnapshot(MergeJson.toJson(MergeDetectConfigUtils.taskConfig(task.getModelId(),
                context.getScope())));
        instance.setMatchRuleSnapshot(MergeJson.toJson(MergeDetectConfigUtils.ruleSnapshot(context.getRules())));
        instance.setSourceDataSnapshot(MergeJson.toJson(MergeDetectConfigUtils.sourceSnapshot(context.getSnapshots())));
        instance.setSourceSnapshotTime(now);
        instance.setStartTime(now);
        instance.setOperatorId(operatorId == null ? 0L : operatorId);
        instance.setErrorMessage(context.getSkippedCount() > 0
                ? "跳过无法解析的来源数据" + context.getSkippedCount() + "条" : null);
        instance.setValidFlag(MergeCodes.YES);
        instance.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(instance, operatorId, operatorName);
        mergeDuplicateTaskInstanceMapper.insert(instance);

        return instance;
    }

    /** 仅保存包含待治理目标的组，历史记录独立成组时不创建治理对象。 */
    private MergeDetectResultBO saveGroups(MergeDuplicateTaskInstanceDO instance, MergeDetectContextBO context,
            List<MergeMatchClusterBO> clusters, Long operatorId, String operatorName) {
        List<SourceSnapshot> snapshots = context.getSnapshots();
        long groupCount = 0L;
        long singleCount = 0L;
        long historyCount = 0L;
        List<Long> taken = new ArrayList<>();
        long lastProgressTime = System.nanoTime();
        runLogService.publish(instance, "INFO", "SAVE", "开始保存检测结果，单条组进入合并管理，候选组进入匹配复核");
        int seq = 1;
        for (SourceSnapshot snapshot : snapshots) {
            if (!snapshot.isTarget()) {
                historyCount++;
            }
        }
        for (MergeMatchClusterBO cluster : clusters) {
            int targets = 0;
            for (Integer index : cluster.getIndexes()) {
                if (snapshots.get(index).isTarget()) {
                    targets++;
                }
            }
            if (targets == 0) {
                continue;
            }
            boolean single = cluster.getIndexes().size() == 1;
            MergeCandidateGroupDO group = new MergeCandidateGroupDO();
            group.setTaskInstanceId(instance.getId());
            group.setGroupNo(MergeAudit.no(single ? "SG" : "CG", seq++));
            group.setModelId(context.getModel().getId());
            group.setType(single ? MergeCodes.GROUP_TYPE_SINGLE : MergeCodes.GROUP_TYPE_CANDIDATE);
            group.setStatus(single ? MergeCodes.GROUP_STATUS_SINGLE : MergeCodes.GROUP_STATUS_REVIEW);
            group.setTransferTime(new Date());
            group.setMaxMatchScore(cluster.getMaxScore());
            group.setValidFlag(MergeCodes.YES);
            group.setDelFlag(MergeCodes.NO);
            MergeAudit.fill(group, instance.getCreatorId(), instance.getCreateBy());
            mergeCandidateGroupMapper.insert(group);
            Map<Integer, Long> memberIds = saveMembers(group, cluster, snapshots, taken, operatorId, operatorName);
            saveMatchDetails(group, cluster, memberIds, operatorId, operatorName);
            if (single) {
                // 单条结果直接进入合并管理，使用本次检测固定的模型版本和来源快照。
                mergeResultFactory.create(group.getId(), group.getModelId(), context.getModel().getVersionId(),
                        MergeCodes.PROCESS_SINGLE,
                        Collections.singletonList(snapshots.get(cluster.getIndexes().get(0))),
                        context.getColumns(), Collections.emptyList(), operatorId, operatorName);
                singleCount++;
            } else {
                groupCount++;
            }
            instance.setGroupCount(groupCount);
            instance.setSingleCount(singleCount);
            long now = System.nanoTime();
            if ((groupCount + singleCount) % 100 == 0 || now - lastProgressTime >= 3000000000L) {
                runLogService.publish(instance, "INFO", "SAVE_PROGRESS", "已处理目标数据 " + taken.size()
                        + " / " + instance.getSourceCount() + " 条，已保存候选组 " + groupCount
                        + " 组，单条组 " + singleCount + " 组（等待本次事务提交）");
                lastProgressTime = now;
            }
        }
        runLogService.publish(instance, "INFO", "SAVE_COMPLETE", "结果保存完成，目标数据 " + taken.size()
                + " 条，候选组 " + groupCount + " 组，单条组 " + singleCount + " 组，等待提交");
        MergeDetectResultBO result = new MergeDetectResultBO();
        result.setGroupCount(groupCount);
        result.setSingleCount(singleCount);
        result.setHistoryCount(historyCount);
        result.setSourceIds(taken);
        return result;
    }

    /** 保存候选组成员，目标成员供后续占用；历史成员仅提供比较与溯源。 */
    private Map<Integer, Long> saveMembers(MergeCandidateGroupDO group, MergeMatchClusterBO cluster,
            List<SourceSnapshot> snapshots, List<Long> targetIds,
            Long operatorId, String operatorName) {
        Map<Integer, Long> memberIds = new HashMap<>();
        for (Integer index : cluster.getIndexes()) {
            SourceSnapshot snapshot = snapshots.get(index);
            MergeCandidateGroupMemberDO member = new MergeCandidateGroupMemberDO();
            member.setGroupId(group.getId());
            member.setSourceRecordId(snapshot.getSourceRecordId());
            member.setSourceVersionId(snapshot.getVersionId());
            member.setMemberRole(snapshot.isTarget() ? MergeCodes.MEMBER_ROLE_TARGET : MergeCodes.MEMBER_ROLE_HISTORY);
            member.setJoinType(MergeCodes.JOIN_AUTO);
            member.setValidFlag(MergeCodes.YES);
            member.setDelFlag(MergeCodes.NO);
            MergeAudit.fill(member, operatorId, operatorName);
            mergeCandidateGroupMemberMapper.insert(member);
            memberIds.put(index, member.getId());
            if (snapshot.isTarget()) {
                targetIds.add(snapshot.getSourceRecordId());
            }
        }
        return memberIds;
    }

    /** 将命中的全部规则、字段和评分保存为成员间匹配明细。 */
    private void saveMatchDetails(MergeCandidateGroupDO group, MergeMatchClusterBO cluster,
            Map<Integer, Long> memberIds, Long operatorId, String operatorName) {
        for (MergeMatchEdgeBO edge : cluster.getEdges()) {
            MergeCandidateMatchDetailDO detail = new MergeCandidateMatchDetailDO();
            detail.setGroupId(group.getId());
            detail.setLeftMemberId(memberIds.get(edge.getLeft()));
            detail.setRightMemberId(memberIds.get(edge.getRight()));
            detail.setMatchRuleId(edge.getRuleId() == null ? 0L : edge.getRuleId());
            detail.setMatchScore(edge.getScore());
            detail.setMatchFields(edge.getMatchFields());
            detail.setMatchReason(MergeAudit.cut(edge.getReason(), 2000));
            detail.setRuleSnapshot(edge.getSnapshot() == null ? "{}" : edge.getSnapshot());
            detail.setValidFlag(MergeCodes.YES);
            detail.setDelFlag(MergeCodes.NO);
            MergeAudit.fill(detail, operatorId, operatorName);
            mergeCandidateMatchDetailMapper.insert(detail);
        }
    }

    /** 按模型字段编码编译规则，兼容字段 ID；非法配置记录为本次实例的执行失败。 */
    private List<MergeCompiledRuleBO> compile(List<RuleMatchDO> rules, List<ModelColumnDO> columns) {
        Map<String, String> keyMap = new HashMap<>();
        for (ModelColumnDO column : columns) {
            keyMap.put(String.valueOf(column.getId()), column.getCode());
            keyMap.put(column.getCode(), column.getCode());
        }
        List<MergeCompiledRuleBO> compiled = new ArrayList<>();
        for (RuleMatchDO rule : rules) {
            String type = rule.getRuleType() == null ? "" : rule.getRuleType().trim().toUpperCase(java.util.Locale.ROOT);
            if (type.isEmpty()) {
                throw new ServiceException("匹配规则类型不能为空: " + rule.getRuleName());
            }
            MergeCompiledRuleBO item = new MergeCompiledRuleBO();
            item.setId(rule.getId());
            item.setName(rule.getRuleName());
            item.setType(type);
            item.setPriority(rule.getPriority());
            item.setSnapshot(rule.getRuleConfig() == null ? "{}" : rule.getRuleConfig());
            JsonNode root = MergeJson.readStrict(rule.getRuleConfig());
            if (!root.isObject()) {
                throw new ServiceException("匹配规则配置必须是 JSON 对象: " + rule.getRuleName());
            }
            if (root.has("finalThreshold")) {
                if (!root.get("finalThreshold").isIntegralNumber()) {
                    throw new ServiceException("匹配规则最终阈值必须是整数: " + rule.getRuleName());
                }
                item.setFinalThreshold(root.get("finalThreshold").asInt(100));
            }
            if (root.has("similarityThreshold")) {
                if (!root.get("similarityThreshold").isIntegralNumber()
                        || root.get("similarityThreshold").asInt() < 0
                        || root.get("similarityThreshold").asInt() > 100) {
                    throw new ServiceException("相似度阈值必须在 0 到 100 之间: " + rule.getRuleName());
                }
            }
            JsonNode standardOptions = root.get("standardOptions");
            if (standardOptions != null) {
                if (!standardOptions.isArray()) {
                    throw new ServiceException("标准化配置必须是数组: " + rule.getRuleName());
                }
                for (JsonNode option : standardOptions) {
                    if (!option.isTextual()) {
                        throw new ServiceException("标准化选项必须是文本: " + rule.getRuleName());
                    }
                    item.getStandardOptions().add(option.asText());
                }
            }
            JsonNode fieldList = root.get("fieldList");
            if (fieldList == null || !fieldList.isArray() || fieldList.isEmpty()) {
                throw new ServiceException("匹配规则必须配置字段: " + rule.getRuleName());
            }
            for (JsonNode field : fieldList) {
                if (!field.isObject()) {
                    throw new ServiceException("匹配字段配置格式不正确: " + rule.getRuleName());
                }
                MergeMatchFieldBO fieldRule = new MergeMatchFieldBO();
                String raw = field.path("modelFieldId").asText("").trim();
                if (raw.isEmpty() || !keyMap.containsKey(raw)) {
                    throw new ServiceException("匹配规则引用了不存在的模型字段: " + raw);
                }
                fieldRule.setKey(keyMap.get(raw));
                String defaultMethod = "STD_EXACT".equals(type) ? "STD_EXACT"
                        : ("FUZZY".equals(type) ? "FUZZY" : "EXACT");
                String method = field.has("matchMethod") ? field.path("matchMethod").asText("").trim() : defaultMethod;
                if (method.isEmpty()) {
                    throw new ServiceException("匹配字段算法不能为空: " + raw);
                }
                fieldRule.setMethod(method.toUpperCase(java.util.Locale.ROOT));
                if (field.has("weight")) {
                    if (!field.get("weight").isIntegralNumber()) {
                        throw new ServiceException("匹配字段权重必须是整数: " + raw);
                    }
                    fieldRule.setWeight(field.get("weight").asInt());
                } else if ("WEIGHTED".equals(type)) {
                    throw new ServiceException("加权匹配字段必须配置权重: " + raw);
                } else {
                    fieldRule.setWeight(100);
                }
                if ("FUZZY".equals(type) && root.has("similarityThreshold")) {
                    if (!root.get("similarityThreshold").isIntegralNumber()) {
                        throw new ServiceException("相似度阈值必须是整数: " + rule.getRuleName());
                    }
                    fieldRule.setThreshold(root.get("similarityThreshold").asInt());
                } else if (field.has("fieldThreshold")) {
                    if (!field.get("fieldThreshold").isIntegralNumber()) {
                        throw new ServiceException("匹配字段阈值必须是整数: " + raw);
                    }
                    fieldRule.setThreshold(field.get("fieldThreshold").asInt());
                } else {
                    fieldRule.setThreshold(100);
                }
                item.getFields().add(fieldRule);
            }
            matchStrategyRegistry.validate(item);
            compiled.add(item);
        }
        return compiled;
    }

}
