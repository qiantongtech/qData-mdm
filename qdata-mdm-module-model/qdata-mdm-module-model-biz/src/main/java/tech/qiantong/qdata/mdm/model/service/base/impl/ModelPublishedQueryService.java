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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.model.service.base.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.base.dto.*;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelTreeRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.*;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelInstanceMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelMaterializedColumnMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelMaterializedTableMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelPublishConfigMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.layer.ModelLayerMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.project.ModelProjectMapper;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictDataRespDTO;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictTypeRespDTO;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedIndexDefinition;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishSnapshot;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingItemRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingVersionRespDTO;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.model.util.ModelPublishUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 从成功发布指针读取对外查询所需的快照和物化绑定。
 */
@Slf4j
@Service
public class ModelPublishedQueryService implements ModelPublishedApi {

    @Resource
    private ModelInstanceMapper instanceMapper;

    @Resource
    private ModelProjectMapper projectMapper;

    @Resource
    private ModelLayerMapper layerMapper;

    @Resource
    private ModelMaterializedTableMapper materializedTableMapper;

    @Resource
    private ModelMaterializedColumnMapper materializedColumnMapper;

    @Resource
    private ModelPublishConfigMapper configMapper;

    /**
     * 所属域、目录和模型信息按成功快照分组；目录统一放在域的第二层。
     */
    @Transactional(readOnly = true)
    public List<ModelTreeRespVO> getPublishedModelTree() {
        List<ModelPublishedRespDTO> models = new ArrayList<>();
        // 逐条组装，避免在应用中积攒所有完整快照。
        instanceMapper.scanPublishedInstances(row -> {
            ModelPublishedRespDTO model = batchPublishedInfo(row.getResultObject());
            if (model != null) {
                models.add(model);
            }
        });
        if (models.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, ModelTreeRespVO> projects = new LinkedHashMap<>();
        for (ModelProjectDO project : projectMapper.selectList(Wrappers.<ModelProjectDO>lambdaQuery()
                .eq(ModelProjectDO::getValidFlag, true).orderByAsc(ModelProjectDO::getId))) {
            ModelTreeRespVO node = treeNode("project", project.getId(), project.getName(), project.getCode(), null);
            node.setIcon(project.getIcon());
            node.setProjectId(project.getId());
            projects.put(project.getId(), node);
        }
        Map<Long, ModelTreeRespVO> layers = new LinkedHashMap<>();
        for (ModelLayerDO layer : layerMapper.selectList(Wrappers.<ModelLayerDO>lambdaQuery()
                .eq(ModelLayerDO::getValidFlag, true)
                .orderByAsc(ModelLayerDO::getSortOrder).orderByAsc(ModelLayerDO::getId))) {
            ModelTreeRespVO project = projects.get(layer.getProjectId());
            if (project == null) {
                continue;
            }
            ModelTreeRespVO node = treeNode("layer", layer.getId(), layer.getName(), layer.getCode(), project.getId());
            node.setProjectId(layer.getProjectId());
            node.setLayerId(layer.getId());
            layers.put(layer.getId(), node);
        }
        for (ModelPublishedRespDTO model : models) {
            ModelTreeRespVO layer = layers.get(model.getLayerId());
            if (layer == null || !Objects.equals(layer.getProjectId(), model.getProjectId())) {
                continue;
            }
            ModelTreeRespVO node = treeNode("model", model.getId(), model.getName(), model.getCode(), layer.getId());
            node.setProjectId(model.getProjectId());
            node.setLayerId(model.getLayerId());
            node.setModelId(model.getId());
            node.setInstanceId(model.getInstanceId());
            node.setVersionId(model.getVersionId());
            node.setVersionNo(model.getVersionNo());
            layer.getChildren().add(node);
        }
        // 不挂载空目录；无发布模型的主数据域也不产生空分支。
        for (ModelTreeRespVO layer : layers.values()) {
            if (!layer.getChildren().isEmpty()) {
                projects.get(layer.getProjectId()).getChildren().add(layer);
            }
        }
        return projects.values().stream().filter(node -> !node.getChildren().isEmpty()).collect(Collectors.toList());
    }

    /**
     * 使用原始ID组装节点，域、目录和模型通过nodeType区分，不修改业务ID。
     */
    private ModelTreeRespVO treeNode(String type, Long id, String name, String code, Long parentId) {
        ModelTreeRespVO node = new ModelTreeRespVO();
        node.setId(id);
        node.setParentId(parentId);
        node.setNodeType(type);
        node.setName(name);
        node.setCode(code);
        return node;
    }

    /**
     * 数据库按成功快照筛选、计数并分页；pageSize为-1时返回全部匹配记录。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ModelPublishedRespDTO> getPublishedModelPage(ModelPublishedPageReqDTO request) {
        long total = instanceMapper.countPublishedInstances(request);
        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        // 全量查询不传分页参数，计数、分页与全量查询共用筛选条件。
        List<ModelInstanceDO> instances;
        if (PageParam.PAGE_SIZE_NONE.equals(request.getPageSize())) {
            instances = instanceMapper.selectPublishedInstanceList(request);
        } else {
            Page<ModelInstanceDO> page = new Page<>(request.getPageNum(), request.getPageSize(), false);
            instances = instanceMapper.selectPublishedInstancePage(page, request).getRecords();
        }
        Map<Long, Map<String, String>> statisticsColumns = loadStatisticsColumns(instances);
        List<ModelPublishedRespDTO> rows = new ArrayList<>(instances.size());
        for (ModelInstanceDO instance : instances) {
            ModelPublishedRespDTO model = batchPublishedInfo(instance);
            if (model != null) {
                model.setAuthorityStatisticsColumns(statisticsColumns.get(instance.getAuthorityTableId()));
                rows.add(model);
            }
        }
        fillDomainNames(rows);
        return new PageResult<>(rows, total);
    }

    /**
     * 只读取当前页权威主表的三个统计系统列，保留实际列名大小写。
     */
    private Map<Long, Map<String, String>> loadStatisticsColumns(List<ModelInstanceDO> instances) {
        Set<Long> tableIds = instances.stream().map(ModelInstanceDO::getAuthorityTableId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (tableIds.isEmpty()) return Collections.emptyMap();
        List<ModelMaterializedColumnDO> columns = materializedColumnMapper.selectList(
                new LambdaQueryWrapper<ModelMaterializedColumnDO>()
                        .select(ModelMaterializedColumnDO::getMaterializedTableId,
                                ModelMaterializedColumnDO::getSystemFieldKey, ModelMaterializedColumnDO::getColumnName)
                        .in(ModelMaterializedColumnDO::getMaterializedTableId, tableIds)
                        .in(ModelMaterializedColumnDO::getSystemFieldKey, Arrays.asList("STATUS", "UPDATED_TIME", "DEL_FLAG"))
                        .eq(ModelMaterializedColumnDO::getEnabledFlag, true)
                        .eq(ModelMaterializedColumnDO::getValidFlag, true));
        Map<Long, Map<String, String>> result = new HashMap<>();
        for (ModelMaterializedColumnDO column : columns) {
            result.computeIfAbsent(column.getMaterializedTableId(), id -> new LinkedHashMap<>())
                    .put(column.getSystemFieldKey(), column.getColumnName());
        }
        return result;
    }

    /**
     * 不可用的模型不能改为返回草稿或任意一条历史成功记录。
     */
    @Override
    @Transactional(readOnly = true)
    public ModelPublishedRespDTO getPublishedModel(Long modelId) {
        ModelPublishedRespDTO model = publishedInfo(requirePublishedInstance(modelId, false));
        fillDomainNames(Collections.singletonList(model));
        return model;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPublishedModelId(Long instanceId) {
        if (instanceId == null || instanceId <= 0) throw new ServiceException("模型实例ID格式无效", HttpStatus.WARN);
        Long modelId = instanceMapper.selectPublishedModelId(instanceId);
        if (modelId == null) throw new ServiceException("当前实例不可用", HttpStatus.WARN);
        return modelId;
    }

    /**
     * 按成功快照中的域ID批量补齐当前展示名称，避免逐模型查询或读取草稿归属。
     */
    private void fillDomainNames(List<ModelPublishedRespDTO> models) {
        Set<Long> projectIds = models.stream().map(ModelPublishedRespDTO::getProjectId)
                .filter(id -> id != null && id > 0).collect(Collectors.toSet());
        if (projectIds.isEmpty()) return;
        Map<Long, String> names = new HashMap<>();
        for (ModelProjectDO project : projectMapper.selectBatchIds(projectIds)) {
            if (!Boolean.TRUE.equals(project.getDelFlag())) names.put(project.getId(), project.getName());
        }
        for (ModelPublishedRespDTO model : models) model.setDomainName(names.get(model.getProjectId()));
    }

    /**
     * 基础、规则和物化详情各自只定位一次成功实例。
     */
    private ModelInstanceDO requirePublishedInstance(Long modelId, boolean includeMaterialization) {
        validateModelId(modelId);
        return requireAvailable(instanceMapper.selectPublishedInstance(modelId, includeMaterialization));
    }

    private void validateModelId(Long modelId) {
        if (modelId == null || modelId <= 0) {
            throw new ServiceException("请提供有效的主模型ID", HttpStatus.WARN);
        }
    }

    private ModelInstanceDO requireAvailable(ModelInstanceDO instance) {
        if (instance == null) {
            throw new ServiceException("主数据模型不存在或未发布。", HttpStatus.WARN);
        }
        return instance;
    }

    /**
     * 统一核对成功配置格式、根模型及版本归属，不使用草稿补齐损坏内容。
     */
    private ModelPublishedSnapshotData parsePublishedSnapshot(ModelInstanceDO instance) {
        try {
            ModelPublishSnapshot snapshot = JSON.parseObject(instance.getModelSnapshot(), ModelPublishSnapshot.class);
            if (snapshot == null || !Integer.valueOf(1).equals(snapshot.getFormatVersion())
                    || !Objects.equals(instance.getModelId(), snapshot.getModelId())
                    || !Objects.equals(instance.getModelVersionId(), snapshot.getModelVersionId())
                    || snapshot.getModel() == null || instance.getPublishedTime() == null) {
                throw new IllegalArgumentException();
            }
            // 沿用项目BeanUtils转为实体，字符串"1"、"0"会按布尔值处理，也兼容快照中带毫秒的时间。
            ModelBaseDO model = BeanUtils.toBean(snapshot.getModel(), ModelBaseDO.class);
            ModelVersionDO version = model.getVersion();
            // 主模型原有配置可能用NULL或0表示没有父模型，两种都可以；不改写保存的成功快照。
            if (!Objects.equals(instance.getModelId(), model.getId())
                    || Boolean.TRUE.equals(model.getDelFlag()) || (model.getParentId() != null && model.getParentId() != 0L)
                    || version == null || !Objects.equals(instance.getModelVersionId(), version.getId())
                    || !Objects.equals(instance.getModelId(), version.getModelId())
                    || empty(model.getCode()) || empty(model.getName()) || empty(version.getVersionNo())) {
                throw new IllegalArgumentException();
            }
            ModelPublishedSnapshotData result = new ModelPublishedSnapshotData();
            result.snapshot = snapshot;
            result.model = model;
            return result;
        } catch (RuntimeException ex) {
            throw new ServiceException("成功发布配置不完整，无法读取模型信息：" + instance.getModelId());
        }
    }

    /**
     * 批量查询隔离单条快照、连接配置的异常，连接配置的数据库查询错误仍向上抛出。
     */
    private ModelPublishedRespDTO batchPublishedInfo(ModelInstanceDO instance) {
        ModelBaseDO model;
        JSONObject datasource;
        try {
            model = parsePublishedSnapshot(instance).model;
            datasource = currentDatasourceRow(instance.getDatasourceId());
        } catch (ServiceException ex) {
            log.warn("跳过不可用的已发布模型，modelId={}, instanceId={}, reason={}",
                    instance.getModelId(), instance.getId(), ex.getMessage());
            return null;
        }
        try {
            return publishedInfo(instance, model, datasourceProperty(datasource));
        } catch (RuntimeException ex) {
            // 驱动配置异常可能包含连接参数，仅记录安全的失败原因。
            log.warn("跳过数据连接配置无法解析的已发布模型，modelId={}, instanceId={}, errorType={}",
                    instance.getModelId(), instance.getId(), ex.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 明确映射允许返回的基本属性，不把完整配置JSON、字段或连接信息直接向外返回。
     */
    private ModelPublishedRespDTO publishedInfo(ModelInstanceDO instance) {
        return publishedInfo(instance, parsePublishedSnapshot(instance).model);
    }

    /**
     * 使用已经解析好的模型组装基本信息，同一次查询不用重复解析成功快照。
     */
    private ModelPublishedRespDTO publishedInfo(ModelInstanceDO instance, ModelBaseDO model) {
        return publishedInfo(instance, model, currentDatasource(instance.getDatasourceId()));
    }

    private ModelPublishedRespDTO publishedInfo(ModelInstanceDO instance, ModelBaseDO model, DbQueryProperty property) {
        ModelPublishedRespDTO result = new ModelPublishedRespDTO();
        result.setId(instance.getModelId());
        result.setCode(model.getCode());
        result.setName(model.getName());
        result.setProjectId(model.getProjectId());
        result.setLayerId(model.getLayerId());
        result.setType(model.getType());
        result.setDescription(model.getDescription());
        result.setInstanceId(instance.getId());
        result.setVersionId(instance.getModelVersionId());
        result.setVersionNo(model.getVersion().getVersionNo());
        result.setPublishedTime(instance.getPublishedTime());
        result.setAuthorityDatasourceId(instance.getDatasourceId());
        result.setAuthoritySchemaName(DialectFactory.getDialect(DbType.getDbType(property.getDbType())).getSchemaName(property));
        result.setAuthorityTableName(instance.getAuthorityTableName());
        return result;
    }

    /**
     * 仅供本类查询使用：完整快照保留引用配置，实体负责主子模型、版本和字段的类型转换。
     */
    private static class ModelPublishedSnapshotData {
        /**
         * 原始成功快照，不改写其中未映射到实体的属性。
         */
        private ModelPublishSnapshot snapshot;
        /**
         * 本次从快照解析出的模型，包含版本、字段和子模型。
         */
        private ModelBaseDO model;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublishedInstance(Long modelId, Long instanceId, Long versionId) {
        return instanceMapper.countPublishedInstance(modelId, instanceId, versionId) == 1;
    }

    /**
     * 一次读取同一成功实例的主子字段及引用配置，停用只影响返回内容，不修改历史JSON。
     */
    @Override
    @Transactional(readOnly = true)
    public ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId) {
        return getPublishedModelStructure(modelId, false);
    }

    /**
     * 与结构查询共用同一次快照，按需补齐权威主表及列绑定。
     */
    @Override
    @Transactional(readOnly = true)
    public ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId, boolean includeAuthorityTable) {
        validateModelId(modelId);
        ModelInstanceDO instance = requireAvailable(
                instanceMapper.selectPublishedStructure(modelId, includeAuthorityTable));
        ModelPublishedStructureRespDTO result = new ModelPublishedStructureRespDTO();
        try {
            ModelPublishedSnapshotData data = parsePublishedSnapshot(instance);
            ModelPublishSnapshot snapshot = data.snapshot;
            ModelBaseDO root = data.model;
            // 先核对原JSON列表的格式，避免解析实体时把缺少的列表当成空列表。
            publishedObjects(snapshot.getModel(), "fields");
            List<JSONObject> children = publishedObjects(snapshot.getModel(), "subModels");
            Set<Long> referenceIds = new LinkedHashSet<>();
            Set<Long> encodingVersionIds = new LinkedHashSet<>();
            result.setModel(publishedInfo(instance, root));
            result.setModelId(modelId);
            result.setInstanceId(instance.getId());
            result.setVersionId(instance.getModelVersionId());
            result.setVersionNo(root.getVersion().getVersionNo());
            result.setFields(publishedFields(root, referenceIds, encodingVersionIds));
            Set<Long> nodeIds = new LinkedHashSet<>();
            nodeIds.add(modelId);
            for (int i = 0; i < root.getSubModels().size(); i++) {
                ModelBaseDO child = root.getSubModels().get(i);
                ModelVersionDO version = child.getVersion();
                Long status = version == null ? null : version.getStatus();
                // 删除、封存的子模型不返回，沿用原有状态含义。
                if (Boolean.TRUE.equals(child.getDelFlag()) || Long.valueOf(3).equals(status) || Long.valueOf(4).equals(status)) {
                    continue;
                }
                Long childId = child.getId();
                if (childId == null || !modelId.equals(child.getParentId()) || !nodeIds.add(childId)
                        || empty(child.getCode()) || empty(child.getName())
                        || version == null
                        || !instance.getModelVersionId().equals(version.getId())) {
                    throw new IllegalArgumentException();
                }
                ModelPublishedSubmodelRespDTO submodel = new ModelPublishedSubmodelRespDTO();
                submodel.setId(childId);
                submodel.setParentId(modelId);
                submodel.setCode(child.getCode());
                submodel.setName(child.getName());
                submodel.setType(child.getType());
                submodel.setDescription(child.getDescription());
                publishedObjects(children.get(i), "fields");
                submodel.setFields(publishedFields(child, referenceIds, encodingVersionIds));
                result.getSubModels().add(submodel);
            }
            fillReferences(snapshot, result, referenceIds, encodingVersionIds);
        } catch (RuntimeException ex) {
            throw new ServiceException("成功发布字段或引用配置不完整，无法读取模型信息：" + modelId);
        }
        if (includeAuthorityTable) fillAuthorityBinding(instance, result);
        return result;
    }

    /**
     * 只读取启用权威主表的列绑定，不加载完整物化计划、索引或其他角色表。
     */
    private void fillAuthorityBinding(ModelInstanceDO instance, ModelPublishedStructureRespDTO result) {
        if (instance.getAuthorityTableId() == null || empty(instance.getAuthorityTableName())) {
            throw new ServiceException("成功发布的权威主表不存在");
        }
        List<ModelMaterializedColumnDO> columns = materializedColumnMapper.selectList(
                new LambdaQueryWrapper<ModelMaterializedColumnDO>()
                        .select(ModelMaterializedColumnDO::getModelFieldId, ModelMaterializedColumnDO::getSystemFieldKey,
                                ModelMaterializedColumnDO::getColumnName, ModelMaterializedColumnDO::getSystemFlag,
                                ModelMaterializedColumnDO::getEnabledFlag, ModelMaterializedColumnDO::getValidFlag,
                                ModelMaterializedColumnDO::getDelFlag, ModelMaterializedColumnDO::getColumnPosition,
                                ModelMaterializedColumnDO::getLogicalType, ModelMaterializedColumnDO::getNullable,
                                ModelMaterializedColumnDO::getColumnComment)
                        .eq(ModelMaterializedColumnDO::getMaterializedTableId, instance.getAuthorityTableId())
                        .eq(ModelMaterializedColumnDO::getEnabledFlag, true)
                        .eq(ModelMaterializedColumnDO::getValidFlag, true));
        result.setAuthorityColumns(BeanUtils.toBean(columns, ModelMaterializedColumnRespDTO.class));
    }

    /**
     * 只保留正常字段，并收集它们实际使用的字典及编码版本，避免带出停用字段的引用。
     */
    private List<ModelPublishedColumnRespDTO> publishedFields(ModelBaseDO model, Set<Long> referenceIds, Set<Long> encodingVersionIds) {
        List<ModelPublishedColumnRespDTO> fields = new ArrayList<>();
        Set<Long> fieldIds = new LinkedHashSet<>();
        for (ModelColumnDO field : model.getFields()) {
            if (Boolean.TRUE.equals(field.getDelFlag())) {
                continue;
            }
            Long fieldId = field.getId();
            if (fieldId == null || !Objects.equals(model.getId(), field.getModelId())
                    || !fieldIds.add(fieldId) || empty(field.getCode()) || empty(field.getName())) {
                throw new IllegalArgumentException();
            }
            // 转为明确的字段DTO，未声明的内部配置不会作为任意JSON向外返回。
            ModelPublishedColumnRespDTO column = BeanUtils.toBean(field, ModelPublishedColumnRespDTO.class);
            fields.add(column);
            if (column.getDictTypeId() != null) {
                referenceIds.add(column.getDictTypeId());
            }
            if (column.getEncodingVersionId() != null) {
                encodingVersionIds.add(column.getEncodingVersionId());
            }
        }
        return fields;
    }

    /**
     * 引用内容从同一成功配置按ID组装；多个字段引用同一对象只返回一份。
     */
    private void fillReferences(ModelPublishSnapshot snapshot, ModelPublishedStructureRespDTO result,
                                Set<Long> referenceIds, Set<Long> encodingVersionIds) {
        List<JSONObject> types = referenced(snapshot.getReferenceData(), "types", referenceIds);
        result.setReferenceTypes(types.stream().map(row -> BeanUtils.toBean(row, ModelDictTypeRespDTO.class)).collect(Collectors.toList()));
        result.setReferenceItems(relatedItems(snapshot.getReferenceData(), "items", "typeId", referenceIds).stream()
                .map(row -> BeanUtils.toBean(row, ModelDictDataRespDTO.class)).collect(Collectors.toList()));
        List<JSONObject> versions = referenced(snapshot.getEncodingSchemes(), "versions", encodingVersionIds);
        Set<Long> schemeIds = new LinkedHashSet<>();
        for (JSONObject version : versions) {
            if (!Integer.valueOf(3).equals(version.getInteger("status")) || version.getLong("encodingId") == null) {
                throw new IllegalArgumentException();
            }
            schemeIds.add(version.getLong("encodingId"));
        }
        result.setEncodingVersions(versions.stream().map(row -> BeanUtils.toBean(row, ModelEncodingVersionRespDTO.class)).collect(Collectors.toList()));
        result.setEncodingSchemes(referenced(snapshot.getEncodingSchemes(), "schemes", schemeIds).stream()
                .map(row -> BeanUtils.toBean(row, ModelEncodingRespDTO.class)).collect(Collectors.toList()));
        result.setEncodingItems(relatedItems(snapshot.getEncodingSchemes(), "items", "encodingVersionId", encodingVersionIds).stream()
                .map(row -> BeanUtils.toBean(row, ModelEncodingItemRespDTO.class)).collect(Collectors.toList()));
    }

    /**
     * 字段实际引用的对象必须存在且可用；缺少或重复都报错，不去当前字典或编码表补查。
     */
    private List<JSONObject> referenced(JSONObject section, String key, Set<Long> ids) {
        Map<Long, JSONObject> found = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (JSONObject row : publishedObjects(section, key)) {
                Long id = row.getLong("id");
                if (ids.contains(id) && (!publishedRowAvailable(row) || found.putIfAbsent(id, row) != null)) {
                    throw new IllegalArgumentException();
                }
            }
            if (!found.keySet().containsAll(ids)) {
                throw new IllegalArgumentException();
            }
        }
        return new ArrayList<>(found.values());
    }

    /**
     * 只返回选中集合或版本下面正常使用的数据项，不返回无关对象或停用项。
     */
    private List<JSONObject> relatedItems(JSONObject section, String key, String ownerKey, Set<Long> ownerIds) {
        List<JSONObject> result = new ArrayList<>();
        Set<Long> itemIds = new LinkedHashSet<>();
        if (!ownerIds.isEmpty()) {
            for (JSONObject row : publishedObjects(section, key)) {
                if (ownerIds.contains(row.getLong(ownerKey)) && publishedRowAvailable(row)) {
                    if (row.getLong("id") == null || !itemIds.add(row.getLong("id"))) {
                        throw new IllegalArgumentException();
                    }
                    result.add(row);
                }
            }
        }
        return result;
    }

    /**
     * 沿用发布配置中的可用口径：未删除，且没有明确标记为无效。
     */
    private boolean publishedRowAvailable(JSONObject row) {
        return !row.getBooleanValue("delFlag") && !Boolean.FALSE.equals(row.getBoolean("validFlag"));
    }

    /**
     * 必需的列表必须是对象数组，格式损坏时不能静默当成没有字段或没有引用。
     */
    private List<JSONObject> publishedObjects(JSONObject section, String key) {
        if (section == null || !(section.get(key) instanceof JSONArray)) {
            throw new IllegalArgumentException();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object value : section.getJSONArray(key)) {
            if (!(value instanceof JSONObject)) {
                throw new IllegalArgumentException();
            }
            result.add((JSONObject) value);
        }
        return result;
    }

    /**
     * 查询成功物化记录及原目标描述；保留物理上仍存在的停用表列，不生成或执行任何DDL。
     */
    @Override
    @Transactional(readOnly = true)
    public ModelPublishedMaterializationRespDTO getPublishedModelMaterialization(Long modelId) {
        ModelInstanceDO instance = requirePublishedInstance(modelId, true);
        try {
            DbQueryProperty property = currentDatasource(instance.getDatasourceId());
            ModelPublishedMaterializationRespDTO result = BeanUtils.toBean(
                    publishedInfo(instance, parsePublishedSnapshot(instance).model, property), ModelPublishedMaterializationRespDTO.class);
            if (!Integer.valueOf(1).equals(instance.getStructureFormatVersion())
                    || instance.getDatasourceId() == null) {
                throw new IllegalArgumentException();
            }
            // 发布记录只确定数据源ID和物化结构，连接描述始终读取当前数据连接配置。
            result.setDatasourceId(instance.getDatasourceId());
            result.setDatabaseType(property.getDbType());
            result.setDatabaseName(property.getDbName());
            result.setSchemaName(result.getAuthoritySchemaName());
            result.setTables(materializedTables(instance));
            return result;
        } catch (RuntimeException ex) {
            throw new ServiceException("成功物化记录不完整，无法读取模型信息：" + modelId);
        }
    }

    /**
     * 按成功实例分组字段并核对主子、角色和配对，避免把历史实例或其他目标的记录当作当前绑定。
     */
    private List<ModelPublishedMaterializedTableRespDTO> materializedTables(ModelInstanceDO instance) {
        List<ModelMaterializedTableDO> tables = materializedTableMapper.selectPublishedTables(instance.getId());
        Map<Long, ModelPublishedMaterializedTableRespDTO> byId = new LinkedHashMap<>();
        Set<String> identities = new HashSet<>();
        Set<String> tableNames = new HashSet<>();
        for (ModelMaterializedTableDO table : tables) {
            if (table.getId() == null || table.getModelId() == null || table.getParentId() == null
                    || !instance.getId().equals(table.getInstanceId()) || !instance.getDatasourceId().equals(table.getDatasourceId())
                    || !Boolean.TRUE.equals(table.getValidFlag()) || !Boolean.FALSE.equals(table.getDelFlag())
                    || table.getEnabledFlag() == null || empty(table.getTableName()) || !tableNames.add(table.getTableName())) {
                throw new IllegalArgumentException();
            }
            ModelTableRole role = ModelTableRole.valueOf(table.getTableRole());
            if (!identities.add(table.getModelId() + ":" + role.name())) {
                throw new IllegalArgumentException();
            }
            ModelPublishedMaterializedTableRespDTO result = BeanUtils.toBean(table, ModelPublishedMaterializedTableRespDTO.class);
            List<ModelMaterializedIndexDefinition> indexes = JSON.parseArray(table.getIndexDefinition(), ModelMaterializedIndexDefinition.class);
            if (indexes == null || indexes.stream().anyMatch(Objects::isNull) || byId.putIfAbsent(table.getId(), result) != null) {
                throw new IllegalArgumentException();
            }
            // 用明确的索引定义重新输出JSON，未知内部属性不跟随保存文本直接暴露。
            result.setIndexDefinition(JSON.toJSONString(indexes));
        }
        if (!identities.contains(instance.getModelId() + ":SOURCE") || !identities.contains(instance.getModelId() + ":AUTHORITY")) {
            throw new IllegalArgumentException();
        }
        for (ModelPublishedMaterializedTableRespDTO table : byId.values()) {
            if (!identities.contains(table.getModelId() + ":SOURCE") || !identities.contains(table.getModelId() + ":AUTHORITY")) {
                throw new IllegalArgumentException();
            }
            if (Long.valueOf(0).equals(table.getParentId())) {
                if (!instance.getModelId().equals(table.getModelId()) || table.getParentTableId() != null) {
                    throw new IllegalArgumentException();
                }
            } else {
                ModelPublishedMaterializedTableRespDTO parent = byId.get(table.getParentTableId());
                if (!instance.getModelId().equals(table.getParentId()) || parent == null || !Long.valueOf(0).equals(parent.getParentId())
                        || !instance.getModelId().equals(parent.getModelId()) || !table.getTableRole().equals(parent.getTableRole())) {
                    throw new IllegalArgumentException();
                }
            }
        }
        // 已经确认至少有来源、权威主表，表ID集合不会为空；所有字段只批量查一次。
        Map<Long, Set<String>> columnNames = new LinkedHashMap<>();
        for (ModelMaterializedColumnDO column : materializedColumnMapper.selectPublishedColumns(byId.keySet())) {
            ModelPublishedMaterializedTableRespDTO table = byId.get(column.getMaterializedTableId());
            if (table == null || !Boolean.TRUE.equals(column.getValidFlag()) || !Boolean.FALSE.equals(column.getDelFlag())
                    || column.getEnabledFlag() == null || empty(column.getColumnName()) || empty(column.getPhysicalType())
                    || !columnNames.computeIfAbsent(table.getId(), key -> new HashSet<>()).add(column.getColumnName())) {
                throw new IllegalArgumentException();
            }
            table.getColumns().add(BeanUtils.toBean(column, ModelMaterializedColumnRespDTO.class));
        }
        if (byId.values().stream().anyMatch(table -> table.getColumns().isEmpty())) {
            throw new IllegalArgumentException();
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * 按指定类别返回本主模型的已发布规则，不使用当前规则表覆盖历史配置。
     */
    @Override
    @Transactional(readOnly = true)
    public ModelPublishedRulesRespDTO getPublishedModelRules(Long modelId, String ruleCategory) {
        String category = ruleCategory == null ? "" : ruleCategory.trim().toUpperCase(Locale.ROOT);
        if (!"MATCH".equals(category) && !"SURVIVOR".equals(category) && !"QUALITY".equals(category)) {
            throw new ServiceException("规则类别应为MATCH、SURVIVOR或QUALITY", HttpStatus.WARN);
        }
        ModelInstanceDO instance = requirePublishedInstance(modelId, false);
        try {
            ModelPublishedSnapshotData data = parsePublishedSnapshot(instance);
            ModelPublishSnapshot snapshot = data.snapshot;
            ModelPublishedRulesRespDTO result = BeanUtils.toBean(publishedInfo(instance, data.model), ModelPublishedRulesRespDTO.class);
            result.setRuleCategory(category);
            Set<Long> ids = new HashSet<>();
            for (JSONObject rule : publishedObjects(snapshot.getRules(), category.toLowerCase(Locale.ROOT))) {
                Long ownerId = rule.getLong("modelId");
                if (ownerId == null) {
                    throw new IllegalArgumentException();
                }
                // 发布配置同时保存子模型规则；此接口按输入主模型ID精确查询，不把子模型规则混进来。
                if (!modelId.equals(ownerId) || !publishedRowAvailable(rule)) {
                    continue;
                }
                if (rule.getLong("id") == null || !ids.add(rule.getLong("id"))) {
                    throw new IllegalArgumentException();
                }
                result.getRules().add(BeanUtils.toBean(rule, ModelPublishedRuleRespDTO.class));
            }
            return result;
        } catch (RuntimeException ex) {
            throw new ServiceException("成功发布规则配置不完整，无法读取模型信息：" + modelId);
        }
    }

    /**
     * 连接配置可修改，成功发布信息也按数据源ID读取当前配置，不回退到发布快照。
     */
    private DbQueryProperty currentDatasource(Long datasourceId) {
        return datasourceProperty(currentDatasourceRow(datasourceId));
    }

    private JSONObject currentDatasourceRow(Long datasourceId) {
        if (datasourceId == null || datasourceId <= 0) throw new ServiceException("发布数据源ID无效", HttpStatus.WARN);
        List<JSONObject> rows = ModelPublishUtils.configObjects(configMapper.selectPublishDatasource(datasourceId));
        if (rows.size() != 1 || !datasourceId.equals(rows.get(0).getLong("id"))
                || rows.get(0).getBooleanValue("delFlag")) {
            throw new ServiceException("发布模型的数据连接不存在或已删除", HttpStatus.WARN);
        }
        return rows.get(0);
    }

    private DbQueryProperty datasourceProperty(JSONObject row) {
        return new DbQueryProperty(row.getString("datasourceType"), row.getString("ip"),
                row.getLong("port"), row.getString("datasourceConfig"));
    }

    /**
     * 空白查询条件与未传条件采用相同处理。
     */
    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
