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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceMapper;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectScopeRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectStartReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMemberMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskInstanceMapper;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectContextBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeDetectScopeBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectScopeService;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.merge.utils.detect.MergeDetectConfigUtils;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleMatch.RuleMatchMapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 重复检测范围查询服务，负责规则与来源快照加载、预览和列表统计。
 *
 * @author chentangwen
 */
@Service
public class MergeDetectScopeServiceImpl implements IMergeDetectScopeService {

    /** 限制单次 IN 查询的编号数量。 */
    private static final int QUERY_BATCH_SIZE = 500;

    /** 两两比较的容量由压测配置；未配置时不把旧实现的 2000 条作为业务限制。 */
    @Value("${qdata.merge.detect.max-comparable-records:0}")
    private int maxComparableRecords;

    @Resource
    private MergeDuplicateTaskInstanceMapper mergeDuplicateTaskInstanceMapper;

    @Resource
    private MergeCandidateGroupMapper mergeCandidateGroupMapper;

    @Resource
    private MergeCandidateGroupMemberMapper mergeCandidateGroupMemberMapper;

    @Resource
    private IMergeSourceTableService sourceTableService;


    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private ModelBaseMapper modelBaseMapper;

    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private ModelPublishedApi modelPublishedApi;

    @Resource
    private RuleMatchMapper ruleMatchMapper;

    /**
     * 固定本次检测所需模型、规则、字段及来源快照。
     * 自定义范围限制待治理目标，已合并历史记录仍参与比较；损坏来源计入跳过数量。
     *
     * @param request 检测范围配置
     * @return 已校验且包含有效目标的检测上下文
     */
    @Override
    public MergeDetectContextBO prepareDetection(MergeDetectStartReqVO request) {
        Long modelId = request.getModelId();
        ModelBaseDO model = requirePublishedModel(modelId);
        List<RuleMatchDO> rules = executableRules(modelId);
        if (rules.isEmpty()) {
            throw new ServiceException("当前模型没有可执行的匹配规则");
        }
        MergeDetectScopeBO scope = resolveScope(request);
        List<ModelColumnDO> columns = modelColumnMapper.selectList(new LambdaQueryWrapper<ModelColumnDO>()
                .eq(ModelColumnDO::getModelId, modelId));
        List<MergeSourceRecordBO> sources = narrowSources(loadComparableSources(modelId), scope);
        if (maxComparableRecords > 0 && sources.size() > maxComparableRecords) {
            throw new ServiceException("待比较来源数据超过" + maxComparableRecords + "条，请缩小范围后再检测");
        }
        Set<Long> occupied = occupiedSourceIds(modelId, idsOf(sources));
        Map<Long, IngestSourceDO> systems = loadSystems(sources);
        List<SourceSnapshot> snapshots = new ArrayList<>();
        int skippedCount = 0;
        int targetCount = 0;
        for (MergeSourceRecordBO source : sources) {
            boolean target = MergeCodes.GOVERNANCE_PENDING.equals(source.getGovernanceStatus());
            if (target && !isTargetInScope(source, occupied, scope)) {
                continue;
            }
            SourceSnapshot snapshot = SourceSnapshot.from(source,
                    systems.get(source.getSourceSystemId()), columns, target);
            if (snapshot == null) {
                skippedCount++;
                continue;
            }
            snapshots.add(snapshot);
            if (target) {
                targetCount++;
            }
        }
        if (targetCount == 0) {
            throw new ServiceException(scope.isCustom()
                    ? "当前范围内没有可参与检测的待治理来源数据" : "没有可纳入本次检测的来源数据");
        }
        MergeDetectContextBO context = new MergeDetectContextBO();
        context.setModel(model);
        context.setColumns(columns);
        context.setRules(rules);
        context.setScope(scope);
        context.setSnapshots(snapshots);
        context.setSkippedCount(skippedCount);
        context.setTargetCount(targetCount);
        return context;
    }

