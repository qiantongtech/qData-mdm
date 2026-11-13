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

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.MasterDataVersionSnapshotDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.SourceDataVersionSnapshotDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditSourceRelationDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditSourceGroup;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditFieldSourceRelationDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelColumnDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditSourceDataDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditSourceSystemDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.MasterDataVersionSnapshotMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.SourceDataVersionSnapshotMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditSourceRelationMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditSourceDataMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditSourceSystemMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditFieldSourceRelationMapper;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import tech.qiantong.qdata.mdm.audit.service.audit.IAuditSourceTraceService;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceTraceListRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceAuthorityFieldRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceRecordRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceRecordFieldRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationTraceRespVO;

@Service
public class AuditSourceTraceServiceImpl extends AuditTraceReadService implements IAuditSourceTraceService {
    @Resource private MasterDataVersionSnapshotMapper masterDataVersionSnapshotMapper;
    @Resource private SourceDataVersionSnapshotMapper sourceDataVersionSnapshotMapper;
    @Resource private AuditSourceRelationMapper auditSourceRelationMapper;
    @Resource private AuditSourceDataMapper auditSourceDataMapper;
    @Resource private AuditSourceSystemMapper auditSourceSystemMapper;
    @Resource private AuditFieldSourceRelationMapper auditFieldSourceRelationMapper;
    @Resource private AuditChangeTraceServiceImpl auditChangeTraceService;
    @Resource private AuditOperationRelationService auditOperationRelationService;

    @Override
    public AuditTracePageRespVO<AuditSourceTraceListRespVO> sources(Map<String, String> filters) {
        List<AuditSourceTraceListRespVO> result = new ArrayList<>();
        String masterRecordIdFilter = str(filters.get("masterRecordId")).trim();
        Long masterRecordId = number(masterRecordIdFilter);
        if (!masterRecordIdFilter.isEmpty() && (masterRecordId == null || masterRecordId <= 0)) {
            return page(result, filters);
        }
        // 一行 = 模型 + 主数据记录，主数据记录号在不同模型间会重复（每个模型的实例表各自发号）。
        // 取该组最小的来源关系主键 MIN(ID) 作为全局唯一的数字编号：组内稳定、跨组唯一、纯数字、可排序。
        for (AuditSourceGroup raw : auditSourceRelationMapper.selectActiveGroups(masterRecordId)) {
            AuditSourceTraceListRespVO row = sourceRow(raw.getModelId(), raw.getMasterRecordId(), new AuditSourceTraceListRespVO());
            if (row != null && matchesValues(filters, "model", row.getModel(), "name", row.getName(),
                    "systems", row.getSystems(), "sourceRecordId", row.getSourceRecordId())
                    && matchesMode(row.getMode(), filters.get("mode")) && inTime(row.getUpdated(), filters)) {
                row.setSourceRelId(raw.getSourceRelId());
                result.add(row);
            }
        }
        Comparator<AuditSourceTraceListRespVO> sourceOrder = numericSortValue(filters, "sourceRelId", AuditSourceTraceListRespVO::getSourceRelId);
        if ("updated".equals(filters.get("orderByColumn")) && "ascending".equals(filters.get("isAsc"))) {
            result.sort(Comparator.comparing(row -> str(row.getUpdated())));
        } else {
            result.sort(sourceOrder == null ? descendingValue(AuditSourceTraceListRespVO::getUpdated) : sourceOrder);
        }
        AuditTracePageRespVO<AuditSourceTraceListRespVO> page = page(result, filters);
        enrichModels(page.getRows());
        return page;
    }

    @Override
    public AuditSourceTraceRespVO source(Long modelId, Long masterRecordId) {
        AuditSourceTraceRespVO row = sourceRow(modelId, masterRecordId, new AuditSourceTraceRespVO());
        if (row != null) enrichModels(Collections.singletonList(row));
        return row;
    }

