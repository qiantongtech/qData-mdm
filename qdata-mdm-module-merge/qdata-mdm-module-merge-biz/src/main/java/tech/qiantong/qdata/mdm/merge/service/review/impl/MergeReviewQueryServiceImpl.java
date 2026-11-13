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
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskMapper;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewLogRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewMatchRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewMemberRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewPageReqVO;
import tech.qiantong.qdata.mdm.merge.convert.review.MergeReviewConvert;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateMatchDetailDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateReviewLogDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateGroupMemberMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeCandidateMatchDetailMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskInstanceMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskMapper;
import tech.qiantong.qdata.mdm.merge.dal.mapper.review.MergeCandidateReviewLogMapper;
import tech.qiantong.qdata.mdm.merge.domain.review.MergeReviewSummaryBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.merge.enums.MergeReviewQueryEnum;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewQueryService;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewSourceService;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleMatch.RuleMatchMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * 匹配审核查询服务，负责列表摘要、详情及操作记录展示。
 *
 * @author chentangwen
 */
@Service
public class MergeReviewQueryServiceImpl implements IMergeReviewQueryService {

    @Resource
    private MergeCandidateGroupMapper mergeCandidateGroupMapper;

    @Resource
    private MergeCandidateGroupMemberMapper mergeCandidateGroupMemberMapper;

    @Resource
    private MergeCandidateMatchDetailMapper mergeCandidateMatchDetailMapper;

    @Resource
    private MergeCandidateReviewLogMapper mergeCandidateReviewLogMapper;

    @Resource
    private MergeDuplicateTaskInstanceMapper mergeDuplicateTaskInstanceMapper;

    @Resource
    private MergeDuplicateTaskMapper mergeDuplicateTaskMapper;

    @Resource
    private IMergeSourceTableService sourceTableService;


    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private IngestTaskMapper ingestTaskMapper;

    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private IModelBaseService modelBaseService;

    @Resource
    private IMergeReviewSourceService sourceService;

    @Resource
    private RuleMatchMapper ruleMatchMapper;