    /** 批量补充任务范围和执行统计。 */
    @Override
    public void fillTaskMetrics(List<MergeTaskRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (MergeTaskRespVO row : rows) {
            if (row.getId() != null) {
                ids.add(row.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        List<MergeDuplicateTaskInstanceDO> instances =
                mergeDuplicateTaskInstanceMapper.selectList(new LambdaQueryWrapper<MergeDuplicateTaskInstanceDO>()
                        .select(MergeDuplicateTaskInstanceDO::getId, MergeDuplicateTaskInstanceDO::getTaskId,
                        MergeDuplicateTaskInstanceDO::getSourceCount, MergeDuplicateTaskInstanceDO::getGroupCount,
                        MergeDuplicateTaskInstanceDO::getMatchRuleSnapshot,
                        MergeDuplicateTaskInstanceDO::getStartTime, MergeDuplicateTaskInstanceDO::getEndTime)
                        .in(MergeDuplicateTaskInstanceDO::getTaskId, ids)
                        .eq(MergeDuplicateTaskInstanceDO::getValidFlag, MergeCodes.YES)
                        .orderByDesc(MergeDuplicateTaskInstanceDO::getId));
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, MergeDuplicateTaskInstanceDO> latest = new HashMap<>();
        for (MergeDuplicateTaskInstanceDO instance : instances) {
            Integer count = counts.get(instance.getTaskId());
            counts.put(instance.getTaskId(), count == null ? 1 : count + 1);
            if (!latest.containsKey(instance.getTaskId())) {
                latest.put(instance.getTaskId(), instance);
            }
        }
        // 同一模型的多个任务共享本页查询结果，避免逐任务重复加载整批来源数据。
        Map<Long, List<MergeSourceRecordBO>> sourcesByModel = new HashMap<>();
        Map<Long, Set<Long>> occupiedByModel = new HashMap<>();
        Map<Long, Integer> ruleCountsByModel = new HashMap<>();
        for (MergeTaskRespVO row : rows) {
            row.setExecuteCount(counts.containsKey(row.getId()) ? counts.get(row.getId()) : 0);
            MergeDuplicateTaskInstanceDO instance = latest.get(row.getId());
            if (instance == null) {
                Long modelId = row.getModelId();
                List<MergeSourceRecordBO> sources = sourcesByModel.computeIfAbsent(modelId, this::loadComparableSources);
                Set<Long> occupied = occupiedByModel.computeIfAbsent(modelId, key -> occupiedSourceIds(key, idsOf(sources)));
                MergeDetectScopeBO scope = resolveScope(MergeDetectConfigUtils.toScopeRequest(row));
                row.setSourceCount(countTargets(sources, occupied, scope));
                row.setRuleCount(ruleCountsByModel.computeIfAbsent(modelId, key -> executableRules(key).size()));
                continue;
            }
            row.setSourceCount(instance.getSourceCount());
            row.setLastGroupCount(instance.getGroupCount());
            if (instance.getStartTime() != null) {
                row.setLastExecuteDurationMillis(Math.max(0L,
                        (instance.getEndTime() == null ? System.currentTimeMillis() : instance.getEndTime().getTime())
                                - instance.getStartTime().getTime()));
            }
            JsonNode rules = MergeJson.read(instance.getMatchRuleSnapshot());
            row.setRuleCount(rules.isArray() ? rules.size() : 0);
        }
        fillScopeLabels(rows);
    }

    /** 查询启用且无异常标记的规则，保留现有规则顺序。 */
    private List<RuleMatchDO> executableRules(Long modelId) {
        List<RuleMatchDO> rules = ruleMatchMapper.selectList(new LambdaQueryWrapper<RuleMatchDO>()
                .eq(RuleMatchDO::getModelId, modelId)
                .eq(RuleMatchDO::getValidFlag, Boolean.TRUE));
        List<RuleMatchDO> executable = new ArrayList<>();
        for (RuleMatchDO rule : rules) {
            if (!"1".equals(rule.getAbnormalFlag())) {
                executable.add(rule);
            }
        }
        return executable;
    }

    /** 每批最多查询 500 条来源；有效候选组仍占用其成员，防止重复治理。 */
    @Override
    public Set<Long> occupiedSourceIds(Long modelId, List<Long> sourceIds) {
        Set<Long> occupied = new HashSet<>();
        if (sourceIds.isEmpty()) {
            return occupied;
        }
        // 来源行 ID 仅在各模型的物化表内唯一，占用查询必须限定模型。
        List<String> active = Arrays.asList(MergeCodes.GROUP_STATUS_WAIT, MergeCodes.GROUP_STATUS_REVIEW,
                MergeCodes.GROUP_STATUS_SUSPEND, MergeCodes.GROUP_STATUS_CONFIRMED, MergeCodes.GROUP_STATUS_SINGLE);
        for (int from = 0; from < sourceIds.size(); from += QUERY_BATCH_SIZE) {
            List<Long> batch = sourceIds.subList(from, Math.min(from + QUERY_BATCH_SIZE, sourceIds.size()));
            List<MergeCandidateGroupMemberDO> members =
                    mergeCandidateGroupMemberMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                            .in(MergeCandidateGroupMemberDO::getSourceRecordId, batch)
                            .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES));
            if (members.isEmpty()) {
                continue;
            }
            Set<Long> groupIds = new HashSet<>();
            for (MergeCandidateGroupMemberDO member : members) {
                groupIds.add(member.getGroupId());
            }
            List<MergeCandidateGroupDO> groups =
                    mergeCandidateGroupMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupDO>()
                            .in(MergeCandidateGroupDO::getId, groupIds)
                            .eq(MergeCandidateGroupDO::getModelId, modelId)
                            .eq(MergeCandidateGroupDO::getValidFlag, MergeCodes.YES)
                            .in(MergeCandidateGroupDO::getStatus, active));
            Set<Long> activeGroups = new HashSet<>();
            for (MergeCandidateGroupDO group : groups) {
                activeGroups.add(group.getId());
            }
            for (MergeCandidateGroupMemberDO member : members) {
                if (activeGroups.contains(member.getGroupId())) {
                    occupied.add(member.getSourceRecordId());
                }
            }
        }
        return occupied;
    }

