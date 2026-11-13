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

package tech.qiantong.qdata.mdm.merge.service.review.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewActionReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewAddReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewSplitReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateReviewLogDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMemberMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.review.MergeCandidateReviewLogMapper;
import tech.qiantong.qdata.mdm.merge.domain.review.MergeReviewSourceContextBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScopeService;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewQueryService;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewService;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewSourceService;
import tech.qiantong.qdata.mdm.merge.support.MergeAudit;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.MergeResultFactory;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Resource;

/**
 * 匹配审核操作服务，负责审核状态流转、来源占用和结果生成的事务编排。
 *
 * @author chentangwen
 */
@Service
public class MergeReviewServiceImpl implements IMergeReviewService {

    @Resource
    private MergeCandidateGroupMapper mergeCandidateGroupMapper;

    @Resource
    private MergeCandidateGroupMemberMapper mergeCandidateGroupMemberMapper;

    @Resource
    private MergeCandidateReviewLogMapper mergeCandidateReviewLogMapper;

    @Resource
    private MergePendingMasterMapper mergePendingMasterMapper;

    @Resource
    private IMergeSourceTableService sourceTableService;

    @Resource
    private IMergeDetectScopeService scopeService;

    @Resource
    private MergeResultFactory mergeResultFactory;

    @Resource
    private IMergeReviewQueryService queryService;

    @Resource
    private IMergeReviewSourceService sourceService;

    /** 按审核范围分页查询候选组。 */
    @Override
    public PageResult<MergeCandidateGroupDO> page(MergeReviewPageReqVO reqVO) {
        return queryService.page(reqVO);
    }

    /** 批量补充审核列表摘要，不开启审核写事务。 */
    @Override
    public void fillSummary(List<MergeGroupRespVO> rows) {
        queryService.fillSummary(rows);
    }

    /** 查询固定来源版本、匹配依据和审核日志，支持历史组追溯。 */
    @Override
    public MergeReviewDetailRespVO detail(Long groupId) {
        return queryService.detail(groupId);
    }