    private <T extends AuditSourceTraceRespVO> T sourceRow(Long modelId, Long masterRecordId, T row) {
        if (modelId == null || masterRecordId == null) return null;
        List<Map<String, Object>> relations = sourceRelations(modelId, masterRecordId);
        if (relations.isEmpty()) return null;
        boolean listOnly = row instanceof AuditSourceTraceListRespVO;
        List<Map<String, Object>> lineage = listOnly ? Collections.emptyList() : sourceLineage(modelId, masterRecordId);
        MasterDataVersionSnapshotDO currentVersion = masterDataVersionSnapshotMapper.selectLatestSnapshot(modelId, masterRecordId);
        Map<String, Object> snapshot = currentVersion == null ? Collections.emptyMap() : json(currentVersion.getSnapshotData());
        List<AuditSourceRecordRespVO> records = new ArrayList<>();
        List<String> systems = new ArrayList<>();
        List<String> recordIds = new ArrayList<>();
        String updated = "";
        for (Map<String, Object> relation : relations) {
            boolean manual = "MANUAL".equals(str(relation.get("TYPE")));
            String system = manual ? "平台录入" : str(relation.get("SYSTEM_NAME"));
            if (system.isEmpty()) system = "未知来源";
            String recordId = str(relation.get("SOURCE_RECORD_KEY"));
            if (recordId.isEmpty()) recordId = str(relation.get("SOURCE_RECORD_ID"));
            Long versionId = number(relation.get("SOURCE_VERSION_ID"));
            if (versionId == null) versionId = number(relation.get("CURRENT_VERSION_ID"));
            Map<String, Object> sourceData = listOnly ? Collections.emptyMap() : (manual ? snapshot : sourceSnapshot(versionId));
            List<AuditSourceRecordFieldRespVO> sourceFields = new ArrayList<>();
            for (Map<String, Object> field : listOnly ? Collections.<Map<String, Object>>emptyList() : fields(modelId, sourceData)) {
                String code = str(field.get("code"));
                // 来源快照可能使用主数据字段编码；优先匹配来源字段路径，兼容按权威字段路径落库的关系。
                boolean used = lineage.stream().anyMatch(link -> Objects.equals(str(link.get("SOURCE_RECORD_ID")), str(relation.get("SOURCE_RECORD_ID")))
                        && (Objects.equals(str(link.get("SOURCE_FIELD_PATH")), code)
                        || Objects.equals(str(link.get("FIELD_PATH")), code)));
                AuditSourceRecordFieldRespVO item = new AuditSourceRecordFieldRespVO();
                item.setCode(code);
                item.setField((String) field.get("field"));
                item.setValue((String) field.get("value"));
                item.setUsed(used);
                sourceFields.add(item);
            }
            String when = date(relation.get("EFFECTIVE_TIME"));
            AuditSourceRecordRespVO record = new AuditSourceRecordRespVO();
            record.setId(recordId);
            record.setRelationId(number(relation.get("ID")));
            record.setSystem(system);
            record.setSourceSystemId(manual || relation.get("SYSTEM_ID") == null ? null : str(relation.get("SYSTEM_ID")));
            record.setSourceSystemCode(manual ? null : (String) relation.get("SYSTEM_CODE"));
            record.setSourceSystemDescription(manual ? null : (String) relation.get("SYSTEM_DESCRIPTION"));
            record.setSourceSystemTrustScore(manual ? null : (Integer) relation.get("SYSTEM_TRUST_SCORE"));
            record.setStatus("当前有效");
            record.setEstablished(when);
            record.setFields(sourceFields);
            records.add(record);
            systems.add(system);
            recordIds.add(recordId);
            if (when.compareTo(updated) > 0) updated = when;
        }
        List<AuditSourceAuthorityFieldRespVO> authority = new ArrayList<>();
        for (Map<String, Object> field : listOnly ? Collections.<Map<String, Object>>emptyList() : fields(modelId, snapshot)) {
            String code = str(field.get("code"));
            Map<String, Object> link = lineage.stream().filter(item -> code.equals(str(item.get("FIELD_PATH")))).findFirst().orElse(null);
            String sourceName = link == null ? "未记录字段来源" : str(link.get("SYSTEM_NAME"));
            if (link != null && sourceName.isEmpty()) {
                boolean manual = relations.stream().anyMatch(relation -> "MANUAL".equals(str(relation.get("TYPE")))
                        && Objects.equals(str(relation.get("SOURCE_RECORD_ID")), str(link.get("SOURCE_RECORD_ID"))));
                sourceName = manual ? "平台录入" : "未知来源";
            }
            String sourceRecordId = link == null ? null : str(link.get("SOURCE_RECORD_KEY"));
            if (link != null && sourceRecordId.isEmpty()) sourceRecordId = str(link.get("SOURCE_RECORD_ID"));
            Map<String, Object> sourceRelation = link == null ? null : relations.stream()
                    .filter(relation -> Objects.equals(str(relation.get("SOURCE_RECORD_ID")), str(link.get("SOURCE_RECORD_ID"))))
                    .findFirst().orElse(null);
            AuditSourceAuthorityFieldRespVO item = new AuditSourceAuthorityFieldRespVO();
            item.setCode(code);
            item.setField((String) field.get("field"));
            item.setValue((String) field.get("value"));
            item.setSource(sourceName);
            item.setSourceRecordId(sourceRecordId);
            item.setSourceRelationId(sourceRelation == null ? null : number(sourceRelation.get("ID")));
            authority.add(item);
        }
        AuditModelNameDO model = auditModelNameMapper.selectById(modelId);
        row.setId(modelId + "-" + masterRecordId);
        row.setModelId(modelId);
        row.setModelVersionId(currentVersion == null ? null : currentVersion.getModelVersionId());
        row.setMasterRecordId(masterRecordId);
        row.setModel(model == null ? "—" : model.getName());
        Map<String, Object> schemaRow = new LinkedHashMap<>();
        put(schemaRow, "MODEL_ID", modelId, "MODEL_VERSION_ID", row.getModelVersionId());
        List<Map<String, Object>> columns = auditChangeTraceService.publishedModelColumns(Collections.singletonList(schemaRow))
                .get(row.getModelVersionId());
        if (columns == null || columns.isEmpty()) {
            columns = currentIdentityColumns(modelId);
        }
        Map<String, Object> identity = auditChangeTraceService.changeIdentity(columns, snapshot);
        row.setName((String) identity.get("name"));
        row.setCode((String) identity.get("businessCode"));
        String mode = relations.stream().map(relation -> sourceMode(str(relation.get("TYPE"))))
                .filter(AuditSourceTraceServiceImpl::hasText).distinct().collect(Collectors.joining("、"));
        row.setMode(hasText(mode) ? mode : null);
        row.setSystems(systems.stream().distinct().collect(Collectors.joining("、")));
        row.setSourceRecordId(String.join("、", recordIds));
        row.setSourceRecordIds(recordIds);
        row.setUpdated(updated);
        row.setLifecycle(latestState(modelId, masterRecordId));
        row.setFields(authority);
        row.setRecords(records);
        return row;
    }

