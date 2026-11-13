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

package tech.qiantong.qdata.mdm.merge.service.manage.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupRespVO;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewQueryService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingApprovalRecordRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingFieldRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingLogPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingRespVO;
import tech.qiantong.qdata.mdm.merge.convert.manage.MergeManageConvert;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterFieldDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterLogDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeQualityLogDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeRecordDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterFieldMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterLogMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergePendingMasterMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergeQualityLogMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.manage.MergeRecordMapper;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergePendingQueryBO;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergeEffectiveResultBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.enums.MergeManageQueryEnum;
import tech.qiantong.qdata.mdm.merge.service.manage.IMergeManageQueryService;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceSnapshotService;
import tech.qiantong.qdata.mdm.merge.support.MergeAudit;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.source.MasterSourceRelDO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.source.MasterFieldSourceRelDO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestItemMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.source.MasterSourceRelMapper;
import tech.qiantong.qdata.mdm.master.dal.mapper.source.MasterFieldSourceRelMapper;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor.RuleSurvivorMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 合并管理查询服务，批量加载关联展示数据并转换响应。
 *
 * @author chentangwen
 */
@Service
public class MergeManageQueryServiceImpl implements IMergeManageQueryService {

    @Resource
    private MergePendingMasterMapper mergePendingMasterMapper;

    @Resource
    private MergePendingMasterFieldMapper mergePendingMasterFieldMapper;

    @Resource
    private MergePendingMasterLogMapper mergePendingMasterLogMapper;

    @Resource
    private MergeQualityLogMapper mergeQualityLogMapper;

    @Resource
    private MergeRecordMapper mergeRecordMapper;

    @Resource
    private MergeCandidateGroupMapper mergeCandidateGroupMapper;

    @Resource
    private IMergeReviewQueryService mergeReviewQueryService;

    @Resource
    private IMergeSourceTableService sourceTableService;

    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private RuleSurvivorMapper ruleSurvivorMapper;

    @Resource
    private IMergeSourceSnapshotService mergeSourceSnapshotService;

    @Resource
    private MasterChangeRequestMapper masterChangeRequestMapper;

    @Resource
    private MasterChangeRequestItemMapper masterChangeRequestItemMapper;

    @Resource
    private MasterSourceRelMapper masterSourceRelMapper;

    @Resource
    private MasterFieldSourceRelMapper masterFieldSourceRelMapper;