    /** 锁定候选组后确认对象，保留成员固定版本并生成待提交结果。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(MergeReviewActionReqVO reqVO, Long operatorId, String operatorName) {
        // 确认前重新读取候选组和成员，避免页面打开后的旧快照覆盖当前数据。
        MergeCandidateGroupDO group = activeReviewGroup(reqVO.getGroupId());
        List<MergeCandidateGroupMemberDO> members = members(group.getId());
        if (members.isEmpty()) {
            throw new ServiceException("候选组没有可确认的来源记录");
        }
        // 确认前再次检查是否已经生成待提交结果，避免重复确认产生多条结果。
        assertNoPending(group.getId());
        boolean single = members.size() == 1;
        group.setStatus(single ? MergeCodes.GROUP_STATUS_SINGLE : MergeCodes.GROUP_STATUS_CONFIRMED);
        group.setType(single ? MergeCodes.GROUP_TYPE_SINGLE : MergeCodes.GROUP_TYPE_CANDIDATE);
        group.setTransferTime(new Date());
        MergeAudit.touch(group, operatorId, operatorName);
        mergeCandidateGroupMapper.updateById(group);
        // 单条组和多条组使用不同处理类型，但都从审核状态进入待提交结果。
        MergeReviewSourceContextBO context = sourceService.prepareResult(group, members, !single);
        createPending(group, members, context, single ? MergeCodes.PROCESS_SINGLE : MergeCodes.PROCESS_MULTI,
                operatorId, operatorName);
        writeLog(group.getId(), MergeCodes.REVIEW_CONFIRM, operatorId, operatorName, reqVO.getReason(),
                memberIds(members), String.valueOf(group.getId()));
    }

    /** 完整校验分区并按来源编号取锁，历史分区只追溯，目标分区生成子组和结果。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void split(MergeReviewSplitReqVO reqVO, Long operatorId, String operatorName) {
        MergeCandidateGroupDO group = activeReviewGroup(reqVO.getGroupId());
        List<MergeCandidateGroupMemberDO> members = members(group.getId());
        Map<Long, MergeCandidateGroupMemberDO> bySource = new HashMap<>();
        for (MergeCandidateGroupMemberDO member : members) {
            bySource.put(member.getSourceRecordId(), member);
        }
        List<List<Long>> partitions = normalizePartitions(reqVO.getSourceGroups(), bySource);
        assertNoPending(group.getId());
        lockTargetSources(group.getModelId(), members);
        boolean useSurvivorRules = false;
        for (List<Long> partition : partitions) {
            if (partition.size() > 1 && hasTarget(partition, bySource)) {
                useSurvivorRules = true;
                break;
            }
        }
        // 一次固定原成员版本和规则，新子组仅从这份上下文选择自己的来源。
        MergeReviewSourceContextBO context = sourceService.prepareResult(group, members, useSurvivorRules);
        List<Long> childIds = new ArrayList<>();
        int seq = 1;
        for (List<Long> partition : partitions) {
            // 纯历史分区留在原组追溯，不单独生成治理对象。
            if (!hasTarget(partition, bySource)) {
                continue;
            }
            boolean single = partition.size() == 1;
            MergeCandidateGroupDO child = newChildGroup(group, single, seq++, operatorId, operatorName);
            mergeCandidateGroupMapper.insert(child);
            List<MergeCandidateGroupMemberDO> childMembers = saveChildMembers(child, partition, bySource,
                    operatorId, operatorName);
            createPending(child, childMembers, context,
                    single ? MergeCodes.PROCESS_SINGLE : MergeCodes.PROCESS_MULTI, operatorId, operatorName);
            childIds.add(child.getId());
        }
        group.setStatus(MergeCodes.GROUP_STATUS_SPLIT);
        group.setTransferTime(new Date());
        MergeAudit.touch(group, operatorId, operatorName);
        mergeCandidateGroupMapper.updateById(group);
        writeLog(group.getId(), MergeCodes.REVIEW_SPLIT, operatorId, operatorName, reqVO.getReason(),
                memberIds(members), MergeJson.toJson(childIds));
    }

    /** 将待审核组挂起，操作日志保存状态变更前后快照。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspend(MergeReviewActionReqVO reqVO, Long operatorId, String operatorName) {
        MergeCandidateGroupDO group = activeReviewGroup(reqVO.getGroupId());
        String before = statusSnapshot(group);
        group.setStatus(MergeCodes.GROUP_STATUS_SUSPEND);
        MergeAudit.touch(group, operatorId, operatorName);
        mergeCandidateGroupMapper.updateById(group);
        writeLog(group.getId(), MergeCodes.REVIEW_SUSPEND, operatorId, operatorName, reqVO.getReason(), before,
                statusSnapshot(group));
    }

    /** 锁定候选组后恢复挂起状态，避免旧状态覆盖已完成的审核。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resume(MergeReviewActionReqVO reqVO, Long operatorId, String operatorName) {
        MergeCandidateGroupDO group = lockedGroupOrThrow(reqVO.getGroupId());
        if (!MergeCodes.GROUP_STATUS_SUSPEND.equals(group.getStatus())) {
            throw new ServiceException("只有暂缓处理的候选组可以恢复");
        }
        String before = statusSnapshot(group);
        group.setStatus(MergeCodes.GROUP_STATUS_REVIEW);
        MergeAudit.touch(group, operatorId, operatorName);
        mergeCandidateGroupMapper.updateById(group);
        writeLog(group.getId(), MergeCodes.REVIEW_RESUME, operatorId, operatorName, reqVO.getReason(), before,
                statusSnapshot(group));
    }

    /** 先锁候选组再锁来源，校验最新治理与占用状态后固定来源当前版本。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MergeReviewAddReqVO reqVO, Long operatorId, String operatorName) {
        // 手工补入前校验来源记录未被其他有效候选组占用，避免同一来源重复参与合并。
        MergeCandidateGroupDO group = lockedGroupOrThrow(reqVO.getGroupId());
        if (!MergeCodes.GROUP_STATUS_REVIEW.equals(group.getStatus())
                && !MergeCodes.GROUP_STATUS_SUSPEND.equals(group.getStatus())) {
            throw new ServiceException("只有待审核或暂缓处理的候选组可以补入");
        }
        // 统一先锁组再锁来源；持锁后检查当前版本、治理状态和有效组占用。
        MergeSourceRecordBO source = sourceTableService.lock(group.getModelId(), reqVO.getSourceRecordId());
        if (source == null || !group.getModelId().equals(source.getModelId())) {
            throw new ServiceException("来源记录不存在或不属于当前模型");
        }
        if (!MergeCodes.GOVERNANCE_PENDING.equals(source.getGovernanceStatus())
                || source.getId() == null) {
            throw new ServiceException("只能补入待治理且有当前版本的来源数据");
        }
        Long exists = mergeCandidateGroupMemberMapper.selectCount(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                .eq(MergeCandidateGroupMemberDO::getGroupId, group.getId())
                .eq(MergeCandidateGroupMemberDO::getSourceRecordId, source.getId())
                .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES));
        if (exists != null && exists.longValue() > 0L) {
            throw new ServiceException("该来源记录已在当前候选组中");
        }
        // 来源记录只能被一个有效治理流程占用，补入前必须再次检查占用状态。
        if (!scopeService.occupiedSourceIds(group.getModelId(), Collections.singletonList(source.getId())).isEmpty()) {
            throw new ServiceException("该来源记录已被其他有效治理流程占用");
        }
        MergeCandidateGroupMemberDO member = new MergeCandidateGroupMemberDO();
        member.setGroupId(group.getId());
        member.setSourceRecordId(source.getId());
        member.setSourceVersionId(source.getId());
        member.setMemberRole(MergeCodes.MEMBER_ROLE_TARGET);
        member.setJoinType(MergeCodes.JOIN_MANUAL);
        member.setValidFlag(MergeCodes.YES);
        member.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(member, operatorId, operatorName);
        mergeCandidateGroupMemberMapper.insert(member);
        sourceTableService.transition(group.getModelId(), Collections.singletonList(source.getId()),
                MergeCodes.GOVERNANCE_PENDING, MergeCodes.GOVERNANCE_PROCESSING);
        writeLog(group.getId(), MergeCodes.REVIEW_ADD, operatorId, operatorName, reqVO.getReason(), null,
                String.valueOf(source.getId()));
    }

    /** 软删除待审核或暂缓候选组，并释放当前不再被其他有效组占用的目标来源。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId, Long operatorId, String operatorName) {
        MergeCandidateGroupDO group = lockedGroupOrThrow(groupId);
        if (!MergeCodes.GROUP_STATUS_REVIEW.equals(group.getStatus())
                && !MergeCodes.GROUP_STATUS_SUSPEND.equals(group.getStatus())) {
            throw new ServiceException("只有待审核或暂缓处理的候选组可以删除");
        }
        assertNoPending(group.getId());
        List<MergeCandidateGroupMemberDO> members = members(group.getId());
        lockTargetSources(group.getModelId(), members);

        String before = statusSnapshot(group);
        List<Long> targetSourceIds = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            if (MergeCodes.MEMBER_ROLE_TARGET.equals(member.getMemberRole())) {
                targetSourceIds.add(member.getSourceRecordId());
            }
        }
        group.setValidFlag(MergeCodes.NO);
        MergeAudit.touch(group, operatorId, operatorName);
        mergeCandidateGroupMapper.updateById(group);
        releaseSources(group.getModelId(), targetSourceIds);
        writeLog(group.getId(), MergeCodes.REVIEW_DELETE, operatorId, operatorName, "删除候选组", before,
                statusSnapshot(group));
    }

    /** 按来源编号统一取锁，避免不同分区顺序反向持锁。 */
    private void lockTargetSources(Long modelId, List<MergeCandidateGroupMemberDO> members) {
        Set<Long> sourceIds = new TreeSet<>();
        for (MergeCandidateGroupMemberDO member : members) {
            if (MergeCodes.MEMBER_ROLE_TARGET.equals(member.getMemberRole())) {
                sourceIds.add(member.getSourceRecordId());
            }
        }
        for (Long sourceId : sourceIds) {
            sourceTableService.lock(modelId, sourceId);
        }
    }