    /** 解析审核查询范围后执行分页，范围外状态返回空结果。 */
    @Override
    public PageResult<MergeCandidateGroupDO> page(MergeReviewPageReqVO reqVO) {
        String status = reqVO.getStatus();
        List<String> statuses;
        if (status == null || status.trim().isEmpty()) {
            statuses = MergeReviewQueryEnum.ALL.getStatuses();
        } else {
            MergeReviewQueryEnum query = MergeReviewQueryEnum.getByValue(status);
            if (query != null) {
                statuses = query.getStatuses();
            } else if (MergeReviewQueryEnum.ALL.getStatuses().contains(status)) {
                statuses = Collections.singletonList(status);
            } else {
                statuses = Collections.emptyList();
            }
        }
        if (statuses.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        return mergeCandidateGroupMapper.selectReviewPage(reqVO, statuses);
    }

    /** 批量加载本页数据后补充摘要，缺失历史数据保持原有展示回退。 */
    @Override
    public void fillSummary(List<MergeGroupRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        fillModelVersions(rows);
        fillTaskInfo(rows);
        MergeReviewSummaryBO context = collectSummaryMembers(rows);
        if (context == null) {
            return;
        }
        loadSummarySources(context);
        loadSummaryColumns(context);
        loadSummaryMatches(context);
        fillSummaryRows(rows, context);
    }

    /** 版本标签与模型定义一致：优先正式发布版本号，样式取当前版本状态。 */
    private void fillModelVersions(List<MergeGroupRespVO> rows) {
        Set<Long> modelIds = new HashSet<>();
        for (MergeGroupRespVO row : rows) {
            if (row.getModelId() != null) modelIds.add(row.getModelId());
        }
        if (modelIds.isEmpty()) return;
        Map<Long, ModelBaseDO> modelsById = new HashMap<>();
        for (ModelBaseDO model : modelBaseService.getModelBaseListByIds(modelIds)) {
            modelsById.put(model.getId(), model);
        }
        for (MergeGroupRespVO row : rows) {
            ModelBaseDO model = modelsById.get(row.getModelId());
            if (model == null) continue;
            ModelVersionDO version = model.getVersion();
            ModelVersionDO released = model.getReleaseVersion();
            String releasedNo = released == null ? null : released.getVersionNo();
            row.setModelVersionNo(releasedNo == null || releasedNo.isEmpty()
                    ? (version == null ? null : version.getVersionNo()) : releasedNo);
            row.setModelVersionStatus(version == null ? null : version.getStatus());
        }
    }

    /** 批量回填任务名称、描述和检测范围。 */
    private void fillTaskInfo(List<MergeGroupRespVO> rows) {
        List<Long> instanceIds = new ArrayList<>();
        for (MergeGroupRespVO row : rows) {
            if (row.getTaskInstanceId() != null) {
                instanceIds.add(row.getTaskInstanceId());
            }
        }
        if (instanceIds.isEmpty()) {
            return;
        }
        List<MergeDuplicateTaskInstanceDO> instances = mergeDuplicateTaskInstanceMapper.selectList(
                new LambdaQueryWrapper<MergeDuplicateTaskInstanceDO>()
                        .select(MergeDuplicateTaskInstanceDO::getId, MergeDuplicateTaskInstanceDO::getTaskId,
                                MergeDuplicateTaskInstanceDO::getModelVersionId,
                                MergeDuplicateTaskInstanceDO::getTaskConfigSnapshot)
                        .in(MergeDuplicateTaskInstanceDO::getId, instanceIds));
        Map<Long, MergeDuplicateTaskInstanceDO> instancesById = new HashMap<>();
        Set<Long> taskIds = new HashSet<>();
        for (MergeDuplicateTaskInstanceDO instance : instances) {
            instancesById.put(instance.getId(), instance);
            if (instance.getTaskId() != null) {
                taskIds.add(instance.getTaskId());
            }
        }
        Map<Long, MergeDuplicateTaskDO> tasksById = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<MergeDuplicateTaskDO> tasks = mergeDuplicateTaskMapper.selectList(
                    new LambdaQueryWrapper<MergeDuplicateTaskDO>()
                            .select(MergeDuplicateTaskDO::getId, MergeDuplicateTaskDO::getTaskName,
                                    MergeDuplicateTaskDO::getRemark, MergeDuplicateTaskDO::getScopeType)
                            .in(MergeDuplicateTaskDO::getId, taskIds));
            for (MergeDuplicateTaskDO task : tasks) {
                tasksById.put(task.getId(), task);
            }
        }
        for (MergeGroupRespVO row : rows) {
            MergeDuplicateTaskInstanceDO instance = instancesById.get(row.getTaskInstanceId());
            if (instance == null) {
                continue;
            }
            String scopeType = MergeJson.read(instance.getTaskConfigSnapshot()).path("scopeType").asText("");
            row.setScopeType(scopeType.isEmpty() ? null : scopeType);
            MergeDuplicateTaskDO task = tasksById.get(instance.getTaskId());
            if (task != null) {
                row.setTaskName(task.getTaskName());
                row.setRemark(task.getRemark());
                if (row.getScopeType() == null) {
                    row.setScopeType(task.getScopeType());
                }
            }
        }
    }