    /** 按来源系统编号批量加载系统信息与可信度。 */
    @Override
    public Map<Long, IngestSourceDO> loadSystems(List<MergeSourceRecordBO> sources) {
        Map<Long, IngestSourceDO> systems = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        for (MergeSourceRecordBO source : sources) {
            if (source.getSourceSystemId() != null) {
                ids.add(source.getSourceSystemId());
            }
        }
        if (ids.isEmpty()) {
            return systems;
        }
        List<IngestSourceDO> rows = ingestSourceMapper.selectList(new LambdaQueryWrapper<IngestSourceDO>()
                .in(IngestSourceDO::getId, ids));
        for (IngestSourceDO row : rows) {
            systems.put(row.getId(), row);
        }
        return systems;
    }

    /** 提取来源编号供批量占用查询使用。 */
    private List<Long> idsOf(List<MergeSourceRecordBO> sources) {
        List<Long> ids = new ArrayList<>();
        for (MergeSourceRecordBO source : sources) {
            ids.add(source.getId());
        }
        return ids;
    }

    /** 预览检测范围内可参与处理的来源数据和数量。 */
    @Override
    public MergeDetectScopeRespVO previewScope(MergeDetectStartReqVO reqVO) {
        requirePublishedModel(reqVO.getModelId());
        boolean custom = MergeCodes.SCOPE_CUSTOM.equals(reqVO.getScopeType());
        MergeDetectStartReqVO countReq = reqVO;
        if (!custom) {
            countReq = new MergeDetectStartReqVO();
            countReq.setModelId(reqVO.getModelId());
            countReq.setScopeType(MergeCodes.SCOPE_MODEL);
        }
        MergeDetectScopeBO scope = resolveScope(countReq);
        List<MergeSourceRecordBO> sources = loadComparableSources(reqVO.getModelId());
        Set<Long> occupied = occupiedSourceIds(reqVO.getModelId(), idsOf(sources));
        Map<Long, IngestSourceDO> systems = loadSystems(sources);
        MergeDetectScopeRespVO resp = new MergeDetectScopeRespVO();
        resp.setSystems(scopeOptions(sources, occupied, systems));
        resp.setCount(countTargets(sources, occupied, scope));
        resp.setRecords(previewTargets(sources, occupied, systems, scope,
                reqVO.getPreviewPage(), reqVO.getPreviewPageSize()));
        return resp;
    }

