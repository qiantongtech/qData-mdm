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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationTraceRespVO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestItemDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditMasterChangeDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelInstanceDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelLayerDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelProjectDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelVersionDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.MasterDataVersionSnapshotDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelInstanceMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelLayerMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelProjectMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelVersionMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.MasterDataVersionSnapshotMapper;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

/**
 * 操作审计关联信息的校验与批量补全，不生成业务日志或猜测对象关联。
 * @author zyy
 */
@Service
public class AuditOperationRelationService extends AuditTraceReadService {
    @Resource private AuditModelVersionMapper auditModelVersionMapper;
    @Resource private AuditModelProjectMapper auditModelProjectMapper;
    @Resource private AuditModelLayerMapper auditModelLayerMapper;
    @Resource private AuditModelInstanceMapper auditModelInstanceMapper;
    @Resource private MasterDataVersionSnapshotMapper masterDataVersionSnapshotMapper;

    /** 保存前校验关联，变更记录可提供模型、记录和模型版本。 */
    public void validate(AuditOperationLogDO log) {
        AuditMasterChangeDO change = log.getChangeId() == null ? null : auditMasterChangeMapper.selectById(log.getChangeId());
        if (log.getChangeId() != null && change == null) throw new ServiceException("关联的变更记录不存在");
        if (change != null) {
            requireCompatible(log.getModelId(), change.getModelId(), "模型");
            requireCompatible(log.getMasterRecordId(), change.getMasterRecordId(), "主数据");
            requireCompatible(log.getModelVersionId(), change.getModelVersionId(), "模型版本");
            if (log.getModelId() == null) log.setModelId(change.getModelId());
            if (log.getMasterRecordId() == null) log.setMasterRecordId(change.getMasterRecordId());
            if (log.getModelVersionId() == null) log.setModelVersionId(change.getModelVersionId());
        }
        if (log.getModelVersionId() != null) {
            AuditModelVersionDO version = auditModelVersionMapper.selectById(log.getModelVersionId());
            if (version == null) throw new ServiceException("关联的模型版本不存在");
            requireCompatible(log.getModelId(), version.getModelId(), "模型版本所属模型");
            if (log.getModelId() == null) log.setModelId(version.getModelId());
        }
        if (log.getMasterRecordId() != null && log.getModelId() == null) {
            throw new ServiceException("指定主数据记录时必须提供模型ID");
        }
        if (log.getModelId() != null && auditModelNameMapper.selectById(log.getModelId()) == null) {
            throw new ServiceException("关联的模型不存在");
        }
    }

    private void requireCompatible(Long requested, Long actual, String label) {
        if (requested != null && !Objects.equals(requested, actual)) {
            throw new ServiceException(label + "关联不一致");
        }
    }

    /** 仅补全传入的当前页或单条详情。 */
    public void enrich(List<AuditOperationTraceRespVO> rows) {
        if (rows.isEmpty()) return;
        Map<Long, AuditMasterChangeDO> changes = index(ids(rows, AuditOperationTraceRespVO::getChangeId),
                auditMasterChangeMapper::selectOperationRelations, AuditMasterChangeDO::getId);
        for (AuditOperationTraceRespVO row : rows) {
            AuditMasterChangeDO change = changes.get(row.getChangeId());
            if (change == null || !compatible(row, change)) continue;
            if (row.getModelId() == null) row.setModelId(change.getModelId());
            if (row.getModelVersionId() == null) row.setModelVersionId(change.getModelVersionId());
            if (row.getMasterRecordId() == null) row.setMasterRecordId(change.getMasterRecordId());
        }
        enrichModelInfo(rows);
        Map<Long, Map<String, Object>> schemas = loadSchemas(ids(rows, AuditOperationTraceRespVO::getModelVersionId));
        List<Long> dataVersionIds = new ArrayList<>();
        for (AuditMasterChangeDO change : changes.values()) {
            Long id = "1".equals(change.getProcessResult()) ? change.getAfterVersionId() : change.getBeforeVersionId();
            if (id != null) dataVersionIds.add(id);
        }
        Map<Long, MasterDataVersionSnapshotDO> dataVersions = index(dataVersionIds,
                masterDataVersionSnapshotMapper::selectOperationRelations, MasterDataVersionSnapshotDO::getId);
        Map<String, String> currentStates = loadCurrentStates(rows);
        for (AuditOperationTraceRespVO row : rows) {
            if (row.getMasterRecordId() != null && row.getModelId() != null) {
                row.setMasterLifecycle(currentStates.get(recordKey(row.getModelId(), row.getMasterRecordId())));
            }
            AuditMasterChangeDO change = changes.get(row.getChangeId());
            if (change == null || !compatible(row, change)) continue;
            enrichMaster(row, change, schemas.get(row.getModelVersionId()), dataVersions);
        }
    }

