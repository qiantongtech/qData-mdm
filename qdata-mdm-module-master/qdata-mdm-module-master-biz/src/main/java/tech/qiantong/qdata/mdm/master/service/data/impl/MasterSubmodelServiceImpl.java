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

package tech.qiantong.qdata.mdm.master.service.data.impl;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeSubmodelSaveReqVO;
import com.fasterxml.jackson.databind.JsonNode;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate.Operation;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.TableMutation;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceData;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.DateUtils;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelDataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelRecordRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelPageReqVO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterSubmodelQueryRepository;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.RequestContext;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.TableAccess;
import tech.qiantong.qdata.mdm.master.service.data.IMasterSubmodelService;
import tech.qiantong.qdata.mdm.master.util.MasterFieldValueValidator;
import tech.qiantong.qdata.mdm.master.util.MasterRecordUtils;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;

import tech.qiantong.qdata.mdm.model.base.dto.ModelColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedSubmodelRespDTO;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictDataRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权威主数据的 1:N 子模型查询、校验及写入计划。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterSubmodelServiceImpl implements IMasterSubmodelService {
    private static final Pattern SAFE_COLUMN = Pattern.compile("[A-Za-z0-9_$]+");

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository dataQueryRepository;
    private final MasterSubmodelQueryRepository submodelQueryRepository;
    private final ModelPublishedApi modelPublishedApi;

    /**
     * 校验主记录维护申请中携带的子模型变更。这里只读元数据和物化表，不直接改写正式数据。
     */
    @Override
    public void validateChanges(Long instanceId, MasterModelRespDTO mainModel, String mainMode,
                                Long mainRecordId,
                                List<MasterChangeSubmodelSaveReqVO> changes,
                                Map<String, String> errors) {
        if (changes == null || changes.isEmpty()) return;
        if (changes.size() > 500) {
            childError(errors, "submodelChanges", "子模型变更数量不能超过500条");
            return;
        }
        List<Definition> definitions = loadDefinitions(mainModel.getModelId(), instanceId);
        Map<Long, Definition> definitionMap = new HashMap<>();
        for (Definition definition : definitions) definitionMap.put(definition.id, definition);
        Set<String> changedRecords = new HashSet<>();
        Map<String, Integer> requestUniqueValues = new HashMap<>();

        for (int index = 0; index < changes.size(); index++) {
            MasterChangeSubmodelSaveReqVO change = changes.get(index);
            String path = "submodelChanges[" + index + "]";
            if (change == null) {
                childError(errors, path, "第" + (index + 1) + "条子模型变更不能为空");
                continue;
            }
            Long submodelId = positiveId(change.getSubmodelId());
            Definition definition = submodelId == null ? null : definitionMap.get(submodelId);
            if (definition == null) {
                childError(errors, path + ".submodelId", "子模型不属于当前主模型");
                continue;
            }
            if (definition.instance == null) {
                childError(errors, path + ".submodelId", definition.name + "尚未生成可用的物化实例");
                continue;
            }
            Availability availability = inspect(definition);
            if (!availability.available) {
                childError(errors, path + ".submodelId", definition.name + "：" + availability.message);
                continue;
            }
            if (!Objects.equals(mainModel.getDatasourceId(), availability.datasourceId)) {
                childError(errors, path + ".submodelId", "子表与主表必须使用同一物化数据源");
                continue;
            }
            String operation = upper(change.getOperation());
            boolean create = "CREATE".equals(operation);
            boolean update = "UPDATE".equals(operation);
            boolean delete = "DELETE".equals(operation);
            if (!create && !update && !delete) {
                childError(errors, path + ".operation", definition.name + "的变更类型无效");
                continue;
            }
            if (!"UPDATE".equals(mainMode) && !create) {
                childError(errors, path + ".operation", "新增主数据时只能新增子模型记录");
                continue;
            }
            Long subRecordId = positiveId(change.getRecordId());
            if (create && !trim(change.getRecordId()).isEmpty()) {
                childError(errors, path + ".recordId", "新增子模型记录不能指定记录ID");
                continue;
            }
            Map<String, Object> original = null;
            if (update || delete) {
                if (mainRecordId == null || subRecordId == null) {
                    childError(errors, path + ".recordId", definition.name + "的原记录ID格式无效");
                    continue;
                }
                String recordKey = submodelId + ":" + subRecordId;
                if (!changedRecords.add(recordKey)) {
                    childError(errors, path + ".recordId", "同一子模型记录不能重复变更");
                    continue;
                }
                List<Map<String, Object>> rows = submodelQueryRepository.selectRecord(
                        availability.datasourceId, availability.tableName, mainRecordId, subRecordId);
                if (rows.size() != 1) {
                    childError(errors, path + ".recordId", rows.isEmpty()
                            ? "当前主数据下不存在该子模型记录" : "子模型记录ID存在重复数据");
                    continue;
                }
                original = rows.get(0);
                if (!"1".equals(text(original.get("STATUS")))) {
                    childError(errors, path + ".recordId", "只有生效状态的子模型记录可修改或删除");
                    continue;
                }
            }
            List<Field> fields = loadFields(definition, availability.physicalColumns, true);
            Map<String, String> values = change.getValues() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(change.getValues());
            if (create) {
                for (Field field : fields) if (field.business && !values.containsKey(field.prop))
                    values.put(field.prop, field.defaultValue);
                change.setValues(values);
            }
            if (values.size() > 200) {
                childError(errors, path + ".values", definition.name + "的字段数量超限");
                continue;
            }
            if (delete) {
                if (!availability.physicalColumns.contains("DEL_FLAG"))
                    childError(errors, path + ".operation", "子模型物化表缺少逻辑删除字段 DEL_FLAG");
                if (!values.isEmpty()) childError(errors, path + ".values", "删除子模型记录不能修改字段");
                continue;
            }

            Map<String, Field> editableFields = new LinkedHashMap<>();
            for (Field field : fields) if (field.business) editableFields.put(field.prop, field);
            for (String key : values.keySet()) {
                if (key == null || !editableFields.containsKey(key)) {
                    childError(errors, path + ".values", definition.name + "包含未配置的业务字段");
                    break;
                }
            }
            for (Field field : editableFields.values()) {
                String raw = values.containsKey(field.prop) ? values.get(field.prop)
                        : original == null ? null : MaintenanceData.fieldText(original.get(field.sqlCode), field.dataType);
                String value = trim(raw);
                String fieldPath = path + ".values." + field.prop;
                String label = field.label == null ? field.prop : field.label;
                if (value.isEmpty() && (field.required || field.nameField)) {
                    childError(errors, fieldPath, definition.name + "的" + label + "不能为空");
                    continue;
                }
                if (value.isEmpty()) continue;
                if (raw.length() > 65535 || !MasterFieldValueValidator.valid(
                        field.dataType, raw, field.dataLength, field.dataScale)) {
                    childError(errors, fieldPath, definition.name + "的" + label + "不符合字段类型、长度或精度");
                    continue;
                }
                Object typed;
                try {
                    typed = MaintenanceData.convert(raw, field.dataType);
                } catch (ServiceException ex) {
                    childError(errors, fieldPath, definition.name + "的" + label + "不符合字段类型");
                    continue;
                }
                if (field.dictTypeId != null && !validDictionaryValue(definition, field.dictTypeId, raw)) {
                    childError(errors, fieldPath, definition.name + "的" + label + "不是有效的参考数据值");
                    continue;
                }
                if (field.relatedModelId != null) {
                    try {
                        if (!validRelatedValue(field, raw)) {
                            childError(errors, fieldPath, definition.name + "的" + label + "关联数据不存在或不可用");
                            continue;
                        }
                    } catch (ServiceException ex) {
                        childError(errors, fieldPath, definition.name + "的" + label + "关联数据校验失败：" + ex.getMessage());
                        continue;
                    }
                }
                if (field.unique) {
                    String uniqueValue = typed instanceof java.math.BigDecimal
                            ? ((java.math.BigDecimal) typed).stripTrailingZeros().toPlainString() : String.valueOf(typed);
                    String requestKey = definition.id + ":" + field.sqlCode + ":" + uniqueValue;
                    Integer previous = requestUniqueValues.putIfAbsent(requestKey, index);
                    if (previous != null) {
                        childError(errors, fieldPath, definition.name + "的" + label + "在本次申请中重复");
                        continue;
                    }
                    long count = submodelQueryRepository.countByColumn(
                            availability.datasourceId, availability.tableName, field.sqlCode, typed, update ? subRecordId : null);
                    if (count > 0) {
                        childError(errors, fieldPath, definition.name + "的" + label + "已存在");
                    }
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> freezeChanges(MasterModelRespDTO mainModel, Long mainRecordId,
            List<MasterChangeSubmodelSaveReqVO> changes) {
        if (changes == null || changes.isEmpty()) return Collections.emptyList();
        Map<Long, Definition> definitions = writeDefinitions(mainModel, changes);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MasterChangeSubmodelSaveReqVO change : changes) {
            Definition definition = definitions.get(positiveId(change.getSubmodelId()));
            if (definition == null) throw new ServiceException("子模型不属于当前主模型");
            Availability availability = definition.availability;
            List<Field> fields = loadFields(definition, availability.physicalColumns, true);
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("submodelId", change.getSubmodelId());
            saved.put("submodelName", definition.name);
            // 冻结展示所需的业务字段名称和类型，历史申请不依赖最新模型或正式子记录。
            List<Map<String, String>> displayFields = new ArrayList<>();
            for (Field field : fields) {
                if (!field.business) continue;
                Map<String, String> displayField = new LinkedHashMap<>();
                displayField.put("code", field.prop);
                displayField.put("name", field.label);
                displayField.put("dataType", field.dataType);
                displayField.put("category", field.category);
                displayFields.add(displayField);
            }
            saved.put("fields", displayFields);
            saved.put("operation", change.getOperation());
            saved.put("recordId", change.getRecordId());
            saved.put("values", change.getValues() == null ? Collections.emptyMap() : change.getValues());
            if (!"CREATE".equals(change.getOperation())) {
                Map<String, Object> original = requireChild(availability, mainRecordId, positiveId(change.getRecordId()));
                saved.put("beforeData", childSnapshot(fields, original));
            }
            result.add(saved);
        }
        return result;
    }

    @Override
    public WritePlan planChanges(MasterModelRespDTO mainModel, String mainMode, Long mainRecordId,
            String proposedData, MasterChangeRequestDO request) {
        JsonNode savedChanges = MaintenanceData.payload(proposedData).get("submodelChanges");
        if (savedChanges == null) return new WritePlan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        if (!savedChanges.isArray() || savedChanges.size() > 500)
            throw new ServiceException("子模型申请内容格式无效");
        if (savedChanges.isEmpty()) return new WritePlan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        List<MasterChangeSubmodelSaveReqVO> changes = new ArrayList<>();
        for (JsonNode saved : savedChanges) {
            MasterChangeSubmodelSaveReqVO change = new MasterChangeSubmodelSaveReqVO();
            change.setSubmodelId(saved.path("submodelId").asText());
            change.setOperation(saved.path("operation").asText());
            change.setRecordId(saved.path("recordId").isNull() ? null : saved.path("recordId").asText());
            change.setValues(MaintenanceData.values(saved.toString()));
            changes.add(change);
        }
        Map<String, String> errors = new LinkedHashMap<>();
        validateChanges(mainModel.getInstanceId(), mainModel, mainMode, mainRecordId, changes, errors);
        if (!errors.isEmpty()) throw new ServiceException("子模型执行校验未通过：" + errors.values().iterator().next());
        Map<Long, Definition> definitions = writeDefinitions(mainModel, changes);
        List<TableMutation> writes = new ArrayList<>();
        List<Map<String, Object>> before = new ArrayList<>();
        List<Map<String, Object>> after = new ArrayList<>();
        for (int index = 0; index < changes.size(); index++) {
            MasterChangeSubmodelSaveReqVO change = changes.get(index);
            JsonNode saved = savedChanges.get(index);
            Definition definition = definitions.get(positiveId(change.getSubmodelId()));
            Availability availability = definition.availability;
            List<Field> fields = loadFields(definition, availability.physicalColumns, true);
            boolean create = "CREATE".equals(change.getOperation());
            boolean delete = "DELETE".equals(change.getOperation());
            Long recordId = create ? MaintenanceData.targetRecordId(saved.toString()) : positiveId(change.getRecordId());
            Map<String, String> original = new LinkedHashMap<>();
            Map<String, Object> values = new LinkedHashMap<>();
            Map<String, Object> conditions = new LinkedHashMap<>();
            if (!create) {
                if (!saved.path("beforeData").isObject()) throw new ServiceException("子模型申请缺少修改前快照");
                original = MaintenanceData.snapshot(saved.get("beforeData").toString());
                Map<String, String> current = childSnapshot(fields, requireChild(availability, mainRecordId, recordId));
                for (Field field : fields) {
                    if (field.business && !MaintenanceData.sameValue(original.get(field.prop), current.get(field.prop), field.dataType)) {
                        throw new ServiceException("子模型记录已变化，请刷新后重新提交：" + definition.name + "的" + field.label);
                    }
                }
                conditions.put("ID", recordId);
                conditions.put("PARENT_ID", mainRecordId);
                conditions.put("STATUS", "1");
                if (availability.physicalColumns.contains("DEL_FLAG")) conditions.put("DEL_FLAG", false);
                before.add(childResult(definition.id, recordId, original));
            }
            Map<String, String> resulting = new LinkedHashMap<>(original);
            if (delete) {
                values.put("DEL_FLAG", true);
            } else {
                for (Field field : fields) {
                    if (!field.business || !change.getValues().containsKey(field.prop)) continue;
                    String value = change.getValues().get(field.prop);
                    if (create || !MaintenanceData.sameValue(original.get(field.prop), value, field.dataType))
                        values.put(field.sqlCode, MaintenanceData.convert(value, field.dataType));
                    resulting.put(field.prop, value);
                }
                if (create) {
                    if (recordId != null) values.put("ID", recordId);
                    values.put("PARENT_ID", mainRecordId);
                    values.put("STATUS", "1");
                    childSystem(availability, values, "DEL_FLAG", false);
                    childSystem(availability, values, "DATA_SOURCE", "MANUAL");
                    childSystem(availability, values, "CREATED_BY", request.getApplicantName());
                    childSystem(availability, values, "CREATED_BY_ID", String.valueOf(request.getApplicantId()));
                    childSystem(availability, values, "CREATED_TIME", submittedTime(request));
                }
                Map<String, Object> result = childResult(definition.id, recordId, resulting);
                after.add(result);
                childSystem(availability, values, "UPDATED_BY", request.getApplicantName());
                childSystem(availability, values, "UPDATED_BY_ID", String.valueOf(request.getApplicantId()));
                childSystem(availability, values, "UPDATED_TIME", submittedTime(request));
                if (values.isEmpty()) values.put("STATUS", "1");
                Operation operation = create ? Operation.insert(values) : Operation.update(values, conditions);
                writes.add(create && recordId == null
                        ? TableMutation.generatedInsert(availability.tableName, operation, mainRecordId == null, result)
                        : new TableMutation(availability.tableName, operation));
                continue;
            }
            childSystem(availability, values, "UPDATED_BY", request.getApplicantName());
            childSystem(availability, values, "UPDATED_BY_ID", String.valueOf(request.getApplicantId()));
            childSystem(availability, values, "UPDATED_TIME", submittedTime(request));
            if (values.isEmpty()) values.put("STATUS", "1");
            writes.add(new TableMutation(availability.tableName, create
                    ? Operation.insert(values) : Operation.update(values, conditions)));
        }
        return new WritePlan(writes, before, after);
    }

    private Map<Long, Definition> writeDefinitions(MasterModelRespDTO mainModel, List<MasterChangeSubmodelSaveReqVO> changes) {
        Map<Long, Definition> result = new LinkedHashMap<>();
        Set<Long> selected = changes.stream().map(change -> positiveId(change.getSubmodelId())).collect(Collectors.toSet());
        for (Definition definition : loadDefinitions(mainModel.getModelId(), mainModel.getInstanceId())) {
            if (!selected.contains(definition.id)) continue;
            if (definition.instance == null) continue;
            definition.availability = inspect(definition);
            if (!definition.availability.available) throw new ServiceException(definition.availability.message);
            if (!Objects.equals(mainModel.getDatasourceId(), definition.availability.datasourceId))
                throw new ServiceException("子表与主表必须使用同一物化数据源");
            result.put(definition.id, definition);
        }
        return result;
    }

    private Map<String, Object> requireChild(Availability availability, Long parentId, Long recordId) {
        List<Map<String, Object>> rows = submodelQueryRepository.selectRecord(
                availability.datasourceId, availability.tableName, parentId, recordId);
        if (rows.size() != 1 || !"1".equals(text(rows.get(0).get("STATUS"))))
            throw new ServiceException("子模型记录不存在、不属于当前主记录或状态不允许操作");
        return rows.get(0);
    }

    private Map<String, String> childSnapshot(List<Field> fields, Map<String, Object> row) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Field field : fields) if (field.business)
            result.put(field.prop, MaintenanceData.fieldText(row.get(field.sqlCode), field.dataType));
        return result;
    }

    private Map<String, Object> childResult(Long modelId, Long recordId, Map<String, String> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submodelId", String.valueOf(modelId));
        result.put("recordId", String.valueOf(recordId));
        result.put("values", values);
        return result;
    }

    private void childSystem(Availability availability, Map<String, Object> values, String field, Object value) {
        if (availability.physicalColumns.contains(field)) values.put(field, value);
    }

    private java.sql.Timestamp submittedTime(MasterChangeRequestDO request) {
        if (request.getSubmitTime() == null) throw new ServiceException("申请缺少提交时间");
        return new java.sql.Timestamp(request.getSubmitTime().getTime());
    }

    /** 查询维护表单所需的子模型定义，新增主记录时不要求已存在父记录。 */
    @Override
    public MasterSubmodelDataRespVO getDefinitions(Long instanceId, Long submodelId) {
        MasterModelRespDTO mainModel = metadataService.getModel(instanceId);
        List<Definition> definitions = loadDefinitions(mainModel.getModelId(), instanceId);
        MasterSubmodelDataRespVO response = new MasterSubmodelDataRespVO();
        if (definitions.isEmpty()) return response;
        for (Definition definition : definitions) {
            if (definition.instance == null) continue;
            definition.availability = inspect(definition);
        }
        response.setSubmodels(definitions.stream().map(this::toResponseModel).collect(Collectors.toList()));
        Definition selected = selectDefinition(definitions, submodelId);
        response.setSelectedSubmodelId(String.valueOf(selected.id));
        Set<String> physical = selected.availability == null
                ? Collections.emptySet() : selected.availability.physicalColumns;
        response.setColumns(toColumns(loadFields(selected, physical, false)));
        return response;
    }

    /**
     * 查询主数据或子模型分页数据。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 子模型、分页及排序参数
     * @return 子模型定义及选中子模型的分页记录
     */
    @Override
    public MasterSubmodelDataRespVO getData(Long instanceId, Long masterDataId,
                                            MasterSubmodelPageReqVO reqVO) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        Long modelId = modelPublishedApi.getPublishedModelId(instanceId);
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(modelId, true);
        MasterPublishedModelUtils.requirePublication(structure.getModel(), modelId, instanceId, null);
        Long submodelId = reqVO == null ? null : reqVO.getSubmodelId();
        int page = reqVO == null || reqVO.getPageNum() == null ? 1 : Math.max(1, reqVO.getPageNum());
        int size = reqVO == null || reqVO.getPageSize() == null ? 10
                : Math.max(1, Math.min(100, reqVO.getPageSize()));

        try (RequestContext context = dataQueryRepository.openRequest()) {
            String mainTable = MasterPublishedModelUtils.qualifiedTable(structure.getModel().getAuthoritySchemaName(),
                    structure.getModel().getAuthorityTableName());
            TableAccess mainAccess = context.forTable(structure.getModel().getAuthorityDatasourceId(), mainTable,
                    columnMappings(structure.getAuthorityColumns(), structure.getFields()));
            dataQueryRepository.requireRecord(mainAccess, masterDataId);
            MasterSubmodelDataRespVO response = new MasterSubmodelDataRespVO();
            if (structure.getSubModels().isEmpty()) return response;

            ModelPublishedMaterializationRespDTO binding = modelPublishedApi.getPublishedModelMaterialization(modelId);
            List<Definition> definitions = loadDefinitions(modelId, instanceId, structure, binding);
            Definition selected = selectDefinition(definitions, submodelId);
            response.setSelectedSubmodelId(String.valueOf(selected.id));
            for (Definition definition : definitions) {
                Long count = null;
                if (definition.instance != null) {
                    Availability availability = inspect(definition, context);
                    definition.availability = availability;
                    if (availability.available) {
                        try {
                            count = submodelQueryRepository.countByParent(availability.access, masterDataId);
                            if (definition == selected) response.setTotal(count);
                        } catch (RuntimeException ex) {
                            availability.available = false;
                            availability.message = "子模型物化表不存在或当前连接不可访问";
                            log.warn("读取子模型数量失败，submodelId={}", definition.id, ex);
                        }
                    }
                }
                MasterSubmodelDataRespVO.Submodel item = toResponseModel(definition);
                item.setRecordCount(count);
                response.getSubmodels().add(item);
            }

            if (selected.instance == null) return response;

            Availability availability = selected.availability;
            if (!availability.available) return response;

            List<Field> fields = loadFields(selected, availability.physicalColumns, true);
            response.setColumns(toColumns(fields));
            if (response.getTotal() == 0L) return response;

            List<Map<String, Object>> rawRows = submodelQueryRepository.selectPage(
                    availability.access, fields.stream().map(field -> field.sqlCode).collect(Collectors.toList()),
                    masterDataId, reqVO, (long) (page - 1) * size, size);
            response.setRows(rawRows.stream().map(raw -> toRow(fields, raw)).collect(Collectors.toList()));
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询权威主数据子模型失败，instanceId={}, masterDataId={}, submodelId={}",
                    instanceId, masterDataId, submodelId, ex);
            throw new ServiceException("子模型数据查询失败，请检查子模型物化配置");
        }
    }

    /**
     * 查询指定子模型记录。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param submodelId 子模型 ID
     * @param subRecordId 子记录 ID
     * @return 指定子记录及其字段定义
     */
    @Override
    public MasterSubmodelRecordRespVO getRecord(Long instanceId, Long masterDataId,
                                                          Long submodelId, Long subRecordId) {
        if (masterDataId == null || masterDataId <= 0 || submodelId == null || submodelId <= 0
                || subRecordId == null || subRecordId <= 0) {
            throw new ServiceException("子模型记录ID格式无效");
        }
        MasterModelRespDTO mainModel = metadataService.getModel(instanceId);
        MasterRecordUtils.requireRecord(dataQueryRepository, mainModel, masterDataId);
        try {
            Definition selected = selectDefinition(
                    loadDefinitions(mainModel.getModelId(), instanceId), submodelId);
            if (selected.instance == null) throw new ServiceException("子模型尚未物化");
            Availability availability = inspect(selected);
            if (!availability.available) throw new ServiceException(availability.message);

            List<Field> fields = loadFields(selected, availability.physicalColumns, true);
            List<Map<String, Object>> records = submodelQueryRepository.selectRecord(
                    availability.datasourceId, availability.tableName, masterDataId, subRecordId);
            if (records.isEmpty()) throw new ServiceException("当前主数据下不存在该子模型记录");
            if (records.size() > 1) throw new ServiceException("子模型记录ID存在重复数据");
            Map<String, Object> raw = records.get(0);
            Map<String, Object> row = toRow(fields, raw);
            List<MasterColumnRespVO> columns = toColumns(fields);
            Set<String> used = fields.stream().map(field -> field.sqlCode).collect(Collectors.toSet());
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String code = entry.getKey().toUpperCase(Locale.ROOT);
                if ("CREATED_BY_ID".equals(code) || "UPDATED_BY_ID".equals(code)) continue;
                if (used.contains(code)) continue;
                MasterColumnRespVO column = new MasterColumnRespVO();
                column.setProp(code);
                column.setLabel(systemLabel(code));
                if ("CREATED_TIME".equals(code) || "UPDATED_TIME".equals(code)) {
                    column.setDataType("DATETIME");
                }
                column.setBusinessField(false);
                row.put(code, safeValue(code, entry.getValue()));
                columns.add(column);
            }
            MasterSubmodelRecordRespVO response = new MasterSubmodelRecordRespVO();
            response.setSubmodelName(selected.name);
            response.setColumns(columns);
            response.setRow(row);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询子模型记录详情失败，instanceId={}, masterDataId={}, submodelId={}, subRecordId={}",
                    instanceId, masterDataId, submodelId, subRecordId, ex);
            throw new ServiceException("子模型记录详情查询失败");
        }
    }

    private Map<String, Object> toRow(List<Field> fields, Map<String, Object> raw) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (Field field : fields) row.put(field.prop, safeValue(field.sqlCode, raw.get(field.sqlCode)));
        return row;
    }

    private Object safeValue(String code, Object value) {
        if (value == null) return null;
        if (value instanceof java.math.BigDecimal) return ((java.math.BigDecimal) value).toPlainString();
        if (value instanceof java.math.BigInteger) return value.toString();
        if ("ID".equals(code) || "PARENT_ID".equals(code)
                || code.endsWith("_ID")) return String.valueOf(value);
        return value instanceof Date
                ? DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, (Date) value)
                : value;
    }

    private boolean validDictionaryValue(Definition definition, Long typeId, String value) {
        // 参考数据项与子模型字段来自同一次成功发布，禁止查当前可编辑的 MODEL_DICT_DATA。
        return definition.referenceItems.stream().anyMatch(item -> typeId.equals(item.getTypeId())
                && value.equals(item.getCode()) && Boolean.TRUE.equals(item.getValidFlag())
                && !Boolean.TRUE.equals(item.getDelFlag()));
    }

    private boolean validRelatedValue(Field field, String value) {
        if (field.relatedCode == null || !SAFE_COLUMN.matcher(field.relatedCode).matches()) return false;
        ModelPublishedMaterializationRespDTO binding =
                modelPublishedApi.getPublishedModelMaterialization(field.relatedModelId);
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(field.relatedModelId);
        requireSamePublication(field.relatedModelId, binding.getInstanceId(), binding, structure);
        if (structure.getFields().stream().noneMatch(item -> field.relatedCode.equalsIgnoreCase(item.getCode()))) {
            return false;
        }
        ModelPublishedMaterializedTableRespDTO table =
                MasterPublishedModelUtils.requireAuthorityTable(binding, field.relatedModelId, 0L);
        String tableName = MasterPublishedModelUtils.qualifiedTable(binding.getSchemaName(), table.getTableName());
        return submodelQueryRepository.countActiveByColumn(table.getDatasourceId(), tableName,
                field.relatedCode.toUpperCase(Locale.ROOT), value) > 0;
    }

    private void childError(Map<String, String> errors, String key, String message) {
        errors.putIfAbsent(key, message);
        errors.putIfAbsent("submodelChanges", message);
    }

    private Long positiveId(String value) {
        try {
            long id = Long.parseLong(trim(value));
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String upper(String value) {
        return trim(value).toUpperCase(Locale.ROOT);
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String systemLabel(String code) {
        switch (code) {
            case "PARENT_ID": return "所属主数据ID";
            case "DATA_SOURCE": return "形成方式";
            case "CREATED_BY": return "创建人";
            case "CREATED_TIME": return "创建时间";
            case "UPDATED_BY": return "更新人";
            case "UPDATED_TIME": return "更新时间";
            case "REMARK": return "描述";
            default: return code;
        }
    }

    private List<Definition> loadDefinitions(Long mainModelId, Long mainInstanceId) {
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(mainModelId);
        ModelPublishedMaterializationRespDTO binding = modelPublishedApi.getPublishedModelMaterialization(mainModelId);
        return loadDefinitions(mainModelId, mainInstanceId, structure, binding);
    }

    private List<Definition> loadDefinitions(Long mainModelId, Long mainInstanceId,
            ModelPublishedStructureRespDTO structure, ModelPublishedMaterializationRespDTO binding) {
        requireSamePublication(mainModelId, mainInstanceId, binding, structure);
        ModelPublishedMaterializedTableRespDTO root =
                MasterPublishedModelUtils.requireAuthorityTable(binding, mainModelId, 0L);
        List<Definition> result = new ArrayList<>();
        for (ModelPublishedSubmodelRespDTO model : structure.getSubModels()) {
            if (!mainModelId.equals(model.getParentId())) throw new ServiceException("成功发布的子模型归属不一致");
            Definition definition = new Definition();
            definition.id = model.getId();
            definition.code = model.getCode();
            definition.name = model.getName();
            definition.fields = new ArrayList<>(model.getFields());
            definition.referenceItems = structure.getReferenceItems();
            ModelPublishedMaterializedTableRespDTO table =
                    MasterPublishedModelUtils.findAuthorityTable(binding, model.getId(), mainModelId);
            if (table != null) {
                if (!Objects.equals(root.getId(), table.getParentTableId())) {
                    throw new ServiceException("子模型权威表与主表绑定不一致");
                }
                definition.instance = new Instance();
                // 主子表共享本次发布实例；子表通过 modelId 和物化表绑定区分。
                definition.instance.id = mainInstanceId;
                definition.instance.datasourceId = table.getDatasourceId();
                definition.instance.schemaName = binding.getSchemaName();
                definition.instance.tableName = table.getTableName();
                definition.instance.columnMappings = columnMappings(table.getColumns(), definition.fields);
            }
            result.add(definition);
        }
        return result;
    }

    private Map<String, String> columnMappings(List<ModelMaterializedColumnRespDTO> columns,
            List<ModelPublishedColumnRespDTO> fields) {
        Map<Long, String> codes = fields.stream().collect(Collectors.toMap(
                ModelPublishedColumnRespDTO::getId, ModelPublishedColumnRespDTO::getCode));
        Map<String, String> result = new LinkedHashMap<>();
        for (ModelMaterializedColumnRespDTO column : columns) {
            if (!Boolean.TRUE.equals(column.getEnabledFlag()) || !Boolean.TRUE.equals(column.getValidFlag())
                    || !Boolean.FALSE.equals(column.getDelFlag())) continue;
            String code = Boolean.TRUE.equals(column.getSystemFlag())
                    ? column.getSystemFieldKey() : codes.get(column.getModelFieldId());
            if (code == null) throw new ServiceException("成功物化字段缺少已发布绑定");
            result.put(code.toUpperCase(Locale.ROOT), column.getColumnName());
        }
        return result;
    }

    private void requireSamePublication(Long modelId, Long instanceId,
            ModelPublishedMaterializationRespDTO binding, ModelPublishedStructureRespDTO structure) {
        MasterPublishedModelUtils.requirePublication(binding, modelId, instanceId, null);
        MasterPublishedModelUtils.requireStructure(binding, structure);
    }

    private MasterSubmodelDataRespVO.Submodel toResponseModel(Definition definition) {
        MasterSubmodelDataRespVO.Submodel item = new MasterSubmodelDataRespVO.Submodel();
        item.setId(String.valueOf(definition.id));
        item.setCode(definition.code);
        item.setName(definition.name);
        item.setMaterialized(definition.instance != null);
        if (definition.instance == null) {
            item.setAvailable(false);
            item.setMessage("子模型已配置，但尚未生成可用的物化实例");
        } else {
            item.setInstanceId(String.valueOf(definition.instance.id));
            item.setAvailable(definition.availability.available);
            item.setMessage(definition.availability.message);
        }
        return item;
    }

    private Definition selectDefinition(List<Definition> definitions, Long submodelId) {
        if (submodelId == null) return definitions.get(0);
        return definitions.stream().filter(item -> item.id.equals(submodelId)).findFirst()
                .orElseThrow(() -> new ServiceException("子模型不属于当前主模型"));
    }

    private Availability inspect(Definition definition) {
        return inspect(definition, null);
    }

    /** 页面读取复用发布列绑定；维护校验保留目标库结构检查。 */
    private Availability inspect(Definition definition, RequestContext context) {
        Availability result = new Availability();
        try {
            result.tableName = MasterPublishedModelUtils.qualifiedTable(
                    definition.instance.schemaName, definition.instance.tableName);
        } catch (ServiceException ex) {
            result.message = "子模型物化表名无效";
            return result;
        }
        result.datasourceId = definition.instance.datasourceId;
        try {
            if (context == null) {
                result.physicalColumns = submodelQueryRepository.findPhysicalColumns(result.datasourceId, result.tableName);
            } else {
                result.access = context.forTable(result.datasourceId, result.tableName, definition.instance.columnMappings);
                result.physicalColumns = result.access.columns();
            }
        } catch (Exception ex) {
            result.message = "子模型物化表不存在或当前连接不可访问";
            return result;
        }
        if (!result.physicalColumns.contains("ID")) {
            result.message = "子模型物化表缺少 ID";
        } else if (!result.physicalColumns.contains("PARENT_ID")) {
            result.message = "子模型物化表缺少父记录关联字段 PARENT_ID";
        } else if (!result.physicalColumns.contains("STATUS")) {
            result.message = "子模型物化表缺少生命周期字段 STATUS";
        } else {
            result.available = true;
        }
        return result;
    }

    private List<Field> loadFields(Definition definition, Set<String> physicalColumns, boolean requirePhysical) {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field("ID", "ID", "主数据ID", "BIGINT", false, false));
        List<ModelPublishedColumnRespDTO> configured = new ArrayList<>(definition.fields);
        configured.sort(Comparator.comparing(ModelPublishedColumnRespDTO::getSortOrder,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(ModelPublishedColumnRespDTO::getId));
        for (ModelColumnRespDTO row : configured) {
            String code = row.getCode();
            String category = row.getColumnCategory();
            if (code == null || !SAFE_COLUMN.matcher(code).matches()) {
                throw new ServiceException("子模型字段编码无效：" + code);
            }
            String sqlCode = code.toUpperCase(Locale.ROOT);
            if ("ID".equals(sqlCode) || "PARENT_ID".equals(sqlCode)
                    || "STATUS".equals(sqlCode) || "SYSTEM".equalsIgnoreCase(category)) continue;
            if (requirePhysical && !physicalColumns.contains(sqlCode)) {
                throw new ServiceException("子模型字段配置与物化表不一致，缺少字段：" + code);
            }
            fields.add(new Field(code, sqlCode, row.getName(), row.getDataType(),
                    true, Boolean.TRUE.equals(row.getNameFlag()),
                    Boolean.TRUE.equals(row.getRequiredFlag()), Boolean.TRUE.equals(row.getUniqueFlag()),
                    row.getDataLength(), row.getDataScale(), row.getDefaultValue(), category,
                    row.getDictTypeId(), row.getRelatedModelId(), row.getRelatedCode(), row.getDisplayCode()));
        }
        fields.add(new Field("STATUS", "STATUS", "生命周期", "STRING", false, false));
        return fields;
    }

    private List<MasterColumnRespVO> toColumns(List<Field> fields) {
        List<MasterColumnRespVO> result = new ArrayList<>();
        for (Field field : fields) {
            MasterColumnRespVO column = new MasterColumnRespVO();
            column.setProp(field.prop);
            column.setLabel(field.label == null ? field.prop : field.label);
            column.setDataType(field.dataType == null ? null : field.dataType.toUpperCase(Locale.ROOT));
            column.setBusinessField(field.business);
            column.setNameField(field.nameField);
            column.setRequired(field.required);
            column.setUnique(field.unique);
            column.setDataLength(field.dataLength);
            column.setDataScale(field.dataScale);
            column.setDefaultValue(field.defaultValue);
            if ("STATUS".equals(field.sqlCode)) column.setDict("master_data_status");
            result.add(column);
        }
        return result;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static class Definition {
        private Long id;
        private String code;
        private String name;
        private Instance instance;
        private Availability availability;
        private List<ModelPublishedColumnRespDTO> fields;
        private List<ModelDictDataRespDTO> referenceItems;
    }

    private static class Instance {
        private Long id;
        private Long datasourceId;
        private String schemaName;
        private String tableName;
        private Map<String, String> columnMappings;
    }

    private static class Availability {
        private Long datasourceId;
        private boolean available;
        private String message;
        private String tableName;
        private TableAccess access;
        private Set<String> physicalColumns = Collections.emptySet();
    }

    private static class Field {
        private final String prop;
        private final String sqlCode;
        private final String label;
        private final String dataType;
        private final boolean business;
        private final boolean nameField;
        private final boolean required;
        private final boolean unique;
        private final Long dataLength;
        private final Long dataScale;
        private final String defaultValue;
        private final String category;
        private final Long dictTypeId;
        private final Long relatedModelId;
        private final String relatedCode;
        private final String displayCode;

        private Field(String prop, String sqlCode, String label, String dataType,
                      boolean business, boolean nameField) {
            this(prop, sqlCode, label, dataType, business, nameField,
                    false, false, null, null, null, null, null, null, null, null);
        }

        private Field(String prop, String sqlCode, String label, String dataType,
                      boolean business, boolean nameField, boolean required, boolean unique,
                      Long dataLength, Long dataScale, String defaultValue, String category,
                      Long dictTypeId, Long relatedModelId, String relatedCode, String displayCode) {
            this.prop = prop;
            this.sqlCode = sqlCode;
            this.label = label;
            this.dataType = dataType;
            this.business = business;
            this.nameField = nameField;
            this.required = required;
            this.unique = unique;
            this.dataLength = dataLength;
            this.dataScale = dataScale;
            this.defaultValue = defaultValue;
            this.category = category;
            this.dictTypeId = dictTypeId;
            this.relatedModelId = relatedModelId;
            this.relatedCode = relatedCode;
            this.displayCode = displayCode;
        }
    }
}