    /** 模型有效且拥有发布版本时才允许检测。 */
    @Override
    public ModelBaseDO requirePublishedModel(Long modelId) {
        ModelBaseDO model = modelBaseMapper.selectById(modelId);
        if (model == null) {
            throw new ServiceException("主数据模型不存在或未发布");
        }
        try {
            tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO published =
                    modelPublishedApi.getPublishedModel(modelId);
            model.setVersionId(published.getVersionId());
        } catch (RuntimeException ex) {
            throw new ServiceException("主数据模型不存在或未发布");
        }
        return model;
    }

    /** 加载待治理目标和已合并历史记录，历史记录仅作为比较参考。 */
    private List<MergeSourceRecordBO> loadComparableSources(Long modelId) {
        List<MergeSourceRecordBO> sources = sourceTableService.load(modelId, null);
        sources.removeIf(source -> !MergeCodes.GOVERNANCE_PENDING.equals(source.getGovernanceStatus())
                && !MergeCodes.GOVERNANCE_MERGED.equals(source.getGovernanceStatus()));
        return sources;
    }

    /** 自定义范围仅限制待治理目标的来源系统，历史比较记录仍保留。 */
    private List<MergeSourceRecordBO> narrowSources(List<MergeSourceRecordBO> sources, MergeDetectScopeBO scope) {
        if (!scope.isCustom()) {
            return sources;
        }
        List<MergeSourceRecordBO> narrowed = new ArrayList<>();
        for (MergeSourceRecordBO source : sources) {
            boolean target = MergeCodes.GOVERNANCE_PENDING.equals(source.getGovernanceStatus());
            if (target && (source.getSourceSystemId() == null
                    || !scope.getSystemIds().contains(source.getSourceSystemId()))) {
                continue;
            }
            narrowed.add(source);
        }
        return narrowed;
    }

    /** 校验指定来源系统与日期，结束日期包含当天全部时间。 */
    private MergeDetectScopeBO resolveScope(MergeDetectStartReqVO reqVO) {
        MergeDetectScopeBO scope = new MergeDetectScopeBO();
        String type = reqVO.getScopeType();
        if (type == null || type.trim().isEmpty() || MergeCodes.SCOPE_MODEL.equals(type)) {
            return scope;
        }
        if (!MergeCodes.SCOPE_CUSTOM.equals(type)) {
            throw new ServiceException("检测范围不正确");
        }
        scope.setCustom(true);
        scope.setSystemIds(MergeDetectConfigUtils.distinctIds(reqVO.getSourceSystemIds()));
        if (scope.getSystemIds().isEmpty()) {
            throw new ServiceException("指定范围时至少选择一个来源系统");
        }
        List<IngestSourceDO> systems = ingestSourceMapper.selectList(new LambdaQueryWrapper<IngestSourceDO>()
                .in(IngestSourceDO::getId, scope.getSystemIds())
                .eq(IngestSourceDO::getValidFlag, MergeCodes.YES));
        if (systems.size() != scope.getSystemIds().size()) {
            throw new ServiceException("来源系统不存在或已停用");
        }
        if (reqVO.getReceiveStartTime() != null && reqVO.getReceiveEndTime() != null
                && reqVO.getReceiveStartTime().after(reqVO.getReceiveEndTime())) {
            throw new ServiceException("接收时间起始日期不能晚于结束日期");
        }
        scope.setStart(reqVO.getReceiveStartTime());
        scope.setEnd(reqVO.getReceiveEndTime());
        if (scope.getEnd() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(scope.getEnd());
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            scope.setEndInclusive(calendar.getTime());
        }
        return scope;
    }