    /**
     * 分页查询合并结果，状态范围和旧关键字兼容逻辑在此解析。
     */
    @Override
    public PageResult<MergePendingRespVO> page(MergePendingPageReqVO reqVO) {
        MergePendingQueryBO query = new MergePendingQueryBO();
        query.setStatuses(MergeManageQueryEnum.resolveStatuses(reqVO.getStatus()));
        List<Long> recordIds = null;
        if (reqVO.getProcessType() != null && !reqVO.getProcessType().trim().isEmpty()) {
            // 处理类型存储在合并记录表，先解析记录 ID 再过滤待提交结果。
            List<MergeRecordDO> records = mergeRecordMapper.selectList(
                new LambdaQueryWrapper<MergeRecordDO>()
                    .eq(reqVO.getModelId() != null, MergeRecordDO::getModelId, reqVO.getModelId())
                    .eq(MergeRecordDO::getProcessType, reqVO.getProcessType())
                    .eq(MergeRecordDO::getValidFlag, MergeCodes.YES));
            recordIds = new ArrayList<>();
            for (MergeRecordDO record : records) {
                recordIds.add(record.getId());
            }
            if (recordIds.isEmpty()) {
                return PageResult.empty();
            }
        }
        String groupNo = MergeAudit.text(reqVO.getGroupNo());
        String businessName = MergeAudit.text(reqVO.getBusinessName());
        String keyword = MergeAudit.text(reqVO.getKeyword());
        boolean hasGroupNo = !groupNo.isEmpty();
        boolean hasBusinessName = !businessName.isEmpty();
        boolean legacyKeyword = !keyword.isEmpty() && !hasGroupNo && !hasBusinessName;
        List<Long> groupIds = null;
        if (hasGroupNo) {
            groupIds = groupIdsByNo(groupNo);
            if (groupIds.isEmpty()) {
                return PageResult.empty();
            }
        }
        List<Long> keywordGroupIds = legacyKeyword ? groupIdsByNo(keyword) : Collections.emptyList();
        query.setRecordIds(recordIds);
        query.setGroupIds(groupIds);
        query.setKeywordGroupIds(keywordGroupIds);
        query.setBusinessName(businessName);
        query.setLegacyKeyword(legacyKeyword ? keyword : "");
        PageResult<MergePendingMasterDO> page = mergePendingMasterMapper.selectPage(reqVO, query);
        // 列表主表只保存关联 ID，以下批量加载关联数据后再组装响应，避免循环查询。
        Map<Long, String> processTypes = processTypes(page.getRows());
        Map<Long, MergeCandidateGroupDO> groupMap = loadGroups(page.getRows());
        Map<Long, MergeGroupRespVO> summaries = groupSummaries(groupMap);
        Map<Long, List<ModelColumnDO>> nameColumns = nameColumns(page.getRows());
        List<MergePendingRespVO> rows = new ArrayList<>();
        for (MergePendingMasterDO pending : page.getRows()) {
            syncMasterResult(pending);
            MergePendingRespVO row = MergeManageConvert.toPage(pending);
            row.setProcessType(processTypes.get(pending.getMergeRecordId()));
            MergeCandidateGroupDO group = groupMap.get(pending.getObjectGroupId());
            row.setGroupNo(group == null ? null : group.getGroupNo());
            row.setBusinessName(businessName(pending.getResultData(), nameColumns.get(pending.getModelId())));
            MergeGroupRespVO summary = summaries.get(pending.getObjectGroupId());
            row.setSourceCount(summary == null ? 0 : summary.getMemberCount());
            row.setRuleCount(summary == null ? 0 : summary.getRuleCount());
            row.setRuleNames(summary == null ? Collections.emptyList() : summary.getRuleNames());
            if (summary != null) {
                row.setTaskName(summary.getTaskName());
                row.setRemark(summary.getRemark());
                row.setModelVersionNo(summary.getModelVersionNo());
                row.setModelVersionStatus(summary.getModelVersionStatus());
                row.setSourceSystems(summary.getSourceSystems());
            }
            if (group != null) {
                row.setTransferTime(group.getTransferTime());
                row.setMaxMatchScore(group.getMaxMatchScore());
            }
            rows.add(row);
        }
        return new PageResult<>(rows, page.getTotal());
    }

    /** 复用匹配审核的批量摘要，统一任务、检测版本、来源编码和规则名称。 */
    private Map<Long, MergeGroupRespVO> groupSummaries(Map<Long, MergeCandidateGroupDO> groups) {
        List<MergeGroupRespVO> rows = BeanUtils.toBean(new ArrayList<>(groups.values()), MergeGroupRespVO.class);
        mergeReviewQueryService.fillSummary(rows);
        Map<Long, MergeGroupRespVO> result = new HashMap<>();
        for (MergeGroupRespVO row : rows) {
            result.put(row.getId(), row);
        }
        return result;
    }

    /**
     * 查询结果、字段候选值及质量日志，来源值始终使用成员固定版本。
     */
    @Override
    public MergePendingDetailRespVO detail(Long pendingId) {
        MergePendingMasterDO pending = pendingOrThrow(pendingId);
        syncMasterResult(pending);
        MergeRecordDO record = pending.getMergeRecordId() == null ? null
                : mergeRecordMapper.selectById(pending.getMergeRecordId());
        MergeCandidateGroupDO group = pending.getObjectGroupId() == null ? null
                : mergeCandidateGroupMapper.selectById(pending.getObjectGroupId());
        List<MergePendingMasterFieldDO> fields = fields(pendingId);
        Map<Long, ModelColumnDO> columns = columns(pending.getModelId());
        List<SourceSnapshot> snapshots = mergeSourceSnapshotService.loadGroupSnapshots(
                pending.getObjectGroupId(), new ArrayList<>(columns.values()));
        MergePendingDetailRespVO detail = MergeManageConvert.toDetail(pending);
        detail.setProcessType(record == null ? null : record.getProcessType());
        detail.setGroupNo(group == null ? null : group.getGroupNo());
        detail.setGroupType(group == null ? null : group.getType());
        fillDetailSummary(detail, group);
        if (pending.getQualityLogId() != null) {
            MergeQualityLogDO qualityLog = mergeQualityLogMapper.selectById(pending.getQualityLogId());
            if (qualityLog != null) {
                detail.setQualityMessage(qualityLog.getResultMessage());
                detail.setQualityDetail(qualityLog.getResultDetail());
            }
        }
        Map<Long, String> survivorRuleNames = survivorRuleNames(fields);
        List<MergePendingFieldRespVO> views = new ArrayList<>();
        for (MergePendingMasterFieldDO field : fields) {
            ModelColumnDO column = columns.get(field.getModelColumnId());
            MergePendingFieldRespVO view = MergeManageConvert.toField(
                    field, column, survivorRuleNames.get(field.getSurvivorRuleId()));
            view.setCandidates(MergeManageConvert.candidates(snapshots, field.getFieldPath()));
            views.add(view);
        }
        detail.setFields(views);
        if (MergeCodes.PENDING_EFFECTIVE.equals(pending.getStatus())) {
            fillEffectiveResult(detail);
        }
        return detail;
    }