    /** 审计页面共用的模型、指定版本和当前归属查询，不读取主数据或生命周期。 */
    public void enrichModelInfo(List<AuditOperationTraceRespVO> rows) {
        if (rows.isEmpty()) return;
        Map<Long, AuditModelVersionDO> versions = index(ids(rows, AuditOperationTraceRespVO::getModelVersionId),
                auditModelVersionMapper::selectOperationRelations, AuditModelVersionDO::getId);
        for (AuditOperationTraceRespVO row : rows) {
            AuditModelVersionDO version = versions.get(row.getModelVersionId());
            if (row.getModelId() == null && version != null) row.setModelId(version.getModelId());
        }
        Map<Long, AuditModelNameDO> models = index(ids(rows, AuditOperationTraceRespVO::getModelId),
                auditModelNameMapper::selectOperationRelations, AuditModelNameDO::getId);
        List<AuditModelNameDO> modelRows = new ArrayList<>(models.values());
        Map<Long, AuditModelProjectDO> projects = index(ids(modelRows, AuditModelNameDO::getProjectId),
                auditModelProjectMapper::selectOperationRelations, AuditModelProjectDO::getId);
        Map<Long, AuditModelLayerDO> layers = loadLayers(ids(modelRows, AuditModelNameDO::getLayerId));
        for (AuditOperationTraceRespVO row : rows) {
            enrichModel(row, models.get(row.getModelId()), versions.get(row.getModelVersionId()), projects, layers);
        }
    }

    private boolean compatible(AuditOperationTraceRespVO row, AuditMasterChangeDO change) {
        return (row.getModelId() == null || Objects.equals(row.getModelId(), change.getModelId()))
                && (row.getModelVersionId() == null || Objects.equals(row.getModelVersionId(), change.getModelVersionId()))
                && (row.getMasterRecordId() == null || Objects.equals(row.getMasterRecordId(), change.getMasterRecordId()));
    }

    private void enrichModel(AuditOperationTraceRespVO row, AuditModelNameDO model, AuditModelVersionDO version,
                             Map<Long, AuditModelProjectDO> projects, Map<Long, AuditModelLayerDO> layers) {
        if (model != null) {
            row.setModelName(model.getName());
            row.setModelDisplayName(model.getName());
            row.setModelCode(model.getCode());
            row.setModelBusinessCode(model.getCode());
            row.setModelType(model.getType());
            row.setModelIcon(model.getIcon());
            row.setModelDescription(model.getDescription());
            AuditModelProjectDO project = projects.get(model.getProjectId());
            if (project != null) {
                row.setProjectName(project.getName());
                row.setProjectIcon(project.getIcon());
            }
            row.setLayerName(layerPath(model.getLayerId(), model.getProjectId(), layers));
        }
        if (version != null && Objects.equals(row.getModelId(), version.getModelId())) {
            row.setModelVersionNo(version.getVersionNo());
            row.setModelVersion(version.getVersionNo());
        }
    }

    private void enrichMaster(AuditOperationTraceRespVO row, AuditMasterChangeDO change, Map<String, Object> schema,
                              Map<Long, MasterDataVersionSnapshotDO> dataVersions) {
        boolean effective = "1".equals(change.getProcessResult());
        Long versionId = effective ? change.getAfterVersionId() : change.getBeforeVersionId();
        MasterDataVersionSnapshotDO version = dataVersions.get(versionId);
        Map<String, Object> snapshot = json(effective ? change.getAfterData() : change.getBeforeData());
        if (version != null && Objects.equals(version.getModelId(), row.getModelId())
                && Objects.equals(version.getMasterRecordId(), row.getMasterRecordId())) {
            Map<String, Object> versionSnapshot = json(version.getSnapshotData());
            if (!versionSnapshot.isEmpty()) snapshot = versionSnapshot;
            row.setMasterVersion(version.getVersionNo());
        } else {
            row.setMasterVersion(effective ? change.getAfterVersionNo() : change.getBeforeVersionNo());
        }
        if (hasText(change.getChangeReason())) row.setOperateDesc("变更原因：" + change.getChangeReason());
        if (schema == null || !Objects.equals(number(schema.get("id")), row.getModelId())) return;
        Object fields = schema.get("fields");
        if (!(fields instanceof List)) return;
        List<Map<String, Object>> names = new ArrayList<>();
        List<Map<String, Object>> codes = new ArrayList<>();
        for (Object value : (List<?>) fields) {
            Map<String, Object> field = object(value);
            if (flag(field.get("delFlag"))) continue;
            if (flag(field.get("nameFlag"))) names.add(field);
            if ("AUTOMATIC".equalsIgnoreCase(str(field.get("encodingMode")))
                    && number(field.get("encodingVersionId")) != null) codes.add(field);
        }
        if (names.size() == 1) {
            String name = fieldValue(snapshot, str(names.get(0).get("code")));
            if (hasText(name)) row.setMasterDataName(name);
        }
        // 没有明确且唯一的编码方案绑定时不猜字段，也不把内部ID当业务编码。
        if (codes.size() == 1) row.setMasterDataCode(fieldValue(snapshot, str(codes.get(0).get("code"))));
    }