    /** 接收时间包含上下边界；指定日期时缺少接收时间的记录不参与。 */
    private boolean inReceiveRange(Date receiveTime, MergeDetectScopeBO scope) {
        if (scope.getStart() == null && scope.getEndInclusive() == null) {
            return true;
        }
        if (receiveTime == null) {
            return false;
        }
        if (scope.getStart() != null && receiveTime.before(scope.getStart())) {
            return false;
        }
        return scope.getEndInclusive() == null || !receiveTime.after(scope.getEndInclusive());
    }

    /** 统计有效待治理目标，不包含历史数据与已被占用的记录。 */
    private long countTargets(List<MergeSourceRecordBO> sources, Set<Long> occupied,
            MergeDetectScopeBO scope) {
        long count = 0L;
        for (MergeSourceRecordBO source : sources) {
            if (isPreviewTarget(source, occupied, scope)) {
                count++;
            }
        }
        return count;
    }

    /** 筛选目标后进行内存分页，预览页大小限制在 1 至 50。 */
    private List<MergeDetectScopeRespVO.PreviewRecord> previewTargets(List<MergeSourceRecordBO> sources,
            Set<Long> occupied,
            Map<Long, IngestSourceDO> systems,
            MergeDetectScopeBO scope,
            Integer previewPage,
            Integer previewPageSize) {
        List<MergeDetectScopeRespVO.PreviewRecord> records = new ArrayList<>();
        int page = previewPage == null || previewPage < 1 ? 1 : previewPage;
        int pageSize = previewPageSize == null || previewPageSize < 1 ? 5 : Math.min(previewPageSize, 50);
        long offset = (long) (page - 1) * pageSize;
        long targetIndex = 0L;
        for (MergeSourceRecordBO source : sources) {
            if (!isPreviewTarget(source, occupied, scope)) {
                continue;
            }
            if (targetIndex++ < offset) {
                continue;
            }
            IngestSourceDO system = systems.get(source.getSourceSystemId());
            MergeDetectScopeRespVO.PreviewRecord record = new MergeDetectScopeRespVO.PreviewRecord();
            record.setSourceRecordId(source.getId());
            record.setSourceRecordKey(source.getSourceRecordKey());
            record.setSystemCode(system == null ? null : system.getCode());
            record.setSystemName(system == null ? null : system.getName());
            record.setReceiveTime(source.getReceiveTime());
            record.setModelData(source.getModelData());
            records.add(record);
            if (records.size() >= pageSize) {
                break;
            }
        }
        return records;
    }

    /** 检查待治理、占用、来源系统、日期和有效 JSON 对象。 */
    private boolean isPreviewTarget(MergeSourceRecordBO source, Set<Long> occupied,
            MergeDetectScopeBO scope) {
        if (!isTargetInScope(source, occupied, scope)) {
            return false;
        }
        try {
            return MergeJson.readStrict(source.getModelData()).isObject();
        } catch (ServiceException ex) {
            return false;
        }
    }