    /** 详情基础字段与列表共用检测任务及候选组摘要。 */
    private void fillDetailSummary(MergePendingDetailRespVO detail, MergeCandidateGroupDO group) {
        if (group == null) {
            return;
        }
        MergeGroupRespVO summary = groupSummaries(Collections.singletonMap(group.getId(), group)).get(group.getId());
        detail.setTaskName(summary.getTaskName());
        detail.setRemark(summary.getRemark());
        detail.setScopeType(summary.getScopeType());
        detail.setModelVersionNo(summary.getModelVersionNo());
        detail.setModelVersionStatus(summary.getModelVersionStatus());
        detail.setSourceCount(summary.getMemberCount());
        detail.setRuleCount(summary.getRuleCount());
        detail.setMaxMatchScore(group.getMaxMatchScore());
        detail.setTransferTime(group.getTransferTime());
    }

    /** 将主数据执行器的最终结果回写到融合结果。 */
    private void syncMasterResult(MergePendingMasterDO pending) {
        if (pending == null || pending.getRequestId() == null
                || !MergeCodes.PENDING_WAIT_EFFECT.equals(pending.getStatus())) {
            return;
        }
        MasterChangeRequestDO request = masterChangeRequestMapper.selectById(pending.getRequestId());
        if (request == null) {
            return;
        }
        List<MasterChangeRequestItemDO> items = masterChangeRequestItemMapper
                .selectByRequestIdForExecute(pending.getRequestId());
        if (items.isEmpty()) {
            return;
        }
        MasterChangeRequestItemDO item = items.get(0);
        if ("2".equals(request.getExecuteStatus())
                && item.getResultMasterRecordId() != null && item.getResultVersionId() != null) {
            pending.setTargetRecordId(item.getResultMasterRecordId());
            pending.setResultRecordId(item.getResultMasterRecordId());
            pending.setResultVersionId(item.getResultVersionId());
            pending.setErrorMessage(null);
            // 先登记来源关系，确保关系写入失败时不会提前把融合结果标成已生效；关系写入具备幂等条件，可重试。
            writeSourceRelations(pending, item.getResultMasterRecordId());
            List<Long> targetSources = new ArrayList<>();
            for (SourceSnapshot snapshot : mergeSourceSnapshotService.loadGroupSnapshots(
                    pending.getObjectGroupId(), new ArrayList<>(columns(pending.getModelId()).values()))) {
                if (snapshot.isTarget()) targetSources.add(snapshot.getSourceRecordId());
            }
            sourceTableService.transition(pending.getModelId(), targetSources, MergeCodes.GOVERNANCE_PROCESSING,
                    MergeCodes.GOVERNANCE_MERGED);
            pending.setStatus(MergeCodes.PENDING_EFFECTIVE);
            mergePendingMasterMapper.updateById(pending);
        } else if ("3".equals(request.getExecuteStatus()) || "4".equals(request.getExecuteStatus())) {
            pending.setStatus(MergeCodes.PENDING_REJECTED);
            pending.setErrorMessage(request.getExecuteMessage());
            mergePendingMasterMapper.updateById(pending);
        }
    }