    /** 从已加载的数据组装名称、命中字段、数量和来源系统，不逐组查询数据库。 */
    private void fillSummaryRows(List<MergeGroupRespVO> rows, MergeReviewSummaryBO context) {
        Map<Long, List<Long>> sourceIds = context.getSourceIds();
        Map<Long, List<ModelColumnDO>> columnsByModel = context.getColumnsByModel();
        Map<Long, Set<Long>> ruleIds = context.getRuleIds();
        Map<Long, Long> firstSourceRowIds = context.getFirstSourceRowIds();
        Map<Long, LinkedHashSet<String>> hitCodes = context.getHitCodes();
        for (MergeGroupRespVO row : rows) {
            List<Long> ids = sourceIds.get(row.getId());
            List<ModelColumnDO> columns = columnsByModel.get(row.getModelId());
            row.setMemberCount(ids == null ? 0 : ids.size());
            Set<Long> rules = ruleIds.get(row.getId());
            row.setRuleCount(rules == null ? 0 : rules.size());
            List<String> ruleNames = new ArrayList<>();
            if (rules != null) {
                for (Long ruleId : rules) {
                    ruleNames.add(context.getRuleNames().getOrDefault(ruleId, "规则 " + ruleId));
                }
            }
            row.setRuleNames(ruleNames);
            Long sourceRowId = firstSourceRowIds.get(row.getId());
            Map<Long, MergeSourceRecordBO> sources = context.getSourcesByModel()
                    .getOrDefault(row.getModelId(), Collections.emptyMap());
            MergeSourceRecordBO firstSource = sources.get(sourceRowId);
            row.setPrimaryObject(primaryObject(firstSource == null ? null : firstSource.getModelData(),
                    columns, row.getMemberCount()));
            row.setHitFields(hitFieldText(hitCodes.get(row.getId()), columns));
            String sourceSystems = sourceSystemText(ids, sources, context);
            if (sourceSystems != null) {
                row.setSourceSystems(sourceSystems);
            }
        }
    }

