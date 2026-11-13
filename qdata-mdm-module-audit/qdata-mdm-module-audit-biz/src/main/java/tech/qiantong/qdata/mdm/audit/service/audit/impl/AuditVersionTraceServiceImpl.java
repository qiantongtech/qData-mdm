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

package tech.qiantong.qdata.mdm.audit.service.audit.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelColumnDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditMasterChangeDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestItemDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperatorDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.MasterDataVersionSnapshotDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.MasterDataVersionSnapshotMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditOperatorMapper;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import tech.qiantong.qdata.mdm.audit.service.audit.IAuditVersionTraceService;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditVersionTraceRespVO;

@Service
public class AuditVersionTraceServiceImpl extends AuditTraceReadService implements IAuditVersionTraceService {
    @Resource private MasterDataVersionSnapshotMapper masterDataVersionSnapshotMapper;
    @Resource private AuditChangeTraceServiceImpl auditChangeTraceService;
    @Resource private AuditOperatorMapper auditOperatorMapper;

    @Override
    public AuditTracePageRespVO<AuditVersionTraceRespVO> versions(Map<String, String> filters) {
        if (canPageInDatabase(filters)) {
            int pageNum = bounded(filters.get("pageNum"), 1, 1, Integer.MAX_VALUE);
            int pageSize = bounded(filters.get("pageSize"), 10, 1, 200);
            String order = "id".equals(filters.get("orderByColumn")) ? filters.get("isAsc") : null;
            Page<MasterDataVersionSnapshotDO> result =
                    masterDataVersionSnapshotMapper.selectTracePage(new Page<>(pageNum, pageSize), order);
            return new AuditTracePageRespVO<>(pageRows(result.getRecords()), result.getTotal());
        }
        List<MasterDataVersionSnapshotDO> versions = masterDataVersionSnapshotMapper.selectTraceVersions();
        List<AuditVersionTraceRespVO> result = pageRows(versions);
        Comparator<AuditVersionTraceRespVO> versionOrder = numericSortValue(filters, "id", AuditVersionTraceRespVO::getId);
        List<AuditVersionTraceRespVO> filtered = result.stream().filter(row -> matchesValues(filters,
                "model", row.getModel(), "name", row.getName(), "code", row.getCode(), "version", row.getVersion(), "state", row.getState())
                && inTime(row.getTime(), filters))
                .sorted(versionOrder == null ? descendingValue(AuditVersionTraceRespVO::getTime) : versionOrder).collect(Collectors.toList());
        return page(filtered, filters);
    }

    private boolean canPageInDatabase(Map<String, String> filters) {
        for (String key : new String[]{"model", "name", "code", "version", "state", "beginTime", "endTime"}) {
            if (hasText(filters.get(key))) return false;
        }
        return true;
    }