    /** 将本次合并实际参与的来源记录登记到权威主数据来源关系。 */
    private void writeSourceRelations(MergePendingMasterDO pending, Long masterRecordId) {
        if (masterRecordId == null || pending.getObjectGroupId() == null) {
            return;
        }
        List<SourceSnapshot> snapshots = mergeSourceSnapshotService.loadGroupSnapshots(
                pending.getObjectGroupId(), new ArrayList<>(columns(pending.getModelId()).values()));
        for (SourceSnapshot snapshot : snapshots) {
            if (snapshot == null || !snapshot.isTarget() || snapshot.getSystemId() == null
                    || snapshot.getSourceRecordId() == null || snapshot.getVersionId() == null) {
                continue;
            }
            long exists = masterSourceRelMapper.selectCount(new LambdaQueryWrapper<MasterSourceRelDO>()
                    .eq(MasterSourceRelDO::getModelId, pending.getModelId())
                    .eq(MasterSourceRelDO::getMasterRecordId, masterRecordId)
                    .eq(MasterSourceRelDO::getSourceRecordId, snapshot.getSourceRecordId())
                    .eq(MasterSourceRelDO::getSourceVersionId, snapshot.getVersionId())
                    .eq(MasterSourceRelDO::getMergeRecordId, pending.getMergeRecordId())
                    .eq(MasterSourceRelDO::getValidFlag, true)
                    .eq(MasterSourceRelDO::getDelFlag, false));
            if (exists > 0) {
                continue;
            }
            MasterSourceRelDO relation = new MasterSourceRelDO();
            relation.setModelId(pending.getModelId());
            relation.setMasterRecordId(masterRecordId);
            relation.setSourceId(snapshot.getSystemId());
            relation.setSourceRecordId(snapshot.getSourceRecordId());
            relation.setSourceVersionId(snapshot.getVersionId());
            relation.setType("MERGE");
            relation.setStatus("1");
            relation.setEffectiveTime(new java.util.Date());
            relation.setRequestId(pending.getRequestId());
            relation.setMergeRecordId(pending.getMergeRecordId());
            relation.setValidFlag(true);
            relation.setDelFlag(false);
            MergeAudit.fill(relation, pending.getUpdatorId(), pending.getUpdateBy());
            masterSourceRelMapper.insert(relation);
        }
        Map<Long, SourceSnapshot> sourceByVersion = new HashMap<>();
        for (SourceSnapshot snapshot : snapshots) {
            if (snapshot != null && snapshot.getVersionId() != null) {
                sourceByVersion.put(snapshot.getVersionId(), snapshot);
            }
        }
        for (MergePendingMasterFieldDO field : fields(pending.getId())) {
            if (field.getSourceVersionId() == null || field.getModelColumnId() == null) {
                continue;
            }
            SourceSnapshot snapshot = sourceByVersion.get(field.getSourceVersionId());
            if (snapshot == null || snapshot.getSystemId() == null || snapshot.getSourceRecordId() == null) {
                continue;
            }
            long exists = masterFieldSourceRelMapper.selectCount(new LambdaQueryWrapper<MasterFieldSourceRelDO>()
                    .eq(MasterFieldSourceRelDO::getModelId, pending.getModelId())
                    .eq(MasterFieldSourceRelDO::getMasterRecordId, masterRecordId)
                    .eq(MasterFieldSourceRelDO::getMasterVersionId, pending.getResultVersionId())
                    .eq(MasterFieldSourceRelDO::getModelColumnId, field.getModelColumnId())
                    .eq(MasterFieldSourceRelDO::getSourceRecordId, snapshot.getSourceRecordId())
                    .eq(MasterFieldSourceRelDO::getSourceVersionId, field.getSourceVersionId())
                    .eq(MasterFieldSourceRelDO::getRequestId, pending.getRequestId())
                    .eq(MasterFieldSourceRelDO::getValidFlag, true)
                    .eq(MasterFieldSourceRelDO::getDelFlag, false));
            if (exists > 0) {
                continue;
            }
            MasterFieldSourceRelDO relation = new MasterFieldSourceRelDO();
            relation.setId(IdWorker.getId());
            relation.setModelId(pending.getModelId());
            relation.setMasterRecordId(masterRecordId);
            relation.setMasterVersionId(pending.getResultVersionId());
            relation.setModelColumnId(field.getModelColumnId());
            relation.setFieldPath(field.getFieldPath());
            relation.setSourceId(snapshot.getSystemId());
            relation.setSourceRecordId(snapshot.getSourceRecordId());
            relation.setSourceVersionId(field.getSourceVersionId());
            relation.setSourceFieldPath(field.getFieldPath());
            relation.setRelationType("MERGE");
            relation.setStatus("1");
            relation.setEffectiveTime(new java.util.Date());
            relation.setRequestId(pending.getRequestId());
            relation.setValidFlag(true);
            relation.setDelFlag(false);
            MergeAudit.fill(relation, pending.getUpdatorId(), pending.getUpdateBy());
            masterFieldSourceRelMapper.insert(relation);
        }
    }