    private Map<Long, Map<String, Object>> loadSchemas(List<Long> versionIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (int start = 0; start < versionIds.size(); start += 500) {
            for (AuditModelInstanceDO instance : auditModelInstanceMapper.selectPublishedSnapshots(
                    versionIds.subList(start, Math.min(start + 500, versionIds.size())))) {
                Map<String, Object> model = object(json(instance.getModelSnapshot()).get("model"));
                if (Objects.equals(number(model.get("id")), instance.getModelId())) {
                    result.putIfAbsent(instance.getModelVersionId(), model);
                }
            }
        }
        return result;
    }

    private Map<Long, AuditModelLayerDO> loadLayers(List<Long> ids) {
        Map<Long, AuditModelLayerDO> result = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        List<Long> pending = ids;
        while (!pending.isEmpty()) {
            List<Long> batch = pending.stream().filter(id -> id != null && id != 0 && visited.add(id))
                    .collect(Collectors.toList());
            if (batch.isEmpty()) break;
            Map<Long, AuditModelLayerDO> found = index(batch, auditModelLayerMapper::selectOperationRelations, AuditModelLayerDO::getId);
            result.putAll(found);
            pending = ids(new ArrayList<>(found.values()), AuditModelLayerDO::getParentId);
        }
        return result;
    }

    private String layerPath(Long id, Long projectId, Map<Long, AuditModelLayerDO> layers) {
        List<String> parts = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        while (id != null && id != 0) {
            if (!visited.add(id)) return null;
            AuditModelLayerDO layer = layers.get(id);
            if (layer == null || !Objects.equals(projectId, layer.getProjectId())) return null;
            parts.add(layer.getName());
            id = layer.getParentId();
        }
        Collections.reverse(parts);
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private Map<String, String> loadCurrentStates(List<AuditOperationTraceRespVO> rows) {
        Map<Long, Set<Long>> byModel = new HashMap<>();
        for (AuditOperationTraceRespVO row : rows) {
            if (row.getModelId() != null && row.getMasterRecordId() != null) {
                byModel.computeIfAbsent(row.getModelId(), id -> new HashSet<>()).add(row.getMasterRecordId());
            }
        }
        Map<String, String> states = new HashMap<>();
        Map<String, Date> times = new HashMap<>();
        byModel.forEach((modelId, recordIds) -> {
            List<Long> ids = new ArrayList<>(recordIds);
            for (int start = 0; start < ids.size(); start += 500) {
                List<Long> batch = ids.subList(start, Math.min(start + 500, ids.size()));
                for (AuditMasterChangeDO change : auditMasterChangeMapper.selectEffectiveStatesForRecords(modelId, batch)) {
                    applyState(states, times, modelId, change.getMasterRecordId(), change.getAfterStatus(), change.getChangeTime());
                }
                for (AuditChangeRequestItemDO item : auditChangeRequestItemMapper.selectEffectiveStatesForRecords(modelId, batch)) {
                    Long recordId = item.getResultMasterRecordId() == null ? item.getMasterRecordId() : item.getResultMasterRecordId();
                    applyState(states, times, modelId, recordId, item.getTargetStatus(), item.getExecuteTime());
                }
            }
        });
        return states;
    }

    private void applyState(Map<String, String> states, Map<String, Date> times, Long modelId, Long recordId, String status, Date time) {
        if (recordId == null || time == null || !hasText(status)) return;
        String key = recordKey(modelId, recordId);
        if (!times.containsKey(key) || time.after(times.get(key))) {
            times.put(key, time);
            states.put(key, lifecycleStatus(status));
        }
    }

    private String recordKey(Long modelId, Long recordId) {
        return modelId + ":" + recordId;
    }

    private <T> List<Long> ids(List<T> rows, Function<T, Long> key) {
        return rows.stream().map(key).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private <T> Map<Long, T> index(List<Long> ids, Function<List<Long>, List<T>> query, Function<T, Long> key) {
        Map<Long, T> result = new HashMap<>();
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (int start = 0; start < distinct.size(); start += 500) {
            for (T row : query.apply(distinct.subList(start, Math.min(start + 500, distinct.size())))) result.put(key.apply(row), row);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private boolean flag(Object value) {
        return "1".equals(str(value)) || "true".equalsIgnoreCase(str(value));
    }

    private String fieldValue(Map<String, Object> snapshot, String code) {
        if (!hasText(code)) return null;
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            if (code.equalsIgnoreCase(entry.getKey())) return entry.getValue() == null ? null : str(entry.getValue());
        }
        return null;
    }
}