    /** 只为当前页读取模型字段和生命周期；详情仍沿用完整快照读取。 */
    private List<AuditVersionTraceRespVO> pageRows(List<MasterDataVersionSnapshotDO> versions) {
        if (versions.isEmpty()) return Collections.emptyList();
        Map<Long, String> names = modelNames(versions);
        List<Long> modelIds = versions.stream().map(MasterDataVersionSnapshotDO::getModelId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, List<AuditModelColumnDO>> columns = new HashMap<>();
        if (!modelIds.isEmpty()) {
            for (AuditModelColumnDO column : auditModelColumnMapper.selectForModels(modelIds)) {
                columns.computeIfAbsent(column.getModelId(), ignored -> new ArrayList<>()).add(column);
            }
        }
        Map<Long, Map<Long, String>> states = pageStates(versions);
        List<Map<String, Object>> schemaRows = new ArrayList<>();
        for (MasterDataVersionSnapshotDO version : versions) {
            Map<String, Object> row = new HashMap<>();
            row.put("MODEL_ID", version.getModelId());
            row.put("MODEL_VERSION_ID", version.getModelVersionId());
            schemaRows.add(row);
        }
        Map<Long, List<Map<String, Object>>> publishedColumns = auditChangeTraceService.publishedModelColumns(schemaRows);
        Map<Long, AuditMasterChangeDO> changes = versionChanges(versions);
        Map<Long, String> phones = operatorPhones(changes);
        List<AuditVersionTraceRespVO> result = new ArrayList<>();
        for (MasterDataVersionSnapshotDO raw : versions) {
            Map<String, Object> snapshot = json(raw.getSnapshotData());
            List<AuditModelColumnDO> modelColumns = columns.getOrDefault(raw.getModelId(), Collections.emptyList());
            Map<String, String> labels = new HashMap<>();
            for (AuditModelColumnDO column : modelColumns) {
                String code = str(column.getCode());
                labels.put(code, str(column.getName()));
            }
            Map<String, Object> identity = auditChangeTraceService.changeIdentity(publishedColumns.get(raw.getModelVersionId()), snapshot);
            List<Map<String, Object>> fields = new ArrayList<>();
            for (Map.Entry<String, Object> field : snapshot.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                put(item, "code", field.getKey(), "field", labels.getOrDefault(field.getKey(), field.getKey()), "value", display(field.getValue()));
                fields.add(item);
            }
            AuditVersionTraceRespVO row = new AuditVersionTraceRespVO();
            row.setId(raw.getId());
            row.setModelId(raw.getModelId());
            row.setMasterRecordId(raw.getMasterRecordId());
            row.setModel(names.get(raw.getModelId()));
            row.setName((String) identity.get("name"));
            row.setBusinessCode((String) identity.get("businessCode"));
            AuditMasterChangeDO change = changes.get(raw.getChangeId());
            if (change != null && Objects.equals(raw.getModelId(), change.getModelId())
                    && Objects.equals(raw.getMasterRecordId(), change.getMasterRecordId())) {
                row.setOperatorName(change.getOperatorName());
                row.setOperatorPhone(phones.get(change.getOperatorId()));
            }
            row.setCode(str(raw.getMasterRecordId()));
            row.setVersion(raw.getVersionNo());
            row.setTime(date(raw.getGenerateTime()));
            row.setReason(type(str(raw.getGenerateType())));
            row.setCurrent("1".equals(str(raw.getCurrentFlag())));
            row.setState(states.getOrDefault(raw.getModelId(), Collections.emptyMap()).getOrDefault(raw.getMasterRecordId(), "—"));
            row.setSnapshot(fields);
            result.add(row);
        }
        return result;
    }

    private Map<Long, AuditMasterChangeDO> versionChanges(List<MasterDataVersionSnapshotDO> versions) {
        List<Long> ids = versions.stream().map(MasterDataVersionSnapshotDO::getChangeId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, AuditMasterChangeDO> changes = new HashMap<>();
        for (int start = 0; start < ids.size(); start += 500) {
            for (AuditMasterChangeDO change : auditMasterChangeMapper.selectOperationRelations(
                    ids.subList(start, Math.min(start + 500, ids.size())))) changes.put(change.getId(), change);
        }
        return changes;
    }

    private Map<Long, String> operatorPhones(Map<Long, AuditMasterChangeDO> changes) {
        List<Long> ids = changes.values().stream().map(AuditMasterChangeDO::getOperatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> phones = new HashMap<>();
        for (int start = 0; start < ids.size(); start += 500) {
            for (AuditOperatorDO user : auditOperatorMapper.selectCurrentPhones(
                    ids.subList(start, Math.min(start + 500, ids.size())))) phones.put(user.getUserId(), user.getPhoneNumber());
        }
        return phones;
    }

    private Map<Long, Map<Long, String>> pageStates(List<MasterDataVersionSnapshotDO> versions) {
        Map<Long, List<Long>> byModel = versions.stream()
                .filter(row -> row.getModelId() != null && row.getMasterRecordId() != null)
                .collect(Collectors.groupingBy(MasterDataVersionSnapshotDO::getModelId,
                        Collectors.mapping(MasterDataVersionSnapshotDO::getMasterRecordId, Collectors.toList())));
        Map<Long, Map<Long, String>> result = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : byModel.entrySet()) {
            Long modelId = entry.getKey();
            List<Long> ids = entry.getValue().stream().distinct().collect(Collectors.toList());
            Map<Long, AuditMasterChangeDO> changes = new HashMap<>();
            for (AuditMasterChangeDO change : auditMasterChangeMapper.selectEffectiveStatesForRecords(modelId, ids)) {
                changes.putIfAbsent(change.getMasterRecordId(), change);
            }
            Map<Long, AuditChangeRequestItemDO> lifecycle = new HashMap<>();
            for (AuditChangeRequestItemDO item : auditChangeRequestItemMapper.selectEffectiveStatesForRecords(modelId, ids)) {
                if (ids.contains(item.getMasterRecordId())) lifecycle.putIfAbsent(item.getMasterRecordId(), item);
                if (ids.contains(item.getResultMasterRecordId())) lifecycle.putIfAbsent(item.getResultMasterRecordId(), item);
            }
            Map<Long, String> values = new HashMap<>();
            for (Long id : ids) {
                AuditMasterChangeDO change = changes.get(id);
                AuditChangeRequestItemDO item = lifecycle.get(id);
                if (change != null && (item == null || date(change.getChangeTime()).compareTo(date(item.getExecuteTime())) >= 0))
                    values.put(id, lifecycleStatus(change.getAfterStatus()));
                else if (item != null) values.put(id, lifecycleStatus(item.getTargetStatus()));
            }
            result.put(modelId, values);
        }
        return result;
    }

    @Override
    public AuditVersionTraceRespVO version(long id) {
        MasterDataVersionSnapshotDO row = masterDataVersionSnapshotMapper.selectById(id);
        if (row == null) return null;
        return pageRows(Collections.singletonList(row)).get(0);
    }

    @Override
    public List<AuditVersionTraceRespVO> objectVersions(long modelId, long masterRecordId) {
        return pageRows(masterDataVersionSnapshotMapper.selectObjectVersions(modelId, masterRecordId));
    }

    private Map<Long, String> modelNames(List<MasterDataVersionSnapshotDO> rows) {
        List<Long> ids = rows.stream().map(MasterDataVersionSnapshotDO::getModelId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        return modelNamesByIds(ids);
    }

}