    /** 读取本次正式版本，兼容权威库快照中使用大写列编码的字段。 */
    private void fillEffectiveResult(MergePendingDetailRespVO detail) {
        MergeEffectiveResultBO effective = mergePendingMasterMapper.selectEffectiveResult(detail.getId());
        if (effective == null) {
            return;
        }
        detail.setEffectiveVersionNo(effective.getVersionNo());
        detail.setSourceMappingCount(detail.getResultRecordId() == null ? null : effective.getSourceMappingCount());
        JsonNode snapshot = MergeJson.read(effective.getSnapshotData());
        Map<String, Object> values = new LinkedHashMap<>();
        for (MergePendingFieldRespVO field : detail.getFields()) {
            String path = field.getFieldPath();
            if (path == null) {
                continue;
            }
            JsonNode value = snapshot.get(path);
            if (value == null) {
                value = snapshot.get(path.toUpperCase(java.util.Locale.ROOT));
            }
            values.put(path, value == null || value.isNull() ? null
                    : value.isValueNode() ? value.asText() : value);
        }
        detail.setEffectiveData(values);
    }

    /**
     * 校验待合并结果后分页读取操作日志，并转换为审批记录展示对象。
     *
     * @param pendingId 待合并结果编号
     * @param reqVO 分页参数
     * @return 审批与流转记录分页数据
     */
    @Override
    public PageResult<MergePendingApprovalRecordRespVO> approvalRecords(
            Long pendingId, MergePendingLogPageReqVO reqVO) {
        pendingOrThrow(pendingId);
        PageResult<MergePendingMasterLogDO> page = mergePendingMasterLogMapper.selectPageByPendingId(pendingId, reqVO);
        List<MergePendingApprovalRecordRespVO> rows = new ArrayList<>();
        for (MergePendingMasterLogDO log : page.getRows()) {
            rows.add(MergeManageConvert.toApprovalRecord(log));
        }
        return new PageResult<>(rows, page.getTotal());
    }

    /**
     * 将组编号条件解析成候选组编号。
     */
    private List<Long> groupIdsByNo(String groupNo) {
        List<MergeCandidateGroupDO> matchedGroups = mergeCandidateGroupMapper.selectList(
                new LambdaQueryWrapper<MergeCandidateGroupDO>()
                        .select(MergeCandidateGroupDO::getId)
                .like(MergeCandidateGroupDO::getGroupNo, groupNo));
        List<Long> ids = new ArrayList<>();
        for (MergeCandidateGroupDO matchedGroup : matchedGroups) {
            if (matchedGroup.getId() != null) {
                ids.add(matchedGroup.getId());
            }
        }
        return ids;
    }

    /**
     * 批量加载当前页涉及的候选组，避免循环查询。
     */
    private Map<Long, MergeCandidateGroupDO> loadGroups(List<MergePendingMasterDO> pendings) {
        Map<Long, MergeCandidateGroupDO> result = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        if (pendings != null) {
            for (MergePendingMasterDO pending : pendings) {
                if (pending.getObjectGroupId() != null) {
                    ids.add(pending.getObjectGroupId());
                }
            }
        }
        if (ids.isEmpty()) {
            return result;
        }
        List<MergeCandidateGroupDO> groups = mergeCandidateGroupMapper.selectList(
                new LambdaQueryWrapper<MergeCandidateGroupDO>()
                        .in(MergeCandidateGroupDO::getId, ids));
        for (MergeCandidateGroupDO group : groups) {
            result.put(group.getId(), group);
        }
        return result;
    }

    /**
     * 一次加载当前页模型字段，供业务名称解析使用。
     */
    private Map<Long, List<ModelColumnDO>> nameColumns(List<MergePendingMasterDO> pendings) {
        Map<Long, List<ModelColumnDO>> result = new HashMap<>();
        Set<Long> modelIds = new HashSet<>();
        if (pendings != null) {
            for (MergePendingMasterDO pending : pendings) {
                if (pending.getModelId() != null) {
                    modelIds.add(pending.getModelId());
                }
            }
        }
        if (modelIds.isEmpty()) {
            return result;
        }
        List<ModelColumnDO> columns = modelColumnMapper.selectList(
                new LambdaQueryWrapper<ModelColumnDO>()
                        .in(ModelColumnDO::getModelId, modelIds));
        for (ModelColumnDO column : columns) {
            List<ModelColumnDO> rows = result.get(column.getModelId());
            if (rows == null) {
                rows = new ArrayList<>();
                result.put(column.getModelId(), rows);
            }
            rows.add(column);
        }
        return result;
    }

