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

package tech.qiantong.qdata.mdm.merge.service.source.impl;

import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskMapper;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMemberMapper;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceSnapshotService;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 来源快照服务，按成员保存的物理行主键加载来源、任务及系统信息。
 *
 * @author chentangwen
 */
@Service
public class MergeSourceSnapshotServiceImpl implements IMergeSourceSnapshotService {

    @Resource
    private MergeCandidateGroupMemberMapper mergeCandidateGroupMemberMapper;

    @Resource
    private IMergeSourceTableService sourceTableService;


    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private IngestTaskMapper ingestTaskMapper;

    @Resource
    private MergeCandidateGroupMapper groupMapper;

    /**
     * 按成员关联的来源表行批量构造快照，缺失记录保留 null 供审核方处理。
     */
    @Override
    public Map<Long, SourceSnapshot> loadSnapshots(List<MergeCandidateGroupMemberDO> members,
            List<ModelColumnDO> columns) {
        Map<Long, SourceSnapshot> snapshots = new LinkedHashMap<>();
        if (members.isEmpty()) {
            return snapshots;
        }
        Set<Long> sourceIds = new HashSet<>();
        for (MergeCandidateGroupMemberDO member : members) {
            sourceIds.add(member.getSourceRecordId());
        }
        Map<Long, MergeSourceRecordBO> sources = new HashMap<>();
        Set<Long> modelIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        for (MergeCandidateGroupMemberDO member : members) groupIds.add(member.getGroupId());
        for (MergeCandidateGroupDO group : groupMapper.selectBatchIds(groupIds)) {
            modelIds.add(group.getModelId());
        }
        for (Long modelId : modelIds) {
            for (MergeSourceRecordBO source : sourceTableService.load(modelId, sourceIds)) sources.put(source.getId(), source);
        }
        Map<Long, IngestTaskDO> tasks = new HashMap<>();
        Set<Long> taskIds = new HashSet<>();
        for (MergeSourceRecordBO source : sources.values()) {
            if (source.getTaskId() != null) {
                taskIds.add(source.getTaskId());
            }
        }
        if (!taskIds.isEmpty()) {
            for (IngestTaskDO task : ingestTaskMapper.selectByIdsForSourceData(taskIds)) {
                tasks.put(task.getId(), task);
            }
        }
        Map<Long, IngestSourceDO> systems = loadSystems(sources.values(), tasks.values());
        for (MergeCandidateGroupMemberDO member : members) {
            MergeSourceRecordBO source = sources.get(member.getSourceVersionId());
            Long systemId = source == null ? null : source.getSourceSystemId();
            if (systemId == null && source != null) {
                IngestTaskDO task = tasks.get(source.getTaskId());
                systemId = task == null ? null : task.getSourceId();
            }
            boolean target = MergeCodes.MEMBER_ROLE_TARGET.equals(member.getMemberRole());
            SourceSnapshot snapshot = SourceSnapshot.from(source, systems.get(systemId), columns, target);
            // 历史来源记录可能没有直接保存来源系统，使用接入任务回退出的系统编号留痕。
            if (snapshot != null && snapshot.getSystemId() == null) {
                snapshot.setSystemId(systemId);
            }
            snapshots.put(member.getId(), snapshot);
        }
        return snapshots;
    }

    /**
     * 管理页面只返回有效快照，按成员编号顺序保留候选值顺序。
     */
    @Override
    public List<SourceSnapshot> loadGroupSnapshots(Long groupId, List<ModelColumnDO> columns) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        List<MergeCandidateGroupMemberDO> members = mergeCandidateGroupMemberMapper.selectList(
                new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                        .eq(MergeCandidateGroupMemberDO::getGroupId, groupId)
                        .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES)
                        .orderByAsc(MergeCandidateGroupMemberDO::getId));
        List<SourceSnapshot> snapshots = new ArrayList<>();
        for (SourceSnapshot snapshot : loadSnapshots(members, columns).values()) {
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    /**
     * 一次加载直接来源和历史接入任务涉及的来源系统。
     */
    private Map<Long, IngestSourceDO> loadSystems(Collection<MergeSourceRecordBO> sources,
            Collection<IngestTaskDO> tasks) {
        Set<Long> systemIds = new HashSet<>();
        for (MergeSourceRecordBO source : sources) {
            if (source.getSourceSystemId() != null) {
                systemIds.add(source.getSourceSystemId());
            }
        }
        for (IngestTaskDO task : tasks) {
            if (task.getSourceId() != null) {
                systemIds.add(task.getSourceId());
            }
        }
        Map<Long, IngestSourceDO> systems = new HashMap<>();
        if (!systemIds.isEmpty()) {
            List<IngestSourceDO> rows = ingestSourceMapper.selectList(
                    new LambdaQueryWrapper<IngestSourceDO>().in(IngestSourceDO::getId, systemIds));
            for (IngestSourceDO row : rows) {
                systems.put(row.getId(), row);
            }
        }
        return systems;
    }
}
