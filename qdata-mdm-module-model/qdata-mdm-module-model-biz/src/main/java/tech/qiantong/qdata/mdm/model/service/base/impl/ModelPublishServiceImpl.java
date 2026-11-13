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

package tech.qiantong.qdata.mdm.model.service.base.impl;

import com.alibaba.druid.sql.parser.Keywords;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbIndex;
import tech.qiantong.qdata.mdm.common.database.datasource.DefaultDataSourceFactoryBean;
import tech.qiantong.qdata.mdm.common.database.dialect.MySqlDialect;
import tech.qiantong.qdata.mdm.common.database.dialect.SQLServer2008Dialect;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.base.dto.*;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelTreeRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.*;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.*;
import tech.qiantong.qdata.mdm.model.dal.mapper.layer.ModelLayerMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.project.ModelProjectMapper;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictDataRespDTO;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictTypeRespDTO;
import tech.qiantong.qdata.mdm.model.domain.*;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingItemRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingVersionRespDTO;
import tech.qiantong.qdata.mdm.model.enums.ModelColumnCategoryEnum;
import tech.qiantong.qdata.mdm.model.enums.materialization.*;
import tech.qiantong.qdata.mdm.model.service.base.IModelDdlPlanService;
import tech.qiantong.qdata.mdm.model.service.base.IModelPublishService;
import tech.qiantong.qdata.mdm.model.util.ModelMaterializationDbUtils;
import tech.qiantong.qdata.mdm.model.util.ModelPublishDatabaseSession;
import tech.qiantong.qdata.mdm.model.util.ModelPublishDatabaseSession.ModelPublishTargetResourceScope;
import tech.qiantong.qdata.mdm.model.util.ModelPublishTimingUtils;
import tech.qiantong.qdata.mdm.model.util.ModelPublishUtils;
import tech.qiantong.qdata.mdm.rule.api.rule.dto.RuleModelRulesRespDTO;
import tech.qiantong.qdata.mdm.rule.api.service.rule.RuleModelApi;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 统一处理模型发布及成功信息查询，保留各步骤原有的校验和事务边界。
 */
@Slf4j
@Service
public class ModelPublishServiceImpl implements IModelPublishService {

    /**
     * 读取和保存发布实例；成功模型查询通过成功指针定位同一份发布内容。
     */
    @Resource
    private ModelInstanceMapper instanceMapper;

    /**
     * 按发布实例读取和保存来源、权威主子表的真实物化记录。
     */
    @Resource
    private ModelMaterializedTableMapper materializedTableMapper;

    /**
     * 批量读取和保存物化字段，记录已经核验的实际列结构。
     */
    @Resource
    private ModelMaterializedColumnMapper materializedColumnMapper;

    /**
     * 查询已发布模型所属的数据域，组装模型树。
     */
    @Resource
    private ModelProjectMapper projectMapper;

    /**
     * 查询模型目录及父子关系，只保留含成功模型的目录。
     */
    @Resource
    private ModelLayerMapper layerMapper;

    /**
     * 查询模型主体，包括当前版本ID和上次成功发布记录ID。
     */
    @Resource
    private ModelBaseMapper modelMapper;

    /**
     * 查询主模型当前版本，判断它是草稿、修订中、已发布还是封存。
     */
    @Resource
    private ModelVersionMapper versionMapper;

    /**
     * 复用已有计划、执行、恢复、补偿和正式保存能力，不在入口重复生成或拼接SQL。
     */
    @Resource
    @Lazy
    private IModelDdlPlanService ddlPlanService;

    /**
     * 查询模型、字段、参考数据、编码方案和规则的原始配置，保留逻辑删除标记。
     */
    @Resource
    private ModelPublishConfigMapper configMapper;

    /**
     * 规则由rule模块按自身关联关系读取，model只负责保存本次发布配置。
     */
    @Resource
    @Lazy
    private RuleModelApi ruleModelApi;

    /**
     * 保存每个发布实例唯一的全流程日志，按事件序号追加内容并更新原行。
     */
    @Resource
    private ModelInstanceLogMapper logMapper;