    /** 只有包含待治理目标的分区才进入结果生成。 */
    private boolean hasTarget(List<Long> partition, Map<Long, MergeCandidateGroupMemberDO> bySource) {
        for (Long sourceId : partition) {
            if (MergeCodes.MEMBER_ROLE_TARGET.equals(bySource.get(sourceId).getMemberRole())) {
                return true;
            }
        }
        return false;
    }

    /** 子组保留父组与检测实例关联，按分区成员数进入单条或多条结果流程。 */
    private MergeCandidateGroupDO newChildGroup(MergeCandidateGroupDO group, boolean single, int seq,
            Long operatorId, String operatorName) {
        MergeCandidateGroupDO child = new MergeCandidateGroupDO();
        child.setTaskInstanceId(group.getTaskInstanceId());
        child.setGroupNo(MergeAudit.no(single ? "SG" : "CG", seq));
        child.setModelId(group.getModelId());
        child.setType(single ? MergeCodes.GROUP_TYPE_SINGLE : MergeCodes.GROUP_TYPE_CANDIDATE);
        child.setStatus(single ? MergeCodes.GROUP_STATUS_SINGLE : MergeCodes.GROUP_STATUS_CONFIRMED);
        child.setTransferTime(new Date());
        child.setMaxMatchScore(group.getMaxMatchScore());
        child.setParentGroupId(group.getId());
        child.setValidFlag(MergeCodes.YES);
        child.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(child, operatorId, operatorName);

        return child;
    }

