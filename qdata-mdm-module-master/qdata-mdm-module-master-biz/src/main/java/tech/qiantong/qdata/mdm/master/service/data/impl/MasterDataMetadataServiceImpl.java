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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataMetadataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterMaintenanceColumnRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterMaintenanceFormConfigRespVO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedSubmodelRespDTO;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictDataRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 通过 model-api 读取成功发布定义；列表元数据不连接业务数据库，也不查询统计。 */
@Service
@RequiredArgsConstructor
public class MasterDataMetadataServiceImpl implements IMasterDataMetadataService {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");
    private static final Set<String> INTERNAL_COLUMNS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "VALID_FLAG", "DEL_FLAG", "CREATED_BY_ID", "UPDATED_BY_ID")));
    private final ModelPublishedApi modelPublishedApi;

    @Override
    public ModelPublishedRespDTO getPublishedModel(Long instanceId, Long modelId) {
        return findPublishedModel(instanceId, modelId);
    }

    @Override
    public MasterDataMetadataRespVO getMetadata(Long instanceId) {
        return getMetadata(instanceId, null);
    }

    @Override
    public MasterDataMetadataRespVO getMetadata(Long instanceId, Long modelId) {
        MasterModelRespDTO model = loadDefinition(instanceId, modelId);
        return getMetadata(model);
    }

    @Override
    public MasterMaintenanceFormConfigRespVO getMaintenanceFormConfig(Long instanceId, Long modelId,
            Long modelVersionId) {
        if (modelId == null || modelVersionId == null || modelVersionId <= 0) {
            throw new ServiceException("维护表单缺少有效的模型ID或版本ID");
        }
        ModelPublishedRespDTO published = findPublishedModel(instanceId, modelId);
        MasterPublishedModelUtils.requirePublication(published, modelId, instanceId, modelVersionId);
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(modelId);
        MasterModelRespDTO model = loadDefinition(instanceId, published, structure);
        MasterDataMetadataRespVO metadata = getMetadata(model);
        Map<String, ModelPublishedColumnRespDTO> fields = new LinkedHashMap<>();
        for (ModelPublishedColumnRespDTO field : structure.getFields()) {
            if (!Boolean.TRUE.equals(field.getDelFlag())) fields.put(field.getCode().toUpperCase(Locale.ROOT), field);
        }
        MasterMaintenanceFormConfigRespVO response = new MasterMaintenanceFormConfigRespVO();
        response.setInstanceId(instanceId);
        response.setModelId(modelId);
        response.setModelVersionId(modelVersionId);
        for (MasterColumnRespVO column : metadata.getColumns()) {
            if (!Boolean.TRUE.equals(column.getBusinessField())) continue;
            ModelPublishedColumnRespDTO field = fields.get(column.getProp());
            if (field == null || "SYSTEM".equalsIgnoreCase(field.getColumnCategory())) {
                throw new ServiceException("维护字段与成功发布配置不一致：" + column.getProp());
            }
            response.getColumns().add(maintenanceColumn(column, field, structure.getReferenceItems()));
        }
        for (ModelPublishedSubmodelRespDTO submodel : structure.getSubModels()) {
            MasterMaintenanceFormConfigRespVO.Submodel item = new MasterMaintenanceFormConfigRespVO.Submodel();
            item.setId(submodel.getId());
            for (ModelPublishedColumnRespDTO field : submodel.getFields()) {
                if (Boolean.TRUE.equals(field.getDelFlag()) || "SYSTEM".equalsIgnoreCase(field.getColumnCategory())) continue;
                MasterColumnRespVO column = new MasterColumnRespVO();
                column.setProp(field.getCode().toUpperCase(Locale.ROOT));
                column.setBusinessField(true);
                column.setLabel(hasText(field.getName()) ? field.getName()
                        : hasText(field.getDescription()) ? field.getDescription() : field.getCode());
                item.getColumns().add(maintenanceColumn(column, field, structure.getReferenceItems()));
            }
            response.getSubmodels().add(item);
        }
        return response;
    }

    private MasterMaintenanceColumnRespVO maintenanceColumn(MasterColumnRespVO column,
            ModelPublishedColumnRespDTO field, List<ModelDictDataRespDTO> referenceItems) {
        MasterMaintenanceColumnRespVO response = new MasterMaintenanceColumnRespVO();
        BeanUtils.copyProperties(column, response);
        response.setLabel(hasText(field.getName()) ? field.getName() : column.getLabel());
        response.setColumnCategory(field.getColumnCategory());
        response.setDataType(field.getDataType());
        response.setDataLength(field.getDataLength());
        response.setDataScale(field.getDataScale());
        response.setRequired(Boolean.TRUE.equals(field.getRequiredFlag()));
        response.setNameField(Boolean.TRUE.equals(field.getNameFlag()));
        response.setUnique(Boolean.TRUE.equals(field.getUniqueFlag()));
        response.setDefaultValue(field.getDefaultValue());
        response.setEncodingMode(field.getEncodingMode());
        response.setDictTypeId(field.getDictTypeId());
        response.setRelatedModelId(field.getRelatedModelId());
        response.setRelatedCode(field.getRelatedCode());
        response.setDisplayCode(field.getDisplayCode());
        response.setNodeRule(field.getNodeRule());
        response.setDisplayMode(field.getDisplayMode());
        if (field.getDictTypeId() != null) {
            // 发布接口已筛选有效项，此处只匹配集合并按配置顺序生成选项。
            referenceItems.stream()
                    .filter(item -> Objects.equals(field.getDictTypeId(), item.getTypeId())
                            && !Boolean.TRUE.equals(item.getDelFlag()))
                    .sorted(Comparator.comparingLong(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()))
                    .forEach(item -> {
                        MasterMaintenanceColumnRespVO.ReferenceOption option = new MasterMaintenanceColumnRespVO.ReferenceOption();
                        option.setValue(item.getCode());
                        option.setLabel(hasText(item.getName()) ? item.getName() : item.getCode());
                        response.getReferenceOptions().add(option);
                    });
        }
        return response;
    }

    private MasterDataMetadataRespVO getMetadata(MasterModelRespDTO model) {
        ModelPublishedMaterializationRespDTO binding =
                modelPublishedApi.getPublishedModelMaterialization(model.getModelId());
        MasterPublishedModelUtils.requirePublication(binding, model.getModelId(), model.getInstanceId(), model.getModelVersionId());
        ModelPublishedMaterializedTableRespDTO table =
                MasterPublishedModelUtils.requireAuthorityTable(binding, model.getModelId(), 0L);
        bindColumns(model, table.getColumns());
        MasterDataMetadataRespVO response = new MasterDataMetadataRespVO();
        response.setInstanceId(model.getInstanceId());
        response.setModelId(model.getModelId());
        response.setModelVersionId(model.getModelVersionId());
        response.setModelCode(model.getModelCode());
        response.setModelName(model.getModelName());
        response.setModelType(model.getModelType());
        response.setVersionNo(model.getVersionNo());
        response.setDomainName(model.getDomainName());
        response.setColumns(getMaterializedColumns(model, table));
        return response;
    }

    /** 列集合、顺序及约束取成功物化字段；表头名称取发布配置，完整注释单独保留。 */
    private List<MasterColumnRespVO> getMaterializedColumns(MasterModelRespDTO model,
            ModelPublishedMaterializedTableRespDTO table) {
        Map<String, MasterColumnRespVO> configured = new LinkedHashMap<>();
        for (MasterColumnRespVO column : getColumns(model)) {
            String physicalName = model.getColumnMappings().get(column.getProp());
            configured.put(physicalName.toUpperCase(Locale.ROOT), column);
        }
        List<ModelMaterializedColumnRespDTO> physicalColumns = new ArrayList<>(table.getColumns());
        physicalColumns.sort(Comparator.comparing(ModelMaterializedColumnRespDTO::getColumnPosition,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ModelMaterializedColumnRespDTO::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        List<MasterColumnRespVO> columns = new ArrayList<>();
        for (ModelMaterializedColumnRespDTO physical : physicalColumns) {
            if (!Boolean.TRUE.equals(physical.getEnabledFlag()) || !Boolean.TRUE.equals(physical.getValidFlag())
                    || !Boolean.FALSE.equals(physical.getDelFlag())) continue;
            MasterColumnRespVO column = configured.get(physical.getColumnName().toUpperCase(Locale.ROOT));
            // 内部标记字段仍保留绑定，但不作为用户可见表头。
            if (column == null) continue;
            column.setComment(physical.getColumnComment());
            column.setDataType(physical.getLogicalType());
            column.setSortable(!"TEXT".equalsIgnoreCase(physical.getLogicalType()));
            column.setRequired(Boolean.FALSE.equals(physical.getNullable()));
            column.setDataLength("DECIMAL".equalsIgnoreCase(physical.getLogicalType())
                    ? physical.getNumericPrecision() == null ? null : physical.getNumericPrecision().longValue()
                    : physical.getCharacterLength());
            column.setDataScale(physical.getNumericScale() == null ? null : physical.getNumericScale().longValue());
            column.setSortOrder(columns.size() + 1);
            columns.add(column);
        }
        return columns;
    }

    @Override
    public MasterModelRespDTO getModel(Long instanceId) {
        return getModel(instanceId, null);
    }

    @Override
    public MasterModelRespDTO getModel(Long instanceId, Long modelId) {
        MasterModelRespDTO model = loadDefinition(instanceId, modelId);
        ModelPublishedMaterializationRespDTO binding =
                modelPublishedApi.getPublishedModelMaterialization(model.getModelId());
        MasterPublishedModelUtils.requirePublication(binding, model.getModelId(), instanceId, model.getModelVersionId());
        ModelPublishedMaterializedTableRespDTO table =
                MasterPublishedModelUtils.requireAuthorityTable(binding, model.getModelId(), 0L);
        model.setDatasourceId(table.getDatasourceId());
        model.setSchemaName(binding.getSchemaName());
        model.setPhysicalTableName(table.getTableName());
        bindColumns(model, table.getColumns());
        return model;
    }

    @Override
    public MasterModelRespDTO getPublishedDefinition(Long instanceId, Long modelId, boolean includeAuthorityTable) {
        if (modelId == null) modelId = modelPublishedApi.getPublishedModelId(instanceId);
        ModelPublishedStructureRespDTO definition = modelPublishedApi.getPublishedModelStructure(modelId, includeAuthorityTable);
        ModelPublishedRespDTO published = definition.getModel();
        MasterPublishedModelUtils.requirePublication(published, modelId, instanceId, null);
        MasterModelRespDTO model = loadDefinition(published.getInstanceId(), published, definition);
        if (includeAuthorityTable) {
            model.setDatasourceId(published.getAuthorityDatasourceId());
            model.setSchemaName(published.getAuthoritySchemaName());
            model.setPhysicalTableName(published.getAuthorityTableName());
            bindColumns(model, definition.getAuthorityColumns());
        }
        return model;
    }

    /** 业务字段按稳定字段ID绑定，系统字段按systemFieldKey绑定；不推导真实列名。 */
    private void bindColumns(MasterModelRespDTO model, List<ModelMaterializedColumnRespDTO> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new ServiceException("成功发布的权威表缺少物化字段");
        }
        Map<Long, ModelMaterializedColumnRespDTO> business = new LinkedHashMap<>();
        Map<String, ModelMaterializedColumnRespDTO> systems = new LinkedHashMap<>();
        Set<String> physicalNames = new HashSet<>();
        for (ModelMaterializedColumnRespDTO column : columns) {
            if (!Boolean.TRUE.equals(column.getEnabledFlag()) || !Boolean.TRUE.equals(column.getValidFlag())
                    || !Boolean.FALSE.equals(column.getDelFlag())) continue;
            String physicalName = column.getColumnName();
            if (!validIdentifier(physicalName) || !physicalNames.add(physicalName.toUpperCase(Locale.ROOT))) {
                throw new ServiceException("成功物化字段名称无效或重复");
            }
            if (Boolean.TRUE.equals(column.getSystemFlag())) {
                String key = column.getSystemFieldKey();
                if (!validIdentifier(key) || systems.putIfAbsent(key.toUpperCase(Locale.ROOT), column) != null) {
                    throw new ServiceException("成功物化的系统字段标识无效或重复");
                }
            } else if (column.getModelFieldId() == null
                    || business.putIfAbsent(column.getModelFieldId(), column) != null) {
                throw new ServiceException("成功物化的业务字段绑定无效或重复");
            }
        }
        Map<String, String> mappings = new LinkedHashMap<>();
        List<MasterFieldRespDTO> fields = new ArrayList<>();
        for (MasterFieldRespDTO field : model.getFields()) {
            boolean system = "SYSTEM".equalsIgnoreCase(field.getCategory());
            ModelMaterializedColumnRespDTO column = system ? systems.remove(field.getCode()) : business.remove(field.getId());
            // 模型可能同时描述来源角色的系统字段，权威列表只使用本角色存在的列。
            if (system && column == null) continue;
            if (column == null) throw new ServiceException("模型字段缺少成功物化绑定：" + field.getCode());
            mappings.put(field.getCode(), column.getColumnName());
            field.setComment(column.getColumnComment());
            fields.add(field);
        }
        if (!business.isEmpty()) throw new ServiceException("成功物化的业务字段与发布配置不一致");
        List<Map.Entry<String, ModelMaterializedColumnRespDTO>> remaining = new ArrayList<>(systems.entrySet());
        remaining.sort(Comparator.comparing(entry -> entry.getValue().getColumnPosition(),
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (Map.Entry<String, ModelMaterializedColumnRespDTO> entry : remaining) {
            String key = entry.getKey();
            ModelMaterializedColumnRespDTO column = entry.getValue();
            if (mappings.putIfAbsent(key, column.getColumnName()) != null) {
                throw new ServiceException("业务字段与系统字段标识冲突：" + key);
            }
            MasterFieldRespDTO field = new MasterFieldRespDTO();
            field.setCode(key);
            field.setName(column.getColumnName());
            field.setComment(column.getColumnComment());
            field.setCategory("SYSTEM");
            field.setDataType(column.getLogicalType());
            field.setRequired(Boolean.FALSE.equals(column.getNullable()));
            field.setNameField(false);
            fields.add(field);
        }
        if (!mappings.containsKey("ID") || !mappings.containsKey("STATUS")) {
            throw new ServiceException("权威表缺少ID或STATUS系统字段绑定");
        }
        if ("CATEGORY".equals(model.getModelType()) && !mappings.containsKey("CATEGORY_ID")) {
            throw new ServiceException("分类模型缺少CATEGORY_ID系统字段绑定");
        }
        if (fields.stream().filter(field -> Boolean.TRUE.equals(field.getNameField())).count() != 1) {
            throw new ServiceException("成功发布的权威字段必须且只能有一个名称字段");
        }
        model.setFields(fields);
        model.setColumnMappings(Collections.unmodifiableMap(mappings));
    }

    private boolean validIdentifier(String value) {
        return value != null && IDENTIFIER.matcher(value).matches();
    }

    private MasterModelRespDTO loadDefinition(Long instanceId, Long modelId) {
        ModelPublishedRespDTO published = findPublishedModel(instanceId, modelId);
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(published.getId());
        return loadDefinition(instanceId, published, structure);
    }

    private MasterModelRespDTO loadDefinition(Long instanceId, ModelPublishedRespDTO published,
            ModelPublishedStructureRespDTO structure) {
        MasterPublishedModelUtils.requireStructure(published, structure);
        MasterModelRespDTO model = new MasterModelRespDTO();
        model.setInstanceId(instanceId);
        model.setModelId(published.getId());
        model.setModelVersionId(published.getVersionId());
        model.setModelCode(published.getCode());
        model.setModelName(published.getName());
        model.setModelType(published.getType());
        model.setDomainName(published.getDomainName());
        model.setVersionNo(published.getVersionNo());
        List<MasterFieldRespDTO> fields = new ArrayList<>();
        for (ModelPublishedColumnRespDTO field : structure.getFields()) {
            MasterFieldRespDTO item = new MasterFieldRespDTO();
            item.setId(field.getId());
            // 保持主数据页面既有大写字段键；物理查询仍由连接层按真实列名引用。
            item.setCode(field.getCode() == null ? null : field.getCode().toUpperCase(Locale.ROOT));
            item.setName(field.getName());
            item.setComment(field.getDescription());
            item.setCategory(field.getColumnCategory());
            item.setDataType(field.getDataType());
            item.setNameField(field.getNameFlag());
            item.setRequired(field.getRequiredFlag());
            item.setUnique(field.getUniqueFlag());
            item.setDataLength(field.getDataLength());
            item.setDataScale(field.getDataScale());
            item.setDefaultValue(field.getDefaultValue());
            item.setDictTypeId(field.getDictTypeId());
            item.setEncodingVersionId(field.getEncodingVersionId());
            item.setRelatedModelId(field.getRelatedModelId());
            item.setRelatedCode(field.getRelatedCode());
            item.setDisplayCode(field.getDisplayCode());
            item.setNodeRule(field.getNodeRule());
            item.setDisplayMode(field.getDisplayMode());
            if (field.getSortOrder() != null && (field.getSortOrder() < Integer.MIN_VALUE || field.getSortOrder() > Integer.MAX_VALUE)) {
                throw new ServiceException("模型字段顺序超出支持范围：" + field.getCode());
            }
            item.setSortOrder(field.getSortOrder() == null ? null : field.getSortOrder().intValue());
            fields.add(item);
        }
        model.setFields(fields);
        validateModel(instanceId, model);
        return model;
    }

    /** 按成功实例直接定位模型，避免分页遍历已发布模型。 */
    private ModelPublishedRespDTO findPublishedModel(Long instanceId, Long modelId) {
        if (instanceId == null || instanceId <= 0) throw new ServiceException("模型实例ID格式无效");
        if (modelId == null) modelId = modelPublishedApi.getPublishedModelId(instanceId);
        if (modelId <= 0) throw new ServiceException("模型ID格式无效");
        ModelPublishedRespDTO model = modelPublishedApi.getPublishedModel(modelId);
        MasterPublishedModelUtils.requirePublication(model, modelId, instanceId, null);
        return model;
    }

    @Override
    public List<MasterColumnRespVO> getColumns(MasterModelRespDTO model) {
        List<MasterFieldRespDTO> fields = new ArrayList<>(model.getFields());
        fields.sort(Comparator.comparing(MasterFieldRespDTO::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MasterFieldRespDTO::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        List<MasterColumnRespVO> columns = new ArrayList<>();
        for (MasterFieldRespDTO field : fields) {
            if (INTERNAL_COLUMNS.contains(field.getCode())) continue;
            MasterColumnRespVO column = new MasterColumnRespVO();
            column.setProp(field.getCode());
            column.setLabel(hasText(field.getName()) ? field.getName()
                    : hasText(field.getComment()) ? field.getComment() : field.getCode());
            column.setComment(field.getComment());
            column.setDataType(field.getDataType());
            column.setSortable(!"TEXT".equalsIgnoreCase(field.getDataType()));
            if ("STATUS".equalsIgnoreCase(field.getCode())) column.setDict("master_data_status");
            column.setSortOrder(columns.size() + 1);
            column.setBusinessField(!"SYSTEM".equalsIgnoreCase(field.getCategory())
                    && !"ID".equalsIgnoreCase(field.getCode())
                    && !"PARENT_ID".equalsIgnoreCase(field.getCode())
                    && !"CATEGORY_ID".equalsIgnoreCase(field.getCode()));
            column.setNameField(Boolean.TRUE.equals(field.getNameField()));
            column.setRequired(field.getRequired());
            column.setUnique(field.getUnique());
            column.setDataLength(field.getDataLength());
            column.setDataScale(field.getDataScale());
            column.setDefaultValue(field.getDefaultValue());
            columns.add(column);
        }
        return columns;
    }

    private void validateModel(Long instanceId, MasterModelRespDTO model) {
        if (model == null || !instanceId.equals(model.getInstanceId())) {
            throw new ServiceException("模型元数据与卡片实例不匹配");
        }
        if (model.getModelId() == null || model.getModelId() <= 0
                || model.getModelVersionId() == null || model.getModelVersionId() <= 0) {
            throw new ServiceException("模型元数据缺少有效的模型ID或版本ID");
        }
        if (!"NORMAL".equals(model.getModelType()) && !"CATEGORY".equals(model.getModelType())) {
            throw new ServiceException("模型类型必须为 NORMAL 或 CATEGORY");
        }
        if (model.getFields() == null || model.getFields().isEmpty()) {
            throw new ServiceException("模型字段配置不能为空");
        }
        Set<String> codes = new HashSet<>();
        for (MasterFieldRespDTO field : model.getFields()) {
            if (field == null || field.getCode() == null || !IDENTIFIER.matcher(field.getCode()).matches()) {
                throw new ServiceException("模型字段编码无效");
            }
            if (!codes.add(field.getCode().toUpperCase(Locale.ROOT))) {
                throw new ServiceException("模型字段编码重复：" + field.getCode());
            }
            if (!hasText(field.getDataType())) throw new ServiceException("模型字段缺少类型：" + field.getCode());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