    /**
     * 平台允许的逻辑类型，真实数据库类型留给common方言转换。
     */
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "STRING", "TEXT", "INTEGER", "DECIMAL", "DATE", "DATETIME", "BOOLEAN"));

    /**
     * 经过Spring代理调用本服务中的独立事务方法，保证发布失败后过程记录不会被一起回滚。
     */
    @Resource
    @Lazy
    private IModelPublishService modelPublishService;

    /**
     * 每批最多保存500条字段记录，减少逐条提交给数据库的次数，也避免一次堆积过多记录。
     */
    private static final int MATERIALIZED_COLUMN_BATCH_SIZE = 500;

    // 成功模型查询

    /**
     * 所属域、目录和模型信息按成功快照分组；目录统一放在域的第二层。
     */
    @Transactional(readOnly = true)
    @Override
    public List<ModelTreeRespVO> getPublishedModelTree() {
        List<ModelPublishedRespDTO> models = new ArrayList<>();
        readPublished(null, instance -> models.add(publishedInfo(instance)));
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
    @Transactional(readOnly = true)
    @Override
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
        List<ModelPublishedRespDTO> rows = instances.stream()
                .map(instance -> {
                    ModelPublishedRespDTO model = publishedInfo(instance);
                    model.setAuthorityStatisticsColumns(statisticsColumns.get(instance.getAuthorityTableId()));
                    return model;
                }).collect(Collectors.toList());
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
    @Transactional(readOnly = true)
    @Override
    public ModelPublishedRespDTO getPublishedModel(Long modelId) {
        ModelPublishedRespDTO model = publishedInfo(requirePublishedInstance(modelId));
        fillDomainNames(Collections.singletonList(model));
        return model;
    }

    @Transactional(readOnly = true)
    @Override
    public Long getPublishedModelId(Long instanceId) {
        if (instanceId == null || instanceId <= 0) throw new ServiceException("模型实例ID格式无效");
        Long modelId = instanceMapper.selectPublishedModelId(instanceId);
        if (modelId == null) throw new ServiceException("当前实例不可用");
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
     * 基础、字段查询都从同一次指针查询取原实例，不先查基础信息再重新查询指针。
     */
    private ModelInstanceDO requirePublishedInstance(Long modelId) {
        return requirePublishedInstance(modelId, false);
    }

    /**
     * 物化信息查询额外读取结构格式版本，不读取历史连接配置。
     */
    private ModelInstanceDO requirePublishedInstance(Long modelId, boolean includeMaterialization) {
        if (modelId == null || modelId <= 0) {
            throw new ServiceException("请提供有效的主模型ID");
        }
        List<ModelInstanceDO> instances = new ArrayList<>();
        readPublished(modelId, includeMaterialization, instances::add);
        if (instances.size() != 1) {
            throw new ServiceException("主数据模型不存在或未发布。");
        }
        return instances.get(0);
    }

    /**
     * 在Mapper连接关闭前逐条处理配置；配置错误先停止读取，再在查询外抛出业务提示，
     * 避免MyBatis把配置错误包装成数据库访问错误。
     */
    private void readPublished(Long modelId, Consumer<ModelInstanceDO> consumer) {
        readPublished(modelId, false, consumer);
    }

    /**
     * 同一次SQL选定成功指针和返回内容，连接信息另按数据源ID读取当前配置。
     */
    private void readPublished(Long modelId, boolean includeMaterialization, Consumer<ModelInstanceDO> consumer) {
        ServiceException[] failure = {null};
        instanceMapper.selectPublishedInstances(modelId, includeMaterialization, result -> {
            try {
                consumer.accept(result.getResultObject());
            } catch (ServiceException ex) {
                failure[0] = ex;
                result.stop();
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    /**
     * 统一核对成功配置格式、根模型及版本归属，不使用草稿补齐损坏内容。
     */
    private ModelPublishedSnapshotData publishedSnapshot(ModelInstanceDO instance) {
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
     * 明确映射允许返回的基本属性，不把完整配置JSON、字段或连接信息直接向外返回。
     */
    private ModelPublishedRespDTO publishedInfo(ModelInstanceDO instance) {
        return publishedInfo(instance, publishedSnapshot(instance).model);
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

    @Transactional(readOnly = true)
    @Override
    public boolean isPublishedInstance(Long modelId, Long instanceId, Long versionId) {
        return instanceMapper.countPublishedInstance(modelId, instanceId, versionId) == 1;
    }

    /**
     * 一次读取同一成功实例的主子字段及引用配置，停用只影响返回内容，不修改历史JSON。
     */
    @Transactional(readOnly = true)
    @Override
    public ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId) {
        return getPublishedModelStructure(modelId, false);
    }

    /**
     * 与结构查询共用同一次快照，按需补齐权威主表及列绑定。
     */
    @Transactional(readOnly = true)
    @Override
    public ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId, boolean includeAuthorityTable) {
        if (modelId == null || modelId <= 0) throw new ServiceException("请提供有效的主模型ID");
        ModelInstanceDO instance = instanceMapper.selectPublishedStructure(modelId, includeAuthorityTable);
        if (instance == null) throw new ServiceException("主数据模型不存在或未发布。");
        ModelPublishedStructureRespDTO result = new ModelPublishedStructureRespDTO();
        try {
            ModelPublishedSnapshotData data = publishedSnapshot(instance);
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
    @Transactional(readOnly = true)
    @Override
    public ModelPublishedMaterializationRespDTO getPublishedModelMaterialization(Long modelId) {
        ModelInstanceDO instance = requirePublishedInstance(modelId, true);
        try {
            DbQueryProperty property = currentDatasource(instance.getDatasourceId());
            ModelPublishedMaterializationRespDTO result = BeanUtils.toBean(
                    publishedInfo(instance, publishedSnapshot(instance).model, property), ModelPublishedMaterializationRespDTO.class);
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
    @Transactional(readOnly = true)
    @Override
    public ModelPublishedRulesRespDTO getPublishedModelRules(Long modelId, String ruleCategory) {
        String category = ruleCategory == null ? "" : ruleCategory.trim().toUpperCase(Locale.ROOT);
        if (!"MATCH".equals(category) && !"SURVIVOR".equals(category) && !"QUALITY".equals(category)) {
            throw new ServiceException("规则类别应为MATCH、SURVIVOR或QUALITY");
        }
        ModelInstanceDO instance = requirePublishedInstance(modelId);
        try {
            ModelPublishedSnapshotData data = publishedSnapshot(instance);
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
     * 空白查询条件与未传条件采用相同处理。
     */
    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 整体发布和恢复

    /**
     * 统一发布入口；只有全部物理核验和正式保存成功才返回，失败始终使用原实例记录。
     * <p>
     * 主调用链：prepare读取并保存配置 → preparePlan构建/比较/校验并保存SQL → executePlan执行和核验 → completePublish正式生效。
     * 未完成实例走resumePublication恢复；执行失败走compensateFailure；正式保存失败只补保存。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelPublishContext publish(Long modelId, Long datasourceId, Long userId, String userName) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.begin(modelId, datasourceId);
        // 作用域只属于本次同步调用；任意返回或异常都会统一释放目标连接并清理线程引用。
        try (ModelPublishLogScope logs = new ModelPublishLogScope();
             ModelPublishTargetResourceScope resources = ModelPublishTargetResourceScope.begin()) {
            // 此入口不包平台长事务：业务库DDL可能耗时或隐式提交，平台过程记录由各步骤的独立短事务保存。
            // 发布顺序：处理旧实例 → 读取本次配置 → 校验并生成计划 → 逐条执行及核验 → 保存正式结果。

            // 第1步：先查上一次是否还没处理完，避免重复点击后另起一份SQL计划。
            ModelInstanceDO pending = findUnfinishedInstance(modelId);
            if (pending != null) {
                // 旧实例没有处理完时，本次点击只处理它，不拿当前草稿生成另一份计划。
                return resumePublication(modelId, datasourceId, pending);
            }
            // 第2步：登记发布实例，保存点击发布时的完整配置，并读取上一次成功结构。
            ModelPublishContext context = prepare(modelId, datasourceId, userId, userName);
            // 当前版本已经整体成功时直接复用结果，不重复建表，也不再生成发布实例。
            if (context.getStatus() == ModelPublishStatus.SUCCEEDED) {
                return context;
            }
            try {
                // preparePlan内部依次执行CONFIG、BUILD、DIFF、CHANGE、TARGET及计划保存。
                // 第3步：得到已保存的完整SQL计划；此时只做准备，尚未向业务库发送DDL。
                ddlPlanService.preparePlan(context);
            } catch (RuntimeException ex) {
                throw publishFailure(context, ex instanceof ServiceException ? ex.getMessage() : "发布配置校验或计划生成失败");
            }
            try {
                // 第4步：逐条执行SQL并暂存结果，整批结束统一核验、保存；失败立即停止并保存回滚所需结果。
                // 本次执行一旦失败就走回滚或补偿，不在这里自动恢复后继续发送。
                ddlPlanService.executePlan(context);
            } catch (RuntimeException ex) {
                // 执行服务负责保存原始证据；这里不把已经发送DDL的异常记成普通准备失败。
                log.error("模型物化执行未完成，modelId={}, instanceId={}", modelId, context.getInstanceId(), ex);
            }
            // COMMIT_PENDING表示业务库结构已经核验通过、平台正式结果尚未保存；其他状态先进入失败处理。
            if (context.getStatus() != ModelPublishStatus.COMMIT_PENDING) {
                compensateFailure(context);
                throw publishFailure(context, "物化执行未完成，请查看原实例的执行、核验和补偿日志");
            }
            // 第5步：保存真实表列信息、版本状态和成功指针；这一步完成才算整次发布成功。
            return completePublish(context);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 只查当前主模型的未完成实例；有多条时不猜哪条可以执行，也不创建新的发布。
     */
    private ModelInstanceDO findUnfinishedInstance(Long modelId) {
        // 只排除成功和已确认结束的失败；执行中、待核验、补偿未完和待保存的记录仍需处理。
        List<ModelInstanceDO> records = instanceMapper.selectList(new LambdaQueryWrapper<ModelInstanceDO>()
                .eq(ModelInstanceDO::getModelId, modelId)
                .notIn(ModelInstanceDO::getStatus, ModelPublishStatus.SUCCEEDED.name(), ModelPublishStatus.FAILED.name()));
        if (records == null) throw new ServiceException("未完成发布记录读取失败");
        if (records.size() > 1)
            throw new ServiceException("模型存在多条未完成发布记录，不能自动选择执行，请先核对实例记录");
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * 唯一发布入口转回原实例；目标仍用原数据源，恢复不读取或替换当前草稿。
     */
    private ModelPublishContext resumePublication(Long modelId, Long datasourceId, ModelInstanceDO record) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("RESUME_PUBLICATION", record);
        try {
            ModelBaseDO model = modelMapper.selectById(modelId);
            // 恢复也只能从原主模型进入，不能拿其他模型的实例继续执行。
            if (model == null || (model.getParentId() != null && model.getParentId() != 0L)
                    || !Objects.equals(modelId, record.getModelId())) {
                throw new ServiceException("必须从存在且未删除的主模型恢复发布");
            }
            if (!Objects.equals(datasourceId, record.getDatasourceId())) {
                throw new ServiceException("模型还有未完成发布，请使用原数据源处理，不能更换本次发布目标");
            }
            // 准备中断且连完整计划都没保存时，只能核对后结束本次准备，不能凭当前草稿猜原SQL。
            if (ModelPublishStatus.PREPARING.name().equals(record.getStatus()) && StringUtils.isBlank(record.getExecutionPlan())) {
                ModelPublishContext context = readInstanceContext(modelId, record.getId());
                // 正常流程必须先保存完整计划才会发送SQL。仅对仍处于准备阶段、没有执行证据的记录结束失败。
                // 已有计划摘要或SQL记录却缺计划，属于记录不完整，不能清掉状态再重新建表。
                if (context.getCurrentStage().ordinal() > ModelPublishStage.PLAN.ordinal()
                        || StringUtils.isNotBlank(record.getExecutedSql()) || StringUtils.isNotBlank(record.getStructureHash())) {
                    throw publishFailure(context, "原计划缺失但存在执行信息，保留记录，不能重新发布");
                }
                modelPublishService.failPlanning(record, context.getCurrentStage(), "准备阶段中断，尚未保存完整计划且没有执行SQL，本次发布结束失败");
                context.setStatus(ModelPublishStatus.FAILED);
                throw publishFailure(context, "原准备过程已结束失败，当前草稿保留，请重新点击发布");
            }
            // recover内部按原阶段分流：待提交只保存，补偿只继续补偿，执行中断先核验再决定是否重试。
            return recover(modelId, record.getId());
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 恢复只使用原实例。正向执行与补偿分开处理，不能把已经补偿过的计划重新从头执行。
     */
    private ModelPublishContext recover(Long modelId, Long instanceId) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("RECOVER", modelId, instanceId);
        try {
            ModelPublishContext context = readInstanceContext(modelId, instanceId);
            // 已经成功则返回原结果；待保存则补存正式结果，两种情况都不重新执行DDL。
            if (context.getStatus() == ModelPublishStatus.SUCCEEDED || context.getStatus() == ModelPublishStatus.COMMIT_PENDING) {
                return completePublish(context);
            }
            // 进入过补偿的实例只能继续处理补偿，不能再回到正向建表流程。
            if (context.getCurrentStage() == ModelPublishStage.COMPENSATE || context.getStatus() == ModelPublishStatus.COMPENSATING) {
                compensateFailure(context);
                throw publishFailure(context, context.getStatus() == ModelPublishStatus.FAILED
                        ? "原实例补偿已处理完成，本次发布仍失败，可修改草稿后重新发布"
                        : "原实例仍有补偿失败或未知效果，请查看原实例记录");
            }
            // 由现有恢复服务核对归属、原计划、执行证据和目标身份；不读取草稿，也不重新生成计划。
            context = ddlPlanService.recoverPlan(modelId, instanceId);
            context = recoverExecution(context);
            if (context.getStatus() != ModelPublishStatus.COMMIT_PENDING) {
                compensateFailure(context);
                throw publishFailure(context, "原实例恢复尚未完成，请查看执行、核验和补偿日志");
            }
            return completePublish(context);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 读取原实例的基本状态，先核对主模型归属；不从请求接收配置、SQL或结束证明。
     */
    private ModelPublishContext readInstanceContext(Long modelId, Long instanceId) {
        if (modelId == null || modelId <= 0 || instanceId == null || instanceId <= 0) {
            throw new ServiceException("主模型ID和原发布实例ID必须为正数");
        }
        ModelInstanceDO record = instanceMapper.selectById(instanceId);
        if (record == null || !modelId.equals(record.getModelId())) {
            throw new ServiceException("原发布实例不存在或不属于当前主模型");
        }
        ModelPublishContext context = new ModelPublishContext();
        context.setModelId(modelId);
        context.setInstanceId(instanceId);
        context.setModelVersionId(record.getModelVersionId());
        context.setDatasourceId(record.getDatasourceId());
        context.setBaseInstanceId(record.getBaseInstanceId());
        try {
            context.setStatus(ModelPublishStatus.valueOf(record.getStatus()));
            context.setCurrentStage(ModelPublishStage.valueOf(record.getCurrentStage()));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("原发布实例状态或阶段无法识别，请先核对实例记录");
        }
        ModelPublishTimingUtils.bind(context);
        return context;
    }

    /**
     * 发布和恢复共用正式提交及关闭日志；提交失败保持原实例状态，不调用补偿。
     */
    private ModelPublishContext completePublish(ModelPublishContext context) {
        ModelPublishContext result;
        try {
            // 把本次完整核验结果交给正式保存，避免再采集一轮；平台保存仍使用原来的独立事务。
            result = ddlPlanService.commitPlan(context);
        } catch (RuntimeException ex) {
            throw publishFailure(context, "正式结果保存未完成，请使用原实例核验后补保存");
        }
        try {
            // 正式保存已成功，先释放本次共享目标资源，再记录CLOSE；关闭失败不会反转正式成功。
            ModelPublishTargetResourceScope.closeCurrent();
            modelPublishService.saveStage(result, ModelPublishStage.CLOSE, "本次目标库资源已关闭，发布流程结束");
            result.setCurrentStage(ModelPublishStage.CLOSE);
        } catch (RuntimeException ex) {
            // 正式事务已经成功，补记关闭日志失败不能向前端谎报整次发布失败。
            log.warn("发布已成功，关闭阶段日志未保存，modelId={}, instanceId={}", context.getModelId(), result.getInstanceId(), ex);
        }
        return result;
    }

    /**
     * 仅在恢复旧实例时核对未完成尝试，不从头执行整套计划；新发布失败不走自动恢复。
     */
    private ModelPublishContext recoverExecution(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("RECOVER_EXECUTION", context);
        try {
            // 循环必须同时满足“状态需要恢复”和“旧尝试可安全重试”；每轮还会比较是否确实有进展。
            while ((context.getStatus() == ModelPublishStatus.UNKNOWN || context.getStatus() == ModelPublishStatus.PARTIAL_FAILED)
                    && canRecover(context)) {
                long before = executionProgress(context, false);
                ModelPublishContext recovered = ddlPlanService.recoverPlan(context.getModelId(), context.getInstanceId());
                // 次数和已成功步骤均无变化时，本轮无法继续推进，避免反复查询同一未决结果。
                if (executionProgress(recovered, false) <= before) {
                    return recovered;
                }
                context = recovered;
            }
            return context;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 任何旧SQL结束不明确都不自动恢复发送；全部已生效时允许补核验，临时错误按原计划额度处理。
     */
    private boolean canRecover(ModelPublishContext context) {
        if (context.getExecutionPlan() != null && context.getExecutionPlan().getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE)
            return false;
        if (context.getExecutionPlan() == null) {
            return false;
        }
        // 按执行顺序找到第一个未成功步骤；后面的SQL仍依赖它，不能越过它重试。
        for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
            if (step.getAttemptCount() > 0 && !Boolean.TRUE.equals(step.getLastAttemptCompleted())
                    && !Boolean.TRUE.equals(step.getRecoveredEffect())) {
                return false;
            }
            if (step.getStatus() != ModelSqlStepStatus.SUCCEEDED) {
                // 三个条件缺一不可：确实尝试过、错误允许重试、次数仍在原计划额度内。
                return step.getAttemptCount() > 0 && Boolean.TRUE.equals(step.getLastAttemptRetryable())
                        && step.getAttemptCount() < 1 + context.getExecutionPlan().getForwardMaxRetries();
            }
        }
        return true;
    }

    /**
     * 对失败原实例评估安全补偿；未知结果只保留记录，旧表加列及扩长等不可撤销效果继续保留。
     */
    private void compensateFailure(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("COMPENSATE_FAILURE", context);
        try {
            if (context.getExecutionPlan() != null && context.getExecutionPlan().getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE) {
                // 原业务库事务结果不能用新连接发DROP来替代；恢复服务负责只读核验提交结果。
                return;
            }
            if (context.getStatus() != ModelPublishStatus.UNKNOWN && context.getStatus() != ModelPublishStatus.PARTIAL_FAILED
                    && context.getStatus() != ModelPublishStatus.COMPENSATING) {
                return;
            }
            try {
                // 安全补偿与整批事务回滚分开：这里只评估本次新对象，旧表加列、扩长等效果按记录保留。
                ModelPublishContext result = ddlPlanService.compensatePlan(context.getModelId(), context.getInstanceId());
                while (canRetryCompensation(result)) {
                    long before = executionProgress(result, true);
                    ModelPublishContext next = ddlPlanService.compensatePlan(result.getModelId(), result.getInstanceId());
                    if (executionProgress(next, true) <= before) {
                        result = next;
                        break;
                    }
                    result = next;
                }
                context.setStatus(result.getStatus());
                context.setCurrentStage(result.getCurrentStage());
                context.setExecutionPlan(result.getExecutionPlan());
                context.setRemainingEffects(result.getRemainingEffects());
            } catch (RuntimeException ex) {
                // 补偿中断由原实例保留，不能覆盖成已清理完成，也不影响上次成功版本。
                log.error("模型发布失败处理未完成，modelId={}, instanceId={}", context.getModelId(), context.getInstanceId(), ex);
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 补偿只重试已明确结束的临时错误，次数独立于正向；任一未结束尝试都停止自动删除。
     */
    private boolean canRetryCompensation(ModelPublishContext context) {
        if (context.getStatus() != ModelPublishStatus.PARTIAL_FAILED || context.getExecutionPlan() == null) {
            return false;
        }
        // 发现任一未明确结束的正向或逆向SQL就停止；只有明确结束的临时补偿错误才有重试资格。
        boolean retryable = false;
        for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
            if ((step.getAttemptCount() > 0 && !Boolean.TRUE.equals(step.getLastAttemptCompleted())
                    && !Boolean.TRUE.equals(step.getRecoveredEffect()))
                    || (step.getCompensationAttemptCount() > 0 && !Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted()))) {
                return false;
            }
            retryable |= step.getCompensationStatus() == ModelCompensationStatus.FAILED
                    && Boolean.TRUE.equals(step.getCompensationLastAttemptRetryable())
                    && step.getCompensationAttemptCount() < 1 + context.getExecutionPlan().getCompensationMaxRetries();
        }
        return retryable;
    }

    /**
     * 用累计发送次数和成功步骤数判断是否有实际推进，不额外改变保存计划里的重试上限。
     */
    private long executionProgress(ModelPublishContext context, boolean compensation) {
        long progress = 0;
        for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
            // 次数增加或有步骤变为成功都算推进，用来防止恢复循环只查询而不前进。
            progress += compensation ? step.getCompensationAttemptCount() : step.getAttemptCount();
            if (compensation ? step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED
                    : step.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                progress++;
            }
        }
        return progress;
    }

    /**
     * 返回原实例ID和实际阶段，便于直接查看记录；不把数据库异常或连接参数拼到前端提示里。
     */
    private ServiceException publishFailure(ModelPublishContext context, String reason) {
        log.error(ModelPublishUtils.toResponse(context, reason).toString());
        return new ServiceException(reason, 500);
    }

    /**
     * 先检查当前版本和没有处理完的发布记录，再保存本次配置，并读取上次发布成功的表结构作为比较依据。
     * 同一版本已经发布成功时直接返回原结果；这里只做准备，不执行建表SQL，也不把模型改成已发布。
     *
     * <p>
     * 此方法是统一入口的读取阶段，不直接执行DDL；所有保存记录的方法均经自身Spring代理调用，保留独立事务。
     *
     * @param modelId      主模型ID；一次发布包含它的全部子模型，不能只发布一个子模型
     * @param datasourceId 用户选择的目标数据源ID，真实身份在TARGET阶段核验
     * @param userId       操作人ID
     * @param userName     操作人名称
     * @return 本次发布要用的数据，或该版本上次已经成功的发布结果
     */
    private ModelPublishContext prepare(Long modelId, Long datasourceId, Long userId, String userName) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("READ_PREPARE", modelId, null);
        try {
            ModelBaseDO model = modelMapper.selectById(modelId);
            if (model == null || (model.getParentId() != null && model.getParentId() != 0)) {
                throw new ServiceException("必须从存在且未删除的主模型整体发布");
            }
            // 只排除整体成功和已确认处理完的失败，其余记录都要先核对。
            // 例如服务重启后留下的PREPARING，也不能直接当作没发布过。
            Long unresolved = instanceMapper.selectCount(new LambdaQueryWrapper<ModelInstanceDO>()
                    // 先限定当前主模型，再排除已确认结束的SUCCEEDED/FAILED，统计剩余待处理记录。
                    .eq(ModelInstanceDO::getModelId, modelId)
                    .notIn(ModelInstanceDO::getStatus, ModelPublishStatus.SUCCEEDED.name(), ModelPublishStatus.FAILED.name()));
            if (unresolved == null || unresolved > 0) {
                throw new ServiceException("模型存在未决发布实例，请先核验恢复，不能重新发布");
            }
            // 先整理本次发布记录；BASE_INSTANCE_ID指向上次成功实例，不是本次实例自身。
            ModelInstanceDO record = new ModelInstanceDO();
            record.setModelId(modelId);
            record.setModelVersionId(model.getVersionId());
            record.setBaseInstanceId(model.getPublishedInstanceId());
            record.setDatasourceId(datasourceId);
            record.setCreatorId(userId);
            record.setCreateBy(userName);
            // 读取当前版本和PUBLISHED_INSTANCE_ID（上次成功发布记录ID），用于判断是否重复发布。
            ModelVersionDO version;
            ModelInstanceDO base;
            try {
                version = model.getVersionId() == null ? null : versionMapper.selectById(model.getVersionId());
                base = model.getPublishedInstanceId() == null ? null : instanceMapper.selectById(model.getPublishedInstanceId());
            } catch (RuntimeException ex) {
                try {
                    modelPublishService.start(record);
                    modelPublishService.failPreparation(record, "模型当前版本或成功指针读取失败");
                } catch (RuntimeException recordFailure) {
                    ex.addSuppressed(recordFailure);
                }
                throw ex;
            }
            // 只有“当前版本已发布、版本属于本模型、上次记录确实成功、两个版本ID一致”都满足，才算重复发布。
            // 这里逐项使用&&判断，前面的对象不存在时，后面不会继续访问它的属性。
            if (version != null && ModelVersionStatus.PUBLISHED.getStatus().equals(version.getStatus())
                    && modelId.equals(version.getModelId()) && validBase(modelId, base)
                    && version.getId().equals(base.getModelVersionId())) {
                if (!java.util.Objects.equals(datasourceId, base.getDatasourceId())) {
                    throw new ServiceException("已成功版本不能通过重复发布切换目标数据源");
                }
                ModelPublishContext existing = context(model, version, base.getId());
                existing.setStatus(ModelPublishStatus.SUCCEEDED);
                existing.setCurrentStage(ModelPublishStage.CLOSE);
                return existing;
            }
            // 通过自身的Spring代理开启独立事务：实例和唯一全流程日志一起保存，后续失败也保留此次记录。
            modelPublishService.start(record);
            try {
                if (version == null || !modelId.equals(version.getModelId())) {
                    throw new ServiceException("主模型当前版本不存在");
                }
                ModelVersionStatus versionStatus = ModelVersionStatus.fromStatus(version.getStatus());
                // 草稿是第一次发布，修订中是已有成功版本后再次发布；其余状态不能走新的发布流程。
                if (versionStatus != ModelVersionStatus.DRAFT && versionStatus != ModelVersionStatus.REVISING) {
                    throw new ServiceException("只有草稿或修订中的模型可以发布");
                }
                if (datasourceId == null || datasourceId <= 0) {
                    throw new ServiceException("目标数据源未指定");
                }
                // 有上次成功记录ID却查不到对应的有效记录，说明历史信息有问题，不能按首次发布重新建表。
                if (model.getPublishedInstanceId() != null && !validBase(modelId, base)) {
                    throw new ServiceException("模型元数据出错");
                }
                if (base != null && !datasourceId.equals(base.getDatasourceId())) {
                    throw new ServiceException("已发布模型不能切换目标数据源");
                }
                ModelPublishContext context = context(model, version, record.getId());
                // 通过代理开启独立只读事务，一次读取主子模型、字段及关联配置，形成这次发布的固定副本。
                ModelPublishSnapshot snapshot = modelPublishService.readCurrentConfig(modelId, version);
                context.setSnapshot(snapshot);
                context.setDatasourceId(datasourceId);
                // 先把点击发布时的配置单独保存；即使后面发现旧结构有问题，也能查到这次到底准备发布什么。
                modelPublishService.saveSnapshot(record, snapshot);

                // 修订时从上次成功记录读旧表、旧列和索引；首次发布则使用空旧结构。
                modelPublishService.readPublishedStructure(context, base);

                // 这里只表示读取阶段完成，尚未通过CONFIG、CHANGE、TARGET校验，更不代表已经发布成功。
                modelPublishService.completePreparation(record);
                return context;
            } catch (RuntimeException ex) {
                // 业务校验错误可以直接保存具体原因；其他异常只保存通用提示，详细堆栈写服务端日志。
                String reason = ex instanceof ServiceException ? ex.getMessage() : "模型发布准备失败，请核对本次配置及上次成功发布的结构";
                boolean failureSaved = false;
                try {
                    modelPublishService.failPreparation(record, reason);
                    failureSaved = true;
                } catch (RuntimeException recordFailure) {
                    // 如果失败记录没存成功，就保留原来的待处理记录。
                    // 后面必须先核对这条记录，不能直接新建一次发布。
                    ex.addSuppressed(recordFailure);
                }
                log.error("模型发布准备失败，modelId={}, instanceId={}", modelId, record.getId(), ex);
                // 已经有实例时，准备失败也返回定位信息；失败记录未保存则保留PREPARING，不伪报已结束。
                ModelPublishContext failed = new ModelPublishContext();
                failed.setModelId(modelId);
                failed.setInstanceId(record.getId());
                failed.setModelVersionId(record.getModelVersionId());
                failed.setBaseInstanceId(record.getBaseInstanceId());
                failed.setStatus(failureSaved ? ModelPublishStatus.FAILED : ModelPublishStatus.PREPARING);
                throw publishFailure(failed, reason);
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 检查上次成功发布记录是否真的存在、属于这个主模型，并且已经整体成功。
     *
     * @param modelId 根模型ID
     * @param base    PUBLISHED_INSTANCE_ID对应的发布记录
     * @return 记录存在且归属与成功状态一致时为true
     */
    private boolean validBase(Long modelId, ModelInstanceDO base) {
        // 必须同时满足：记录存在、属于当前模型、整体成功、带有明确的模型版本ID。
        return base != null && modelId.equals(base.getModelId())
                && ModelPublishStatus.SUCCEEDED.name().equals(base.getStatus()) && base.getModelVersionId() != null;
    }

    /**
     * 把本次发布的模型ID、版本ID等放进一个对象，方便后面的步骤继续使用；这里不修改数据库。
     *
     * @param model      根模型
     * @param version    根当前版本
     * @param instanceId 本次或原成功实例ID
     * @return 发布上下文
     */
    private ModelPublishContext context(ModelBaseDO model, ModelVersionDO version, Long instanceId) {
        ModelPublishContext context = new ModelPublishContext();
        context.setInstanceId(instanceId);
        context.setModelId(model.getId());
        context.setModelVersionId(version.getId());
        context.setModelVersionStatus(ModelVersionStatus.fromStatus(version.getStatus()));
        context.setBaseInstanceId(model.getPublishedInstanceId());
        ModelPublishTimingUtils.bind(context);
        return context;
    }

    // 模型配置及成功结构读取

    /**
     * 每次查询平台数据源配置并核验真实目标；统一发布内复用查询资源，凭证不返回前端、不保存进计划。
     * 此方法不把业务库连接混入平台只读事务。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ModelPublishDatabaseSession openTarget(ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("TARGET_OPEN", context);
        try {
            if (context.getDatasourceId() == null) {
                throw new ServiceException("发布缺少目标数据源ID");
            }
            List<JSONObject> rows = configObjects(configMapper.selectPublishDatasource(context.getDatasourceId()));
            if (rows.size() != 1 || !context.getDatasourceId().equals(rows.get(0).getLong("id"))
                    || rows.get(0).getBooleanValue("delFlag") || !rows.get(0).getBooleanValue("validFlag")) {
                throw new ServiceException("目标数据源不存在、已删除或已停用");
            }
            JSONObject row = rows.get(0);
            DbQuery query = null;
            ModelPublishTargetResourceScope.ModelPublishTargetResource resource = null;
            try {
                DbQueryProperty property = new DbQueryProperty(row.getString("datasourceType"), row.getString("ip"),
                        row.getLong("port"), row.getString("datasourceConfig"));
                DbType type = DbType.getDbType(property.getDbType());
                // 由common选择数据库方言：model描述要做什么，common负责转换成目标数据库的SQL。
                DbDialect dialect = DialectFactory.getDialect(type);
                ModelPublishTargetResourceScope scope = ModelPublishTargetResourceScope.current();
                if (scope == null) {
                    // 单独调用内部接口仍保持原来局部创建、局部关闭的行为。
                    query = createTargetQuery(property);
                } else {
                    // 数据源仍然每次重新查询；参数没变才复用，账号等参数变化会创建新资源。
                    resource = scope.acquire(context.getDatasourceId(), property, dialect, () -> createTargetQuery(property));
                    query = resource.getQuery();
                }
                ModelPublishTarget target = new ModelPublishTarget();
                target.setDatasourceId(context.getDatasourceId());
                target.setDatabaseType(property.getDbType());
                target.setDatabaseName(property.getDbName());
                target.setSchemaName(dialect.getSchemaName(property));
                try (Connection connection = query.getConnection()) {
                    target.setDatabaseVersion(connection.getMetaData().getDatabaseProductVersion());
                    // 只核对目标库和Schema等连接位置，账号密码不参与比较，也不把URL写入计划。
                    String configuredSchema = property.getSid();
                    if (dialect instanceof SQLServer2008Dialect
                            && (configuredSchema == null || configuredSchema.trim().isEmpty())) {
                        configuredSchema = "dbo";
                    }
                    String identity = JSON.toJSONString(Arrays.asList(property.getDbType(), property.getHost(),
                            property.getPort(), property.getDbName(), property.getSid(),
                            connection.getCatalog(), ModelMaterializationDbUtils.readSchema(connection, configuredSchema)));
                    target.setConnectionFingerprint(DigestUtils.md5DigestAsHex(identity.getBytes(StandardCharsets.UTF_8)));
                }
                // 修订沿用数据源ID，但连接配置使用当前值；上次成功的连接指纹不能覆盖最新配置。
                if (context.getBaseTarget() != null
                        && !Objects.equals(context.getBaseTarget().getDatasourceId(), target.getDatasourceId())) {
                    throw new ServiceException("修订发布的数据源ID与上次成功记录不一致");
                }
                // 同一个未完成DDL计划仍核对目标身份，避免执行到一半时切换物理库。
                verifyTargetIdentity(context.getTarget(), target);
                if (context.getBaseInstanceId() != null && context.getBaseTarget() == null) {
                    throw new ServiceException("上次成功目标身份缺失，不能继续生成DDL");
                }
                context.setTarget(target);
                // 调用方仍用try-with-resources关闭会话；发布内共享资源由入口统一释放，连接凭证不进入计划。
                return resource == null ? new ModelPublishDatabaseSession(query, property, dialect, target)
                        : resource.session(property, dialect, target);
            } catch (Exception ex) {
                if (query != null && resource == null) {
                    query.close();
                }
                if (ex instanceof ServiceException) {
                    throw (ServiceException) ex;
                }
                // 驱动异常可能包含连接URL，不能原样写到发布记录或返回前端。
                throw new ServiceException("目标数据库连接或身份读取失败，请检查数据源配置和权限");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 创建common原查询器；统一发布内相同连接配置只调用一次，其他内部调用仍局部持有资源。
     */
    protected DbQuery createTargetQuery(DbQueryProperty property) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("TARGET_CREATE", null, null);
        try {
            return new DefaultDataSourceFactoryBean().createDbQuery(property);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 连接配置可修改，成功发布信息也按数据源ID读取当前配置，不回退到发布快照。
     */
    private DbQueryProperty currentDatasource(Long datasourceId) {
        if (datasourceId == null || datasourceId <= 0) throw new ServiceException("发布数据源ID无效");
        List<JSONObject> rows = configObjects(configMapper.selectPublishDatasource(datasourceId));
        if (rows.size() != 1 || !datasourceId.equals(rows.get(0).getLong("id"))
                || rows.get(0).getBooleanValue("delFlag")) {
            throw new ServiceException("发布模型的数据连接不存在或已删除");
        }
        JSONObject row = rows.get(0);
        return new DbQueryProperty(row.getString("datasourceType"), row.getString("ip"),
                row.getLong("port"), row.getString("datasourceConfig"));
    }

    /**
     * 同一个未完成DDL计划不能在执行期间切换物理目标。
     */
    private void verifyTargetIdentity(ModelPublishTarget expected, ModelPublishTarget actual) {
        if (expected != null && (!Objects.equals(expected.getDatasourceId(), actual.getDatasourceId())
                || !Objects.equals(expected.getConnectionFingerprint(), actual.getConnectionFingerprint()))) {
            throw new ServiceException("目标数据源对应的实际数据库已变化，不能继续本次物化");
        }
    }

    /**
     * 把模型、字段、参考数据、编码方案和规则一起读出来。使用独立的只读事务，读取失败就报错。
     *
     * @param modelId 根模型ID
     * @param version 已核对归属的根当前版本，子模型沿用根版本
     * @return 本次发布完整配置快照
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public ModelPublishSnapshot readCurrentConfig(Long modelId, ModelVersionDO version) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("READ_CONFIG", modelId, null);
        try {
            // 配置读取由独立只读事务包住；模型、字段、引用数据及规则共同组成MODEL_SNAPSHOT。
            List<JSONObject> nodes = configObjects(configMapper.selectNodes(modelId));
            // 先从模型及子模型清单中找到主模型；找不到就报错，不返回一份缺主模型的配置。
            JSONObject root = nodes.stream().filter(node -> modelId.equals(node.getLong("id"))).findFirst()
                    .orElseThrow(() -> new ServiceException("根模型配置不存在"));
            List<Long> ids = new ArrayList<>();
            for (JSONObject node : nodes) {
                ids.add(node.getLong("id"));
                node.remove("enabledFlag");
                // 当前只有主模型单独保存版本，子模型使用主模型的版本信息。
                // 先转成JSON再读回来，是为了复制数据，避免后面修改原对象影响这份配置。
                node.put("version", JSON.parseObject(JSON.toJSONString(version)));
                node.put("fields", new JSONArray());
                node.put("subModels", new JSONArray());
                if (!modelId.equals(node.getLong("id"))) {
                    root.getJSONArray("subModels").add(node);
                }
            }
            // 只取直接子模型ID，再查它们下面是否还有模型。本期不支持第三层，发现后明确报错。
            List<Long> childIds = new ArrayList<>(ids);
            childIds.remove(modelId);
            if (!childIds.isEmpty()) {
                Long descendants = configMapper.countActiveDescendants(childIds);
                if (descendants == null || descendants > 0) {
                    throw new ServiceException("模型存在未支持的下级结构或无法确认层级");
                }
            }
            List<JSONObject> fields = configObjects(configMapper.selectFields(ids));
            // 使用Set去重，多个字段引用同一字典或编码版本时，只需要查询一次。
            Set<Long> referenceIds = new LinkedHashSet<>();
            Set<Long> encodingVersionIds = new LinkedHashSet<>();
            for (JSONObject field : fields) {
                field.remove("enabledFlag");
                // 按字段的modelId找到所属模型，再把字段放入它的fields列表，避免把子模型字段混进主模型。
                JSONObject owner = nodes.stream().filter(node -> node.getLong("id").equals(field.getLong("modelId")))
                        .findFirst().orElseThrow(() -> new ServiceException("字段模型归属不完整"));
                owner.getJSONArray("fields").add(field);
                // 字段本身即使已删除也要记录下来，用来比较变化。
                // 但不用再查它现在关联的字典或编码方案；旧关联内容可以从上次成功配置中读取。
                if (!field.getBooleanValue("delFlag")) {
                    if (field.getLong("dictTypeId") != null) {
                        referenceIds.add(field.getLong("dictTypeId"));
                    }
                    if (field.getLong("encodingVersionId") != null) {
                        encodingVersionIds.add(field.getLong("encodingVersionId"));
                    }
                }
            }
            ModelPublishSnapshot snapshot = new ModelPublishSnapshot();
            snapshot.setModelId(modelId);
            snapshot.setModelVersionId(version.getId());
            snapshot.setModel(root);
            // 先查需要的参考数据集合并检查是否查全，再查这些集合下面的数据项。没有引用就不查询。
            if (!referenceIds.isEmpty()) {
                snapshot.getReferenceData().put("types", requireIds(configObjects(configMapper.selectReferenceTypes(referenceIds)), referenceIds, "参考数据集合"));
                snapshot.getReferenceData().put("items", configObjects(configMapper.selectReferenceItems(referenceIds)));
            }
            // 先查字段实际引用的编码版本，再由版本查编码方案主体，最后补齐版本的全部组成项。
            if (!encodingVersionIds.isEmpty()) {
                List<JSONObject> versions = requireIds(configObjects(configMapper.selectEncodingVersions(encodingVersionIds)), encodingVersionIds, "编码方案版本");
                Set<Long> encodingIds = new LinkedHashSet<>();
                for (JSONObject encodingVersion : versions) {
                    encodingIds.add(encodingVersion.getLong("encodingId"));
                }
                snapshot.getEncodingSchemes().put("versions", versions);
                snapshot.getEncodingSchemes().put("schemes", requireIds(configObjects(configMapper.selectEncodings(encodingIds)), encodingIds, "编码方案"));
                snapshot.getEncodingSchemes().put("items", configObjects(configMapper.selectEncodingItems(encodingVersionIds)));
            }
            // 通过规则模块公共API读取主子模型的完整规则，再复制成独立JSON保存。
            // 后续查询成功版本时读取这份配置，不受当前规则修改或API返回对象变化影响。
            // 规则的关联关系由rule模块API解释，model不直接拼接规则表查询；结果也写入本次配置副本。
            RuleModelRulesRespDTO rules = ruleModelApi.getModelRules(ids);
            if (rules == null || rules.getMatch() == null || rules.getSurvivor() == null || rules.getQuality() == null) {
                throw new ServiceException("规则模块返回的发布配置不完整");
            }
            snapshot.setRules(JSON.parseObject(JSON.toJSONString(rules)));
            return snapshot;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 把数据库查询结果转成独立JSON：大写下划线列名转成驼峰命名，Clob（大文本）读成字符串。DO里暂时没有的配置列也保留。
     *
     * @param rows 原始查询结果，不能以null表示读取失败
     * @return 独立JSON记录列表
     */
    private List<JSONObject> configObjects(List<Map<String, Object>> rows) {
        if (rows == null) {
            throw new ServiceException("发布配置读取结果无效");
        }
        List<JSONObject> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                // 例如MODEL_VERSION_ID先拆成model、version、id，再拼成modelVersionId，方便后面按Java属性名读取。
                String[] parts = entry.getKey().toLowerCase(Locale.ROOT).split("_");
                StringBuilder key = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    key.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
                }
                Object value = entry.getValue();
                if (value instanceof Clob) {
                    try {
                        Clob clob = (Clob) value;
                        // Clob是数据库的大文本对象，趁连接还有效时读成普通字符串；它的读取位置从1开始。
                        value = clob.getSubString(1, Math.toIntExact(clob.length()));
                    } catch (SQLException ex) {
                        throw new ServiceException("发布配置大文本读取失败");
                    }
                }
                object.put(key.toString(), value);
            }
            // 通过JSON转换再复制一份，避免快照继续引用查询结果中的可变对象。
            result.add(JSON.parseObject(JSON.toJSONString(object)));
        }
        return result;
    }

    /**
     * 逐个检查需要的字典、编码方案是否都查到了，少了任何一条就报错。
     *
     * @param rows  读取的引用记录
     * @param ids   字段实际引用的记录ID
     * @param label 引用对象中文名称
     * @return 已核对记录
     */
    private List<JSONObject> requireIds(List<JSONObject> rows, Collection<Long> ids, String label) {
        for (Long id : ids) {
            // noneMatch表示“一条也没找到”；需要的ID为空或找不到对应记录，都说明引用配置不完整。
            if (id == null || rows.stream().noneMatch(row -> id.equals(row.getLong("id")))) {
                throw new ServiceException(label + "不存在，ID：" + id);
            }
        }
        return rows;
    }

    /**
     * 读取上次成功的配置、表、字段和索引；已经停用但仍在数据库中的表、列也要保留。
     *
     * @param context      本次发布上下文
     * @param baseInstance 已核对归属与成功状态的历史实例，首次发布为空
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public void readPublishedStructure(ModelPublishContext context, ModelInstanceDO baseInstance) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("READ_PUBLISHED_STRUCTURE", context);
        try {
            if (baseInstance != null) {
                ModelPublishExecutionPlan previousPlan;
                try {
                    previousPlan = JSON.parseObject(baseInstance.getExecutionPlan(),
                            ModelPublishExecutionPlan.class);
                } catch (RuntimeException ex) {
                    throw new ServiceException("上次成功发布的目标信息无法读取");
                }
                if (previousPlan == null || previousPlan.getTarget() == null
                        || previousPlan.getTarget().getConnectionFingerprint() == null) {
                    throw new ServiceException("上次成功发布缺少目标连接身份，不能重新物化");
                }
                context.setBaseTarget(previousPlan.getTarget());
            }
            context.setBaseStructure(new ModelMaterializedStructure());
            // 没有上次成功记录就是首次发布，旧结构留空即可，不去目标库寻找同名表接管。
            if (baseInstance == null) {
                return;
            }
            try {
                if (!Integer.valueOf(1).equals(baseInstance.getStructureFormatVersion())) {
                    throw new ServiceException("不支持的成功结构格式版本");
                }
                ModelPublishSnapshot snapshot = JSON.parseObject(baseInstance.getModelSnapshot(), ModelPublishSnapshot.class);
                // 依次检查JSON能否读出、格式版本是否支持、主模型和版本ID是否对应，最后核对配置正文中的模型ID。
                // 任何一项不一致都先停下来，不能用错误的旧配置生成本次SQL。
                if (snapshot == null || !Integer.valueOf(1).equals(snapshot.getFormatVersion())
                        || !context.getModelId().equals(snapshot.getModelId())
                        || !baseInstance.getModelVersionId().equals(snapshot.getModelVersionId())
                        || snapshot.getModel() == null || !context.getModelId().equals(snapshot.getModel().getLong("id"))) {
                    throw new ServiceException("成功模型配置快照不完整或归属不一致");
                }
                // 这里读取的是平台保存的成功物化记录；目标数据库的真实结构会在TARGET阶段另行核对。
                List<JSONObject> tables = configObjects(configMapper.selectMaterializedTables(baseInstance.getId()));
                if (tables.isEmpty()) {
                    throw new ServiceException("成功实例缺少物化表");
                }
                List<Long> tableIds = new ArrayList<>();
                for (JSONObject table : tables) {
                    requireHistoryRow(table);
                    tableIds.add(table.getLong("id"));
                }
                List<JSONObject> columns = configObjects(configMapper.selectMaterializedColumns(tableIds));
                Set<ModelMaterializedTableIdentity> identities = new HashSet<>();
                for (JSONObject row : tables) {
                    ModelMaterializedTableDefinition table = JSON.parseObject(row.toJSONString(), ModelMaterializedTableDefinition.class);
                    // 主表的parentId必须为0且模型ID是当前主模型；子表的parentId必须是当前主模型ID。
                    // Set.add返回false表示“模型ID＋来源/权威角色”已经出现过，即同一个表被重复登记。
                    if (table.getParentId() == null || (table.getParentId() != 0 && !context.getModelId().equals(table.getParentId()))
                            || (table.getParentId() == 0 && !context.getModelId().equals(table.getModelId()))
                            || !identities.add(ModelMaterializedTableIdentity.of(table.getModelId(), table.getTableRole()))) {
                        throw new ServiceException("成功物化表主子关系或稳定身份不合法");
                    }
                    if (table.getParentId() != 0) {
                        // 在本次读出的旧表清单中找父表：记录ID要匹配，必须是当前主模型的主表，角色也要一致。
                        // 也就是说，来源子表只能关联来源主表，权威子表只能关联权威主表；找到任意一条符合条件的即可。
                        boolean validParent = tables.stream().anyMatch(parent -> parent.getLong("id").equals(row.getLong("parentTableId"))
                                && context.getModelId().equals(parent.getLong("modelId")) && parent.getLongValue("parentId") == 0
                                && parent.getString("tableRole").equals(row.getString("tableRole")));
                        if (!validParent) {
                            throw new ServiceException("成功子表缺少同角色父表绑定");
                        }
                    }
                    String indexJson = row.getString("indexDefinition");
                    if (indexJson == null) {
                        throw new ServiceException("上次成功发布的表缺少索引定义");
                    }
                    List<ModelMaterializedIndexDefinition> indexes = JSON.parseArray(indexJson, ModelMaterializedIndexDefinition.class);
                    if (indexes == null) {
                        throw new ServiceException("上次成功发布的表的索引定义不是完整数组");
                    }
                    table.setIndexes(indexes);
                    for (JSONObject column : columns) {
                        // 按物化表记录ID，把这张表自己的列装进去；停用列也要保留，后面才能知道数据库里仍有它。
                        if (row.getLong("id").equals(column.getLong("materializedTableId"))) {
                            requireHistoryRow(column);
                            table.getColumns().add(JSON.parseObject(column.toJSONString(), ModelMaterializedColumnDefinition.class));
                        }
                    }
                    if (table.getColumns().isEmpty()) {
                        throw new ServiceException("上次成功发布的表缺少物化字段记录");
                    }
                    context.getBaseStructure().getTables().add(table);
                }
                for (ModelMaterializedTableDefinition table : context.getBaseStructure().getTables()) {
                    // 每个模型都应该有来源和权威两张表：当前是来源就查权威，当前是权威就查来源。
                    ModelTableRole otherRole = table.getTableRole() == ModelTableRole.SOURCE ? ModelTableRole.AUTHORITY : ModelTableRole.SOURCE;
                    if (!identities.contains(ModelMaterializedTableIdentity.of(table.getModelId(), otherRole))) {
                        throw new ServiceException("上次成功发布的结构缺少配对的来源表或权威表");
                    }
                }
                if (!identities.contains(ModelMaterializedTableIdentity.of(context.getModelId(), ModelTableRole.SOURCE))) {
                    throw new ServiceException("上次成功发布的结构缺少主模型物化表");
                }
                context.setBaseSnapshot(snapshot);
            } catch (ServiceException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new ServiceException("无法读取上次成功发布的结构，不能按首次发布重新建表");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 检查保存的表、列记录是否被误删或设为无效。记录损坏时先停止发布，不能把原来存在的表、列当作不存在。
     *
     * @param row 物化表或列记录
     */
    private void requireHistoryRow(JSONObject row) {
        if (!row.getBooleanValue("validFlag") || row.getBooleanValue("delFlag")) {
            throw new ServiceException("上次成功发布的表或字段记录已无效或被逻辑删除，请先核对历史结构");
        }
    }

    // 发布实例、过程日志及成功结果保存

    /**
     * 日志与实例阶段一起保存；准备阶段不修改成功结构，CLOSE只处理已经正式成功的实例。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveStage(ModelPublishContext context, ModelPublishStage stage, String message) {
        // CLOSE只给已正式成功的实例补结束日志，不能借保存日志改变整次发布结果。
        if (stage == ModelPublishStage.CLOSE) {
            ModelInstanceDO stored = instanceMapper.selectCloseById(context.getInstanceId());
            if (stored == null || !Objects.equals(context.getModelId(), stored.getModelId())
                    || !ModelPublishStatus.SUCCEEDED.name().equals(stored.getStatus())) {
                throw new ServiceException("只有正式成功的实例可以记录发布资源关闭");
            }
            if (ModelPublishStage.CLOSE.name().equals(stored.getCurrentStage())) {
                return;
            }
            ModelInstanceDO update = new ModelInstanceDO();
            update.setCurrentStage(stage.name());
            updateExecution(stored, update);
            // 复用本事务刚读取并更新成功的实例，日志序号条件和结束时间处理仍走原方法。
            stored.setCurrentStage(stage.name());
            appendContent(stored, stage, "SUCCEEDED", JSONObject.of("message", message), true, stored);
            return;
        }
        if (stage != ModelPublishStage.CONFIG && stage != ModelPublishStage.BUILD
                && stage != ModelPublishStage.CHANGE && stage != ModelPublishStage.TARGET) {
            throw new ServiceException("当前阶段不能作为准备阶段完成日志保存");
        }
        ModelInstanceDO record = new ModelInstanceDO();
        record.setId(context.getInstanceId());
        record.setModelId(context.getModelId());
        record.setDatasourceId(context.getDatasourceId());
        record.setBaseInstanceId(context.getBaseInstanceId());
        ModelInstanceDO stored = requirePreparing(record);
        ModelInstanceDO update = new ModelInstanceDO();
        update.setCurrentStage(stage.name());
        updateExecution(stored, update);
        append(stored, stage, "SUCCEEDED", message, true);
    }

    /**
     * 只读取原待提交或成功实例；没有完整完成证据时不能跳过DDL执行直接保存成功。
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public ModelPublishContext readPendingCommit(Long modelId, Long instanceId) {
        // 只统计读取和还原待保存上下文，不增加查询；外层COMMIT还包含事务代理的耗时。
        try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_READ_PENDING", modelId, instanceId)) {
            ModelInstanceDO stored = instanceMapper.selectCommitById(instanceId);
            // 校验返回本事务已经解析的原计划，后续还原上下文直接复用。
            ModelPublishExecutionPlan plan = requireCommitInstance(stored, modelId);
            // 成功实例允许重复查询；原校验会直接返回，仍按原流程读取并核对计划。
            if (plan == null) {
                plan = readExecutionPlan(stored);
            }
            ModelPublishContext context = savedContext(stored, plan);
            context.setStatus(ModelPublishStatus.valueOf(stored.getStatus()));
            context.setCurrentStage(ModelPublishStage.valueOf(stored.getCurrentStage()));
            return context;
        }
    }

    /**
     * 实例、正式结构、版本、成功指针和成功日志必须一起提交；任何一处失败整笔回滚。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void completeSuccess(ModelPublishContext context, ModelMaterializedStructure structure) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("COMMIT_BODY", context);
        try {
            // 这是正式生效的唯一平台保存步骤，REQUIRES_NEW把下方所有表记录、版本、指针及日志包在同一个事务中。

            ModelInstanceDO stored;
            // 分开记录平台读取和内存校验，避免把整段正式保存都误认为插入字段耗时。
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_READ_INSTANCE", context)) {
                stored = instanceMapper.selectCommitById(context.getInstanceId());
            }
            ModelPublishExecutionPlan plan;
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_VALIDATE_INSTANCE", context)) {
                plan = requireCommitInstance(stored, context.getModelId());
            }
            if (ModelPublishStatus.SUCCEEDED.name().equals(stored.getStatus())) {
                return;
            }
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_VALIDATE_STRUCTURE", context)) {
                if (!Objects.equals(context.getModelVersionId(), stored.getModelVersionId())
                        || !Objects.equals(context.getDatasourceId(), stored.getDatasourceId())
                        || !Objects.equals(context.getBaseInstanceId(), stored.getBaseInstanceId())
                        || !Objects.equals(JSON.toJSONString(context.getExecutionPlan()), JSON.toJSONString(plan))) {
                    throw new ServiceException("正式保存的实例、版本或原计划不一致");
                }
                // 先核对实采结构仍属于原计划，不能把调用方临时拼出的表列直接登记为物化成功。
                requireActualStructure(plan.getTargetStructure(), structure,
                        plan.getTarget() == null ? null : plan.getTarget().getDatabaseType());
            }
            Long bindings;
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_READ_BINDINGS", context)) {
                bindings = configMapper.selectPublishedBindingCount(stored.getId());
            }
            if (bindings == null || bindings != 0) {
                throw new ServiceException("待提交实例已存在正式绑定，请先核对保存结果");
            }
            Date publishedTime = new Date();
            // 先追加本次成功的完整物化表和字段记录；下方状态更新失败时，这些记录也会一起回滚。
            saveMaterializedStructure(stored, structure, publishedTime);
            // 状态条件只保护本次保存的版本和成功指针，不申请分布式锁，也不拦截草稿编辑。
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_SAVE_VERSION_POINTER", context)) {
                if (versionMapper.update(null, new LambdaUpdateWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getId, stored.getModelVersionId()).eq(ModelVersionDO::getModelId, stored.getModelId())
                        .in(ModelVersionDO::getStatus, 0L, 2L).set(ModelVersionDO::getStatus, 1L)) != 1) {
                    throw new ServiceException("本次模型版本已变化，正式结果未保存");
                }
                // 只在模型仍使用本次工作版本、成功指针仍指向原实例时更新，避免覆盖期间已经变化的状态。
                LambdaUpdateWrapper<ModelBaseDO> modelCondition = new LambdaUpdateWrapper<ModelBaseDO>()
                        .eq(ModelBaseDO::getId, stored.getModelId()).eq(ModelBaseDO::getVersionId, stored.getModelVersionId());
                if (stored.getBaseInstanceId() == null) {
                    modelCondition.isNull(ModelBaseDO::getPublishedInstanceId);
                } else {
                    modelCondition.eq(ModelBaseDO::getPublishedInstanceId, stored.getBaseInstanceId());
                }
                modelCondition.set(ModelBaseDO::getPublishedInstanceId, stored.getId());
                if (modelMapper.update(null, modelCondition) != 1) {
                    throw new ServiceException("模型当前版本或成功指针已变化，正式结果未保存");
                }
            }
            // 最后把当前实例从“待保存”改为“成功”，并在同一事务追加成功日志。
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("COMMIT_SAVE_RESULT_LOG", context)) {
                ModelInstanceDO update = new ModelInstanceDO();
                update.setStatus(ModelPublishStatus.SUCCEEDED.name());
                update.setCurrentStage(ModelPublishStage.COMMIT.name());
                update.setFinishedTime(publishedTime);
                update.setPublishedTime(publishedTime);
                update.setCompensationStatus(ModelCompensationStatus.NOT_REQUIRED.name());
                update.setRemainingEffects("[]");
                if (instanceMapper.update(update, new LambdaUpdateWrapper<ModelInstanceDO>()
                        .eq(ModelInstanceDO::getId, stored.getId()).eq(ModelInstanceDO::getStatus, ModelPublishStatus.COMMIT_PENDING.name())
                        .eq(ModelInstanceDO::getCurrentStage, ModelPublishStage.VERIFY.name())
                        .set(ModelInstanceDO::getFailureReason, null).set(ModelInstanceDO::getFailedStage, null)) != 1) {
                    throw new ServiceException("发布实例状态已变化，正式结果未保存");
                }
                // 实例刚在本事务更新成功，复用它追加日志；同步结束时间，避免日志仍写入更新前的空时间。
                stored.setStatus(update.getStatus());
                stored.setCurrentStage(update.getCurrentStage());
                stored.setFinishedTime(publishedTime);
                stored.setPublishedTime(publishedTime);
                appendContent(stored, ModelPublishStage.COMMIT, ModelPublishStatus.SUCCEEDED.name(),
                        JSONObject.of("message", "完整真实结构、模型版本及成功指针已统一保存"), true, stored);
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 保存失败原因与日志时仍保持COMMIT_PENDING；已经成功的记录直接跳过，避免迟到异常覆盖成功结果。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveCommitFailure(Long modelId, Long instanceId) {
        ModelInstanceDO stored = instanceMapper.selectById(instanceId);
        requireCommitInstance(stored, modelId);
        if (ModelPublishStatus.SUCCEEDED.name().equals(stored.getStatus())) {
            return;
        }
        ModelInstanceDO update = new ModelInstanceDO();
        update.setFailureReason("正式结构采集或平台保存未完成，请使用原实例重新核验并补保存");
        update.setFailedStage(ModelPublishStage.COMMIT.name());
        updateExecution(stored, update);
        append(stored, ModelPublishStage.COMMIT, "FAILED", update.getFailureReason(), true);
    }

    /**
     * 待提交必须来自全部SQL成功且结束明确的原计划；成功实例允许重复查询，不重复写入。
     * 返回本事务已经校验的计划，避免调用方再次解析；已成功实例返回null，保留原有直接放行规则。
     */
    private ModelPublishExecutionPlan requireCommitInstance(ModelInstanceDO stored, Long modelId) {
        if (stored == null || !Objects.equals(modelId, stored.getModelId())) {
            throw new ServiceException("正式保存的实例不存在或不属于当前主模型");
        }
        if (ModelPublishStatus.SUCCEEDED.name().equals(stored.getStatus())) {
            return null;
        }
        if (!ModelPublishStatus.COMMIT_PENDING.name().equals(stored.getStatus())
                || !ModelPublishStage.VERIFY.name().equals(stored.getCurrentStage())
                || !ModelCompensationStatus.NOT_REQUIRED.name().equals(stored.getCompensationStatus())) {
            throw new ServiceException("只有完整物化核验通过的待提交实例才能保存正式结果");
        }
        // 计划只在本次方法调用中解析一次；调用方继续使用同一个已校验对象。
        ModelPublishExecutionPlan savedPlan;
        try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.startRecord("COMMIT_PARSE_PLAN", stored)) {
            savedPlan = readExecutionPlan(stored);
        }
        if (savedPlan.getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE
                && savedPlan.getDdlTransactionStatus() != ModelDdlTransactionStatus.COMMITTED) {
            throw new ServiceException("业务库事务尚未明确提交，不能保存正式成功结果");
        }
        // 每条SQL都要有成功及结束记录；完整结构已经在整批结束时统一核验。
        for (ModelSqlExecutionStep step : savedPlan.getSteps()) {
            if (!isSqlExecutedSuccessfully(step)) {
                throw new ServiceException("原计划仍有未完成或未明确结束的SQL，不能正式保存");
            }
        }
        return savedPlan;
    }

    /**
     * 正式结构按计划中的原始表名核对，不对任何数据库的表名转换大小写。
     * 物理表的定位和结构核验由common方言及采集阶段完成，此处核对正式结构的归属名称。
     *
     * @param databaseType 原计划保存的数据库类型
     * @param expected     计划中的表名
     * @param actual       采集完成的正式结构表名
     * @return 正式结构表名是否符合该数据库的保存规则
     */
    private boolean matchesActualTableName(String databaseType, String expected, String actual) {
        return Objects.equals(expected, actual);
    }

    /**
     * 按稳定身份核对采集清单，不允许漏表漏字段；数据库实际名称、注释和类型独立保存。
     */
    private void requireActualStructure(ModelMaterializedStructure expected, ModelMaterializedStructure actual,
                                        String databaseType) {
        if (actual == null || actual.getTables() == null || actual.getTables().size() != expected.getTables().size()) {
            throw new ServiceException("真实物化表清单不完整");
        }
        Map<ModelMaterializedTableIdentity, ModelMaterializedTableDefinition> tables = new LinkedHashMap<>();
        for (ModelMaterializedTableDefinition table : actual.getTables()) {
            if (table == null || table.getModelId() == null || table.getTableRole() == null
                    || tables.put(ModelMaterializedTableIdentity.of(table.getModelId(), table.getTableRole()), table) != null) {
                throw new ServiceException("真实物化表归属缺失或重复");
            }
        }
        for (ModelMaterializedTableDefinition table : expected.getTables()) {
            ModelMaterializedTableDefinition observed = tables.get(ModelMaterializedTableIdentity.of(table.getModelId(), table.getTableRole()));
            if (observed == null || !Objects.equals(table.getParentId(), observed.getParentId())
                    || !matchesActualTableName(databaseType, table.getTableName(), observed.getTableName())
                    || !Objects.equals(table.getEnabledFlag(), observed.getEnabledFlag())
                    || observed.getColumns() == null || observed.getColumns().size() != table.getColumns().size()
                    || observed.getIndexes() == null || observed.getIndexes().size() != table.getIndexes().size()) {
                throw new ServiceException("真实物化表与原计划的归属或完整结构不一致");
            }
            Map<String, ModelMaterializedColumnDefinition> columns = new LinkedHashMap<>();
            Set<Integer> positions = new HashSet<>();
            for (ModelMaterializedColumnDefinition column : observed.getColumns()) {
                if (column == null || column.getPhysicalType() == null || column.getPhysicalType().trim().isEmpty()
                        || column.getColumnPosition() == null || column.getColumnPosition() <= 0
                        || !positions.add(column.getColumnPosition()) || columns.put(column.getColumnName(), column) != null) {
                    throw new ServiceException("真实物化字段的类型、顺序或名称无效");
                }
            }
            for (ModelMaterializedColumnDefinition column : table.getColumns()) {
                ModelMaterializedColumnDefinition value = columns.get(column.getColumnName());
                if (value == null || !Objects.equals(column.getModelFieldId(), value.getModelFieldId())
                        || !Objects.equals(column.getSystemFieldKey(), value.getSystemFieldKey())
                        || !Objects.equals(column.getLogicalType(), value.getLogicalType())
                        || !Objects.equals(column.getSystemFlag(), value.getSystemFlag())
                        || !Objects.equals(column.getEnabledFlag(), value.getEnabledFlag())
                        || !Objects.equals(column.getNullable(), value.getNullable())
                        || !Objects.equals(column.getPrimaryKeyFlag(), value.getPrimaryKeyFlag())
                        || !Objects.equals(column.getIdentityFlag(), value.getIdentityFlag())) {
                    throw new ServiceException("真实物化字段与原计划的归属或约束不一致");
                }
            }
            for (ModelMaterializedIndexDefinition index : table.getIndexes()) {
                boolean found = observed.getIndexes().stream().anyMatch(value -> Objects.equals(index.getStableKey(), value.getStableKey())
                        && index.getIndexType() == value.getIndexType() && index.getColumnNames().equals(value.getColumnNames())
                        && index.getColumnKeys().equals(value.getColumnKeys())
                        && (index.getIndexType() == ModelIndexType.PRIMARY_KEY || Objects.equals(index.getIndexName(), value.getIndexName())));
                if (!found) {
                    throw new ServiceException("真实物化索引与原计划不一致");
                }
            }
        }
    }

    /**
     * 主表先保存，子表通过本实例同角色的主表记录ID关联；每次成功都追加完整历史。
     */
    private void saveMaterializedStructure(ModelInstanceDO record, ModelMaterializedStructure structure, Date verifiedTime) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("SAVE_MATERIALIZED_BODY", record);
        try {
            // 这个Map保存的是本次物化记录ID，子表的PARENT_TABLE_ID必须关联同次、同角色的主表记录。
            Map<ModelMaterializedTableIdentity, Long> tableIds = new LinkedHashMap<>();
            // 表记录先取得真实ID，字段再带着所属表ID分批保存；所有批次仍在completeSuccess的同一事务中。
            List<ModelMaterializedColumnDO> fields = new ArrayList<>(MATERIALIZED_COLUMN_BATCH_SIZE);
            // 分两轮处理，避免采集结果中子表排在主表前面时找不到父表记录。
            for (boolean main : new boolean[]{true, false}) {
                for (ModelMaterializedTableDefinition table : structure.getTables()) {
                    if (main != Long.valueOf(0).equals(table.getParentId())) {
                        continue;
                    }
                    Long parentTableId = main ? null : tableIds.get(ModelMaterializedTableIdentity.of(table.getParentId(), table.getTableRole()));
                    if (!main && parentTableId == null) {
                        throw new ServiceException("子表缺少本次成功实例中同角色的主表");
                    }
                    ModelMaterializedTableDO saved = BeanUtils.toBean(table, ModelMaterializedTableDO.class);
                    saved.setId(null);
                    saved.setInstanceId(record.getId());
                    saved.setDatasourceId(record.getDatasourceId());
                    saved.setTableRole(table.getTableRole().name());
                    saved.setParentTableId(parentTableId);
                    saved.setIndexDefinition(JSON.toJSONString(table.getIndexes()));
                    saved.setVerifiedTime(verifiedTime);
                    saved.setCreatorId(record.getCreatorId());
                    saved.setCreateBy(record.getCreateBy());
                    saved.setValidFlag(true);
                    saved.setDelFlag(false);
                    if (materializedTableMapper.insert(saved) != 1 || saved.getId() == null) {
                        throw new ServiceException("正式物化表记录保存失败");
                    }
                    tableIds.put(ModelMaterializedTableIdentity.of(table.getModelId(), table.getTableRole()), saved.getId());
                    for (ModelMaterializedColumnDefinition column : table.getColumns()) {
                        ModelMaterializedColumnDO field = BeanUtils.toBean(column, ModelMaterializedColumnDO.class);
                        field.setId(null);
                        field.setMaterializedTableId(saved.getId());
                        field.setCreatorId(record.getCreatorId());
                        field.setCreateBy(record.getCreateBy());
                        field.setValidFlag(true);
                        field.setDelFlag(false);
                        fields.add(field);
                        if (fields.size() == MATERIALIZED_COLUMN_BATCH_SIZE) {
                            saveMaterializedColumnBatch(fields);
                        }
                    }
                }
            }
            // 最后一批可能不足500条，也必须保存；恰好整批时列表已清空，不再执行空批次。
            saveMaterializedColumnBatch(fields);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 复用项目已有的批量插入能力，加入外层成功保存事务；批次失败直接抛错，由外层一起回滚。
     * 保存完成才清空缓冲列表，后续字段继续使用这个列表，避免把全部字段同时攒在内存中。
     */
    private void saveMaterializedColumnBatch(List<ModelMaterializedColumnDO> fields) {
        if (fields.isEmpty()) {
            return;
        }
        if (!Boolean.TRUE.equals(materializedColumnMapper.insertBatch(fields, MATERIALIZED_COLUMN_BATCH_SIZE))) {
            throw new ServiceException("正式物化字段记录保存失败");
        }
        fields.clear();
    }

    /**
     * 在独立事务中保存目标、逐条SQL、核验条件和补偿条件；仍处于准备中，不冒充发布成功。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveExecutionPlan(ModelInstanceDO record, ModelPublishExecutionPlan plan) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("SAVE_PLAN_BODY", record);
        try {
            // 计划只能写入准备中的原实例；保存成功以后，执行器必须回读这份计划，不能用内存SQL替代。
            ModelInstanceDO stored = requirePreparing(record);
            if (plan == null || plan.getTarget() == null || plan.getTargetStructure() == null
                    || plan.getSteps() == null || !Objects.equals(stored.getDatasourceId(), plan.getTarget().getDatasourceId())) {
                throw new ServiceException("SQL计划缺失或目标数据源不属于本次实例");
            }
            for (int index = 0; index < plan.getSteps().size(); index++) {
                ModelSqlExecutionStep step = plan.getSteps().get(index);
                if (step == null || !Integer.valueOf(index + 1).equals(step.getStepNo())
                        || step.getForwardSql() == null || step.getForwardSql().trim().isEmpty()
                        || step.getStatus() != ModelSqlStepStatus.PENDING) {
                    throw new ServiceException("逐条SQL计划的顺序、语句或初始状态不正确");
                }
            }
            ModelInstanceDO update = new ModelInstanceDO();
            update.setId(record.getId());
            update.setCurrentStage(ModelPublishStage.PLAN.name());
            update.setExecutionPlan(JSON.toJSONString(plan));
            update.setStructureHash(structureHash(plan));
            if (record.getBaseInstanceId() != null && !plan.getSteps().isEmpty()) {
                // 模型配置可能没变化，但物理表或字段丢失后补建仍属于结构变化，不能把记录写成“无变化”。
                if (ModelPublishChangeStatus.NO_CHANGE.name().equals(stored.getChangeStatus())) {
                    update.setChangeStatus(ModelPublishChangeStatus.STRUCTURE_CHANGE.name());
                } else if (ModelPublishChangeStatus.METADATA_ONLY.name().equals(stored.getChangeStatus())
                        || ModelPublishChangeStatus.DISABLE.name().equals(stored.getChangeStatus())) {
                    update.setChangeStatus(ModelPublishChangeStatus.MIXED.name());
                }
            }
            if (instanceMapper.updateById(update) != 1) {
                throw new ServiceException("SQL执行计划保存失败");
            }
            append(stored, ModelPublishStage.TARGET, "SUCCEEDED", "目标身份、真实结构和数据条件已核对", true);
            append(stored, ModelPublishStage.PLAN, "SUCCEEDED", "完整执行计划已保存，共" + plan.getSteps().size()
                    + "条SQL，等待执行；当前尚未修改业务库", true);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 计划生成失败时保存具体阶段；仅允许处理尚未执行DDL的准备实例。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void failPlanning(ModelInstanceDO record, ModelPublishStage stage, String reason) {
        ModelInstanceDO stored = requirePreparing(record);
        if (stage == null || stage.ordinal() > ModelPublishStage.PLAN.ordinal()) {
            throw new ServiceException("执行阶段的失败不能按计划生成失败处理");
        }
        ModelInstanceDO update = new ModelInstanceDO();
        update.setId(record.getId());
        update.setCurrentStage(stage.name());
        update.setFailedStage(stage.name());
        update.setStatus(ModelPublishStatus.FAILED.name());
        update.setFailureReason(reason);
        update.setFinishedTime(new Date());
        if (instanceMapper.updateById(update) != 1) {
            throw new ServiceException("计划阶段失败结果保存失败");
        }
        append(stored, stage, "FAILED", reason, true);
    }

    /**
     * 查询真实实例并检查归属和准备状态，禁止覆盖已执行或已成功实例。
     */
    private ModelInstanceDO requirePreparing(ModelInstanceDO record) {
        ModelInstanceDO stored = instanceMapper.selectById(record.getId());
        if (stored == null || !Objects.equals(record.getModelId(), stored.getModelId())
                || !Objects.equals(record.getBaseInstanceId(), stored.getBaseInstanceId())
                || !Objects.equals(record.getDatasourceId(), stored.getDatasourceId())
                || !ModelPublishStatus.PREPARING.name().equals(stored.getStatus())) {
            throw new ServiceException("发布实例不存在、归属不一致或已离开准备阶段");
        }
        return stored;
    }

    /**
     * 创建发布实例和唯一的全流程日志，再将“开始读取配置”写入日志内容；任一失败一起回滚。
     *
     * @param record 已检查根归属的发布记录，可空版本和数据源用于记录早期失败
     * @return 已生成主键的发布记录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public ModelInstanceDO start(ModelInstanceDO record) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("START_RECORD_BODY", record);
        try {
            record.setStatus(ModelPublishStatus.PREPARING.name());
            record.setCurrentStage(ModelPublishStage.READ_CONFIG.name());
            record.setStartedTime(new Date());
            record.setStructureFormatVersion(1);
            if (instanceMapper.insert(record) != 1) {
                throw new ServiceException("发布记录保存失败");
            }
            // 发布实例和初始日志处于同一个独立事务：任一个保存失败都会回滚，避免产生孤立记录。
            initializeInstanceLog(record);
            append(record, "RUNNING", "开始读取本次模型配置及上次成功发布的结构", false);
            return record;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 把本次配置保存成JSON，发布仍处于准备中；不修改上次成功记录ID，也不把版本改成已发布。
     *
     * @param record   当前发布实例
     * @param snapshot 完整且独立的模型配置
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveSnapshot(ModelInstanceDO record, ModelPublishSnapshot snapshot) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("SAVE_SNAPSHOT_BODY", record);
        try {
            // 把本次完整配置单独提交保存；后续校验失败时仍能查到当时准备发布的内容。
            ModelInstanceDO update = new ModelInstanceDO();
            update.setId(record.getId());
            update.setModelSnapshot(JSON.toJSONString(snapshot));
            if (instanceMapper.updateById(update) != 1) {
                throw new ServiceException("发布配置快照保存失败");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 单独保存完整比较结果和DIFF日志；CHANGE即使随后失败，也能查到失败前实际比较出的变化。
     *
     * @param record 当前发布实例，保存前核对归属和状态
     * @param diff   本次完整新旧结构比较结果，不代表变更已获批准
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveComparison(ModelInstanceDO record, ModelPublishStructureDiff diff) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("SAVE_DIFF_BODY", record);
        try {
            if (diff == null || diff.getChangeStatus() == null
                    || !Objects.equals(record.getBaseInstanceId(), diff.getBaseInstanceId())) {
                throw new ServiceException("发布比较结果缺失或与本次实例的上次成功记录不一致");
            }
            ModelInstanceDO stored = instanceMapper.selectById(record.getId());
            if (stored == null || !Objects.equals(record.getModelId(), stored.getModelId())
                    || !Objects.equals(diff.getBaseInstanceId(), stored.getBaseInstanceId())
                    || !ModelPublishStatus.PREPARING.name().equals(stored.getStatus())) {
                throw new ServiceException("发布实例不存在、模型归属不一致或已离开准备阶段，不能覆盖比较记录");
            }
            ModelInstanceDO update = new ModelInstanceDO();
            update.setId(record.getId());
            update.setCurrentStage(ModelPublishStage.DIFF.name());
            update.setChangeStatus(diff.getChangeStatus().name());
            update.setChangeContent(JSON.toJSONString(diff));
            if (instanceMapper.updateById(update) != 1) {
                throw new ServiceException("发布比较结果保存失败");
            }
            // 比较完成只代表已得到差异，不把实例或模型版本提前改成发布成功。
            append(record, ModelPublishStage.DIFF, "SUCCEEDED", "完整结构和配置比较完成，共记录"
                    + diff.getChanges().size() + "项对象变化，等待CHANGE校验", true);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 本次配置和上次成功结构读完后，向全流程日志追加“读取完成”事件，不代表整个发布已经成功。
     *
     * @param record 当前发布实例
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void completePreparation(ModelInstanceDO record) {
        append(record, "SUCCEEDED", "本次模型配置及上次成功发布的结构已读取完成，等待CONFIG校验", true);
    }

    /**
     * 准备阶段出错时保存失败原因和日志。此时还没有执行目标SQL，所以只记录本次失败，上次成功结构保持原样。
     *
     * @param record 当前发布实例
     * @param reason 可安全记录的失败原因
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void failPreparation(ModelInstanceDO record, String reason) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("FAIL_PREPARATION_BODY", record);
        try {
            ModelInstanceDO update = new ModelInstanceDO();
            update.setId(record.getId());
            update.setStatus(ModelPublishStatus.FAILED.name());
            update.setFailedStage(ModelPublishStage.READ_CONFIG.name());
            update.setFailureReason(reason);
            update.setFinishedTime(new Date());
            if (instanceMapper.updateById(update) != 1) {
                throw new ServiceException("发布准备失败结果保存失败");
            }
            append(record, "FAILED", reason, true);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 将读取阶段记录追加到本实例的全流程日志，不新增日志行。
     *
     * @param record   当前发布实例
     * @param status   本阶段日志状态
     * @param message  阶段内容，不包含连接凭证
     * @param finished 当前阶段是否结束
     */
    private void append(ModelInstanceDO record, String status, String message, boolean finished) {
        append(record, ModelPublishStage.READ_CONFIG, status, message, finished);
    }

    /**
     * 将指定阶段追加到同一条日志，连续序号记录在LOG_CONTENT的事件列表中。
     *
     * @param record   当前实例
     * @param stage    实际完成的阶段
     * @param status   本阶段状态
     * @param message  日志说明
     * @param finished 阶段是否结束
     */
    private void append(ModelInstanceDO record, ModelPublishStage stage, String status, String message, boolean finished) {
        appendContent(record, stage, status, JSONObject.of("message", message), finished);
    }

    /**
     * 每个发布实例只在开始时创建一条日志，之后所有阶段、SQL执行、核验和回撤都更新这一行。
     *
     * @param record 刚创建的发布实例
     */
    private void initializeInstanceLog(ModelInstanceDO record) {
        ModelInstanceLogDO log = new ModelInstanceLogDO();
        log.setInstanceId(record.getId());
        log.setSequenceNo(0L);
        log.setStage(ModelPublishStage.READ_CONFIG.name());
        log.setStatus("RUNNING");
        log.setStartedTime(record.getStartedTime());
        log.setLogContent(JSONObject.of("events", new JSONArray()).toJSONString());
        log.setCreatorId(record.getCreatorId());
        log.setCreateBy(record.getCreateBy());
        log.setValidFlag(true);
        log.setDelFlag(false);
        if (logMapper.insert(log) != 1) {
            throw new ServiceException("发布全流程日志创建失败");
        }
    }

    /**
     * 读取原有全流程内容，追加本次阶段和SQL证据，再更新同一条日志；原事件不会被覆盖。
     *
     * @param record   当前发布实例
     * @param stage    本次记录的阶段
     * @param status   本次阶段或执行状态
     * @param content  本次的说明、SQL尝试、核验和回撤证据
     * @param finished 本次阶段是否结束，不代表整个发布已经结束
     */
    private void appendContent(ModelInstanceDO record, ModelPublishStage stage, String status, JSONObject content, boolean finished) {
        appendContent(record, stage, status, content, finished, null);
    }

    /**
     * 执行或补偿保存已在同一事务中读取并更新过实例时，复用该实例，省掉一次重复查询。
     * 这里只接受内部刚核对过的实例；其他调用仍从数据库读取。原日志和序号每次都重新读取，防止覆盖旧事件。
     */
    private void appendContent(ModelInstanceDO record, ModelPublishStage stage, String status, JSONObject content,
                               boolean finished, ModelInstanceDO currentInstance) {
        appendContent(record, stage, status, content, finished, currentInstance, new JSONArray());
    }

    /**
     * 同一笔平台事务可以追加开始事件和当前结果，但日志仍只做一次UPDATE。
     * 开始事件使用检查真正开始的时间，发送意图仍在DDL执行前提交。
     *
     * @param precedingEvents 需要先追加的开始事件，没有时传空数组
     */
    private void appendContent(ModelInstanceDO record, ModelPublishStage stage, String status, JSONObject content,
                               boolean finished, ModelInstanceDO currentInstance, JSONArray precedingEvents) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("SAVE_LOG_BODY", record);
        try {
            ModelPublishLogTransaction transaction = currentLogTransaction();
            ModelPublishLogState log = transaction == null ? null : transaction.find(record.getId());
            if (log == null) {
                log = readLogState(record.getId());
            }
            ModelInstanceDO instance = currentInstance == null ? instanceMapper.selectById(record.getId()) : currentInstance;
            if (instance == null || !Objects.equals(record.getId(), instance.getId())
                    || !Objects.equals(record.getModelId(), instance.getModelId())) {
                throw new ServiceException("发布实例或对应的全流程日志不存在，不能追加记录");
            }
            // 旧事件只读，另建数组追加本次事件；事务回滚时不会污染上次已提交的日志。
            JSONObject document = new JSONObject(log.document);
            JSONArray events = new JSONArray();
            events.addAll(log.document.getJSONArray("events"));
            document.put("events", events);
            long sequenceNo = log.sequenceNo;
            // 逐个分配事件序号，再把整个文档一次写回；不为开始事件另开事务或重复UPDATE。
            for (Object value : precedingEvents) {
                JSONObject preceding = (JSONObject) value;
                sequenceNo = Math.addExact(sequenceNo, 1L);
                preceding.put("sequenceNo", sequenceNo);
                document.getJSONArray("events").add(preceding);
            }
            sequenceNo = Math.addExact(sequenceNo, 1L);
            Date now = new Date();
            JSONObject event = JSONObject.of("sequenceNo", sequenceNo, "stage", stage.name(), "status", status);
            event.put("startedTime", stage == ModelPublishStage.READ_CONFIG ? instance.getStartedTime() : now);
            event.put("finishedTime", finished ? now : null);
            event.put("content", content);
            // 只往已有事件数组追加本次内容，随后UPDATE原日志行；一个发布实例始终对应一条全流程日志。
            document.getJSONArray("events").add(event);

            ModelInstanceLogDO update = new ModelInstanceLogDO();
            update.setSequenceNo(sequenceNo);
            update.setStage(stage.name());
            update.setStatus(status);
            update.setLogContent(JSON.toJSONString(document, "yyyy-MM-dd HH:mm:ss.SSS"));
            // 结束时间取整个发布实例的时间，不能把某一个阶段完成误记为全流程已经结束。
            // 用上次事件序号限制更新，避免并发写入时静默覆盖其他步骤刚追加的内容。
            LambdaUpdateWrapper<ModelInstanceLogDO> condition = new LambdaUpdateWrapper<ModelInstanceLogDO>()
                    .eq(ModelInstanceLogDO::getId, log.id)
                    .eq(ModelInstanceLogDO::getInstanceId, record.getId())
                    .eq(ModelInstanceLogDO::getSequenceNo, log.sequenceNo)
                    .set(ModelInstanceLogDO::getFinishedTime, instance.getFinishedTime());
            if (logMapper.update(update, condition) != 1) {
                if (transaction != null) {
                    transaction.scope.committed.remove(record.getId());
                }
                throw new ServiceException("发布全流程日志更新失败或已被其他步骤更新，原记录保留");
            }
            if (transaction != null) {
                transaction.pending.put(record.getId(), new ModelPublishLogState(log.id, sequenceNo, document));
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 首次使用时读取日志；后续步骤复用同一次发布内已提交的内容。
     */
    private ModelPublishLogState readLogState(Long instanceId) {
        ModelInstanceLogDO log = logMapper.selectByInstanceId(instanceId);
        if (log == null || Boolean.TRUE.equals(log.getDelFlag())) {
            throw new ServiceException("发布实例或对应的全流程日志不存在，不能追加记录");
        }
        JSONObject document;
        try {
            document = JSON.parseObject(log.getLogContent());
        } catch (RuntimeException ex) {
            throw new ServiceException("发布全流程日志内容无法读取，不能覆盖原记录");
        }
        if (document == null || !(document.get("events") instanceof JSONArray) || log.getSequenceNo() == null) {
            throw new ServiceException("发布全流程日志缺少事件列表或事件序号，不能覆盖原记录");
        }
        return new ModelPublishLogState(log.getId(), log.getSequenceNo(), document);
    }

    /**
     * 复用当前平台事务的待提交日志。REQUIRES_NEW有独立的事务回调列表，
     * 因此内外事务各管自己的内容，不会把尚未提交的事件提前交给后续事务。
     */
    private ModelPublishLogTransaction currentLogTransaction() {
        ModelPublishLogScope scope = ModelPublishLogScope.CURRENT.get();
        if (scope == null || !TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return null;
        }
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof ModelPublishLogTransaction
                    && ((ModelPublishLogTransaction) synchronization).scope == scope) {
                return (ModelPublishLogTransaction) synchronization;
            }
        }
        ModelPublishLogTransaction transaction = new ModelPublishLogTransaction(scope);
        TransactionSynchronizationManager.registerSynchronization(transaction);
        return transaction;
    }

    /**
     * 本次发布内的日志副本；请求结束即清理，不在不同请求之间共享。
     */
    private static class ModelPublishLogScope implements AutoCloseable {
        private static final ThreadLocal<ModelPublishLogScope> CURRENT = new ThreadLocal<>();
        private ModelPublishLogScope previous;
        private Map<Long, ModelPublishLogState> committed = new HashMap<>();
        private boolean closed;

        /**
         * 保存外层作用域，兼容同线程嵌套调用。
         */
        private ModelPublishLogScope() {
            previous = CURRENT.get();
            CURRENT.set(this);
        }

        /**
         * 无论成功或失败都释放日志引用，恢复外层作用域。
         */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            committed.clear();
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
            previous = null;
        }
    }

    /**
     * 日志行标识、事件序号及文档一起保存，供下一次UPDATE校验序号。
     */
    private static class ModelPublishLogState {
        private Long id;
        private long sequenceNo;
        private JSONObject document;

        /**
         * 保存一份只读的日志状态，追加事件时另建数组。
         */
        private ModelPublishLogState(Long id, long sequenceNo, JSONObject document) {
            this.id = id;
            this.sequenceNo = sequenceNo;
            this.document = document;
        }
    }

    /**
     * 只监听原有注解事务的结果，不负责开启、提交或回滚事务。
     */
    private static class ModelPublishLogTransaction implements TransactionSynchronization {
        private ModelPublishLogScope scope;
        private Map<Long, ModelPublishLogState> pending = new HashMap<>();

        /**
         * 每笔事务使用单独的待提交内容。
         */
        private ModelPublishLogTransaction(ModelPublishLogScope scope) {
            this.scope = scope;
        }

        /**
         * 同一事务多次追加时先取待提交内容，否则取上次已提交内容。
         */
        private ModelPublishLogState find(Long instanceId) {
            ModelPublishLogState state = pending.get(instanceId);
            return state == null ? scope.committed.get(instanceId) : state;
        }

        /**
         * 只有数据库提交成功，后续事务才能复用本次追加的事件。
         */
        @Override
        public void afterCommit() {
            if (!scope.closed) {
                scope.committed.putAll(pending);
            }
        }

        /**
         * 回滚后重新读取受影响日志，避免继续使用可能过期的序号。
         */
        @Override
        public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
                for (Long instanceId : pending.keySet()) {
                    scope.committed.remove(instanceId);
                }
            }
            pending.clear();
        }
    }

    /**
     * 只读取平台已保存的计划并原子切换到执行中，调用方内存里改过的SQL不会进入这里。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public ModelPublishExecutionPlan beginExecution(ModelInstanceDO record) {
        ModelInstanceDO stored = requirePreparing(record);
        if (!Objects.equals(record.getModelVersionId(), stored.getModelVersionId()) || stored.getModelVersionId() == null
                || !ModelPublishStage.PLAN.name().equals(stored.getCurrentStage())) {
            throw new ServiceException("实例尚未完成计划保存，或工作版本不属于本次发布");
        }
        ModelPublishSnapshot snapshot;
        try {
            snapshot = JSON.parseObject(stored.getModelSnapshot(), ModelPublishSnapshot.class);
        } catch (RuntimeException ex) {
            throw new ServiceException("发布配置无法读取，不能开始执行DDL");
        }
        if (snapshot == null || !Objects.equals(stored.getModelId(), snapshot.getModelId())
                || !Objects.equals(stored.getModelVersionId(), snapshot.getModelVersionId())) {
            throw new ServiceException("发布配置缺失或模型、版本不属于本次实例");
        }
        ModelPublishExecutionPlan plan = readExecutionPlan(stored);
        if (plan.getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE) {
            throw new ServiceException("准备中的计划不能已有业务库事务结果");
        }
        // 逐条检查原计划是否仍处于初始状态，步骤必须连续、依赖必须在前，且前后核验条件完整。
        for (int index = 0; index < plan.getSteps().size(); index++) {
            ModelSqlExecutionStep step = plan.getSteps().get(index);
            if (step == null || !Integer.valueOf(index + 1).equals(step.getStepNo())
                    || step.getStatus() != ModelSqlStepStatus.PENDING || !Integer.valueOf(0).equals(step.getAttemptCount())
                    || Boolean.TRUE.equals(step.getLastAttemptCompleted())
                    || Boolean.TRUE.equals(step.getLastAttemptRetryable())
                    || step.getPreconditions() == null || step.getPreconditions().isEmpty()
                    || step.getPostconditions() == null
                    || (step.getOperation() != ModelDdlOperation.COMMENT && step.getPostconditions().isEmpty())
                    || step.getDependencyStepNos() == null || step.getDependencyStepNos().stream()
                    .anyMatch(no -> no == null || no < 1 || no >= step.getStepNo())) {
                throw new ServiceException("保存的SQL顺序、初始状态、依赖或核验条件不完整，不能执行");
            }
            ModelMaterializationDbUtils.assertForwardSql(step);
        }
        ModelInstanceDO update = new ModelInstanceDO();
        update.setStatus(ModelPublishStatus.EXECUTING.name());
        update.setCurrentStage(ModelPublishStage.EXECUTE.name());
        update.setCompensationStatus(ModelCompensationStatus.NOT_REQUIRED.name());
        // 先提交“开始执行”的状态和日志，再把原计划交给DDL执行器；这里还没发送业务库SQL。
        updateExecution(stored, update);
        append(stored, ModelPublishStage.EXECUTE, "RUNNING", "已读取保存的计划，开始逐条执行；尚未发送SQL", false);
        return plan;
    }

    /**
     * 恢复只认原实例中保存的配置与计划；先保存恢复开始日志，再允许读取业务库或继续执行。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public ModelPublishContext beginRecovery(Long modelId, Long instanceId) {
        ModelInstanceDO stored = instanceMapper.selectById(instanceId);
        // 计划已完整保存但尚未开始执行，也能恢复；更早的准备中断没有完整计划，不在这里猜SQL。
        boolean prepared = stored != null && ModelPublishStatus.PREPARING.name().equals(stored.getStatus())
                && ModelPublishStage.PLAN.name().equals(stored.getCurrentStage());
        // 失败终结、正式成功或补偿中的实例不再发正向SQL。
        if (stored == null || !Objects.equals(modelId, stored.getModelId()) || stored.getModelVersionId() == null
                || !(prepared || ModelPublishStatus.EXECUTING.name().equals(stored.getStatus())
                || ModelPublishStatus.UNKNOWN.name().equals(stored.getStatus())
                || ModelPublishStatus.PARTIAL_FAILED.name().equals(stored.getStatus())
                || ModelPublishStatus.COMMIT_PENDING.name().equals(stored.getStatus()))
                || !(prepared || ModelPublishStage.EXECUTE.name().equals(stored.getCurrentStage())
                || ModelPublishStage.VERIFY.name().equals(stored.getCurrentStage()))
                || !(ModelCompensationStatus.NOT_REQUIRED.name().equals(stored.getCompensationStatus())
                || ModelCompensationStatus.PENDING.name().equals(stored.getCompensationStatus()))) {
            throw new ServiceException("实例不属于本主模型、计划未保存完整、已经结束或已进入补偿，不能恢复正向执行");
        }
        ModelPublishExecutionPlan plan = readExecutionPlan(stored);
        // 恢复先校验旧计划的顺序、发送次数和补偿状态，不能从损坏或已补偿的计划继续发正向SQL。
        validateRecoverablePlan(plan);
        if (prepared && plan.getSteps().stream().anyMatch(step -> step.getStatus() != ModelSqlStepStatus.PENDING
                || step.getAttemptCount() != 0)) {
            throw new ServiceException("准备中的计划存在执行痕迹，不能直接恢复");
        }
        if (ModelPublishStatus.COMMIT_PENDING.name().equals(stored.getStatus())
                && plan.getSteps().stream().anyMatch(step -> step.getStatus() != ModelSqlStepStatus.SUCCEEDED)) {
            throw new ServiceException("待提交实例存在未完成SQL，不能恢复");
        }
        ModelPublishContext context = savedContext(stored, plan);
        context.setStatus(ModelPublishStatus.EXECUTING);
        context.setCurrentStage(ModelPublishStage.VERIFY);
        ModelInstanceDO update = new ModelInstanceDO();
        update.setStatus(ModelPublishStatus.EXECUTING.name());
        update.setCurrentStage(ModelPublishStage.VERIFY.name());
        updateExecution(stored, update);
        append(stored, ModelPublishStage.VERIFY, "RUNNING", "开始恢复原发布实例，先查真实效果，不重建计划或直接重发SQL", false);
        return context;
    }

    /**
     * 从原记录恢复配置、目标和上次成功结构；恢复与补偿共用，不重新读取当前草稿。
     */
    private ModelPublishContext savedContext(ModelInstanceDO stored, ModelPublishExecutionPlan plan) {
        ModelPublishContext context = new ModelPublishContext();
        context.setModelId(stored.getModelId());
        context.setInstanceId(stored.getId());
        context.setModelVersionId(stored.getModelVersionId());
        context.setDatasourceId(stored.getDatasourceId());
        context.setBaseInstanceId(stored.getBaseInstanceId());
        try {
            context.setSnapshot(JSON.parseObject(stored.getModelSnapshot(), ModelPublishSnapshot.class));
        } catch (RuntimeException ex) {
            throw new ServiceException("原发布配置无法读取，不能使用当前草稿替代恢复");
        }
        if (context.getSnapshot() == null || !Objects.equals(stored.getModelId(), context.getSnapshot().getModelId())
                || !Objects.equals(stored.getModelVersionId(), context.getSnapshot().getModelVersionId())) {
            throw new ServiceException("原发布配置缺失或版本归属不一致");
        }
        // 恢复沿用原发布时引用的成功实例和旧目标，不把当前草稿的新内容混入原计划。
        if (stored.getBaseInstanceId() != null) {
            ModelInstanceDO base = instanceMapper.selectById(stored.getBaseInstanceId());
            if (base == null || !Objects.equals(stored.getModelId(), base.getModelId())
                    || !ModelPublishStatus.SUCCEEDED.name().equals(base.getStatus())) {
                throw new ServiceException("原计划引用的上次成功实例无效，不能恢复");
            }
            ModelPublishExecutionPlan basePlan = readExecutionPlan(base);
            context.setBaseTarget(basePlan.getTarget());
            context.setBaseStructure(basePlan.getTargetStructure());
            if (!Objects.equals(plan.getTarget().getDatasourceId(), context.getBaseTarget().getDatasourceId())
                    || !Objects.equals(plan.getTarget().getConnectionFingerprint(), context.getBaseTarget().getConnectionFingerprint())) {
                throw new ServiceException("原计划与上次成功实例的目标身份不一致");
            }
        }
        context.setExecutionPlan(plan);
        context.setTarget(plan.getTarget());
        context.setTargetStructure(plan.getTargetStructure());
        ModelPublishTimingUtils.bind(context);
        return context;
    }

    /**
     * 只从失败或补偿未完成的原实例开始；待提交的成功执行不能误走清理流程。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public ModelPublishContext beginCompensation(Long modelId, Long instanceId) {
        ModelInstanceDO stored = instanceMapper.selectById(instanceId);
        if (stored == null || !Objects.equals(modelId, stored.getModelId())) {
            throw new ServiceException("补偿实例不存在或不属于当前主模型");
        }
        // finished表示此前已确认清理完成，重复进入只返回原结果；continuing表示继续原补偿，不新建实例。
        boolean finished = ModelPublishStatus.FAILED.name().equals(stored.getStatus())
                && ModelCompensationStatus.SUCCEEDED.name().equals(stored.getCompensationStatus());
        boolean continuing = ModelPublishStage.COMPENSATE.name().equals(stored.getCurrentStage());
        if (!finished && (!(ModelPublishStatus.UNKNOWN.name().equals(stored.getStatus())
                || ModelPublishStatus.PARTIAL_FAILED.name().equals(stored.getStatus())
                || ModelPublishStatus.COMPENSATING.name().equals(stored.getStatus()))
                || !(continuing || ModelPublishStage.EXECUTE.name().equals(stored.getCurrentStage())
                || ModelPublishStage.VERIFY.name().equals(stored.getCurrentStage())))) {
            throw new ServiceException("只有失败或中断的执行实例可补偿，已成功、待提交及准备中的实例不允许清理");
        }
        // 先确认实例从未正式绑定成功；哪怕模型后来被停用，也不能把已正式生效的物理表当失败对象清理。
        assertNotPublished(instanceId);
        ModelPublishExecutionPlan plan = readExecutionPlan(stored);
        if (plan.getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE) {
            throw new ServiceException("事务型DDL不能走逆向删除补偿，请核对原业务库事务结果");
        }
        if (plan.getCompensationMaxRetries() == null || plan.getCompensationMaxRetries() < 0 || plan.getCompensationMaxRetries() > 3) {
            throw new ServiceException("保存计划的补偿重试次数无效");
        }
        for (int index = 0; index < plan.getSteps().size(); index++) {
            ModelSqlExecutionStep step = plan.getSteps().get(index);
            if (step == null || !Integer.valueOf(index + 1).equals(step.getStepNo()) || step.getStatus() == null
                    || step.getAttemptCount() == null || step.getAttemptCount() < 0 || step.getAttemptCount() > 1 + plan.getForwardMaxRetries()
                    || step.getCompensationStatus() == null || step.getCompensationAttemptCount() == null
                    || step.getCompensationAttemptCount() < 0 || step.getCompensationAttemptCount() > 1 + plan.getCompensationMaxRetries()
                    || step.getObject() == null || step.getPreconditions() == null || step.getPostconditions() == null) {
                throw new ServiceException("原计划缺少步骤、次数或补偿证据，停止清理");
            }
            ModelMaterializationDbUtils.assertForwardSql(step);
            if (!finished && (step.getAttemptCount() > 0 || Boolean.TRUE.equals(step.getRecoveredEffect()))
                    && step.getCompensationStatus() == ModelCompensationStatus.NOT_REQUIRED) {
                // 把尚未评估的已发送步骤明确标成待评估，不能把初始“无需补偿”误当成已经检查过。
                step.setCompensationStatus(ModelCompensationStatus.PENDING);
            }
        }
        ModelPublishContext context = savedContext(stored, plan);
        context.setCurrentStage(ModelPublishStage.COMPENSATE);
        context.setStatus(finished ? ModelPublishStatus.FAILED : ModelPublishStatus.COMPENSATING);
        if (!finished) {
            ModelInstanceDO update = new ModelInstanceDO();
            update.setStatus(ModelPublishStatus.COMPENSATING.name());
            update.setCurrentStage(ModelPublishStage.COMPENSATE.name());
            update.setCompensationStatus(ModelCompensationStatus.RUNNING.name());
            update.setExecutionPlan(JSON.toJSONString(plan));
            updateExecution(stored, update);
            append(stored, ModelPublishStage.COMPENSATE, "RUNNING", "开始逆序评估全部执行效果，只清理已证明安全的本次新增对象", false);
        }
        return context;
    }

    /**
     * 正式结构或模型成功指针一旦引用本实例，即使记录已停用、删除也禁止补偿删除。
     */
    private void assertNotPublished(Long instanceId) {
        Long count = configMapper.selectPublishedBindingCount(instanceId);
        if (count == null || count != 0) {
            throw new ServiceException("实例存在正式绑定或绑定情况无法确认，禁止补偿删除");
        }
    }

    /**
     * 对象归属来自计划中的新建声明及已提交的SQL发送、完成和成功记录，并排除旧成功对象。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public boolean ownsCompensationObject(Long modelId, Long instanceId, Integer stepNo) {
        ModelInstanceDO stored = instanceMapper.selectById(instanceId);
        if (stored == null || !Objects.equals(modelId, stored.getModelId())
                || !ModelPublishStatus.COMPENSATING.name().equals(stored.getStatus())
                || !ModelPublishStage.COMPENSATE.name().equals(stored.getCurrentStage())) {
            return false;
        }
        assertNotPublished(instanceId);
        ModelPublishExecutionPlan plan = readExecutionPlan(stored);
        if (stepNo == null || stepNo < 1 || stepNo > plan.getSteps().size()) {
            return false;
        }
        ModelSqlExecutionStep step = plan.getSteps().get(stepNo - 1);
        // 只认计划中新建的表或索引；必须有SQL成功记录，并排除原来已经存在的正式对象。
        if ((step.getOperation() != ModelDdlOperation.CREATE_TABLE && step.getOperation() != ModelDdlOperation.CREATE_INDEX)
                || !isSqlExecutedSuccessfully(step) || step.getPreconditions().stream()
                .noneMatch(condition -> condition.getType() == ModelVerificationType.OBJECT_ABSENT)) {
            return false;
        }
        ModelPublishContext context = savedContext(stored, plan);
        // 根据本次执行前的实际结构排除原有对象；历史表本次确实丢失并重建时，可以撤销本次重建效果。
        ModelMaterializedStructure existing = plan.getExistingStructure() == null ? context.getBaseStructure() : plan.getExistingStructure();
        return existing == null || existing.getTables().stream()
                .filter(table -> Objects.equals(table.getTableName(), step.getObject().getTableName()))
                .noneMatch(table -> step.getOperation() == ModelDdlOperation.CREATE_TABLE || table.getIndexes().stream()
                        .anyMatch(index -> Objects.equals(index.getIndexName(), step.getObject().getIndexName())));
    }

    /**
     * 每次补偿评估、发送及核验都单独保存；正向定义和原执行证据保持原样。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveCompensationProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan, ModelPublishStatus status,
                                         List<ModelPublishRemainingEffect> effects, String reason) {
        ModelInstanceDO stored = instanceMapper.selectById(record.getId());
        if (stored == null || !Objects.equals(record.getModelId(), stored.getModelId())
                || !Objects.equals(record.getModelVersionId(), stored.getModelVersionId())
                || !Objects.equals(record.getDatasourceId(), stored.getDatasourceId())
                || !Objects.equals(record.getBaseInstanceId(), stored.getBaseInstanceId())
                || !ModelPublishStatus.COMPENSATING.name().equals(stored.getStatus())) {
            throw new ServiceException("实例不在补偿中或归属不一致，不能覆盖补偿证据");
        }
        assertNotPublished(stored.getId());
        // 读取平台上一份补偿证据，校验本次仅推进补偿结果，没有改写原正向SQL及其执行事实。
        ModelPublishExecutionPlan previous = readExecutionPlan(stored);
        Set<Integer> changedSteps = validateCompensationProgress(previous, plan, status, effects);
        ModelInstanceDO update = new ModelInstanceDO();
        update.setStatus(status.name());
        update.setCurrentStage(ModelPublishStage.COMPENSATE.name());
        update.setExecutionPlan(JSON.toJSONString(plan));
        update.setRemainingEffects(JSON.toJSONString(effects));
        update.setFailureReason(reason);
        update.setFailedStage(ModelPublishStage.COMPENSATE.name());
        if (status == ModelPublishStatus.FAILED) {
            update.setFinishedTime(new Date());
        }
        boolean failed = plan.getSteps().stream().anyMatch(step -> step.getCompensationStatus() == ModelCompensationStatus.FAILED);
        update.setCompensationStatus(status == ModelPublishStatus.COMPENSATING ? ModelCompensationStatus.RUNNING.name()
                : status == ModelPublishStatus.FAILED ? ModelCompensationStatus.SUCCEEDED.name()
                  : status == ModelPublishStatus.UNKNOWN || failed ? ModelCompensationStatus.FAILED.name() : ModelCompensationStatus.RETAINED.name());
        JSONArray sql = new JSONArray();
        JSONArray changed = new JSONArray();
        for (int index = 0; index < plan.getSteps().size(); index++) {
            ModelSqlExecutionStep step = plan.getSteps().get(index);
            if (step.getAttemptCount() > 0 || Boolean.TRUE.equals(step.getRecoveredEffect())) {
                JSONObject item = JSONObject.of("stepNo", step.getStepNo(), "direction", "FORWARD", "attempt", step.getAttemptCount(),
                        "sql", step.getForwardSql(), "status", step.getStatus());
                item.put("dispatchState", Boolean.TRUE.equals(step.getRecoveredEffect()) ? "RECOVERED_EFFECT"
                        : step.getStatus() == ModelSqlStepStatus.SUCCEEDED ? "VERIFIED" : "SEND_INTENT_OR_SENT");
                sql.add(item);
            }
            if (step.getCompensationAttemptCount() > 0) {
                JSONObject item = JSONObject.of("stepNo", step.getStepNo(), "direction", "COMPENSATION", "attempt", step.getCompensationAttemptCount(),
                        "sql", step.getCompensationSql(), "status", step.getCompensationStatus());
                item.put("dispatchState", step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED ? "VERIFIED" : "SEND_INTENT_OR_SENT");
                sql.add(item);
            }
            if (changedSteps.contains(index)) {
                JSONObject item = JSONObject.of("stepNo", step.getStepNo(), "direction", "COMPENSATION",
                        "attempt", step.getCompensationAttemptCount(), "status", step.getCompensationStatus());
                item.put("lastAttemptCompleted", step.getCompensationLastAttemptCompleted());
                item.put("lastAttemptRetryable", step.getCompensationLastAttemptRetryable());
                item.put("preconditions", logConditions(step.getCompensationPreconditions()));
                item.put("postconditions", logConditions(step.getCompensationPostconditions()));
                item.put("reason", step.getCompensationFailureReason());
                changed.add(item);
            }
        }
        update.setExecutedSql(sql.toJSONString());
        updateExecution(stored, update);
        // 更新成功后复用本事务刚读取的实例；有结束时间时带上本次写入值，日志仍记录整个实例的结束时间。
        if (update.getFinishedTime() != null) {
            stored.setFinishedTime(update.getFinishedTime());
        }
        appendContent(stored, ModelPublishStage.COMPENSATE, status.name(), JSONObject.of("message", reason, "steps", changed, "effects", logEffects(effects)), true, stored);
    }

    /**
     * 去掉补偿结果再比较，正向执行证据、目标、SQL和所有条件定义都不能在补偿时改写。
     */
    private JSONObject compensationDefinition(ModelPublishExecutionPlan plan) {
        JSONObject value = JSON.parseObject(JSON.toJSONString(plan));
        for (Object item : value.getJSONArray("steps")) {
            JSONObject step = (JSONObject) item;
            for (String field : new String[]{"compensationStatus", "compensationAttemptCount", "compensationFailureReason",
                    "compensationLastAttemptCompleted", "compensationLastAttemptRetryable"}) {
                step.remove(field);
            }
            for (String field : new String[]{"compensationPreconditions", "compensationPostconditions"}) {
                JSONArray conditions = step.getJSONArray(field);
                if (conditions != null) {
                    for (Object condition : conditions) {
                        ((JSONObject) condition).remove("status");
                        ((JSONObject) condition).remove("failureReason");
                    }
                }
            }
        }
        return value;
    }

    /**
     * 一次只允许改一条逆序步骤；发送前要有安全条件，发送后未知不能伪装成已经清理完成。
     *
     * @return 通过校验后返回变化步骤的下标，后续日志直接复用
     */
    private Set<Integer> validateCompensationProgress(ModelPublishExecutionPlan previous, ModelPublishExecutionPlan next,
                                                      ModelPublishStatus status, List<ModelPublishRemainingEffect> effects) {
        if (next == null || effects == null || !compensationDefinition(previous).equals(compensationDefinition(next))
                || !(status == ModelPublishStatus.COMPENSATING || status == ModelPublishStatus.FAILED
                || status == ModelPublishStatus.PARTIAL_FAILED || status == ModelPublishStatus.UNKNOWN)) {
            throw new ServiceException("补偿不能修改原计划、正向证据或标记发布成功");
        }
        // 只在本次保存内算一遍差异，下面的校验和随后写日志都使用这份结果。
        Set<Integer> changedSteps = changedStepIndexes(previous, next);
        // 一次记录最多推进一个步骤的补偿；发送次数、结束标记和核验状态必须按真实发生的顺序更新。
        int changed = 0;
        for (int index = 0; index < next.getSteps().size(); index++) {
            ModelSqlExecutionStep old = previous.getSteps().get(index);
            ModelSqlExecutionStep step = next.getSteps().get(index);
            if (!changedSteps.contains(index)) {
                continue;
            }
            if (++changed > 1 || old.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED
                    || step.getCompensationStatus() == null || step.getCompensationAttemptCount() == null
                    || step.getCompensationAttemptCount() < old.getCompensationAttemptCount()
                    || step.getCompensationAttemptCount() > old.getCompensationAttemptCount() + 1
                    || step.getCompensationAttemptCount() > 1 + next.getCompensationMaxRetries()) {
                throw new ServiceException("补偿必须逐条保存，不能重置次数、越过上限或覆盖已清理的结果");
            }
            for (int later = index + 1; later < next.getSteps().size(); later++) {
                ModelSqlExecutionStep following = next.getSteps().get(later);
                if ((following.getAttemptCount() > 0 || Boolean.TRUE.equals(following.getRecoveredEffect()))
                        && following.getCompensationStatus() == ModelCompensationStatus.PENDING) {
                    throw new ServiceException("后面的执行效果尚未评估，不能跳过逆序补偿");
                }
            }
            boolean sending = step.getCompensationAttemptCount() > old.getCompensationAttemptCount();
            boolean flagsChanged = !Objects.equals(step.getCompensationLastAttemptCompleted(), old.getCompensationLastAttemptCompleted())
                    || !Objects.equals(step.getCompensationLastAttemptRetryable(), old.getCompensationLastAttemptRetryable());
            // 发送次数增加表示准备真正发送逆向SQL，必须已有RUNNING记录、正向成功证据和全部安全条件。
            if (sending) {
                if (old.getCompensationStatus() != ModelCompensationStatus.RUNNING || step.getCompensationStatus() != ModelCompensationStatus.RUNNING
                        || !(step.getOperation() == ModelDdlOperation.CREATE_INDEX || step.getOperation() == ModelDdlOperation.CREATE_TABLE)
                        || step.getStatus() != ModelSqlStepStatus.SUCCEEDED
                        || !isSqlExecutedSuccessfully(step) || !conditionsSatisfied(step.getCompensationPreconditions())
                        || Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted()) || Boolean.TRUE.equals(step.getCompensationLastAttemptRetryable())
                        || (old.getCompensationAttemptCount() > 0 && (!Boolean.TRUE.equals(old.getCompensationLastAttemptCompleted())
                        || !Boolean.TRUE.equals(old.getCompensationLastAttemptRetryable())))) {
                    throw new ServiceException("补偿发送缺少开始、归属、安全条件或上次结束及临时错误证据");
                }
                // 次数未增加但结束标记改变，说明正在保存上一次逆向SQL的结果，不能凭空补写结束证明。
            } else if (flagsChanged && (old.getCompensationStatus() != ModelCompensationStatus.RUNNING
                    || old.getCompensationAttemptCount() == 0 || Boolean.TRUE.equals(old.getCompensationLastAttemptCompleted())
                    || !Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted())
                    || !(step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED || step.getCompensationStatus() == ModelCompensationStatus.FAILED))) {
                throw new ServiceException("补偿结束证据只能在已发送后的结果保存中更新");
            }
            if (step.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED && !conditionsSatisfied(step.getCompensationPostconditions())
                    && !removedTableCovers(next, step)) {
                throw new ServiceException("没有实际不存在的后置证据，不能标记补偿成功");
            }
            if (step.getCompensationStatus() == ModelCompensationStatus.NOT_REQUIRED
                    && (step.getCompensationAttemptCount() > 0 || (step.getStatus() == ModelSqlStepStatus.SUCCEEDED
                    && (old.getCompensationStatus() != ModelCompensationStatus.NOT_REQUIRED || step.getCompensationFailureReason() != null)))) {
                throw new ServiceException("已产生效果或已经补偿的步骤不能直接改成无需处理");
            }
            if (Boolean.TRUE.equals(step.getCompensationLastAttemptRetryable()) && !Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted())) {
                throw new ServiceException("补偿尚未明确结束，不能标记可重试");
            }
        }
        // 补偿整体结束要求没有残留、没有未明确结束的尝试，所有产生效果的步骤都已确认处理完成。
        if (status == ModelPublishStatus.FAILED && (!effects.isEmpty() || next.getSteps().stream().anyMatch(step ->
                (step.getAttemptCount() > 0 && !Boolean.TRUE.equals(step.getLastAttemptCompleted())
                        && !Boolean.TRUE.equals(step.getRecoveredEffect()))
                        || (step.getStatus() == ModelSqlStepStatus.SUCCEEDED && step.getCompensationStatus() == ModelCompensationStatus.NOT_REQUIRED)
                        || (step.getCompensationAttemptCount() > 0 && !Boolean.TRUE.equals(step.getCompensationLastAttemptCompleted()))
                        || (step.getCompensationStatus() != ModelCompensationStatus.NOT_REQUIRED && step.getCompensationStatus() != ModelCompensationStatus.SUCCEEDED)))) {
            throw new ServiceException("仍有保留、未完成或未知效果，不能标记已经清理完成");
        }
        return changedSteps;
    }

    /**
     * 表已经有实际不存在的后置证明时，同表索引和注释可以随表结束，不伪造额外删除尝试。
     */
    private boolean removedTableCovers(ModelPublishExecutionPlan plan, ModelSqlExecutionStep step) {
        if (!(step.getOperation() == ModelDdlOperation.CREATE_INDEX || step.getOperation() == ModelDdlOperation.COMMENT)
                || (!Boolean.TRUE.equals(step.getLastAttemptCompleted()) && !Boolean.TRUE.equals(step.getRecoveredEffect())))
            return false;
        return plan.getSteps().stream().anyMatch(table -> table.getOperation() == ModelDdlOperation.CREATE_TABLE
                && Objects.equals(table.getObject().getTableName(), step.getObject().getTableName())
                && table.getCompensationStatus() == ModelCompensationStatus.SUCCEEDED
                && conditionsSatisfied(table.getCompensationPostconditions())
                && table.getCompensationPostconditions().stream().anyMatch(condition -> condition.getType() == ModelVerificationType.OBJECT_ABSENT));
    }

    /**
     * 步骤必须保持成功前缀，后面最多有一条已发送但未完成的SQL；任何补偿痕迹都禁止继续正向执行。
     */
    private void validateRecoverablePlan(ModelPublishExecutionPlan plan) {
        if (Boolean.TRUE.equals(plan.getDeferredProgress())) {
            validateDeferredExecutionProgress(plan, plan, ModelPublishStage.VERIFY, ModelPublishStatus.EXECUTING,
                    Collections.emptySet());
            return;
        }
        if (plan.getForwardMaxRetries() == null || plan.getForwardMaxRetries() < 0 || plan.getForwardMaxRetries() > 4) {
            throw new ServiceException("原计划重试次数无效，不能恢复");
        }
        // incomplete标记前面是否出现未完成步骤；后面再有已发送或成功记录，就说明执行顺序有问题。
        boolean incomplete = false;
        for (int index = 0; index < plan.getSteps().size(); index++) {
            ModelSqlExecutionStep step = plan.getSteps().get(index);
            if (step == null || !Integer.valueOf(index + 1).equals(step.getStepNo()) || step.getStatus() == null
                    || step.getStatus() == ModelSqlStepStatus.SKIPPED
                    || step.getAttemptCount() == null || step.getAttemptCount() < 0 || step.getAttemptCount() > 1 + plan.getForwardMaxRetries()
                    || step.getPreconditions() == null || step.getPreconditions().isEmpty()
                    || step.getPostconditions() == null
                    || (step.getOperation() != ModelDdlOperation.COMMENT && step.getPostconditions().isEmpty())
                    || step.getDependencyStepNos() == null || step.getDependencyStepNos().stream()
                    .anyMatch(no -> no == null || no < 1 || no >= step.getStepNo())
                    || !Integer.valueOf(0).equals(step.getCompensationAttemptCount())
                    || (Boolean.TRUE.equals(step.getLastAttemptRetryable()) && !Boolean.TRUE.equals(step.getLastAttemptCompleted()))
                    || (Integer.valueOf(0).equals(step.getAttemptCount()) && Boolean.TRUE.equals(step.getLastAttemptCompleted()))
                    || step.getCompensationStatus() != ModelCompensationStatus.NOT_REQUIRED) {
                throw new ServiceException("原计划步骤、次数、依赖或补偿证据不完整，不能恢复正向执行");
            }
            if (step.getStatus() == ModelSqlStepStatus.PENDING && step.getAttemptCount() > 0
                    && (!Boolean.TRUE.equals(step.getLastAttemptCompleted()) || !Boolean.TRUE.equals(step.getLastAttemptRetryable())
                    || !postconditionsNotSatisfied(step))) {
                throw new ServiceException("已发送SQL缺少恢复放行证据，不能当作待执行步骤");
            }
            ModelMaterializationDbUtils.assertForwardSql(step);
            if (step.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                if (incomplete || step.getAttemptCount() == 0 || !isSqlExecutedSuccessfully(step)) {
                    throw new ServiceException("原计划成功步骤缺少证据或执行顺序有误");
                }
            } else {
                if (incomplete && step.getAttemptCount() > 0) {
                    throw new ServiceException("前面的SQL尚未完成却存在后续发送记录，不能自动恢复");
                }
                incomplete = true;
            }
        }
    }

    /**
     * 恢复日志与最新核验结果一起保存；本方法只判断结果，不增加发送次数。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveRecoveryProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan, String reason) {
        saveProgress(record, plan, ModelPublishStage.VERIFY, ModelPublishStatus.EXECUTING, reason, true);
    }

    /**
     * 业务库事务与平台日志分开提交，保留整批提交/回滚阶段，不覆盖任何逐条SQL尝试。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public ModelPublishContext recordDdlTransaction(Long modelId, Long instanceId, ModelDdlTransactionStatus next,
                                                    ModelPublishStatus status, String message) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("SAVE_DDL_TRANSACTION_BODY", modelId, instanceId);
        try {
            ModelInstanceDO stored = instanceMapper.selectById(instanceId);
            if (stored == null || !Objects.equals(modelId, stored.getModelId())
                    || !(ModelPublishStatus.EXECUTING.name().equals(stored.getStatus())
                    || ModelPublishStatus.UNKNOWN.name().equals(stored.getStatus())
                    || ModelPublishStatus.PARTIAL_FAILED.name().equals(stored.getStatus())
                    || ModelPublishStatus.FAILED.name().equals(stored.getStatus()))) {
                throw new ServiceException("原实例不在DDL执行或待核验阶段，不能改写事务结果");
            }
            ModelPublishExecutionPlan plan = readExecutionPlan(stored);
            ModelDdlTransactionStatus before = plan.getDdlTransactionStatus();
            if (next == null) next = before;
            // 提交和回滚必须从原阶段推进；同阶段允许补记未知结果，不允许把终态改回执行中。
            boolean allowed = before == next
                    || (before == ModelDdlTransactionStatus.NONE && next == ModelDdlTransactionStatus.ACTIVE)
                    || (before == ModelDdlTransactionStatus.ACTIVE && (next == ModelDdlTransactionStatus.COMMITTING
                    || next == ModelDdlTransactionStatus.ROLLING_BACK))
                    || (before == ModelDdlTransactionStatus.COMMITTING && (next == ModelDdlTransactionStatus.COMMITTED
                    || next == ModelDdlTransactionStatus.ROLLING_BACK))
                    || (before == ModelDdlTransactionStatus.ROLLING_BACK && next == ModelDdlTransactionStatus.ROLLED_BACK);
            // 除阶段推进合法外，还要核对整批提交时每条SQL都已成功且明确结束；仅回滚完成才能登记终结失败。
            if (!allowed || next == null || !(status == ModelPublishStatus.EXECUTING || status == ModelPublishStatus.UNKNOWN
                    || (status == ModelPublishStatus.FAILED && next == ModelDdlTransactionStatus.ROLLED_BACK))
                    || (next == ModelDdlTransactionStatus.NONE)
                    || (next == ModelDdlTransactionStatus.ROLLED_BACK && status != ModelPublishStatus.FAILED)
                    || (before == ModelDdlTransactionStatus.NONE && (status != ModelPublishStatus.EXECUTING
                    || !ModelPublishStatus.EXECUTING.name().equals(stored.getStatus())))
                    || (before == ModelDdlTransactionStatus.NONE && plan.getSteps().stream().anyMatch(step -> step.getAttemptCount() > 0)
                    && !(Boolean.TRUE.equals(plan.getDeferredProgress()) && plan.getSteps().stream()
                    .allMatch(step -> step.getStatus() == ModelSqlStepStatus.PENDING && !Boolean.TRUE.equals(step.getRecoveredEffect()))))
                    || ((next == ModelDdlTransactionStatus.COMMITTING || next == ModelDdlTransactionStatus.COMMITTED)
                    && plan.getSteps().stream().anyMatch(step -> !isSqlExecutedSuccessfully(step)))) {
                throw new ServiceException("业务库事务阶段、逐SQL完成证据或发布状态不一致");
            }
            plan.setDdlTransactionStatus(next);
            ModelInstanceDO update = new ModelInstanceDO();
            update.setExecutionPlan(JSON.toJSONString(plan));
            update.setStatus(status.name());
            update.setCurrentStage(ModelPublishStage.VERIFY.name());
            if (status == ModelPublishStatus.FAILED) {
                update.setFinishedTime(new Date());
                update.setRemainingEffects("[]");
                update.setCompensationStatus(ModelCompensationStatus.NOT_REQUIRED.name());
            }
            if (status != ModelPublishStatus.EXECUTING) {
                update.setFailureReason(message);
                update.setFailedStage(ModelPublishStage.VERIFY.name());
            }
            updateExecution(stored, update);
            append(stored, ModelPublishStage.VERIFY, status.name(), message + "；业务库事务阶段：" + next.name(), true);
            ModelPublishContext context = savedContext(stored, plan);
            context.setStatus(status);
            context.setCurrentStage(ModelPublishStage.VERIFY);
            return context;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 新计划在整批结束、失败或恢复核实完成后，用独立事务统一保存结果及日志。
     * 旧计划保留逐条进度保存方式；实例和日志在同一平台事务中提交或回滚。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public void saveExecutionProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan,
                                      ModelPublishStage stage, ModelPublishStatus status, String reason) {
        saveProgress(record, plan, stage, status, reason, false);
    }

    /**
     * 复用同一套归属核对、不可变计划检查和短事务保存，恢复不能绕过原来的记录保护。
     */
    private void saveProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan,
                              ModelPublishStage stage, ModelPublishStatus status, String reason, boolean recovering) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.startRecord("SAVE_PROGRESS_BODY", record);
        try {
            ModelInstanceDO stored = instanceMapper.selectExecutionProgressById(record.getId());
            if (stored == null || !Objects.equals(record.getModelId(), stored.getModelId())
                    || !Objects.equals(record.getModelVersionId(), stored.getModelVersionId())
                    || !Objects.equals(record.getDatasourceId(), stored.getDatasourceId())
                    || !Objects.equals(record.getBaseInstanceId(), stored.getBaseInstanceId())
                    || !ModelPublishStatus.EXECUTING.name().equals(stored.getStatus())) {
                throw new ServiceException("实例已离开执行阶段或归属不一致，不能覆盖执行证据");
            }
            ModelPublishExecutionPlan previous = readExecutionPlan(stored);
            // 剔除允许变化的执行状态后比较原计划；目标、SQL、步骤依赖和核验要求不允许在执行中改写。
            if (plan == null) {
                throw new ServiceException("执行过程中不能修改目标、结构、SQL、依赖或核验条件");
            }
            // 本次计划只序列化一次；两份比较对象与原计划完全分开，删比较属性不会污染执行和回撤证据。
            // 不能直接使用JSONObject.from：当前fastjson版本会保留嵌套列表中的Java对象引用。
            String planJson = JSON.toJSONString(plan);
            JSONObject previousDocument = JSON.parseObject(JSON.toJSONString(previous));
            JSONObject currentDocument = JSON.parseObject(planJson);
            // 在精简比较对象前算好变化步骤，后续状态校验和日志直接复用，不再逐步重复序列化。
            Set<Integer> changedSteps = changedStepIndexes(previousDocument, currentDocument);
            if (Boolean.TRUE.equals(plan.getRollbackVerified())) {
                if (!Boolean.TRUE.equals(plan.getRecoveryReconciled())
                        || !(previous.getDdlTransactionStatus() == ModelDdlTransactionStatus.ACTIVE
                        || previous.getDdlTransactionStatus() == ModelDdlTransactionStatus.ROLLING_BACK
                        || previous.getDdlTransactionStatus() == ModelDdlTransactionStatus.COMMITTING)
                        || plan.getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE) {
                    throw new ServiceException("没有原DDL事务已回滚的核验结果，不能重新开始事务");
                }
                previousDocument.put("ddlTransactionStatus", ModelDdlTransactionStatus.NONE.name());
            }
            if (!immutablePlan(previousDocument).equals(immutablePlan(currentDocument))) {
                throw new ServiceException("执行过程中不能修改目标、结构、SQL、依赖或核验条件");
            }
            // 恢复核验与正常执行使用不同的状态转换校验，防止把一次只读探测伪装成新的发送尝试。
            if (Boolean.TRUE.equals(plan.getDeferredProgress())) {
                validateDeferredExecutionProgress(previous, plan, stage, status, changedSteps);
            } else if (recovering) {
                validateRecoveryProgress(previous, plan, changedSteps);
            } else {
                validateExecutionProgress(previous, plan, stage, status, changedSteps);
            }
            ModelInstanceDO update = new ModelInstanceDO();
            update.setExecutionPlan(planJson);
            update.setStatus(status.name());
            update.setCurrentStage(stage.name());
            JSONArray sqlSummary = new JSONArray();
            JSONArray effects = new JSONArray();
            for (ModelSqlExecutionStep step : plan.getSteps()) {
                if (step.getAttemptCount() > 0 || Boolean.TRUE.equals(step.getRecoveredEffect())) {
                    // 恢复确认的效果单独标明，不把结构存在冒充为原进程的发送次数或执行时间。
                    JSONObject sql = JSONObject.of("stepNo", step.getStepNo(), "direction", "FORWARD", "attempt", step.getAttemptCount(),
                            "sql", step.getForwardSql(), "status", step.getStatus());
                    sql.put("dispatchState", Boolean.TRUE.equals(step.getRecoveredEffect()) ? "RECOVERED_EFFECT"
                            : step.getStatus() == ModelSqlStepStatus.SUCCEEDED ? "VERIFIED" : "SEND_INTENT_OR_SENT");
                    sqlSummary.add(sql);
                }
                if (status != ModelPublishStatus.EXECUTING && status != ModelPublishStatus.COMMIT_PENDING
                        && (step.getStatus() == ModelSqlStepStatus.SUCCEEDED || step.getAttemptCount() > 0)) {
                    ModelPublishRemainingEffect effect = new ModelPublishRemainingEffect();
                    effect.setStepNo(step.getStepNo());
                    effect.setObject(step.getObject());
                    effect.setVerificationStatus(step.getStatus() == ModelSqlStepStatus.SUCCEEDED
                            ? ModelVerificationStatus.SATISFIED : ModelVerificationStatus.UNKNOWN);
                    effect.setReason(step.getStatus() == ModelSqlStepStatus.SUCCEEDED
                            ? "本条效果已核验，尚未处理补偿" : "已有发送意图，实际效果仍待核对");
                    // 本步不冒充读取过完整实际结构；已核验条件在步骤中，实际观察值留待恢复时读取。
                    effects.add(effect);
                }
            }
            update.setExecutedSql(sqlSummary.toJSONString());
            update.setRemainingEffects(effects.toJSONString());
            update.setCompensationStatus(effects.isEmpty() ? ModelCompensationStatus.NOT_REQUIRED.name() : ModelCompensationStatus.PENDING.name());
            if (status != ModelPublishStatus.EXECUTING && status != ModelPublishStatus.COMMIT_PENDING) {
                update.setFailureReason(reason);
                update.setFailedStage(stage.name());
                if (status == ModelPublishStatus.FAILED) {
                    update.setFinishedTime(new Date());
                }
            }
            updateExecution(stored, update);
            // 实例保存当前计划，日志另存本次变化的步骤证据，便于按时间追踪每次发送、核验和重试。
            JSONArray changed = new JSONArray();
            JSONArray startedEvents = new JSONArray();
            if (Boolean.TRUE.equals(plan.getDeferredProgress())) {
                // 每条事件的时间和结果来自实际执行时的内存记录，统一写入同一条全流程日志。
                startedEvents.addAll(plan.getExecutionEvents());
            }
            for (int index = 0; index < plan.getSteps().size(); index++) {
                ModelSqlExecutionStep step = plan.getSteps().get(index);
                if (!Boolean.TRUE.equals(plan.getDeferredProgress()) && changedSteps.contains(index)) {
                    if (!recovering && step.getCheckStartedTime() != null) {
                        startedEvents.add(sqlCheckStartedEvent(previous.getSteps().get(index), step));
                    }
                    JSONObject evidence = JSONObject.of("stepNo", step.getStepNo(), "direction", recovering ? "RECOVERY" : "FORWARD",
                            "attempt", step.getAttemptCount(), "status", step.getStatus());
                    evidence.put("lastAttemptCompleted", step.getLastAttemptCompleted());
                    evidence.put("lastAttemptRetryable", step.getLastAttemptRetryable());
                    // 完整SQL和条件定义保存在EXECUTION_PLAN；日志只追加实际有结果的条件，不重复记录PENDING。
                    JSONArray preconditions = logConditions(step.getPreconditions());
                    JSONArray postconditions = logConditions(step.getPostconditions());
                    if (!preconditions.isEmpty()) evidence.put("preconditions", preconditions);
                    if (!postconditions.isEmpty()) evidence.put("postconditions", postconditions);
                    evidence.put("reason", step.getFailureReason());
                    changed.add(evidence);
                }
            }
            // 不再重复查询刚更新的实例；结束时间必须使用本次保存值，失败回滚时日志也跟着回滚。
            if (update.getFinishedTime() != null) {
                stored.setFinishedTime(update.getFinishedTime());
            }
            appendContent(stored, stage, status.name(), JSONObject.of("message", reason, "steps", changed), true, stored, startedEvents);
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 补记本条执行准备开始的事件；发送次数取数据库旧值，不把尚未发送记成已发送。
     * 正常执行不逐条核验；完整条件仍保留在计划中，供异常恢复或回滚使用。
     *
     * @param previous 本次保存前数据库中的步骤
     * @param current  正在处理的步骤，带有本次检查开始时间
     * @return 与原全流程日志格式一致的开始事件
     */
    private JSONObject sqlCheckStartedEvent(ModelSqlExecutionStep previous, ModelSqlExecutionStep current) {
        JSONObject evidence = JSONObject.of("stepNo", current.getStepNo(), "direction", "FORWARD",
                "attempt", previous.getAttemptCount(), "status", ModelSqlStepStatus.RUNNING);
        evidence.put("lastAttemptCompleted", previous.getLastAttemptCompleted());
        evidence.put("lastAttemptRetryable", previous.getLastAttemptRetryable());
        JSONObject content = JSONObject.of("message", "开始准备第" + current.getStepNo() + "条SQL，尚未发送",
                "steps", new JSONArray(Collections.singletonList(evidence)));
        JSONObject event = JSONObject.of("stage", ModelPublishStage.EXECUTE, "status", ModelPublishStatus.EXECUTING,
                "startedTime", current.getCheckStartedTime(), "finishedTime", current.getCheckStartedTime(), "content", content);
        return event;
    }

    /**
     * 日志只保存每个条件当时的结果和原因，不重复复制整张表的预期结构。
     * conditionKey对应同一实例EXECUTION_PLAN中当前步骤的条件，完整定义仍从执行计划读取。
     * 每次都创建新的JSON对象，避免精简日志时改动真正用于执行和回撤的条件。
     *
     * @param conditions 当前步骤的前置或后置条件
     * @return 只包含条件标识、类型、状态和失败原因的日志内容
     */
    private JSONArray logConditions(List<ModelVerificationCondition> conditions) {
        JSONArray result = new JSONArray();
        for (ModelVerificationCondition condition : conditions) {
            // 没有执行过的条件没有核验结果，定义继续保留在计划中，不占用每次日志事件的篇幅。
            if (condition.getStatus() == ModelVerificationStatus.PENDING) continue;
            result.add(JSONObject.of("conditionKey", condition.getConditionKey(), "type", condition.getType(),
                    "status", condition.getStatus(), "failureReason", condition.getFailureReason()));
        }
        return result;
    }

    /**
     * 补偿日志保留哪个对象没有处理完、核验结果及原因，不在每个事件中重复保存实采表结构。
     * 完整实采结果继续写入当前实例的REMAINING_EFFECTS，不影响后续核对和恢复。
     *
     * @param effects 本次核对到的剩余效果
     * @return 不含完整实采结构的日志内容
     */
    private JSONArray logEffects(List<ModelPublishRemainingEffect> effects) {
        JSONArray result = new JSONArray();
        for (ModelPublishRemainingEffect effect : effects) {
            result.add(JSONObject.of("stepNo", effect.getStepNo(), "object", effect.getObject(),
                    "verificationStatus", effect.getVerificationStatus(), "reason", effect.getReason()));
        }
        return result;
    }

    /**
     * 结构摘要与连接身份都必须存在；不能把损坏的计划当成零SQL发布。
     */
    private ModelPublishExecutionPlan readExecutionPlan(ModelInstanceDO stored) {
        ModelPublishExecutionPlan plan;
        try {
            plan = JSON.parseObject(stored.getExecutionPlan(), ModelPublishExecutionPlan.class);
        } catch (RuntimeException ex) {
            throw new ServiceException("保存的执行计划无法读取");
        }
        // 不只检查JSON可读，还核对格式、目标身份、重试额度、完整结构和摘要，防止执行记录被替换。
        if (plan == null || plan.getDdlTransactionStatus() == null
                || !Integer.valueOf(1).equals(plan.getFormatVersion()) || plan.getTarget() == null
                || plan.getForwardMaxRetries() == null || plan.getForwardMaxRetries() < 0 || plan.getForwardMaxRetries() > 4
                || !Objects.equals(stored.getDatasourceId(), plan.getTarget().getDatasourceId())
                || plan.getTarget().getConnectionFingerprint() == null || plan.getTarget().getConnectionFingerprint().trim().isEmpty()
                || plan.getTargetStructure() == null || plan.getTargetStructure().getTables() == null
                || plan.getTargetStructure().getTables().isEmpty() || plan.getSteps() == null
                || !Objects.equals(stored.getStructureHash(), structureHash(plan))) {
            throw new ServiceException("执行计划的目标、完整结构或结构摘要无效");
        }
        return plan;
    }

    /**
     * 去掉允许变化的执行证据后比较计划，其余信息一律不能在执行中改写。
     */
    private JSONObject immutablePlan(JSONObject json) {
        // 使用本次保存已生成的独立JSON对象，不再序列化后重新解析整份计划。
        // 比较时只去掉允许改变的执行证据，SQL、目标、依赖和预期结构仍须与原计划完全一致。
        for (Object item : json.getJSONArray("steps")) {
            JSONObject step = (JSONObject) item;
            step.remove("status");
            step.remove("attemptCount");
            step.remove("lastAttemptCompleted");
            step.remove("lastAttemptRetryable");
            step.remove("failureReason");
            step.remove("recoveredEffect");
            for (String name : new String[]{"preconditions", "postconditions"}) {
                JSONArray conditions = step.getJSONArray(name);
                if (conditions != null) {
                    for (Object value : conditions) {
                        JSONObject condition = (JSONObject) value;
                        condition.remove("status");
                        condition.remove("failureReason");
                    }
                }
            }
        }
        return json;
    }

    /**
     * 整批结束时校验一次全部结果。普通SQL须有真实返回结果；恢复跳过须有真实结构核验结果。
     * 不再要求每条中间RUNNING状态已经单独落库，仍核对计划、次数、依赖和成功证据。
     */
    private void validateDeferredExecutionProgress(ModelPublishExecutionPlan previous, ModelPublishExecutionPlan next,
                                                   ModelPublishStage stage, ModelPublishStatus status,
                                                   Set<Integer> changedSteps) {
        if ((stage != ModelPublishStage.EXECUTE && stage != ModelPublishStage.VERIFY)
                || !(status == ModelPublishStatus.EXECUTING || status == ModelPublishStatus.COMMIT_PENDING
                || status == ModelPublishStatus.FAILED || status == ModelPublishStatus.PARTIAL_FAILED || status == ModelPublishStatus.UNKNOWN)
                || next.getForwardMaxRetries() == null || next.getForwardMaxRetries() < 0 || next.getForwardMaxRetries() > 4
                || previous.getSteps().size() != next.getSteps().size()) {
            throw new ServiceException("整批执行记录的阶段、状态、次数或步骤数量不正确");
        }
        boolean incomplete = false;
        for (int index = 0; index < next.getSteps().size(); index++) {
            ModelSqlExecutionStep old = previous.getSteps().get(index);
            ModelSqlExecutionStep step = next.getSteps().get(index);
            if (step == null || !Integer.valueOf(index + 1).equals(step.getStepNo()) || step.getStatus() == null
                    || step.getAttemptCount() == null || step.getAttemptCount() < old.getAttemptCount()
                    || step.getAttemptCount() > old.getAttemptCount() + 1
                    || step.getAttemptCount() > 1 + next.getForwardMaxRetries()
                    || step.getPreconditions() == null || step.getPreconditions().isEmpty() || step.getPostconditions() == null
                    || (step.getOperation() != ModelDdlOperation.COMMENT && step.getPostconditions().isEmpty())
                    || step.getCompensationAttemptCount() != 0 || step.getCompensationStatus() != ModelCompensationStatus.NOT_REQUIRED
                    || step.getDependencyStepNos() == null || step.getDependencyStepNos().stream()
                    .anyMatch(no -> no == null || no < 1 || no >= step.getStepNo())) {
                throw new ServiceException("整批SQL顺序、次数、依赖或条件不完整");
            }
            ModelMaterializationDbUtils.assertForwardSql(step);
            if (Boolean.TRUE.equals(step.getRecoveredEffect()) && (!Boolean.TRUE.equals(old.getRecoveredEffect())
                    || changedSteps.contains(index)) && !Boolean.TRUE.equals(next.getRecoveryReconciled())) {
                throw new ServiceException("尚未核实中断后的实际结构，不能把SQL标记为恢复完成");
            }
            if (old.getStatus() == ModelSqlStepStatus.SUCCEEDED && changedSteps.contains(index)
                    && !Boolean.TRUE.equals(next.getRecoveryReconciled())) {
                throw new ServiceException("已经保存的成功SQL不能在普通执行中改写");
            }
            if (step.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                if (!isSqlExecutedSuccessfully(step)) throw new ServiceException("SQL没有执行成功或恢复核实的证据");
                // 恢复核实的对象可以早已存在；本次真正发送的SQL仍须遵守原依赖顺序。
                if (!Boolean.TRUE.equals(step.getRecoveredEffect()) && step.getDependencyStepNos().stream()
                        .anyMatch(no -> next.getSteps().get(no - 1).getStatus() != ModelSqlStepStatus.SUCCEEDED)) {
                    throw new ServiceException("SQL依赖尚未完成，不能记录本次发送成功");
                }
                if (!Boolean.TRUE.equals(step.getRecoveredEffect()) && incomplete
                        && !Boolean.TRUE.equals(next.getRecoveryReconciled())) {
                    throw new ServiceException("前面的SQL未完成，不能记录后续SQL执行成功");
                }
            } else {
                incomplete = true;
                if (step.getStatus() == ModelSqlStepStatus.SKIPPED || step.getStatus() == ModelSqlStepStatus.RUNNING) {
                    throw new ServiceException("结束保存时不能把未明确完成的SQL当成成功或跳过");
                }
            }
        }
        if (status == ModelPublishStatus.COMMIT_PENDING && (incomplete
                || !(next.getDdlTransactionStatus() == ModelDdlTransactionStatus.NONE
                || next.getDdlTransactionStatus() == ModelDdlTransactionStatus.COMMITTED))) {
            throw new ServiceException("整批SQL或业务库事务尚未完成，不能保存待发布成功状态");
        }
    }

    /**
     * 恢复只能改一条未完成步骤的核验结果，不能增加次数或补写“上次已结束”的证据。
     *
     * @return 通过校验后返回变化步骤的下标，后续日志直接复用
     */
    private Set<Integer> validateRecoveryProgress(ModelPublishExecutionPlan previous, ModelPublishExecutionPlan next,
                                                  Set<Integer> changedSteps) {
        // 复用本次保存已经计算的差异，不在恢复校验中重复转换每条步骤。
        int changed = 0;
        boolean incomplete = false;
        // 恢复只能补核验结果或把可重试步骤恢复为待执行，不能增加发送次数，也不能撤销已确认成功的事实。
        for (int index = 0; index < next.getSteps().size(); index++) {
            ModelSqlExecutionStep old = previous.getSteps().get(index);
            ModelSqlExecutionStep step = next.getSteps().get(index);
            if (!changedSteps.contains(index)) {
                incomplete |= old.getStatus() != ModelSqlStepStatus.SUCCEEDED;
                continue;
            }
            if (++changed > 1 || incomplete || old.getStatus() == ModelSqlStepStatus.SUCCEEDED
                    || !Objects.equals(old.getAttemptCount(), step.getAttemptCount())
                    || !Objects.equals(old.getLastAttemptCompleted(), step.getLastAttemptCompleted())
                    || !Objects.equals(old.getLastAttemptRetryable(), step.getLastAttemptRetryable())) {
                throw new ServiceException("恢复只能按顺序核验一条未完成SQL，不能改发送次数、结束证据或已成功结果");
            }
            boolean absentAndFinished = Boolean.TRUE.equals(old.getLastAttemptCompleted())
                    && postconditionsNotSatisfied(step);
            if (step.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                if (step.getOperation() == ModelDdlOperation.COMMENT || old.getAttemptCount() == 0
                        || !conditionsSatisfied(step.getPostconditions())) {
                    throw new ServiceException("没有原发送证据或真实效果未核验，不能补记成功");
                }
            } else if (step.getStatus() == ModelSqlStepStatus.PENDING) {
                if ((old.getAttemptCount() > 0 && (!absentAndFinished || !Boolean.TRUE.equals(old.getLastAttemptRetryable())))
                        || old.getAttemptCount() >= 1 + next.getForwardMaxRetries()) {
                    throw new ServiceException("未确认上次结束且未生效，或重试次数耗尽，不能恢复发送");
                }
            } else if (step.getStatus() == ModelSqlStepStatus.FAILED) {
                // 未发送时前置明确失败；已发送时只能是永久拒绝，或核验允许重试但次数已经用尽。
                boolean unsentFailure = old.getAttemptCount() == 0 && conditionsNotSatisfied(step.getPreconditions());
                boolean permanentFailure = !Boolean.TRUE.equals(old.getLastAttemptRetryable());
                boolean exhausted = old.getAttemptCount() >= 1 + next.getForwardMaxRetries();
                boolean sentFailure = old.getAttemptCount() > 0 && absentAndFinished && (permanentFailure || exhausted);
                if (!unsentFailure && !sentFailure) {
                    throw new ServiceException("没有明确的失败或次数耗尽证据，不能记录恢复失败");
                }
            } else if (step.getStatus() != ModelSqlStepStatus.UNKNOWN) {
                throw new ServiceException("恢复核验不能直接进入发送状态");
            }
            incomplete = step.getStatus() != ModelSqlStepStatus.SUCCEEDED;
        }
        return changedSteps;
    }

    /**
     * 按原来的JSON文本比较规则找出变化步骤，每条新旧步骤各序列化一次。
     * 只供本次保存的校验和日志共用，不缓存到下一次保存，避免把旧证据当成数据库当前值。
     */
    private Set<Integer> changedStepIndexes(ModelPublishExecutionPlan previous, ModelPublishExecutionPlan next) {
        Set<Integer> changedSteps = new HashSet<>();
        for (int index = 0; index < next.getSteps().size(); index++) {
            if (!JSON.toJSONString(previous.getSteps().get(index)).equals(JSON.toJSONString(next.getSteps().get(index)))) {
                changedSteps.add(index);
            }
        }
        return changedSteps;
    }

    /**
     * 复用本次已转换的计划对象比较步骤，避免逐条重复序列化完整SQL及条件定义。
     *
     * @param previous 保存前的计划JSON，不在此处修改
     * @param next     本次待保存的计划JSON，不在此处修改
     * @return 有变化的步骤下标，字段数量变化仍由后续完整计划比较拦截
     */
    private Set<Integer> changedStepIndexes(JSONObject previous, JSONObject next) {
        JSONArray oldSteps = previous.getJSONArray("steps");
        JSONArray newSteps = next.getJSONArray("steps");
        Set<Integer> changed = new HashSet<>();
        for (int index = 0; index < newSteps.size(); index++) {
            if (index >= oldSteps.size() || !Objects.equals(oldSteps.get(index), newSteps.get(index))) {
                changed.add(index);
            }
        }
        return changed;
    }

    /**
     * 实际发送须正常结束；中断恢复须另有结构核实证据，不能虚构原发送次数。
     */
    private boolean isSqlExecutedSuccessfully(ModelSqlExecutionStep step) {
        return step.getStatus() == ModelSqlStepStatus.SUCCEEDED && step.getAttemptCount() != null
                && ((step.getAttemptCount() > 0 && Boolean.TRUE.equals(step.getLastAttemptCompleted()))
                || (Boolean.TRUE.equals(step.getRecoveredEffect()) && step.getOperation() != ModelDdlOperation.COMMENT
                && conditionsSatisfied(step.getPostconditions())));
    }

    /**
     * 注释恢复只依据已结束但未成功的执行记录，其他DDL仍依据真实效果不满足的证据。
     */
    private boolean postconditionsNotSatisfied(ModelSqlExecutionStep step) {
        if (step.getOperation() == ModelDdlOperation.COMMENT) {
            return step.getStatus() != ModelSqlStepStatus.SUCCEEDED && step.getAttemptCount() != null
                    && step.getAttemptCount() > 0 && Boolean.TRUE.equals(step.getLastAttemptCompleted());
        }
        return conditionsNotSatisfied(step.getPostconditions());
    }

    /**
     * 必须有条件且每项都核验通过，空条件不能表示成功。
     */
    private boolean conditionsSatisfied(List<ModelVerificationCondition> conditions) {
        return conditions != null && !conditions.isEmpty() && conditions.stream()
                .allMatch(condition -> condition != null && condition.getStatus() == ModelVerificationStatus.SATISFIED);
    }

    /**
     * 至少有一项明确不满足，且没有读取未知的条件；后续尚未检查的条件可以保持PENDING。
     */
    private boolean conditionsNotSatisfied(List<ModelVerificationCondition> conditions) {
        return conditions != null && !conditions.isEmpty() && conditions.stream()
                .noneMatch(condition -> condition == null || condition.getStatus() == ModelVerificationStatus.UNKNOWN)
                && conditions.stream().anyMatch(condition -> condition.getStatus() == ModelVerificationStatus.NOT_SATISFIED);
    }

    /**
     * 每次发送都要先保存开始及意图；只有恢复放行后才能重试，不能重发未知SQL或提前标记整体成功。
     *
     * @return 通过校验后返回变化步骤的下标，后续日志直接复用
     */
    private Set<Integer> validateExecutionProgress(ModelPublishExecutionPlan previous, ModelPublishExecutionPlan next,
                                                   ModelPublishStage stage, ModelPublishStatus status,
                                                   Set<Integer> changedSteps) {
        if ((stage != ModelPublishStage.EXECUTE && stage != ModelPublishStage.VERIFY)
                || (status != ModelPublishStatus.EXECUTING && status != ModelPublishStatus.COMMIT_PENDING
                && status != ModelPublishStatus.FAILED && status != ModelPublishStatus.PARTIAL_FAILED && status != ModelPublishStatus.UNKNOWN)) {
            throw new ServiceException("本步不能保存该阶段或整体发布成功状态");
        }
        // 差异由保存入口统一计算，发送次数、成功证据和步骤顺序的检查仍逐项保留。
        boolean unresolved = false;
        boolean hasEffect = false;
        boolean allSucceeded = true;
        for (int index = 0; index < next.getSteps().size(); index++) {
            ModelSqlExecutionStep old = previous.getSteps().get(index);
            ModelSqlExecutionStep step = next.getSteps().get(index);
            if (step.getStatus() == null || step.getAttemptCount() == null || step.getAttemptCount() < 0
                    || step.getAttemptCount() > 1 + next.getForwardMaxRetries()) {
                throw new ServiceException("SQL状态、尝试次数或重试上限无效");
            }
            // 已成功的步骤不再改写；后续扩长或加列可能改变早期结构，原成功证据仍作为当时的事实保留。
            if (old.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                if (changedSteps.contains(index)) {
                    throw new ServiceException("已核验成功的SQL证据不能被覆盖");
                }
                // PENDING可以一次保存开始和发送意图；检查不通过时只登记未发送的失败，不能直接记成功。
            } else if (old.getStatus() == ModelSqlStepStatus.PENDING) {
                boolean sameAttempt = Objects.equals(step.getAttemptCount(), old.getAttemptCount());
                boolean checkingFailed = stage == ModelPublishStage.EXECUTE
                        && (step.getStatus() == ModelSqlStepStatus.FAILED || step.getStatus() == ModelSqlStepStatus.UNKNOWN);
                boolean sending = step.getStatus() == ModelSqlStepStatus.RUNNING && step.getCheckStartedTime() != null
                        && step.getAttemptCount() == old.getAttemptCount() + 1;
                if (!(sameAttempt && (step.getStatus() == ModelSqlStepStatus.PENDING
                        || step.getStatus() == ModelSqlStepStatus.RUNNING || step.getStatus() == ModelSqlStepStatus.NOT_EXECUTED
                        || checkingFailed) || sending)) {
                    throw new ServiceException("SQL必须先保存发送意图，不能直接声称成功或跳过尝试次数");
                }
                // RUNNING才能保存本条发送和核验结果；次数最多增加一次，失败/成功必须与原发送证据对应。
            } else if (old.getStatus() == ModelSqlStepStatus.RUNNING) {
                if (step.getAttemptCount() < old.getAttemptCount() || step.getAttemptCount() > old.getAttemptCount() + 1
                        || (step.getStatus() != ModelSqlStepStatus.RUNNING && step.getStatus() != ModelSqlStepStatus.SUCCEEDED
                        && step.getStatus() != ModelSqlStepStatus.FAILED && step.getStatus() != ModelSqlStepStatus.UNKNOWN)
                        || (step.getStatus() == ModelSqlStepStatus.SUCCEEDED && (old.getAttemptCount() == 0
                        || !Objects.equals(old.getAttemptCount(), step.getAttemptCount())))
                        || (step.getStatus() == ModelSqlStepStatus.FAILED && (old.getAttemptCount() > 0
                        && !Boolean.TRUE.equals(old.getLastAttemptCompleted())
                        || !Objects.equals(old.getAttemptCount(), step.getAttemptCount())))) {
                    throw new ServiceException("SQL执行结果与已保存的发送证据不一致");
                }
            } else if (changedSteps.contains(index)) {
                throw new ServiceException("已停止的SQL需要先恢复核验，不能继续改写");
            }
            // 次数增加代表一次发送意图：允许合并开始事件，旧尝试必须已经明确结束并允许重试。
            if (step.getAttemptCount() > old.getAttemptCount()) {
                if ((old.getStatus() != ModelSqlStepStatus.PENDING && old.getStatus() != ModelSqlStepStatus.RUNNING)
                        || step.getStatus() != ModelSqlStepStatus.RUNNING
                        || stage != ModelPublishStage.EXECUTE || status != ModelPublishStatus.EXECUTING
                        || (old.getAttemptCount() > 0 && !Boolean.TRUE.equals(old.getLastAttemptCompleted()))
                        || (old.getAttemptCount() > 0 && !Boolean.TRUE.equals(old.getLastAttemptRetryable()))
                        || Boolean.TRUE.equals(step.getLastAttemptCompleted()) || Boolean.TRUE.equals(step.getLastAttemptRetryable())) {
                    throw new ServiceException("发送意图必须在执行阶段保存；旧尝试应已完成恢复核验且结束明确");
                }
            } else if (!Objects.equals(old.getLastAttemptCompleted(), step.getLastAttemptCompleted())
                    || !Objects.equals(old.getLastAttemptRetryable(), step.getLastAttemptRetryable())) {
                if (Boolean.TRUE.equals(old.getLastAttemptCompleted()) || Boolean.TRUE.equals(old.getLastAttemptRetryable())
                        || !Boolean.TRUE.equals(step.getLastAttemptCompleted())
                        || old.getStatus() != ModelSqlStepStatus.RUNNING || old.getAttemptCount() == 0
                        || (step.getStatus() != ModelSqlStepStatus.SUCCEEDED && step.getStatus() != ModelSqlStepStatus.UNKNOWN)) {
                    throw new ServiceException("结束证据只能在已记录发送后的实际结果保存中更新");
                }
            }
            if (Boolean.TRUE.equals(step.getLastAttemptRetryable()) && !Boolean.TRUE.equals(step.getLastAttemptCompleted())) {
                throw new ServiceException("上次尝试尚未明确结束，不能标记为可重试");
            }
            // 顺序约束也在保存证据时检查：前面仍有未成功SQL，后面就不能开始或被标成成功。
            if (step.getStatus() == ModelSqlStepStatus.RUNNING || step.getStatus() == ModelSqlStepStatus.SUCCEEDED) {
                for (int before = 0; before < index; before++) {
                    if (next.getSteps().get(before).getStatus() != ModelSqlStepStatus.SUCCEEDED) {
                        throw new ServiceException("前面的SQL尚未成功，不能开始后续步骤");
                    }
                }
            }
            if (step.getStatus() == ModelSqlStepStatus.SUCCEEDED && !isSqlExecutedSuccessfully(step)) {
                throw new ServiceException("没有SQL正常完成的记录，不能标记成功");
            }
            if (old.getAttemptCount() > 0 && Objects.equals(old.getAttemptCount(), step.getAttemptCount())
                    && !JSON.toJSONString(old.getPreconditions()).equals(JSON.toJSONString(step.getPreconditions()))) {
                throw new ServiceException("已经发送的SQL不能改写发送前的核验结果");
            }
            unresolved |= step.getStatus() == ModelSqlStepStatus.RUNNING || step.getStatus() == ModelSqlStepStatus.UNKNOWN;
            hasEffect |= step.getAttemptCount() > 0;
            allSucceeded &= step.getStatus() == ModelSqlStepStatus.SUCCEEDED;
        }
        if ((status == ModelPublishStatus.COMMIT_PENDING && (!allSucceeded || stage != ModelPublishStage.VERIFY
                || (next.getDdlTransactionStatus() != ModelDdlTransactionStatus.NONE
                && next.getDdlTransactionStatus() != ModelDdlTransactionStatus.COMMITTED)))
                || (status == ModelPublishStatus.FAILED && (hasEffect || unresolved))
                || (status == ModelPublishStatus.PARTIAL_FAILED && (unresolved || (!hasEffect && stage != ModelPublishStage.VERIFY)))) {
            throw new ServiceException("实例状态与逐条SQL的实际证据不一致");
        }
        return changedSteps;
    }

    /**
     * 比对并更新旧状态，防止迟到的写入覆盖另一条已经保存的阶段记录。
     */
    private void updateExecution(ModelInstanceDO stored, ModelInstanceDO update) {
        LambdaUpdateWrapper<ModelInstanceDO> condition = new LambdaUpdateWrapper<ModelInstanceDO>()
                .eq(ModelInstanceDO::getId, stored.getId()).eq(ModelInstanceDO::getModelId, stored.getModelId())
                .eq(ModelInstanceDO::getStatus, stored.getStatus()).eq(ModelInstanceDO::getCurrentStage, stored.getCurrentStage());
        if (ModelPublishStatus.COMMIT_PENDING.name().equals(update.getStatus())) {
            // 恢复后已通过完整核验，清掉当前失败提示；每一次历史失败仍保存在日志里。
            condition.set(ModelInstanceDO::getFailureReason, null).set(ModelInstanceDO::getFailedStage, null);
        }
        // 按读取到的旧状态与阶段更新，影响行数不是1就抛错；由调用方独立事务一起回滚实例和日志。
        if (instanceMapper.update(update, condition) != 1) {
            throw new ServiceException("发布执行状态已变化，本次记录未保存");
        }
    }

    /**
     * 使用与计划保存时相同的摘要算法核对完整目标结构。
     */
    private String structureHash(ModelPublishExecutionPlan plan) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    JSON.toJSONString(plan.getTargetStructure()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte value : digest) {
                hash.append(String.format("%02x", value & 0xff));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new ServiceException("目标结构摘要生成失败");
        }
    }

    // 分阶段校验和SQL条件核验

    /**
     * 按顺序执行阶段规则，遇到第一个错误立即停止，并保存这一个错误供失败记录使用。
     *
     * @param stage   要执行的阶段
     * @param context 本次发布数据
     */
    @Override
    public void validate(ModelValidationStage stage, ModelPublishContext context) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("VALIDATE_" + stage, context);
        try {
            if (stage == null || context == null || context.getSnapshot() == null) {
                throw new ServiceException("发布校验缺少阶段或完整模型配置");
            }
            // 同步实际阶段，后续记录错误时才能区分CONFIG、CHANGE和TARGET。
            context.setCurrentStage(ModelPublishStage.valueOf(stage.name()));
            List<ModelPublishValidationIssue> issues = new ArrayList<>();
            // 规则顺序不变，遇到第一项不满足就停止；整改后重新发布，再继续检查后面的规则。
            try {
                switch (stage) {
                    case CONFIG:
                        // CONFIG先查模型完整性、主子层级和编码，再查字段类型、长度、默认值和引用配置。
                        validateModelStructure(context, issues);
                        validateModelFields(context, issues);
                        break;
                    case CHANGE:
                        // CHANGE使用新旧差异逐项检查修订边界，例如禁止改编码、缩短长度或让旧物理对象消失。
                        validateModelChanges(context, issues);
                        break;
                    case TARGET:
                        // TARGET连接真实业务库，核对同名对象、旧表结构，以及新增必填和唯一字段涉及的历史数据。
                        validateTargetStructure(context, issues);
                        break;
                    default:
                        throw new ServiceException(stage.name() + "阶段的校验规则尚未接入，不能直接放行");
                }
            } finally {
                // 同一阶段重新检查时替换旧错误；只保存本次首个错误，不把后续字段错误拼到一起。
                context.getValidationIssues().removeIf(issue -> stage == issue.getStage());
                context.getValidationIssues().addAll(issues);
            }
            if (context.getInstanceId() != null) {
                // 有正式发布实例时才保存阶段日志；单独构建或规则检查仍可以不创建发布记录。
                modelPublishService.saveStage(context, ModelPublishStage.valueOf(stage.name()), stage.name() + "阶段校验已通过");
            }
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 逐项核对SQL的执行条件。遇到不满足或查不清楚就停止，后面的条件保持待核验。
     * 本方法只读目标库，调用方负责把结果与步骤状态一起保存。
     */
    @Override
    public boolean verifyConditions(ModelPublishContext context, ModelPublishDatabaseSession session,
                                    List<ModelVerificationCondition> conditions) {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("VERIFY_CONDITIONS", context);
        try {
            if (context == null || context.getModelId() == null || session == null || conditions == null || conditions.isEmpty()
                    || conditions.stream().anyMatch(condition -> condition == null || condition.getType() == null)) {
                throw new ServiceException("SQL核验缺少发布信息、连接或明确条件");
            }

            // 清除上次核验结果，未重新检查的条件不能沿用以前的“通过”。
            for (ModelVerificationCondition condition : conditions) {
                condition.setStatus(ModelVerificationStatus.PENDING);
                condition.setFailureReason(null);
            }
            for (ModelVerificationCondition condition : conditions) {
                try {
                    if (!sameTarget(context.getTarget(), session.getTarget())) {
                        condition.setStatus(ModelVerificationStatus.NOT_SATISFIED);
                        condition.setFailureReason("实际目标与本次发布目标不一致");
                        return false;
                    }
                    // 按条件类型调用真实结构或数据检查；false表示明确不满足，读取异常则记UNKNOWN，不能混为一谈。
                    boolean satisfied = verifyCondition(context, session, condition);
                    condition.setStatus(satisfied ? ModelVerificationStatus.SATISFIED : ModelVerificationStatus.NOT_SATISFIED);
                    if (!satisfied) {
                        condition.setFailureReason("目标数据库尚未满足条件：" + condition.getType().name());
                        return false;
                    }
                } catch (Exception ex) {
                    // 驱动异常可能包含连接地址及业务内容，仅保存固定提示，不把读取失败当成不存在。
                    session.invalidateReadConnection();
                    condition.setStatus(ModelVerificationStatus.UNKNOWN);
                    condition.setFailureReason("目标条件无法可靠核验，请检查连接、权限和核验参数");
                    return false;
                }
            }
            return true;
        } catch (RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 解释计划中的声明式条件，不执行计划或条件参数里携带的任何SQL。
     */
    private boolean verifyCondition(ModelPublishContext context, ModelPublishDatabaseSession session, ModelVerificationCondition condition) throws SQLException {
        JSONObject expected = condition.getExpected();
        if (expected == null) {
            throw new ServiceException("核验条件缺少预期信息");
        }
        if (condition.getType() == ModelVerificationType.TARGET_IDENTITY_MATCH) {
            return sameTarget(expected.to(ModelPublishTarget.class), session.getTarget());
        }
        ModelSchemaObjectRef object = condition.getObject();
        if (object == null || object.getObjectType() == null || object.getTableName() == null) {
            throw new ServiceException("核验条件缺少物理对象信息");
        }
        // 同一套核验用于执行前、执行后及安全补偿；只执行预定义检查，不接收外部传入的核验SQL。
        switch (condition.getType()) {
            case OBJECT_ABSENT:
                return ModelMaterializationDbUtils.objectAbsent(session, object);
            case OBJECT_PRESENT:
                return ModelMaterializationDbUtils.objectPresent(session, object);
            case STRUCTURE_MATCH:
                // 已保存的旧计划仍按原结构条件核验，不改原计划内容或摘要。
                if (ModelMaterializationDbUtils.objectAbsent(session, object)) {
                    return false;
                }
                switch (object.getObjectType()) {
                    case TABLE:
                        ModelMaterializedTableDefinition table = expected.to(ModelMaterializedTableDefinition.class);
                        if (!Objects.equals(object.getTableName(), table.getTableName()) || table.getColumns().isEmpty()) {
                            throw new ServiceException("表核验缺少完整预期结构");
                        }
                        return context.getCurrentStage() == ModelPublishStage.COMPENSATE
                                ? compensationTableMatches(context, session, table)
                                : ModelMaterializationDbUtils.structureProblems(session, table).isEmpty();
                    case COLUMN:
                        ModelMaterializedColumnDefinition column = expected.to(ModelMaterializedColumnDefinition.class);
                        if (!Objects.equals(object.getColumnName(), column.getColumnName()) || column.getLogicalType() == null) {
                            throw new ServiceException("列核验缺少完整预期结构");
                        }
                        return ModelMaterializationDbUtils.columnProblems(session, object.getTableName(), column).isEmpty();
                    case INDEX:
                        ModelMaterializedIndexDefinition index = expected.to(ModelMaterializedIndexDefinition.class);
                        if (!Objects.equals(object.getIndexName(), index.getIndexName()) || index.getIndexType() == null) {
                            throw new ServiceException("索引核验缺少明确预期结构");
                        }
                        return ModelMaterializationDbUtils.indexMatches(session, object.getTableName(), index);
                    default:
                        throw new ServiceException("该对象不能用于物理结构核验");
                }
            case NO_DATA:
                return ModelMaterializationDbUtils.rowCount(session, object.getTableName()) == 0;
            case NO_NULL_VALUES:
                return ModelMaterializationDbUtils.noNullValues(session, object.getTableName(), object.getColumnName());
            case NO_DUPLICATE_VALUES:
                return ModelMaterializationDbUtils.noDuplicateValues(session, object.getTableName(), expected.getList("columnNames", String.class));
            case COMMENT_MATCH:
                // 仅保留旧计划的枚举解析；执行器对注释按SQL返回结果处理，不再调用这里回读注释。
                throw new ServiceException("注释不再读取核验，应按注释SQL的执行结果处理");
                // 补偿归属必须回查平台已提交的原实例证据，不能仅凭计划里写了“本次创建”就允许删除。
            case CREATED_BY_INSTANCE:
                return context.getCurrentStage() == ModelPublishStage.COMPENSATE
                        && Objects.equals(context.getInstanceId(), expected.getLong("instanceId"))
                        && expected.getBooleanValue("notCommitted")
                        && modelPublishService.ownsCompensationObject(context.getModelId(), context.getInstanceId(), expected.getInteger("stepNo"));
            case NO_DEPENDENCIES:
                // 新计划不再生成额外依赖扫描；旧计划已有这项条件时保留原要求，不能直接当作已通过。
                throw new ServiceException("原计划包含已停止使用的依赖扫描条件，保留对象，不自动清理");
            default:
                // 未识别的条件不能根据内存参数自行放行。
                throw new ServiceException("补偿条件必须结合实际执行记录核验");
        }
    }

    /**
     * 新表清理前保留已完成的新索引参与结构核对，不能拿刚建表时只有主键的结构误判。
     * 只读取已有字段和索引核验能力，不查询视图、触发器或外键依赖。
     */
    private boolean compensationTableMatches(ModelPublishContext context, ModelPublishDatabaseSession session,
                                             ModelMaterializedTableDefinition definition) throws SQLException {
        ModelMaterializedTableDefinition expected = JSON.parseObject(
                JSON.toJSONString(definition), ModelMaterializedTableDefinition.class);
        if (context.getExecutionPlan() != null)
            for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
                if (step.getOperation() != ModelDdlOperation.CREATE_INDEX || step.getStatus() != ModelSqlStepStatus.SUCCEEDED
                        || !Boolean.TRUE.equals(step.getLastAttemptCompleted())
                        || !Objects.equals(step.getObject().getTableName(), expected.getTableName())) continue;
                for (ModelVerificationCondition condition : step.getPostconditions()) {
                    if (condition.getType() == ModelVerificationType.STRUCTURE_MATCH) {
                        ModelMaterializedIndexDefinition index = condition.getExpected().to(ModelMaterializedIndexDefinition.class);
                        if (expected.getIndexes().stream().noneMatch(item -> Objects.equals(item.getIndexName(), index.getIndexName()))) {
                            expected.getIndexes().add(index);
                        }
                    }
                }
            }
        if (!ModelMaterializationDbUtils.structureProblems(session, expected).isEmpty()) return false;
        // 正常发布允许额外性能索引，但删新表时不能顺带删除不属于原计划的外来索引。
        List<DbIndex> indexes = session.getQuery()
                .getTableIndexes(session.getProperty(), expected.getTableName());
        if (indexes == null) throw new ServiceException("新表实际索引读取失败，保留对象");
        return indexes.stream().allMatch(index -> Boolean.TRUE.equals(index.getPrimaryKey())
                || expected.getIndexes().stream().anyMatch(item -> Objects.equals(item.getIndexName(), index.getIndexName())));
    }

    /**
     * 同一数据源ID还必须对应同一个真实连接身份，空身份不能视为相同。
     */
    private boolean sameTarget(ModelPublishTarget expected, ModelPublishTarget actual) {
        return expected != null && actual != null && expected.getDatasourceId() != null
                && expected.getConnectionFingerprint() != null && !expected.getConnectionFingerprint().trim().isEmpty()
                && Objects.equals(expected.getDatasourceId(), actual.getDatasourceId())
                && Objects.equals(expected.getConnectionFingerprint(), actual.getConnectionFingerprint());
    }

    // 模型配置完整性校验

    /**
     * 先检查结构完整性，再检查模型和字段编码，遇到首个错误立即停止，不修改模型配置。
     *
     * <p>
     * CONFIG中的第一组检查：主子结构 → 模型/字段编码；字段约束及引用由后面的validateModelFields继续检查。
     *
     * @param context 本次发布数据
     * @param issues  保存具体对象和原因的错误列表
     */
    private void validateModelStructure(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        // 沿用原来的检查顺序，前一项通过后才检查下一项。
        validateStructure(context, issues);
        validateNames(context, issues);
    }

    /**
     * 检查主模型身份、直接子模型归属及字段列表，不修改工作配置。
     *
     * @param context 本次发布数据
     * @param issues  收集发现的错误
     */
    private void validateStructure(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        // 完整配置要包含主模型和直接子模型，下面逐项校验所属关系、层级和必需配置，遇到错误立即停止。
        JSONObject root = context.getSnapshot().getModel();
        if (root == null || root.isEmpty()) {
            modelStructureError(issues, "主模型", "完整模型配置为空");
            return;
        }
        Long rootId = context.getModelId();
        if (rootId == null || rootId <= 0 || !rootId.equals(root.getLong("id"))
                || !rootId.equals(context.getSnapshot().getModelId())) {
            modelStructureError(issues, "主模型", "发布请求与配置中的主模型ID不一致");
        }
        if (!Integer.valueOf(1).equals(context.getSnapshot().getFormatVersion())
                || context.getModelVersionId() == null || !context.getModelVersionId().equals(context.getSnapshot().getModelVersionId())) {
            modelStructureError(issues, "主模型", "配置格式版本不支持，或当前发布版本ID不一致");
        }
        if (!ModelPublishUtils.active(root)) {
            modelStructureError(issues, "主模型", "已封存或删除的主模型不能继续发布");
        }
        Long parentId = root.getLong("parentId");
        if (parentId != null && parentId != 0) {
            modelStructureError(issues, "主模型", "主模型不能再关联父模型");
        }
        checkArray(root, "subModels", issues);
        Set<Long> modelIds = new HashSet<>();
        Set<Long> fieldIds = new HashSet<>();
        for (JSONObject model : ModelPublishUtils.models(context)) {
            String path = ModelPublishUtils.path(model);
            if (model.get("delFlag") != null && ModelPublishUtils.flag(model, "delFlag") == null) {
                modelStructureError(issues, path, "模型删除标记只能使用布尔值或0/1");
            }
            Long id = model.getLong("id");
            if (id == null || id <= 0 || !modelIds.add(id)) {
                modelStructureError(issues, path, "模型ID缺失或重复");
            }
            if (model != root && (rootId == null || !rootId.equals(model.getLong("parentId")))) {
                modelStructureError(issues, path, "子模型必须属于本次发布的主模型");
            }
            if (model != root && !ModelPublishUtils.objects(model, "subModels").isEmpty()) {
                modelStructureError(issues, path, "本期只支持主模型和直接子模型，不能再增加下级");
            }
            if (model != root) {
                checkArray(model, "subModels", issues);
            }
            checkArray(model, "fields", issues);
            int businessCount = 0;
            int nameCount = 0;
            for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
                Long fieldId = field.getLong("id");
                if (fieldId == null || fieldId <= 0 || !fieldIds.add(fieldId)) {
                    modelStructureError(issues, path + "/字段", "字段ID缺失或重复，请先保存配置");
                }
                if (id == null || !id.equals(field.getLong("modelId"))) {
                    modelStructureError(issues, path + "/字段[" + field.getString("code") + "]", "字段不属于当前模型");
                }
                if (!Boolean.TRUE.equals(ModelPublishUtils.flag(field, "delFlag")) && !"SYSTEM".equals(field.getString("columnCategory"))) {
                    businessCount++;
                    if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, "nameFlag"))) {
                        nameCount++;
                    }
                }
            }
            if (!ModelPublishUtils.active(model)) {
                continue;
            }
            if (model.getString("name") == null || model.getString("name").trim().isEmpty()) {
                modelStructureError(issues, path, "模型名称不能为空");
            } else if (!model.getString("name").matches("[\\u4e00-\\u9fffA-Za-z0-9_-]+")) {
                // 主模型和子模型的名称使用前端相同的字符范围。
                modelStructureError(issues, path, "模型名称支持中文、英文字符、数字、下划线和短横线");
            }
            if (model == root && !"NORMAL".equals(model.getString("type")) && !"CATEGORY".equals(model.getString("type"))) {
                modelStructureError(issues, path, "主模型类型必须是NORMAL或CATEGORY");
            }
            if (businessCount == 0) {
                modelStructureError(issues, path, "至少需要一个正常使用的业务字段");
            }
            if (nameCount != 1) {
                modelStructureError(issues, path, "必须且只能指定一个正常使用的主数据名称字段");
            }
        }
    }

    /**
     * 字段和子模型必须使用对象列表，不能把错误格式当成空列表继续发布。
     *
     * @param model  所属模型
     * @param key    fields或subModels
     * @param issues 错误列表
     */
    private void checkArray(JSONObject model, String key, List<ModelPublishValidationIssue> issues) {
        Object value = model.get(key);
        if (value == null && "subModels".equals(key)) {
            return;
        }
        if (!(value instanceof List) || ((List<?>) value).size() != ModelPublishUtils.objects(model, key).size()) {
            modelStructureError(issues, ModelPublishUtils.path(model), key + "必须是完整的对象列表");
        }
    }

    /**
     * 保存一条带模型位置的错误，记录后立即提示，不继续校验后续对象。
     *
     * @param issues  错误列表
     * @param path    模型或字段位置
     * @param message 可以直接显示的原因
     */
    private void modelStructureError(List<ModelPublishValidationIssue> issues, String path, String message) {
        rejectValidation(issues, ModelValidationStage.CONFIG, "模型结构校验", path, message);
    }

    /**
     * 检查模型编码全局唯一、字段编码模型内唯一，以及通用数据库关键字和系统保留列。
     *
     * @param context 本次发布数据
     * @param issues  错误列表
     */
    private void validateNames(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        Set<String> modelCodes = new HashSet<>();
        Map<String, Long> expectedOwners = new HashMap<>();
        // 编码用于计算物理名称，这里逐个检查格式与重名；业务字段和预制字段的冲突会在字段校验中继续检查。
        for (JSONObject model : ModelPublishUtils.models(context)) {
            if (!ModelPublishUtils.active(model)) {
                continue;
            }
            String path = ModelPublishUtils.path(model);
            String code = model.getString("code");
            if (!identifier(code)) {
                // 根据根模型ID区分主子模型，主模型沿用原提示，子模型明确提示“子模型编码”。
                String modelLabel = Objects.equals(context.getModelId(), model.getLong("id")) ? "模型" : "子模型";
                modelStructureError(issues, path, modelLabel + "编码只允许英文字母、数字、下划线，且不能是通用数据库关键字");
            } else if (!modelCodes.add(code.toLowerCase(Locale.ROOT))) {
                modelStructureError(issues, path, "本次主子模型编码重复");
            }
            // 允许大写编码后，查询参数仍需匹配数据库原有的 LOWER(CODE) 查询。
            expectedOwners.put(code == null ? null : code.toLowerCase(Locale.ROOT), model.getLong("id"));
            Set<String> fieldCodes = new HashSet<>();
            for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
                if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, "delFlag"))) {
                    continue;
                }
                String fieldCode = field.getString("code");
                String fieldPath = path + "/字段[" + fieldCode + "]";
                if (fieldCode == null || !fieldCodes.add(fieldCode.toLowerCase(Locale.ROOT))) {
                    modelStructureError(issues, fieldPath, "同一模型中的字段编码不能为空或重复");
                }
                if ("SYSTEM".equals(field.getString("columnCategory"))) {
                    if (ModelSystemField.getByCode(fieldCode) == null) {
                        modelStructureError(issues, fieldPath, "系统字段必须来自平台系统字典");
                    }
                } else if (!identifier(fieldCode)) {
                    modelStructureError(issues, fieldPath, "字段编码只允许英文字母、数字、下划线，且不能是通用数据库关键字");
                } else if (ModelSystemField.getByCode(fieldCode) != null) {
                    modelStructureError(issues, fieldPath, "业务字段编码不能占用系统列名");
                }
            }
        }
        if (!modelCodes.isEmpty()) {
            // 查出占用这些编码的记录，只允许当前这一批模型使用自己的原编码，不能占用其他模型编码。
            List<Map<String, Object>> owners = configMapper.selectModelCodeOwners(new ArrayList<>(modelCodes));
            if (owners == null) {
                modelStructureError(issues, "模型编码", "无法确认全局编码是否被占用");
                return;
            }
            for (Map<String, Object> owner : owners) {
                Object id = owner.get("ID");
                if (id == null) {
                    id = owner.get("id");
                }
                Object ownerCode = owner.get("CODE") == null ? owner.get("code") : owner.get("CODE");
                Long expectedId = ownerCode == null ? null : expectedOwners.get(ownerCode.toString().toLowerCase(Locale.ROOT));
                if (id == null || expectedId == null || !expectedId.equals(Long.valueOf(id.toString()))) {
                    modelStructureError(issues, "模型编码", "模型编码已被其他模型占用");
                }
            }
        }
    }

    /**
     * 先做不依赖目标库的通用检查，目标库额外关键字和名称长度仍需TARGET阶段复验。
     *
     * @param code 原始编码
     * @return 符合命名规则且不是通用关键字时为true
     */
    private boolean identifier(String code) {
        return ModelPublishUtils.identifier(code)
                && Keywords.DEFAULT_KEYWORDS.getKeyword(code.toUpperCase(Locale.ROOT)) == null;
    }

    // 字段配置及引用校验

    /**
     * 先检查字段配置，再检查字段引用，遇到首个错误立即停止，不修改模型配置。
     *
     * <p>
     * CONFIG中的字段检查：业务字段和系统预制字段分别处理，类型/长度/精度及静态默认值都在这里检查。
     *
     * @param context 本次发布数据
     * @param issues  保存具体对象和原因的错误列表
     */
    private void validateModelFields(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        // 沿用原来的检查顺序，前一项通过后才检查下一项。
        validateFieldConfiguration(context, issues);
        validateReferences(context, issues);
    }

    /**
     * 检查每个正常使用模型中的字段，已经逻辑删除的字段由旧成功结构保留，不重新物化。
     *
     * @param context 本次发布数据
     * @param issues  错误列表
     */
    private void validateFieldConfiguration(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        // 按模型逐个检查字段：预制字段走固定字典，业务字段检查类型、长度、默认值及必填/唯一配置。
        for (JSONObject model : ModelPublishUtils.models(context)) {
            if (!ModelPublishUtils.active(model)) {
                continue;
            }
            for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
                if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, "delFlag"))) {
                    if ("SYSTEM".equals(field.getString("columnCategory"))) {
                        modelFieldError(issues, ModelPublishUtils.path(model), "系统预置字段不能单独停用或逻辑删除");
                    }
                    continue;
                }
                String path = ModelPublishUtils.path(model) + "/字段[" + field.getString("code") + "]";
                if (field.getString("name") == null || field.getString("name").trim().isEmpty()) {
                    modelFieldError(issues, path, "字段名称不能为空");
                } else if (!field.getString("name").matches("[\\u4e00-\\u9fffA-Za-z0-9_-]+")) {
                    // 字段名称与前端使用相同的字符范围。
                    modelFieldError(issues, path, "字段名称支持中文、英文字符、数字、下划线和短横线");
                }
                if (ModelColumnCategoryEnum.getByValue(field.getString("columnCategory")) == null) {
                    modelFieldError(issues, path, "字段分类必须是SYSTEM、BUSINESS、RELATION或CLASS");
                }
                for (String flag : Arrays.asList("requiredFlag", "uniqueFlag", "primaryKeyFlag", "nameFlag", "delFlag")) {
                    Object value = field.get(flag);
                    if (value != null && !Arrays.asList("0", "1", "true", "false").contains(value.toString().toLowerCase(java.util.Locale.ROOT))) {
                        modelFieldError(issues, path, flag + "必须是布尔值或0/1");
                    }
                }
                if ("SYSTEM".equals(field.getString("columnCategory"))) {
                    validateSystemField(field, path, issues);
                    ModelSystemField system = ModelSystemField.getByCode(field.getString("code"));
                    if (system == ModelSystemField.PARENT_ID && context.getModelId().equals(model.getLong("id"))) {
                        modelFieldError(issues, path, "只有子模型可以配置parent_id");
                    }
                    if (system == ModelSystemField.CATEGORY_ID && !"CATEGORY".equals(context.getSnapshot().getModel().getString("type"))) {
                        modelFieldError(issues, path, "只有分类模型可以配置category_id");
                    }
                    continue;
                }
                if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, "primaryKeyFlag"))) {
                    modelFieldError(issues, path, "只有系统ID字段可以作为主键");
                }
                String reason = validateFieldValue(field.getString("dataType"), field.getLong("dataLength"),
                        field.getLong("dataScale"), field.getString("defaultValue"));
                if (reason != null) {
                    modelFieldError(issues, path, reason);
                }
                if (("RELATION".equals(field.getString("columnCategory")) || "CLASS".equals(field.getString("columnCategory")))
                        && (field.getLong("relatedModelId") == null || field.getLongValue("relatedModelId") <= 0 || field.getString("relatedCode") == null
                        || field.getString("relatedCode").trim().isEmpty())) {
                    modelFieldError(issues, path, "关联字段必须指定关联模型和关联字段编码");
                }
                if ("AUTOMATIC".equals(field.getString("encodingMode")) && field.getLong("encodingVersionId") == null) {
                    modelFieldError(issues, path, "自动编码字段必须绑定编码方案版本");
                }
                if (field.getString("encodingMode") != null && !"AUTOMATIC".equals(field.getString("encodingMode"))
                        && !"MANUAL".equals(field.getString("encodingMode"))) {
                    modelFieldError(issues, path, "编码方式必须是MANUAL或AUTOMATIC");
                }
            }
        }
    }

    /**
     * 系统列按固定字典生成，避免把页面展示用的TEXT长度等配置误当成物理列长度。
     *
     * @param field  预制字段配置
     * @param path   字段位置
     * @param issues 错误列表
     */
    private void validateSystemField(JSONObject field, String path, List<ModelPublishValidationIssue> issues) {
        ModelSystemField system = ModelSystemField.getByCode(field.getString("code"));
        if (system == null) {
            return;
        }
        if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, "nameFlag"))) {
            modelFieldError(issues, path, "系统字段不能指定为主数据名称字段");
        }
        if (system != ModelSystemField.ID && Boolean.TRUE.equals(ModelPublishUtils.flag(field, "requiredFlag")) && system.isNullable()) {
            modelFieldError(issues, path, "不能改变系统字段的固定必填约束");
        }
        if (!system.getLogicalType().equals(field.getString("dataType"))) {
            modelFieldError(issues, path, "系统字段类型不能改变");
        }
        if (system != ModelSystemField.ID && Boolean.TRUE.equals(ModelPublishUtils.flag(field, "primaryKeyFlag"))) {
            modelFieldError(issues, path, "只有系统ID字段可以作为主键");
        }
        if (system == ModelSystemField.ID && (Boolean.FALSE.equals(ModelPublishUtils.flag(field, "requiredFlag"))
                || Boolean.FALSE.equals(ModelPublishUtils.flag(field, "primaryKeyFlag")))) {
            modelFieldError(issues, path, "系统ID必须是非空主键");
        }
        if (system != ModelSystemField.ID && Boolean.TRUE.equals(ModelPublishUtils.flag(field, "uniqueFlag"))) {
            modelFieldError(issues, path, "不能给预制系统字段新增业务唯一约束");
        }
        if (field.getString("defaultValue") != null && !field.getString("defaultValue").isEmpty()
                && !field.getString("defaultValue").equals(system.getDefaultValue())) {
            modelFieldError(issues, path, "不能修改系统字段的固定默认值");
        }
    }

    /**
     * 添加可直接定位到字段的错误，不在提示中暴露SQL或连接信息。
     *
     * @param issues  错误列表
     * @param path    字段位置
     * @param message 错误原因
     */
    private void modelFieldError(List<ModelPublishValidationIssue> issues, String path, String message) {
        rejectValidation(issues, ModelValidationStage.CONFIG, "模型字段校验", path, message);
    }

    /**
     * 检查类型、长度及默认值，返回第一条具体原因；数据库特有的上限在TARGET阶段检查。
     *
     * <p>
     * 被字段配置校验调用；返回null表示当前值合法，返回中文说明则由上层附上字段编码后立即提示。
     *
     * @param type         平台逻辑类型
     * @param length       字符串长度或小数总精度
     * @param scale        小数位数
     * @param defaultValue 静态默认值，null或空字符串表示页面未配置默认值
     * @return 合法时为null，否则返回可以直接提示的错误原因
     */
    private String validateFieldValue(String type, Long length, Long scale, String defaultValue) {
        if (!TYPES.contains(type)) {
            return "字段逻辑类型不合法";
        }
        if ("STRING".equals(type) && length != null && length <= 0) {
            return "字符串长度必须大于0，未填写时默认255";
        }
        if ("DECIMAL".equals(type)) {
            if (length == null || length <= 0 || length > Integer.MAX_VALUE
                    || scale == null || scale < 0 || scale > length) {
                return "小数必须填写总精度和小数位数，且0≤小数位数≤总精度";
            }
        } else if (scale != null) {
            return "只有DECIMAL可以填写小数位数";
        }
        if (!"STRING".equals(type) && !"DECIMAL".equals(type) && length != null) {
            return "当前字段类型不使用长度配置";
        }
        if (defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }
        try {
            // 默认值是页面传入的静态值，按字段类型分别检查；这里只校验内容，SQL字面量转换由common处理。
            switch (type) {
                case "STRING":
                    long limit = length == null ? 255L : length;
                    return defaultValue.codePointCount(0, defaultValue.length()) <= limit ? null : "默认值超过字符串长度";
                case "TEXT":
                    return null;
                case "INTEGER":
                    BigInteger integer = new BigInteger(defaultValue);
                    return integer.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
                            && integer.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 ? null : "整数默认值超出BIGINT范围";
                case "DECIMAL":
                    // 不允许把表达式当默认值；多余的小数只能是0，不能偷偷四舍五入改变用户配置。
                    if (!defaultValue.matches("[+-]?\\d+(?:\\.\\d+)?")) {
                        return "小数默认值必须是静态数字";
                    }
                    BigDecimal decimal = new BigDecimal(defaultValue).stripTrailingZeros();
                    long integerDigits = decimal.signum() == 0 ? 0L : Math.max((long) decimal.precision() - decimal.scale(), 0L);
                    return Math.max(decimal.scale(), 0) <= scale && integerDigits <= length - scale
                            ? null : "小数默认值超过配置精度或小数位数";
                case "DATE":
                    LocalDate.parse(defaultValue, DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));
                    return null;
                case "DATETIME":
                    LocalDateTime.parse(defaultValue.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME.withResolverStyle(ResolverStyle.STRICT));
                    return null;
                case "BOOLEAN":
                    return Arrays.asList("0", "1", "true", "false").contains(defaultValue.toLowerCase(java.util.Locale.ROOT))
                            ? null : "布尔默认值只能是0、1、true或false";
                default:
                    return "字段默认值无法识别";
            }
        } catch (RuntimeException ex) {
            return "默认值与字段类型、长度或精度不匹配，不支持直接填写动态表达式";
        }
    }

    /**
     * 逐字段核对引用ID，不把缺少引用或已停用的编码方案当作正常配置。
     *
     * @param context 本次发布数据
     * @param issues  错误列表
     */
    private void validateReferences(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        // 只校验本次仍使用的引用；字典、编码方案的内容来自当前配置副本，不在这里再次查询并替换。
        for (JSONObject model : ModelPublishUtils.models(context)) {
            if (!ModelPublishUtils.active(model)) {
                continue;
            }
            for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
                if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, "delFlag"))) {
                    continue;
                }
                String path = ModelPublishUtils.path(model) + "/字段[" + field.getString("code") + "]";
                Long referenceId = field.getLong("dictTypeId");
                if (referenceId != null) {
                    JSONObject reference = find(context.getSnapshot().getReferenceData(), "types", referenceId);
                    if (!available(reference)) {
                        modelFieldError(issues, path, "引用的参考数据集合不存在、已停用或已删除");
                    }
                }
                Long versionId = field.getLong("encodingVersionId");
                if (versionId != null) {
                    JSONObject version = find(context.getSnapshot().getEncodingSchemes(), "versions", versionId);
                    if (!available(version) || !Integer.valueOf(3).equals(version.getInteger("status"))) {
                        modelFieldError(issues, path, "引用的编码方案版本必须存在且已发布，不能使用草稿或停用版本");
                    } else if (!available(find(context.getSnapshot().getEncodingSchemes(), "schemes", version.getLong("encodingId")))) {
                        modelFieldError(issues, path, "编码方案主体不存在、已停用或已删除");
                    }
                }
            }
        }
    }

    /**
     * 按稳定ID从已经保存的配置中找到引用记录，不使用名称或列表位置匹配。
     *
     * @param section 配置分组
     * @param key     分组里的记录列表名
     * @param id      需要的记录ID
     * @return 对应记录，未找到返回null
     */
    private JSONObject find(JSONObject section, String key, Long id) {
        return ModelPublishUtils.objects(section, key).stream()
                .filter(row -> id != null && id.equals(row.getLong("id"))).findFirst().orElse(null);
    }

    /**
     * 记录存在、未逻辑删除且没有明确停用时才允许引用。
     *
     * @param row 引用记录
     * @return 是否可用
     */
    private boolean available(JSONObject row) {
        return row != null && !Boolean.TRUE.equals(row.getBoolean("delFlag"))
                && !Boolean.FALSE.equals(row.getBoolean("validFlag"));
    }

    // 模型变更边界校验

    /**
     * 校验完整差异，并重新核对来源权威配对；不连接目标库，不生成SQL。
     *
     * @param context 包含完整新旧结构、配置和比较结果的发布数据
     * @param issues  收集各对象的变更错误
     */
    private void validateModelChanges(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        ModelPublishStructureDiff diff = context.getDiff();
        if (diff == null || context.getTargetStructure() == null) {
            modelChangeError(issues, "主模型", "尚未完成完整结构比较，不能进入CHANGE");
            return;
        }
        if (!Objects.equals(context.getBaseInstanceId(), diff.getBaseInstanceId())
                || !Objects.equals(JSON.toJSONString(context.getTargetStructure()), JSON.toJSONString(diff.getAfterStructure()))
                || (context.getBaseStructure() != null && !Objects.equals(JSON.toJSONString(context.getBaseStructure()),
                JSON.toJSONString(diff.getBeforeStructure())))) {
            modelChangeError(issues, "主模型", "比较结果对应的结构已经变化，请重新构建和比较");
            return;
        }
        if (context.getBaseInstanceId() != null && context.getBaseSnapshot() == null) {
            modelChangeError(issues, "主模型", "缺少上次成功配置，无法检查已发布编码和模型关系");
            return;
        }
        // 新旧两份结构都检查来源/权威配对，确保一次修订不会只调整其中一套业务字段。
        checkPairs(diff.getBeforeStructure(), issues);
        checkPairs(diff.getAfterStructure(), issues);
        Set<String> oldTables = new HashSet<>();
        for (ModelMaterializedTableDefinition table : diff.getBeforeStructure().getTables()) {
            oldTables.add(table.getModelId() + ":" + table.getTableRole());
        }
        for (ModelSchemaObjectChange change : diff.getChanges()) {
            ModelSchemaObjectRef ref = change.getObject();
            if (ref.getObjectType() == ModelSchemaObjectType.MODEL) {
                continue;
            }
            String path = path(ref);
            // 旧对象从结构里直接消失属于非法缺失；逻辑删除应保留对象，只改变使用标记。
            if (change.getAfterDefinition() == null) {
                modelChangeError(issues, path, "已发布的物理对象不能消失；停用或逻辑删除必须保留原结构");
                continue;
            }
            if (change.getBeforeDefinition() == null) {
                checkAdded(change, oldTables, issues);
                continue;
            }
            if (ref.getObjectType() == ModelSchemaObjectType.COLUMN) {
                checkColumn(change, issues);
            } else {
                for (ModelSchemaPropertyChange property : change.getProperties()) {
                    if (ref.getObjectType() != ModelSchemaObjectType.TABLE || !"enabledFlag".equals(property.getProperty())) {
                        modelChangeError(issues, path, "已发布的表或索引不允许修改属性：" + property.getProperty());
                    }
                }
            }
        }
        checkPublishedConfiguration(context, oldTables, issues);
    }

    /**
     * 新表可以带完整主键和唯一；旧表新增普通业务列可以继续，目标库的数据条件后续再检查。
     */
    private void checkAdded(ModelSchemaObjectChange change, Set<String> oldTables, List<ModelPublishValidationIssue> issues) {
        ModelSchemaObjectRef ref = change.getObject();
        if (!oldTables.contains(ref.getModelId() + ":" + ref.getTableRole())) {
            return;
        }
        // 来源表补齐status属于本次明确增加的系统列；其他系统列仍按原修订规则拦截。
        boolean sourceStatus = ref.getTableRole() == ModelTableRole.SOURCE && "SYSTEM:STATUS".equals(ref.getObjectKey());
        if (ref.getObjectType() == ModelSchemaObjectType.COLUMN && !ref.getObjectKey().startsWith("FIELD:") && !sourceStatus) {
            modelChangeError(issues, path(ref), "已有表不能通过普通修订新增系统列或切换模型类型");
        }
        if (ref.getObjectType() == ModelSchemaObjectType.INDEX) {
            JSONObject index = change.getAfterDefinition();
            // 只允许给本次新增业务列建立单字段唯一；给旧字段补唯一或加其他索引不属于本期普通修订。
            boolean newBusinessUnique = "UNIQUE".equals(index.getString("indexType"))
                    && ref.getTableRole() == ModelTableRole.AUTHORITY
                    && index.getJSONArray("columnKeys") != null && index.getJSONArray("columnKeys").size() == 1;
            if (!newBusinessUnique) {
                modelChangeError(issues, path(ref), "已有表不能通过普通修订新增该索引或主键");
            }
        }
    }

    /**
     * 已有业务列允许STRING扩长、放宽必填和使用标记变化，系统列保持固定结构。
     */
    private void checkColumn(ModelSchemaObjectChange change, List<ModelPublishValidationIssue> issues) {
        JSONObject before = change.getBeforeDefinition();
        JSONObject after = change.getAfterDefinition();
        for (ModelSchemaPropertyChange property : change.getProperties()) {
            String name = property.getProperty();
            if ("enabledFlag".equals(name)) {
                // 业务列停用或恢复使用都只改元数据，原NOT NULL等数据库约束保留。
                if (change.getObject().getObjectKey().startsWith("SYSTEM:")) {
                    modelChangeError(issues, path(change.getObject()), "系统预制字段不能单独停用");
                }
                continue;
            }
            if ("nullable".equals(name) && change.getObject().getObjectKey().startsWith("FIELD:")
                    && Boolean.FALSE.equals(before.getBoolean("nullable"))
                    && Boolean.TRUE.equals(after.getBoolean("nullable"))) {
                continue;
            }
            if ("defaultValue".equals(name) && change.getObject().getObjectKey().startsWith("FIELD:")) continue;
            // 只有“属性是字符串长度、前后仍是STRING、长度明确且增大”才是允许的扩长，普通改列不等于扩长。
            boolean expansion = "characterLength".equals(name) && "STRING".equals(before.getString("logicalType"))
                    && "STRING".equals(after.getString("logicalType")) && before.getLong("characterLength") != null
                    && after.getLong("characterLength") != null && after.getLong("characterLength") > before.getLong("characterLength");
            if (!expansion || change.getObject().getObjectKey().startsWith("SYSTEM:")) {
                modelChangeError(issues, path(change.getObject()), "已发布字段只允许STRING长度扩大、放宽必填或修改静态默认值，不能修改属性：" + name);
            }
        }
    }

    /**
     * 检查新旧来源权威业务列的身份、类型和长度一致；来源可空、无业务唯一是独立约束。
     */
    private void checkPairs(ModelMaterializedStructure structure, List<ModelPublishValidationIssue> issues) {
        // 先按模型ID分组，再按SOURCE/AUTHORITY取配对表；字段再用原始字段ID一一匹配。
        Map<Long, Map<ModelTableRole, ModelMaterializedTableDefinition>> models = new LinkedHashMap<>();
        for (ModelMaterializedTableDefinition table : structure.getTables()) {
            Map<ModelTableRole, ModelMaterializedTableDefinition> roles = models.computeIfAbsent(table.getModelId(),
                    key -> new EnumMap<>(ModelTableRole.class));
            if (roles.put(table.getTableRole(), table) != null) {
                modelChangeError(issues, "模型ID=" + table.getModelId(), "同一角色的物化表重复");
            }
        }
        for (Map.Entry<Long, Map<ModelTableRole, ModelMaterializedTableDefinition>> entry : models.entrySet()) {
            ModelMaterializedTableDefinition source = entry.getValue().get(ModelTableRole.SOURCE);
            ModelMaterializedTableDefinition authority = entry.getValue().get(ModelTableRole.AUTHORITY);
            if (source == null || authority == null) {
                modelChangeError(issues, "模型ID=" + entry.getKey(), "来源表和权威表必须成对出现");
                continue;
            }
            if (!Objects.equals(source.getParentId(), authority.getParentId())
                    || !Objects.equals(source.getEnabledFlag(), authority.getEnabledFlag())) {
                modelChangeError(issues, "模型ID=" + entry.getKey(), "来源权威的主子归属和停用状态必须一致");
            }
            Map<Long, ModelMaterializedColumnDefinition> sourceFields = businessColumns(source);
            Map<Long, ModelMaterializedColumnDefinition> authorityFields = businessColumns(authority);
            if (!sourceFields.keySet().equals(authorityFields.keySet())) {
                modelChangeError(issues, "模型ID=" + entry.getKey(), "来源权威的业务字段必须同步新增或保留");
            }
            for (Map.Entry<Long, ModelMaterializedColumnDefinition> field : sourceFields.entrySet()) {
                ModelMaterializedColumnDefinition left = field.getValue();
                ModelMaterializedColumnDefinition right = authorityFields.get(field.getKey());
                if (right == null) {
                    continue;
                }
                if (!Objects.equals(left.getColumnName(), right.getColumnName())
                        || !Objects.equals(left.getLogicalType(), right.getLogicalType())

                        || !Objects.equals(left.getCharacterLength(), right.getCharacterLength())
                        || !Objects.equals(left.getLengthUnit(), right.getLengthUnit())

                        || !Objects.equals(left.getNumericPrecision(), right.getNumericPrecision())
                        || !Objects.equals(left.getNumericScale(), right.getNumericScale())

                        || !Objects.equals(left.getDefaultValue(), right.getDefaultValue())
                        || !Objects.equals(left.getEnabledFlag(), right.getEnabledFlag())) {
                    modelChangeError(issues, "模型ID=" + entry.getKey() + "/字段[" + left.getColumnName() + "]", "来源权威的业务列名称、类型、长度、精度、默认值及使用状态必须同步");
                }
                if (!Boolean.TRUE.equals(left.getNullable())) {
                    modelChangeError(issues, source.getTableName() + "/字段[" + left.getColumnName() + "]", "来源业务列必须允许为空");
                }
            }
            if (source.getIndexes().stream().anyMatch(index -> index.getIndexType() == ModelIndexType.UNIQUE)) {
                modelChangeError(issues, source.getTableName(), "来源表不能有业务唯一索引");
            }
        }
    }

    /**
     * 读取表里的全部业务列，包含停用列，用字段ID匹配。
     */
    private Map<Long, ModelMaterializedColumnDefinition> businessColumns(ModelMaterializedTableDefinition table) {
        Map<Long, ModelMaterializedColumnDefinition> result = new LinkedHashMap<>();
        for (ModelMaterializedColumnDefinition column : table.getColumns()) {
            if (column.getModelFieldId() != null) {
                result.put(column.getModelFieldId(), column);
            }
        }
        return result;
    }

    /**
     * 停用模型时Builder保留整张旧表，所以还要比较原始配置，不能让“先改编码再停用”绕过限制。
     * <p>
     * CHANGE补充检查原始配置，避免某些配置变化在物理结构保留后被掩盖；逻辑删除仍只表示退出正常使用。
     */
    private void checkPublishedConfiguration(ModelPublishContext context, Set<String> oldTables, List<ModelPublishValidationIssue> issues) {
        // 物理结构之外还要对照原模型配置，禁止通过改模型编码、父子关系或旧字段约束绕过修订限制。
        Map<Long, JSONObject> models = new LinkedHashMap<>();
        Map<Long, JSONObject> fields = new LinkedHashMap<>();
        for (JSONObject model : ModelPublishUtils.models(context)) {
            models.put(model.getLong("id"), model);
            for (JSONObject field : ModelPublishUtils.objects(model, "fields")) {
                fields.put(field.getLong("id"), field);
            }
        }
        if (context.getBaseSnapshot() == null) {
            return;
        }
        List<JSONObject> oldModels = new ArrayList<>();
        oldModels.add(context.getBaseSnapshot().getModel());
        oldModels.addAll(ModelPublishUtils.objects(context.getBaseSnapshot().getModel(), "subModels"));
        Set<Long> publishedFieldIds = new HashSet<>();
        for (ModelMaterializedTableDefinition table : context.getDiff().getBeforeStructure().getTables()) {
            publishedFieldIds.addAll(businessColumns(table).keySet());
        }
        for (JSONObject old : oldModels) {
            if (!oldTables.contains(old.getLong("id") + ":" + ModelTableRole.AUTHORITY)) {
                continue;
            }
            JSONObject current = models.get(old.getLong("id"));
            if (current == null) {
                // Builder已把历史物理表并入本次结构；当前没有该配置就沿用历史定义，不阻断发布。
                continue;
            }
            for (String property : Arrays.asList("code", "type", "parentId")) {
                if (!same(old, current, property)) {
                    modelChangeError(issues, ModelPublishUtils.path(old), "已发布模型不能修改：" + property);
                }
            }
            for (JSONObject field : ModelPublishUtils.objects(old, "fields")) {
                if (!publishedFieldIds.contains(field.getLong("id")) && !"SYSTEM".equals(field.getString("columnCategory"))) {
                    continue;
                }
                JSONObject updated = fields.get(field.getLong("id"));
                String path = ModelPublishUtils.path(old) + "/字段[" + field.getString("code") + "]";
                if (updated == null) {
                    // 当前配置未列出时，历史字段仍保留在物理结构中，不把它认作删除。
                    continue;
                }
                for (String property : Arrays.asList("modelId", "code", "dataType", "dataScale", "columnCategory", "defaultValue")) {
                    if ("defaultValue".equals(property) && !"SYSTEM".equals(field.getString("columnCategory")))
                        continue;
                    if (!same(field, updated, property)) {
                        modelChangeError(issues, path, "已发布字段不能修改：" + property);
                    }
                }
                // 与保存校验一致：必填约束只允许放宽，null与false均表示非必填。
                if (!Boolean.TRUE.equals(ModelPublishUtils.flag(field, "requiredFlag"))
                        && Boolean.TRUE.equals(ModelPublishUtils.flag(updated, "requiredFlag"))) {
                    modelChangeError(issues, path, "已发布字段不能修改约束：requiredFlag");
                }
                for (String property : Arrays.asList("uniqueFlag", "primaryKeyFlag")) {
                    if (Boolean.TRUE.equals(ModelPublishUtils.flag(field, property))
                            != Boolean.TRUE.equals(ModelPublishUtils.flag(updated, property))) {
                        modelChangeError(issues, path, "已发布字段不能修改约束：" + property);
                    }
                }
                Long before = field.getLong("dataLength");
                Long after = updated.getLong("dataLength");
                if ("STRING".equals(field.getString("dataType"))) {
                    before = before == null ? 255L : before;
                    after = after == null ? 255L : after;
                    if (after < before) {
                        modelChangeError(issues, path, "已发布STRING字段不能缩小长度");
                    }
                } else if (!Objects.equals(before, after)) {
                    modelChangeError(issues, path, "已发布字段不能修改数值精度或其他类型的长度");
                }
            }
        }
        // 旧字段新增唯一需要检查的是索引引用的字段是否早已物化，而不是只看索引名。
        for (ModelSchemaObjectChange change : context.getDiff().getChanges()) {
            if (change.getObject().getObjectType() == ModelSchemaObjectType.INDEX && change.getBeforeDefinition() == null
                    && oldTables.contains(change.getObject().getModelId() + ":" + change.getObject().getTableRole())) {
                List<String> columnKeys = change.getAfterDefinition().getList("columnKeys", String.class);
                if (columnKeys != null && columnKeys.stream().anyMatch(key -> !key.startsWith("FIELD:")
                        || publishedFieldIds.contains(Long.valueOf(key.substring("FIELD:".length()))))) {
                    modelChangeError(issues, path(change.getObject()), "不能给已经发布的字段新增唯一或其他索引");
                }
            }
        }
    }

    /**
     * 比较原始配置的稳定属性，兼容数据库整数包装类型和空字符串默认值。
     */
    private boolean same(JSONObject before, JSONObject after, String property) {
        if ("parentId".equals(property)) {
            return before.getLongValue(property) == after.getLongValue(property);
        }
        String left = before.getString(property);
        String right = after.getString(property);
        if ("defaultValue".equals(property)) {
            left = left == null || left.isEmpty() ? null : left;
            right = right == null || right.isEmpty() ? null : right;
        }
        return Objects.equals(left, right);
    }

    /**
     * 生成能找到角色、表和固定对象ID的错误位置。
     */
    private String path(ModelSchemaObjectRef ref) {
        String path = "模型ID=" + ref.getModelId() + "/" + ref.getTableRole() + "/" + ref.getTableName() + "/" + ref.getObjectKey();
        return ref.getColumnName() == null ? path : path + "/字段[" + ref.getColumnName() + "]";
    }

    /**
     * 登记并立即抛出当前变更错误，不继续检查下一项。
     */
    private void modelChangeError(List<ModelPublishValidationIssue> issues, String path, String message) {
        rejectValidation(issues, ModelValidationStage.CHANGE, "模型变更校验", path, message);
    }

    // 目标数据库结构校验

    /**
     * 旧表即使仅改名称或停用也要检查，不能因为没有DDL动作就跳过真实库核对。
     * 查询异常只提示读取失败，不把异常当成表不存在或没有冲突。
     */
    private void validateTargetStructure(ModelPublishContext context, List<ModelPublishValidationIssue> issues) {
        if (context.getDiff() == null || context.getTargetStructure() == null) {
            issue(issues, "模型", "TARGET缺少完整结构比较结果");
            return;
        }
        try (ModelPublishDatabaseSession session = modelPublishService.openTarget(context)) {
            Map<String, ModelMaterializedTableDefinition> previous = new LinkedHashMap<>();
            for (ModelMaterializedTableDefinition table : context.getDiff().getBeforeStructure().getTables()) {
                previous.put(key(table), table);
            }
            context.setExistingStructure(null);
            ModelMaterializedStructure existing = new ModelMaterializedStructure();
            Map<String, ModelMaterializedTableDefinition> actualPrevious = new LinkedHashMap<>();
            // 只在本次TARGET方法内保存实际字段列表，缺列判断和已有结构校验共用；DDL执行后不再使用。
            Map<String, List<DbColumn>> actualColumns = new LinkedHashMap<>();
            // 只统计本轮查表和读取历史字段的耗时，不额外读取目标库。
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("TARGET_READ_EXISTING", context)) {
                for (ModelMaterializedTableDefinition table : context.getTargetStructure().getTables()) {
                    ModelMaterializedTableDefinition old = previous.get(key(table));
                    int exists = session.getQuery().generateCheckTableExistsSQL(session.getProperty(), table.getTableName());
                    if (exists < 0 || exists > 1) {
                        issue(issues, table.getTableName(), "目标表存在情况无法唯一确认");
                    }
                    if (old == null) {
                        if (exists != 0 || ModelMaterializationDbUtils.namedObjectExists(session, table.getTableName())) {
                            issue(issues, table.getTableName(), "目标库已有同名表，不能覆盖或接管");
                        }
                        continue;
                    }
                    if (exists == 0) {
                        // 历史记录还在但物理表已丢失，按完整目标定义重新建表；同名视图等对象仍不能覆盖。
                        if (ModelMaterializationDbUtils.namedObjectExists(session, table.getTableName())) {
                            issue(issues, table.getTableName(), "目标库存在同名的非表对象，不能覆盖");
                        }
                        continue;
                    }
                    ModelMaterializedTableDefinition present = JSON.parseObject(JSON.toJSONString(old), ModelMaterializedTableDefinition.class);
                    List<DbColumn> columns = ModelMaterializationDbUtils.readColumns(session, table.getTableName());
                    actualColumns.put(key(table), columns);
                    Set<String> names = columns.stream().map(DbColumn::getColName).collect(Collectors.toSet());
                    Set<String> missing = old.getColumns().stream().map(ModelMaterializedColumnDefinition::getColumnName)
                            .filter(name -> !names.contains(name)).collect(Collectors.toSet());
                    // 原成功结构不动；仅从本次实际结构副本移除缺列，随后把它们识别为ADD_COLUMN。
                    present.getColumns().removeIf(column -> missing.contains(column.getColumnName()));
                    present.getIndexes().removeIf(index -> index.getColumnNames().stream().anyMatch(missing::contains));
                    actualPrevious.put(key(present), present);
                    existing.getTables().add(present);
                }
            }
            // 用本次实际结构校验待新增对象；缺表、缺列本身不再作为发布错误。
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("TARGET_CHECK_LIMITS", context)) {
                checkTargetLimits(session, context, actualPrevious, issues);
            }
            if (!issues.isEmpty()) return;
            try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("TARGET_CHECK_INDEX_NAMES", context)) {
                checkNewIndexNames(session, context, actualPrevious, issues);
            }
            for (ModelMaterializedTableDefinition table : context.getTargetStructure().getTables()) {
                ModelMaterializedTableDefinition present = actualPrevious.get(key(table));
                if (present != null) {
                    try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("TARGET_CHECK_EXISTING_TABLE", context)) {
                        checkExistingTable(session, present, actualColumns.get(key(table)), issues);
                    }
                    try (ModelPublishTimingUtils.ModelPublishTiming detailTiming = ModelPublishTimingUtils.start("TARGET_CHECK_ADDED_COLUMNS", context)) {
                        checkAddedColumns(session, present, table, issues);
                    }
                }
            }
            context.setExistingStructure(existing);
        } catch (Exception ex) {
            // 首个校验错误已经保存，不再包装成另一条“目标数据库”错误，也不继续查后面的字段。
            if (ex instanceof ServiceException && !issues.isEmpty()) {
                throw (ServiceException) ex;
            }
            String message = ex instanceof ServiceException ? ex.getMessage() : "目标库结构或数据条件读取失败，请检查权限和连接";
            issue(issues, "目标数据库", message);
        }
    }

    /**
     * 通过同一目标连接读取驱动报告的名称、关键字和类型范围，不在model里硬编码各数据库上限。
     * 只检查STRING和DECIMAL的参数，不调整日期类型、默认值或标识符大小写。
     */
    private void checkTargetLimits(ModelPublishDatabaseSession session, ModelPublishContext context,
                                   Map<String, ModelMaterializedTableDefinition> previous,
                                   List<ModelPublishValidationIssue> issues) throws SQLException {
        try (Connection connection = session.getQuery().getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            int tableLimit = metadata.getMaxTableNameLength();
            int columnLimit = metadata.getMaxColumnNameLength();
            Set<String> keywords = new HashSet<>();
            String extraKeywords = metadata.getSQLKeywords();
            if (extraKeywords != null) for (String keyword : extraKeywords.split(",")) {
                keywords.add(keyword.trim().toUpperCase(Locale.ROOT));
            }
            // 表名有mdm_前缀，还要检查原模型编码，避免前缀掩盖了业务编码里的数据库关键字。
            if (context.getSnapshot() != null) for (JSONObject model : ModelPublishUtils.models(context)) {
                if (ModelPublishUtils.active(model) && model.getString("code") != null
                        && keywords.contains(model.getString("code").toUpperCase(Locale.ROOT))) {
                    issue(issues, ModelPublishUtils.path(model), "模型编码是目标数据库关键字");
                }
            }
            List<ModelTargetTypeLimit> types = null;
            for (ModelMaterializedTableDefinition table : context.getTargetStructure().getTables()) {
                checkNameLength(table.getTableName(), tableLimit, table.getTableName(), "表名", issues);
                ModelMaterializedTableDefinition old = previous.get(key(table));
                Map<String, ModelMaterializedColumnDefinition> oldColumns = old == null ? Collections.emptyMap()
                        : old.getColumns().stream().collect(Collectors.toMap(ModelMaterializedColumnDefinition::getColumnName, item -> item));
                for (ModelMaterializedColumnDefinition column : table.getColumns()) {
                    String path = table.getTableName() + "/字段[" + column.getColumnName() + "]";
                    checkNameLength(column.getColumnName(), columnLimit, path, "字段名", issues);
                    if (!Boolean.TRUE.equals(column.getSystemFlag()) && column.getColumnName() != null
                            && keywords.contains(column.getColumnName().toUpperCase(Locale.ROOT))) {
                        issue(issues, path, "业务字段编码是目标数据库关键字");
                    }
                    ModelMaterializedColumnDefinition before = oldColumns.get(column.getColumnName());
                    // 元数据修改和停用不重新限制已经存在且参数未变的历史列。
                    if (!("STRING".equals(column.getLogicalType()) || "DECIMAL".equals(column.getLogicalType()))
                            || (before != null && Objects.equals(before.getLogicalType(), column.getLogicalType())
                            && Objects.equals(before.getCharacterLength(), column.getCharacterLength())
                            && Objects.equals(before.getNumericPrecision(), column.getNumericPrecision())
                            && Objects.equals(before.getNumericScale(), column.getNumericScale()))) {
                        continue;
                    }
                    if (types == null) types = readTypeLimits(metadata);
                    checkColumnLimits(session, column, path, types, issues);
                }
            }
        }
    }

    /**
     * JDBC名称长度返回0表示没有明确上限或驱动未知，不能把0误当成任何名称都不允许。
     */
    private void checkNameLength(String name, int limit, String path, String label, List<ModelPublishValidationIssue> issues) {
        if (name != null && limit > 0 && name.length() > limit) {
            issue(issues, path, label + "长度超过目标数据库上限" + limit);
        }
    }

    /**
     * 一次读取类型元数据，供本次全部来源和权威表共用；不读取业务表数据。
     */
    private List<ModelTargetTypeLimit> readTypeLimits(DatabaseMetaData metadata) throws SQLException {
        List<ModelTargetTypeLimit> result = new ArrayList<>();
        try (ResultSet rows = metadata.getTypeInfo()) {
            while (rows.next()) {
                ModelTargetTypeLimit type = new ModelTargetTypeLimit();
                type.name = rows.getString("TYPE_NAME");
                type.jdbcType = rows.getInt("DATA_TYPE");
                type.precision = nullableNumber(rows, "PRECISION");
                type.minScale = nullableNumber(rows, "MINIMUM_SCALE");
                type.maxScale = nullableNumber(rows, "MAXIMUM_SCALE");
                result.add(type);
            }
        }
        return result;
    }

    /**
     * NULL表示驱动没有报告该项范围，不能通过getLong的默认0把它误判成不允许任何长度。
     */
    private Long nullableNumber(ResultSet rows, String name) throws SQLException {
        long value = rows.getLong(name);
        return rows.wasNull() ? null : value;
    }

    /**
     * common负责类型转换；优先匹配驱动同名类型，别名再通过common已有标准化能力匹配。
     */
    private void checkColumnLimits(ModelPublishDatabaseSession session, ModelMaterializedColumnDefinition column, String path,
                                   List<ModelTargetTypeLimit> types, List<ModelPublishValidationIssue> issues) {
        DbColumn converted = ModelMaterializationDbUtils.column(session, column);
        String physical = typeName(converted.getDataType());
        List<ModelTargetTypeLimit> candidates = types.stream().filter(type -> typeName(type.name).equals(physical))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            String standard = typeName(session.getDialect().toStandardColumn(converted).getDataType());
            candidates = types.stream().filter(type -> compatibleType(column.getLogicalType(), type.jdbcType))
                    .filter(type -> standard.equals(typeName(session.getDialect()
                            .toStandardColumn(DbColumn.builder().dataType(type.name).build()).getDataType())))
                    .collect(Collectors.toList());
        }
        if (candidates.isEmpty()) {
            issue(issues, path, "驱动未返回对应字段类型的范围，无法完成发布前校验：" + physical);
            return;
        }
        // getTypeInfo已按最接近JDBC类型的顺序返回；同名类型不拼接不同记录的精度和小数位范围。
        // 从驱动报告中选定一种匹配的物理类型，再分别校验字符数或小数总精度、位数，不能混用不同类型上限。
        ModelTargetTypeLimit limit = candidates.get(0);
        if ("STRING".equals(column.getLogicalType())) {
            long length = column.getCharacterLength() == null ? 255L : column.getCharacterLength();
            if (length <= 0 || (limit.precision != null && limit.precision > 0 && length > limit.precision)) {
                issue(issues, path, "字符串长度超出目标数据库范围，上限：" + limit.precision);
            }
        } else {
            Integer precision = column.getNumericPrecision();
            Integer scale = column.getNumericScale();
            if (precision == null || precision <= 0 || scale == null || scale < 0 || scale > precision
                    || (limit.precision != null && limit.precision > 0 && precision > limit.precision)
                    || (limit.minScale != null && scale < limit.minScale)
                    || (limit.maxScale != null && scale > limit.maxScale)) {
                issue(issues, path, "小数精度或小数位超出目标数据库范围，总精度上限：" + limit.precision
                        + "，小数位范围：" + limit.minScale + "至" + limit.maxScale);
            }
        }
    }

    /**
     * 别名匹配限制在相同类型家族，不借用NVARCHAR、长文本或其他数值类型的范围。
     */
    private boolean compatibleType(String logical, int jdbcType) {
        return "STRING".equals(logical) ? jdbcType == Types.VARCHAR
                : jdbcType == Types.DECIMAL || jdbcType == Types.NUMERIC;
    }

    /**
     * 只移除类型参数以便比较名称，不自行改变common提供的类型映射。
     */
    private String typeName(String name) {
        if (name == null) return "";
        int parameter = name.indexOf('(');
        return (parameter < 0 ? name : name.substring(0, parameter)).trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 本次连接报告的一种类型范围，仅在TARGET内部使用，不新增平台表或对外对象。
     */
    private static class ModelTargetTypeLimit {
        /**
         * 驱动中的类型名。
         */
        private String name;
        /**
         * JDBC类型编号，用来区分字符串和数值别名。
         */
        private int jdbcType;
        /**
         * 字符串最大字符数或数值最大总位数，NULL表示未报告。
         */
        private Long precision;
        /**
         * 驱动报告的小数位下限。
         */
        private Long minScale;
        /**
         * 驱动报告的小数位上限。
         */
        private Long maxScale;
    }

    /**
     * 只检查本次计划内新增索引是否重名，不遍历目标Schema的全部表和索引。
     * 按表管理索引名的数据库允许不同表使用同名索引；其他数据库仍检查计划内跨表重名。
     * 与库中其他对象重名时，由CREATE INDEX执行报错，再沿用现有失败回撤流程。
     */
    private void checkNewIndexNames(ModelPublishDatabaseSession session, ModelPublishContext context,
                                    Map<String, ModelMaterializedTableDefinition> previous,
                                    List<ModelPublishValidationIssue> issues) {
        if (session.getDialect() instanceof MySqlDialect
                || session.getDialect() instanceof SQLServer2008Dialect) {
            return;
        }
        Map<String, String> planned = new LinkedHashMap<>();
        for (ModelMaterializedTableDefinition table : context.getTargetStructure().getTables()) {
            ModelMaterializedTableDefinition old = previous.get(key(table));
            Set<String> oldNames = old == null ? Collections.emptySet() : old.getIndexes().stream()
                                                                          .map(ModelMaterializedIndexDefinition::getIndexName).collect(Collectors.toSet());
            for (ModelMaterializedIndexDefinition index : table.getIndexes()) {
                if (index.getIndexType() != ModelIndexType.PRIMARY_KEY && !oldNames.contains(index.getIndexName())) {
                    if (planned.put(index.getIndexName(), table.getTableName()) != null) {
                        issue(issues, index.getIndexName(), "本次多张表的索引名称重复");
                    }
                }
            }
        }
        // 不为查重逐表读取整个Schema；已有表自身的索引结构仍由原结构核对流程处理。
        // 此处只取消执行前的全库扫描，不影响唯一索引生成、执行或失败后的回撤。
    }

    /**
     * 核对历史发布管理的旧列，包含已停用列；实际库额外的列和索引不参与比较。
     */
    private void checkExistingTable(ModelPublishDatabaseSession session, ModelMaterializedTableDefinition table,
                                    List<DbColumn> columns, List<ModelPublishValidationIssue> issues) throws SQLException {
        // 字段列表来自本轮缺列判断；默认值、主键和索引仍由工具方法读取，不拿计划值代替实际值。
        for (String problem : ModelMaterializationDbUtils.structureProblems(session, table, columns)) {
            issue(issues, table.getTableName(), problem);
        }
    }

    /**
     * 新列的数据条件按物理约束检查；来源业务列可空且无唯一，不受权威约束牵连。
     */
    private void checkAddedColumns(ModelPublishDatabaseSession session, ModelMaterializedTableDefinition old,
                                   ModelMaterializedTableDefinition table, List<ModelPublishValidationIssue> issues) throws SQLException {
        Set<String> oldNames = old.getColumns().stream().map(ModelMaterializedColumnDefinition::getColumnName).collect(Collectors.toSet());
        Long count = null;
        for (ModelMaterializedColumnDefinition column : table.getColumns()) {
            if (oldNames.contains(column.getColumnName())) {
                continue;
            }
            if (count == null) {
                count = ModelMaterializationDbUtils.rowCount(session, table.getTableName());
            }
            DbColumn converted = ModelMaterializationDbUtils.column(session, column);
            boolean hasDefault = ModelMaterializationDbUtils.defaultValue(converted.getDataDefault()) != null;
            // 按本次实际物理约束检查普通新增及历史缺列；来源业务列可空，不受权威表必填规则影响。
            if (count > 0 && Boolean.FALSE.equals(converted.getNullable()) && !hasDefault) {
                issue(issues, table.getTableName() + "/字段[" + column.getColumnName() + "]",
                        "目标表已有数据，必填字段缺失且无默认值，无法自动补建。请恢复字段并补齐数据后重新发布");
            }
            boolean unique = table.getIndexes().stream().anyMatch(index -> index.getIndexType() == ModelIndexType.UNIQUE
                    && index.getColumnNames().contains(column.getColumnName()));
            // 新增唯一列会把同一个默认值填给全部历史行，多条旧记录会立刻违反唯一约束。
            if (unique && count > 1 && hasDefault) {
                issue(issues, table.getTableName() + "/字段[" + column.getColumnName() + "]", "多条历史记录会得到同一默认值，新增唯一字段存在重复");
            }
            // SQL Server普通唯一索引只允许一个NULL，不能把其他库允许多个NULL的规则套过来。
            boolean singleNull = session.getDialect() instanceof SQLServer2008Dialect;
            if (unique && count > 1 && !hasDefault && singleNull) {
                issue(issues, table.getTableName() + "/字段[" + column.getColumnName() + "]", "多条历史记录没有新唯一字段值，需先明确补值后再发布");
            }
        }
    }

    /**
     * 表身份始终使用模型ID和角色，物理名称只是待核对属性。
     */
    private String key(ModelMaterializedTableDefinition table) {
        return table.getModelId() + ":" + table.getTableRole();
    }

    /**
     * 登记并立即抛出当前目标库校验错误，不在提示中暴露连接凭证。
     */
    private void issue(List<ModelPublishValidationIssue> issues, String path, String message) {
        rejectValidation(issues, ModelValidationStage.TARGET, "目标数据库校验", path, message);
    }

    /**
     * 统一登记首个校验错误并停止后续规则。完整定位路径留在内部记录，对外只显示原因和必要的字段编码。
     */
    private void rejectValidation(List<ModelPublishValidationIssue> issues, ModelValidationStage stage,
                                  String ruleName, String path, String message) {
        String displayMessage = message;
        // 字段错误只提示“字段DEPT_CODE编码……”，不拼接模型名称、ID和内部路径。
        int fieldStart = path == null ? -1 : path.lastIndexOf("/字段[");
        if (fieldStart >= 0) {
            int codeStart = fieldStart + "/字段[".length();
            int codeEnd = path.indexOf(']', codeStart);
            if (codeEnd > codeStart) {
                String fieldCode = path.substring(codeStart, codeEnd);
                if (!"null".equals(fieldCode)) {
                    // 原因本身以“字段”开头时去掉重复称呼，例如“字段编码……”变成“字段DEPT_CODE编码……”。
                    displayMessage = "字段" + fieldCode + (message.startsWith("字段") ? message.substring(2) : message);
                }
            }
        }
        ModelPublishValidationIssue issue = new ModelPublishValidationIssue();
        issue.setStage(stage);
        issue.setRuleName(ruleName);
        issue.setObjectPath(path);
        issue.setMessage(displayMessage);
        issues.add(issue);
        throw new ServiceException(displayMessage);
    }

}