    /**
     * 优先使用名称标记，兼容历史模型中字段名包含“名称”的配置。
     */
    private String businessName(String resultData, List<ModelColumnDO> columns) {
        JsonNode root = MergeJson.read(resultData);
        if (!root.isObject() || columns == null) {
            return null;
        }
        for (ModelColumnDO column : columns) {
            if (Boolean.TRUE.equals(column.getNameFlag())) {
                return columnValue(root, column);
            }
        }
        for (ModelColumnDO column : columns) {
            if (column.getName() != null && column.getName().contains("名称")) {
                String value = columnValue(root, column);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * 依次兼容字段编码、字段编号和字段名称读取结果值。
     */
    private String columnValue(JsonNode root, ModelColumnDO column) {
        String value = jsonText(root, column.getCode());
        if (value == null && column.getId() != null) {
            value = jsonText(root, String.valueOf(column.getId()));
        }
        return value == null ? jsonText(root, column.getName()) : value;
    }

    /**
     * 读取非空文本值，缺失或空值不作为显示名称。
     */
    private String jsonText(JsonNode root, String key) {
        if (key == null || !root.has(key) || root.get(key).isNull()) {
            return null;
        }
        String value = root.get(key).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 批量读取字段关联的生存规则名称。
     */
    private Map<Long, String> survivorRuleNames(List<MergePendingMasterFieldDO> fields) {
        Set<Long> ids = new LinkedHashSet<>();
        for (MergePendingMasterFieldDO field : fields) {
            if (field.getSurvivorRuleId() != null) {
                ids.add(field.getSurvivorRuleId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        List<RuleSurvivorDO> rules = ruleSurvivorMapper.selectBatchIds(ids);
        for (RuleSurvivorDO rule : rules) {
            names.put(rule.getId(), rule.getRuleName());
        }
        return names;
    }

    /**
     * 加载模型字段并按编号索引。
     */
    private Map<Long, ModelColumnDO> columns(Long modelId) {
        Map<Long, ModelColumnDO> result = new HashMap<>();
        List<ModelColumnDO> rows = modelColumnMapper.selectList(
                new LambdaQueryWrapper<ModelColumnDO>()
                        .eq(ModelColumnDO::getModelId, modelId));
        for (ModelColumnDO row : rows) {
            result.put(row.getId(), row);
        }
        return result;
    }

    /**
     * 按编号顺序读取有效结果字段。
     */
    private List<MergePendingMasterFieldDO> fields(Long pendingId) {
        return mergePendingMasterFieldMapper.selectList(
                new LambdaQueryWrapper<MergePendingMasterFieldDO>()
                        .eq(MergePendingMasterFieldDO::getPendingId, pendingId)
                .eq(MergePendingMasterFieldDO::getValidFlag, MergeCodes.YES)
                .orderByAsc(MergePendingMasterFieldDO::getId));
    }

    /**
     * 读取有效结果，查询操作不获取写锁。
     */
    private MergePendingMasterDO pendingOrThrow(Long pendingId) {
        MergePendingMasterDO pending = mergePendingMasterMapper.selectById(pendingId);
        if (pending == null || !MergeCodes.YES.equals(pending.getValidFlag())) {
            throw new ServiceException("待提交结果不存在");
        }
        return pending;
    }

    /**
     * 批量读取当前页的处理类型。
     */
    private Map<Long, String> processTypes(List<MergePendingMasterDO> rows) {
        Map<Long, String> result = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        Set<Long> ids = new HashSet<>();
        for (MergePendingMasterDO row : rows) {
            if (row.getMergeRecordId() != null) {
                ids.add(row.getMergeRecordId());
            }
        }
        if (ids.isEmpty()) {
            return result;
        }
        List<MergeRecordDO> records = mergeRecordMapper.selectList(
                new LambdaQueryWrapper<MergeRecordDO>().in(MergeRecordDO::getId, ids));
        for (MergeRecordDO record : records) {
            result.put(record.getId(), record.getProcessType());
        }
        return result;
    }
}