    /** 列出存在未占用待治理记录的有效来源系统，按名称排序。 */
    private List<MergeDetectScopeRespVO.Option> scopeOptions(List<MergeSourceRecordBO> sources, Set<Long> occupied,
            Map<Long, IngestSourceDO> systems) {
        Map<Long, MergeDetectScopeRespVO.Option> options = new LinkedHashMap<>();
        for (MergeSourceRecordBO source : sources) {
            if (!MergeCodes.GOVERNANCE_PENDING.equals(source.getGovernanceStatus())
                    || occupied.contains(source.getId()) || source.getSourceSystemId() == null
                    || options.containsKey(source.getSourceSystemId())) {
                continue;
            }
            IngestSourceDO system = systems.get(source.getSourceSystemId());
            if (system == null || !MergeCodes.YES.equals(system.getValidFlag())) {
                continue;
            }
            String name = system.getName() == null ? "" : system.getName().trim();
            if (name.isEmpty()) {
                continue;
            }
            MergeDetectScopeRespVO.Option option = new MergeDetectScopeRespVO.Option();
            option.setId(source.getSourceSystemId());
            option.setName(name);
            options.put(option.getId(), option);
        }
        List<MergeDetectScopeRespVO.Option> rows = new ArrayList<MergeDetectScopeRespVO.Option>(options.values());
        rows.sort(Comparator.comparing(MergeDetectScopeRespVO.Option::getName,
                Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    /** 批量加载来源系统名称，补充任务列表的范围说明。 */
    private void fillScopeLabels(List<MergeTaskRespVO> rows) {
        Set<Long> ids = new HashSet<>();
        for (MergeTaskRespVO row : rows) {
            ids.addAll(MergeDetectConfigUtils.readSystemIds(row.getSourceSystemIds()));
        }
        Map<Long, IngestSourceDO> systems = new HashMap<>();
        if (!ids.isEmpty()) {
            List<IngestSourceDO> loaded = ingestSourceMapper.selectList(new LambdaQueryWrapper<IngestSourceDO>()
                    .in(IngestSourceDO::getId, ids));
            for (IngestSourceDO system : loaded) {
                systems.put(system.getId(), system);
            }
        }
        for (MergeTaskRespVO row : rows) {
            row.setScopeLabel(buildScopeLabel(row, systems));
        }
    }

    /** 构造范围说明，失效系统回退显示编号。 */
    private String buildScopeLabel(MergeTaskRespVO row, Map<Long, IngestSourceDO> systems) {
        if (!MergeCodes.SCOPE_CUSTOM.equals(row.getScopeType())) {
            return "全部待治理来源数据";
        }
        List<String> names = new ArrayList<>();
        for (Long id : MergeDetectConfigUtils.readSystemIds(row.getSourceSystemIds())) {
            IngestSourceDO system = systems.get(id);
            String name = system == null ? null : system.getName();
            names.add(name == null || name.trim().isEmpty() ? String.valueOf(id) : name);
        }
        String text = names.isEmpty() ? "指定来源系统和接收时间" : String.join("、", names);
        String range = dateRangeText(row.getReceiveStartTime(), row.getReceiveEndTime());
        return range.isEmpty() ? text : text + " · " + range;
    }

    /** 将接收日期范围转换为列表展示文本。 */
    private String dateRangeText(Date start, Date end) {
        if (start == null && end == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if (start != null && end != null) {
            return format.format(start) + "~" + format.format(end);
        }
        if (start != null) {
            return format.format(start) + "起";
        }
        return "至" + format.format(end);
    }

    /** 范围条件仅筛选待治理目标；数据格式在快照或预览边界单独校验。 */
    private boolean isTargetInScope(MergeSourceRecordBO source, Set<Long> occupied,
            MergeDetectScopeBO scope) {
        if (!MergeCodes.GOVERNANCE_PENDING.equals(source.getGovernanceStatus())
                || occupied.contains(source.getId())) {
            return false;
        }
        if (!scope.isCustom()) {
            return true;
        }
        if (source.getSourceSystemId() == null || !scope.getSystemIds().contains(source.getSourceSystemId())) {
            return false;
        }
        return inReceiveRange(source.getReceiveTime(), scope);
    }
}