    /** 按成员顺序去重来源系统英文编码；编码缺失时不拼入中文名称。 */
    private String sourceSystemText(List<Long> sourceIds, Map<Long, MergeSourceRecordBO> sources,
            MergeReviewSummaryBO context) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> systemCodes = new LinkedHashSet<>();
        for (Long sourceId : sourceIds) {
            MergeSourceRecordBO source = sources.get(sourceId);
            Long systemId = source == null ? null : source.getSourceSystemId();
            IngestSourceDO system = context.getSystems().get(systemId);
            if (system == null) {
                continue;
            }
            if (system.getCode() != null && !system.getCode().trim().isEmpty()) {
                systemCodes.add(system.getCode().trim());
            }
        }
        return systemCodes.isEmpty() ? null : String.join(" / ", systemCodes);
    }

    /** 批量收集本页组、模型和目标成员编号，保留固定版本及成员顺序。 */
    private MergeReviewSummaryBO collectSummaryMembers(List<MergeGroupRespVO> rows) {
        List<Long> groupIds = new ArrayList<>();
        Set<Long> modelIds = new HashSet<>();
        for (MergeGroupRespVO row : rows) {
            if (row.getId() != null) {
                groupIds.add(row.getId());
            }
            if (row.getModelId() != null) {
                modelIds.add(row.getModelId());
            }
        }
        if (groupIds.isEmpty()) {
            return null;
        }
        List<MergeCandidateGroupMemberDO> members =
                mergeCandidateGroupMemberMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                        .in(MergeCandidateGroupMemberDO::getGroupId, groupIds)
                        .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES)
                        .eq(MergeCandidateGroupMemberDO::getMemberRole, MergeCodes.MEMBER_ROLE_TARGET)
                        .orderByAsc(MergeCandidateGroupMemberDO::getId));
        Map<Long, List<Long>> sourceIds = new HashMap<>();
        Map<Long, Long> firstSourceRowIds = new HashMap<>();
        Set<Long> allSourceIds = new HashSet<>();
        for (MergeCandidateGroupMemberDO member : members) {
            List<Long> ids = sourceIds.get(member.getGroupId());
            if (ids == null) {
                ids = new ArrayList<>();
                sourceIds.put(member.getGroupId(), ids);
            }
            if (member.getSourceRecordId() != null) {
                ids.add(member.getSourceRecordId());
                allSourceIds.add(member.getSourceRecordId());
            }
            if (member.getSourceVersionId() != null) {
                if (!firstSourceRowIds.containsKey(member.getGroupId())) {
                    firstSourceRowIds.put(member.getGroupId(), member.getSourceVersionId());
                }
            }
        }

        MergeReviewSummaryBO context = new MergeReviewSummaryBO();
        context.setGroupIds(groupIds);
        context.setModelIds(modelIds);
        context.setMembers(members);
        context.setSourceIds(sourceIds);
        context.setFirstSourceRowIds(firstSourceRowIds);
        context.setAllSourceIds(allSourceIds);
        return context;
    }

    /** 一次加载来源表行、接入任务和系统，记录缺失来源系统时的回退关系。 */
    private void loadSummarySources(MergeReviewSummaryBO context) {
        Set<Long> allSourceIds = context.getAllSourceIds();
        Map<Long, Map<Long, MergeSourceRecordBO>> sourcesByModel = new HashMap<>();
        List<MergeSourceRecordBO> sourceRows = new ArrayList<>();
        Set<Long> systemIds = new HashSet<>();
        if (!allSourceIds.isEmpty()) {
            for (Long modelId : context.getModelIds()) {
                Map<Long, MergeSourceRecordBO> sources = new HashMap<>();
                for (MergeSourceRecordBO source : sourceTableService.load(modelId, allSourceIds)) {
                    sources.put(source.getId(), source);
                    sourceRows.add(source);
                    if (source.getSourceSystemId() != null) {
                        systemIds.add(source.getSourceSystemId());
                    }
                }
                sourcesByModel.put(modelId, sources);
            }
        }
        Set<Long> taskIds = new HashSet<>();
        for (MergeSourceRecordBO source : sourceRows) {
            if (source.getTaskId() != null) {
                taskIds.add(source.getTaskId());
            }
        }
        Map<Long, IngestTaskDO> tasks = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<IngestTaskDO> taskRows = ingestTaskMapper.selectByIdsForSourceData(taskIds);
            for (IngestTaskDO task : taskRows) {
                tasks.put(task.getId(), task);
                if (task.getSourceId() != null) {
                    systemIds.add(task.getSourceId());
                }
            }
        }
        Map<Long, IngestSourceDO> systems = new HashMap<>();
        if (!systemIds.isEmpty()) {
            List<IngestSourceDO> systemRows = ingestSourceMapper.selectList(new LambdaQueryWrapper<IngestSourceDO>()
                    .in(IngestSourceDO::getId, systemIds));
            for (IngestSourceDO system : systemRows) {
                systems.put(system.getId(), system);
            }
        }

        for (MergeSourceRecordBO source : sourceRows) {
            IngestTaskDO task = tasks.get(source.getTaskId());
            if (source.getSourceSystemId() == null && task != null) {
                source.setSourceSystemId(task.getSourceId());
            }
        }
        context.setSourcesByModel(sourcesByModel);
        context.setSystems(systems);
    }

    /** 按本页模型批量查询字段，供名称摘要和命中字段展示复用。 */
    private void loadSummaryColumns(MergeReviewSummaryBO context) {
        Set<Long> modelIds = context.getModelIds();
        Map<Long, List<ModelColumnDO>> columnsByModel = new HashMap<>();
        if (!modelIds.isEmpty()) {
            List<ModelColumnDO> columnRows = modelColumnMapper.selectList(new LambdaQueryWrapper<ModelColumnDO>()
                    .in(ModelColumnDO::getModelId, modelIds));
            for (ModelColumnDO column : columnRows) {
                List<ModelColumnDO> list = columnsByModel.get(column.getModelId());
                if (list == null) {
                    list = new ArrayList<>();
                    columnsByModel.put(column.getModelId(), list);
                }
                list.add(column);
            }
        }

        context.setColumnsByModel(columnsByModel);
    }

    /** 批量加载命中规则与字段，字段顺序沿用首次出现顺序。 */
    private void loadSummaryMatches(MergeReviewSummaryBO context) {
        List<Long> groupIds = context.getGroupIds();
        List<MergeCandidateMatchDetailDO> matches =
                mergeCandidateMatchDetailMapper.selectList(new LambdaQueryWrapper<MergeCandidateMatchDetailDO>()
                        .select(MergeCandidateMatchDetailDO::getGroupId, MergeCandidateMatchDetailDO::getMatchRuleId,
                                MergeCandidateMatchDetailDO::getMatchFields)
                        .in(MergeCandidateMatchDetailDO::getGroupId, groupIds)
                        .eq(MergeCandidateMatchDetailDO::getValidFlag, MergeCodes.YES)
                        .orderByAsc(MergeCandidateMatchDetailDO::getId));
        Map<Long, Set<Long>> ruleIds = new HashMap<>();
        Set<Long> allRuleIds = new HashSet<>();
        Map<Long, LinkedHashSet<String>> hitCodes = new HashMap<>();
        for (MergeCandidateMatchDetailDO match : matches) {
            Set<Long> ids = ruleIds.get(match.getGroupId());
            if (ids == null) {
                ids = new LinkedHashSet<>();
                ruleIds.put(match.getGroupId(), ids);
            }
            if (match.getMatchRuleId() != null) {
                ids.add(match.getMatchRuleId());
                allRuleIds.add(match.getMatchRuleId());
            }
            LinkedHashSet<String> codes = hitCodes.get(match.getGroupId());
            if (codes == null) {
                codes = new LinkedHashSet<>();
                hitCodes.put(match.getGroupId(), codes);
            }
            JsonNode fields = MergeJson.read(match.getMatchFields());
            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    String code = field.asText("").trim();
                    if (!code.isEmpty()) {
                        codes.add(code);
                    }
                }
            }
        }

        context.setRuleIds(ruleIds);
        if (!allRuleIds.isEmpty()) {
            for (RuleMatchDO rule : ruleMatchMapper.selectBatchIds(allRuleIds)) {
                if (rule.getRuleName() != null && !rule.getRuleName().trim().isEmpty()) {
                    context.getRuleNames().put(rule.getId(), rule.getRuleName().trim());
                }
            }
        }
        context.setHitCodes(hitCodes);
    }

    /** 组合对象名称和目标成员数量，名称不存在时不生成摘要。 */
    private String primaryObject(String modelData, List<ModelColumnDO> columns, Integer memberCount) {
        String name = namedValue(modelData, columns);
        if (name == null) {
            return null;
        }
        int count = memberCount == null ? 0 : memberCount;
        return count > 1 ? name + " 等 " + count + " 条" : name;
    }

    /** 名称字段优先使用名称标记，兼容历史模型中包含“名称”的字段。 */
    private String namedValue(String modelData, List<ModelColumnDO> columns) {
        JsonNode root = MergeJson.read(modelData);
        if (!root.isObject() || columns == null) {
            return null;
        }
        ModelColumnDO nameColumn = null;
        for (ModelColumnDO column : columns) {
            if (Boolean.TRUE.equals(column.getNameFlag())) {
                nameColumn = column;
                break;
            }
        }
        if (nameColumn == null) {
            for (ModelColumnDO column : columns) {
                if (column.getName() != null && column.getName().contains("名称")) {
                    nameColumn = column;
                    break;
                }
            }
        }
        return columnValue(root, nameColumn);
    }

    /** 依次按字段编码、字段编号和字段名称兼容读取历史内容。 */
    private String columnValue(JsonNode root, ModelColumnDO column) {
        if (column == null) {
            return null;
        }
        String value = jsonText(root, column.getCode());
        if (value == null && column.getId() != null) {
            value = jsonText(root, String.valueOf(column.getId()));
        }
        if (value == null) {
            value = jsonText(root, column.getName());
        }
        return value;
    }

    /** 读取非空文本值，展示层保留历史 JSON 的容错方式。 */
    private String jsonText(JsonNode root, String key) {
        if (key == null || key.isEmpty() || !root.has(key) || root.get(key).isNull()) {
            return null;
        }
        String value = root.get(key).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    /** 将命中字段编码转换为模型字段名称，并保持首次出现的顺序。 */
    private String hitFieldText(Set<String> codes, List<ModelColumnDO> columns) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (String code : codes) {
            String name = code;
            if (columns != null) {
                for (ModelColumnDO column : columns) {
                    if (code.equals(column.getCode()) || (column.getId() != null
                            && code.equals(String.valueOf(column.getId())))) {
                        if (column.getName() != null && !column.getName().trim().isEmpty()) {
                            name = column.getName().trim();
                        }
                        break;
                    }
                }
            }
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names.isEmpty() ? null : String.join("、", names);
    }

    /** 组装审核详情，成员数据使用候选组保存的固定来源版本。 */
    @Override
    public MergeReviewDetailRespVO detail(Long groupId) {
        MergeCandidateGroupDO group = mergeCandidateGroupMapper.selectById(groupId);
        if (group == null) {
            throw new ServiceException("候选组不存在或已失效");
        }
        List<MergeCandidateGroupMemberDO> members = members(groupId);
        Map<Long, SourceSnapshot> snapshots = sourceService.loadContext(group.getModelId(), members).getSnapshots();
        MergeReviewDetailRespVO detail = MergeReviewConvert.toDetail(group);
        MergeGroupRespVO taskInfo = new MergeGroupRespVO();
        taskInfo.setId(group.getId());
        taskInfo.setModelId(group.getModelId());
        taskInfo.setTaskInstanceId(group.getTaskInstanceId());
        fillSummary(Collections.singletonList(taskInfo));
        detail.setTaskName(taskInfo.getTaskName());
        detail.setRemark(taskInfo.getRemark());
        detail.setSourceCount(taskInfo.getMemberCount());
        detail.setRuleCount(taskInfo.getRuleCount());
        detail.setTransferTime(group.getTransferTime());
        detail.setScopeType(taskInfo.getScopeType());
        detail.setModelVersionNo(taskInfo.getModelVersionNo());
        detail.setModelVersionStatus(taskInfo.getModelVersionStatus());
        List<MergeReviewMemberRespVO> memberViews = new ArrayList<>();
        for (MergeCandidateGroupMemberDO member : members) {
            SourceSnapshot snapshot = snapshots.get(member.getId());
            MergeReviewMemberRespVO view = MergeReviewConvert.toMember(member, snapshot);
            memberViews.add(view);
        }
        detail.setMembers(memberViews);
        List<MergeCandidateMatchDetailDO> matches =
                mergeCandidateMatchDetailMapper.selectList(new LambdaQueryWrapper<MergeCandidateMatchDetailDO>()
                        .eq(MergeCandidateMatchDetailDO::getGroupId, groupId)
                        .eq(MergeCandidateMatchDetailDO::getValidFlag, MergeCodes.YES)
                        .orderByAsc(MergeCandidateMatchDetailDO::getId));
        List<MergeReviewMatchRespVO> matchViews = new ArrayList<>();
        for (MergeCandidateMatchDetailDO match : matches) {
            MergeReviewMatchRespVO view = MergeReviewConvert.toMatch(match);
            matchViews.add(view);
        }
        detail.setMatches(matchViews);
        List<MergeCandidateReviewLogDO> logs = mergeCandidateReviewLogMapper.selectList(
                new LambdaQueryWrapper<MergeCandidateReviewLogDO>()
                        .eq(MergeCandidateReviewLogDO::getGroupId, groupId)
                        .eq(MergeCandidateReviewLogDO::getValidFlag, MergeCodes.YES)
                        .orderByAsc(MergeCandidateReviewLogDO::getReviewTime)
                        .orderByAsc(MergeCandidateReviewLogDO::getId));
        List<MergeReviewLogRespVO> logViews = new ArrayList<>();
        for (MergeCandidateReviewLogDO log : logs) {
            MergeReviewLogRespVO view = MergeReviewConvert.toLog(log);
            logViews.add(view);
        }
        detail.setLogs(logViews);
        return detail;
    }

    /** 按编号顺序读取有效成员，包含目标和历史参考记录。 */
    private List<MergeCandidateGroupMemberDO> members(Long groupId) {
        return mergeCandidateGroupMemberMapper.selectList(new LambdaQueryWrapper<MergeCandidateGroupMemberDO>()
                .eq(MergeCandidateGroupMemberDO::getGroupId, groupId)
                .eq(MergeCandidateGroupMemberDO::getValidFlag, MergeCodes.YES)
                .orderByAsc(MergeCandidateGroupMemberDO::getId));
    }
}