    /** 复制固定版本和成员角色，仅成员编号与组归属变化。 */
    private List<MergeCandidateGroupMemberDO> saveChildMembers(MergeCandidateGroupDO child, List<Long> partition,
            Map<Long, MergeCandidateGroupMemberDO> bySource, Long operatorId, String operatorName) {
        List<MergeCandidateGroupMemberDO> childMembers = new ArrayList<>();
        for (Long sourceId : partition) {
            MergeCandidateGroupMemberDO source = bySource.get(sourceId);
            MergeCandidateGroupMemberDO member = new MergeCandidateGroupMemberDO();
            member.setGroupId(child.getId());
            member.setSourceRecordId(source.getSourceRecordId());
            member.setSourceVersionId(source.getSourceVersionId());
            member.setMemberRole(source.getMemberRole());
            member.setJoinType(source.getJoinType());
            member.setValidFlag(MergeCodes.YES);
            member.setDelFlag(MergeCodes.NO);
            MergeAudit.fill(member, operatorId, operatorName);
            mergeCandidateGroupMemberMapper.insert(member);
            childMembers.add(member);
        }

        return childMembers;
    }

    /** 从操作固定的上下文选取本组来源，单条结果不采用生存规则。 */
    private void createPending(MergeCandidateGroupDO group, List<MergeCandidateGroupMemberDO> members,
            MergeReviewSourceContextBO context, String processType, Long operatorId, String operatorName) {
        List<SourceSnapshot> snapshots = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            SourceSnapshot snapshot = context.getSourceSnapshots().get(member.getSourceRecordId());
            if (snapshot == null) {
                throw new ServiceException("来源版本无法解析，不能形成待提交结果");
            }
            snapshots.add(snapshot);
        }
        List<RuleSurvivorDO> rules = MergeCodes.PROCESS_SINGLE.equals(processType)
                ? Collections.emptyList() : context.getSurvivorRules();
        mergeResultFactory.create(group.getId(), group.getModelId(), context.getModelVersionId(), processType,
                snapshots, context.getColumns(), rules, operatorId, operatorName);
    }

    /** 写事务内锁定候选组，并限定只有待审核状态才能确认、拆分或挂起。 */
    private MergeCandidateGroupDO activeReviewGroup(Long groupId) {
        // 确认与拆分先锁原组，避免并发拆分按不同分区顺序持有来源锁。
        MergeCandidateGroupDO group = lockedGroupOrThrow(groupId);
        if (!MergeCodes.GROUP_STATUS_REVIEW.equals(group.getStatus())) {
            throw new ServiceException("只有待审核候选组可以执行该操作");
        }
        return group;
    }

    /** 写事务内锁定有效候选组，所有审核动作均在持锁后校验状态。 */
    private MergeCandidateGroupDO lockedGroupOrThrow(Long groupId) {
        MergeCandidateGroupDO group = mergeCandidateGroupMapper.selectForUpdate(groupId);
        if (group == null || !MergeCodes.YES.equals(group.getValidFlag())) {
            throw new ServiceException("候选组不存在或已失效");
        }
        return group;
    }

    /** 按编号顺序读取有效成员，包含目标和历史参考记录。 */
    private List<MergeCandidateGroupMemberDO> members(Long groupId) {
        return mergeCandidateGroupMemberMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                .eq(MergeCandidateGroupMemberDO::getGroupId, groupId)
                .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES)
                .orderByAsc(MergeCandidateGroupMemberDO::getId));
    }

    /** 已有有效待提交结果时禁止重复确认或拆分。 */
    private void assertNoPending(Long groupId) {
        Long count = mergePendingMasterMapper.selectCount(new LambdaQueryWrapper<MergePendingMasterDO>()
                .eq(MergePendingMasterDO::getObjectGroupId, groupId)
                .eq(MergePendingMasterDO::getValidFlag, MergeCodes.YES));
        if (count != null && count.longValue() > 0L) {
            throw new ServiceException("候选组已经形成待提交结果");
        }
    }

    /** 拆分必须完整覆盖原组成员，每个来源只能分配一次，且至少形成两个分区。 */
    private List<List<Long>> normalizePartitions(List<List<Long>> sourceGroups,
            Map<Long, MergeCandidateGroupMemberDO> bySource) {
        if (sourceGroups == null || sourceGroups.size() < 2) {
            throw new ServiceException("拆分至少形成两个对象");
        }
        Set<Long> seen = new HashSet<>();
        List<List<Long>> partitions = new ArrayList<>();
        for (List<Long> sourceGroup : sourceGroups) {
            if (sourceGroup == null || sourceGroup.isEmpty()) {
                throw new ServiceException("拆分对象不能为空");
            }
            List<Long> partition = new ArrayList<>();
            for (Long sourceId : sourceGroup) {
                if (sourceId == null || !bySource.containsKey(sourceId)) {
                    throw new ServiceException("来源记录不属于当前候选组");
                }
                if (!seen.add(sourceId)) {
                    throw new ServiceException("来源记录不能重复分配");
                }
                partition.add(sourceId);
            }
            partitions.add(partition);
        }
        if (seen.size() != bySource.size()) {
            throw new ServiceException("还有来源记录未分配");
        }
        return partitions;
    }

    /** 仅将未被其他有效候选组占用且仍处于处理中状态的来源退回待治理。 */
    private void releaseSources(Long modelId, List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return;
        }
        Set<Long> stillOccupied = scopeService.occupiedSourceIds(modelId, sourceIds);
        List<Long> releaseIds = new ArrayList<>();
        for (Long sourceId : sourceIds) {
            if (!stillOccupied.contains(sourceId)) {
                releaseIds.add(sourceId);
            }
        }
        if (releaseIds.isEmpty()) {
            return;
        }
        sourceTableService.transition(modelId, releaseIds, MergeCodes.GOVERNANCE_PROCESSING,
                MergeCodes.GOVERNANCE_PENDING);
    }

    /** 在审核写事务内保存操作人、原因与前后快照，失败时随审核动作回滚。 */
    private void writeLog(Long groupId, String action, Long operatorId, String operatorName, String reason,
            String before, String after) {
        MergeCandidateReviewLogDO log = new MergeCandidateReviewLogDO();
        log.setGroupId(groupId);
        log.setActionType(action);
        log.setReviewerId(operatorId == null ? 0L : operatorId);
        log.setReviewerName(operatorName);
        log.setReviewTime(new Date());
        log.setReason(MergeAudit.cut(reason, 500));
        log.setBeforeData(before);
        log.setAfterData(after);
        log.setValidFlag(MergeCodes.YES);
        log.setDelFlag(MergeCodes.NO);
        MergeAudit.fill(log, operatorId, operatorName);
        mergeCandidateReviewLogMapper.insert(log);
    }

    /** 保存审核状态前后快照，供操作记录展示。 */
    private String statusSnapshot(MergeCandidateGroupDO group) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("groupId", group.getId());
        snapshot.put("status", group.getStatus());
        return MergeJson.toJson(snapshot);
    }

    /** 审核日志记录来源编号，包含目标与历史参考成员。 */
    private String memberIds(List<MergeCandidateGroupMemberDO> members) {
        List<Long> ids = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            ids.add(member.getSourceRecordId());
        }
        return MergeJson.toJson(ids);
    }
}