    private List<Map<String, Object>> currentIdentityColumns(Long modelId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditModelColumnDO column : auditModelColumnMapper.selectIdentityColumns(modelId)) {
            Map<String, Object> field = new LinkedHashMap<>();
            put(field, "code", column.getCode(), "nameFlag", column.getNameFlag(),
                    "encodingMode", column.getEncodingMode(), "encodingVersionId", column.getEncodingVersionId());
            result.add(field);
        }
        return result;
    }

    private List<Map<String, Object>> sourceRelations(Long modelId, Long masterRecordId) {
        List<AuditSourceRelationDO> relations = auditSourceRelationMapper.selectActive(modelId, masterRecordId);
        List<Long> dataIds = relations.stream().map(AuditSourceRelationDO::getSourceRecordId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<AuditSourceDataDO> sourceData = dataIds.isEmpty() ? Collections.emptyList()
                : auditSourceDataMapper.selectBatchIds(dataIds);
        Map<Long, AuditSourceDataDO> dataById = sourceData.stream()
                .collect(Collectors.toMap(AuditSourceDataDO::getId, item -> item));
        List<Long> systemIds = sourceData.stream().map(AuditSourceDataDO::getSourceSystemId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<AuditSourceSystemDO> systems = systemIds.isEmpty() ? Collections.emptyList()
                : auditSourceSystemMapper.selectBatchIds(systemIds);
        Map<Long, AuditSourceSystemDO> systemsById = systems.stream()
                .collect(Collectors.toMap(AuditSourceSystemDO::getId, item -> item));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditSourceRelationDO relation : relations) {
            AuditSourceDataDO data = dataById.get(relation.getSourceRecordId());
            AuditSourceSystemDO system = data == null ? null : systemsById.get(data.getSourceSystemId());
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "ID", relation.getId(), "TYPE", relation.getType(),
                    "SOURCE_RECORD_ID", relation.getSourceRecordId(), "SOURCE_VERSION_ID", relation.getSourceVersionId(),
                    "EFFECTIVE_TIME", relation.getEffectiveTime(),
                    "SOURCE_RECORD_KEY", data == null ? null : data.getSourceRecordKey(),
                    "CURRENT_VERSION_ID", data == null ? null : data.getCurrentVersionId(),
                    "SYSTEM_ID", system == null ? null : system.getId(),
                    "SYSTEM_NAME", system == null ? null : system.getName(),
                    "SYSTEM_CODE", system == null ? null : system.getCode(),
                    "SYSTEM_DESCRIPTION", system == null ? null : system.getDescription(),
                    "SYSTEM_TRUST_SCORE", system == null ? null : system.getTrustScore());
            result.add(row);
        }
        return result;
    }

    private String sourceMode(String type) {
        switch (type) {
            case "MANUAL": return "平台录入";
            case "MERGE": return "多源融合";
            case "IMPORT": return "批量导入";
            default: return null;
        }
    }

    private boolean matchesMode(String modes, String wanted) {
        if (!hasText(wanted)) return true;
        if (!hasText(modes)) return false;
        for (String mode : modes.split("、")) if (mode.equals(wanted.trim())) return true;
        return false;
    }

    private List<Map<String, Object>> sourceLineage(Long modelId, Long masterRecordId) {
        List<AuditFieldSourceRelationDO> fields = auditFieldSourceRelationMapper.selectActive(modelId, masterRecordId);
        List<Long> dataIds = fields.stream().map(AuditFieldSourceRelationDO::getSourceRecordId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<AuditSourceDataDO> sourceData = dataIds.isEmpty() ? Collections.emptyList()
                : auditSourceDataMapper.selectBatchIds(dataIds);
        Map<Long, AuditSourceDataDO> dataById = sourceData.stream()
                .collect(Collectors.toMap(AuditSourceDataDO::getId, item -> item));
        List<Long> systemIds = sourceData.stream().map(AuditSourceDataDO::getSourceSystemId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<AuditSourceSystemDO> systems = systemIds.isEmpty() ? Collections.emptyList()
                : auditSourceSystemMapper.selectBatchIds(systemIds);
        Map<Long, AuditSourceSystemDO> systemsById = systems.stream()
                .collect(Collectors.toMap(AuditSourceSystemDO::getId, item -> item));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditFieldSourceRelationDO field : fields) {
            AuditSourceDataDO data = dataById.get(field.getSourceRecordId());
            AuditSourceSystemDO system = data == null ? null : systemsById.get(data.getSourceSystemId());
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "SOURCE_RECORD_ID", field.getSourceRecordId(), "SOURCE_FIELD_PATH", field.getSourceFieldPath(),
                    "FIELD_PATH", field.getFieldPath(), "SOURCE_RECORD_KEY", data == null ? null : data.getSourceRecordKey(),
                    "SYSTEM_NAME", system == null ? null : system.getName());
            result.add(row);
        }
        return result;
    }

    private void enrichModels(List<? extends AuditSourceTraceRespVO> rows) {
        List<AuditOperationTraceRespVO> models = new ArrayList<>();
        for (AuditSourceTraceRespVO row : rows) {
            AuditOperationTraceRespVO model = new AuditOperationTraceRespVO();
            model.setModelId(row.getModelId());
            model.setModelVersionId(row.getModelVersionId());
            models.add(model);
        }
        auditOperationRelationService.enrichModelInfo(models);
        for (int i = 0; i < rows.size(); i++) {
            AuditSourceTraceRespVO row = rows.get(i);
            AuditOperationTraceRespVO model = models.get(i);
            row.setModelCode(model.getModelBusinessCode());
            row.setModelType(model.getModelType());
            row.setModelIcon(model.getModelIcon());
            row.setModelDescription(model.getModelDescription());
            row.setModelVersionNo(model.getModelVersionNo());
            row.setProjectName(model.getProjectName());
            row.setProjectIcon(model.getProjectIcon());
            row.setLayerName(model.getLayerName());
        }
    }

    private Map<String, Object> sourceSnapshot(Long versionId) {
        if (versionId == null) return Collections.emptyMap();
        SourceDataVersionSnapshotDO version = sourceDataVersionSnapshotMapper.selectSnapshot(versionId);
        return version == null ? Collections.emptyMap() : json(version.getModelData());
    }
}
