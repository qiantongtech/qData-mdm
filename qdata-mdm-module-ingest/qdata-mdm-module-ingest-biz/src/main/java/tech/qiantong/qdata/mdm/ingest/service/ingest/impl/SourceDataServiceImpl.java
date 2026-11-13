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

package tech.qiantong.qdata.mdm.ingest.service.ingest.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataDetailRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataModelColumnRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataModelTreeRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.SourceDataReferenceRow;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.ingest.SourceDataReferenceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.task.IngestTaskMapper;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;
import tech.qiantong.qdata.mdm.ingest.dal.repository.ingest.SourceDataTableRepository;
import tech.qiantong.qdata.mdm.ingest.service.ingest.SourceDataService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskService;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.model.service.layer.IModelLayerService;
import tech.qiantong.qdata.mdm.model.service.project.IModelProjectService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.annotation.Resource;

/**
 * 来源数据服务实现。
 */
@Service
public class SourceDataServiceImpl implements SourceDataService {

    @Resource
    private SourceDataReferenceMapper sourceDataReferenceMapper;
    @Resource
    private IngestTaskMapper ingestTaskMapper;
    @Resource
    private IModelBaseService modelBaseService;
    @Resource
    private IModelProjectService modelProjectService;
    @Resource
    private IModelLayerService modelLayerService;

    @Resource
    private ModelPublishedApi modelPublishedApi;
    @Resource
    private SourceDataTableRepository sourceDataTableRepository;
    @Resource
    private IIngestTaskService ingestTaskService;

    /**
     * 查询当前模型成功物化的来源表。
     */
    @Override
    public PageResult<SourceDataPageRespVO> getPage(SourceDataPageReqVO request) {
        ModelPublishedMaterializationRespDTO target = materialization(request.getModelId());
        ModelPublishedMaterializedTableRespDTO table = sourceTable(target, request.getModelId());
        PageResult<Map<String, Object>> page = sourceDataTableRepository.getPage(target, table, request);
        ModelPublishedRespDTO model = modelPublishedApi.getPublishedModel(request.getModelId());
        List<SourceDataPageRespVO> rows = page.getRows().stream()
                .map(row -> toPageRow(row, table, model)).collect(Collectors.toList());
        fillSourceSystemNames(rows);
        fillTaskDetails(rows);
        if (!rows.isEmpty()) {
            ModelBaseDO modelBase = modelBaseService.getModelBaseById(request.getModelId());
            if (modelBase != null) {
                rows.forEach(row -> {
                    row.setModelIcon(modelBase.getIcon());
                    row.setModelType(modelBase.getType());
                    row.setModelDescription(modelBase.getDescription());
                    if (modelBase.getReleaseVersion() != null) {
                        row.setModelVersionNo(modelBase.getReleaseVersion().getVersionNo());
                    } else if (modelBase.getVersion() != null) {
                        row.setModelVersionNo(modelBase.getVersion().getVersionNo());
                    }
                    if (modelBase.getVersion() != null) {
                        row.setModelVersionStatus(modelBase.getVersion().getStatus());
                    }
                });
            }
        }
        return new PageResult<>(rows, page.getTotal());
    }

    private void fillSourceSystemNames(List<SourceDataPageRespVO> rows) {
        List<Long> ids = rows.stream().map(SourceDataPageRespVO::getSourceSystemId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, SourceDataReferenceRow> sources = sourceDataReferenceMapper.selectSourcesByIds(ids).stream()
                .collect(Collectors.toMap(SourceDataReferenceRow::getId, source -> source));
        rows.forEach(row -> {
            SourceDataReferenceRow source = sources.get(row.getSourceSystemId());
            if (source != null) {
                row.setSourceSystemName(source.getName());
                row.setSourceSystemCode(source.getCode());
                row.setSourceSystemIcon(source.getIcon());
            }
        });
    }

    private void fillTaskDetails(List<SourceDataPageRespVO> rows) {
        List<Long> taskIds = rows.stream().map(SourceDataPageRespVO::getTaskId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (taskIds.isEmpty()) return;
        Map<Long, IngestTaskDO> tasks = ingestTaskMapper.selectByIdsForSourceData(taskIds).stream()
                .collect(Collectors.toMap(IngestTaskDO::getId, task -> task));
        rows.forEach(row -> {
            IngestTaskDO task = tasks.get(row.getTaskId());
            if (task != null) {
                row.setTaskName(task.getName());
                row.setTaskCode(task.getCode());
                row.setTaskType(task.getType());
                row.setTaskDescription(task.getRemark());
            }
        });
    }

    /**
     * 将主数据域、目录和模型组织成树，并给模型叶子附上来源记录数。
     * 目录层级本身不代表可查询的数据，只有模型节点对应 modelId。
     */
    @Override
    public List<SourceDataModelTreeRespVO> getModelTree() {
        List<ModelBaseDO> modelRows = modelBaseService.getModelBaseList();
        Map<Long, Long> sourceCountMap = new LinkedHashMap<>();
        for (ModelBaseDO model : modelRows) {
            if (model.getPublishedInstanceId() == null) {
                continue;
            }
            ModelPublishedMaterializationRespDTO target = materialization(model.getId());
            for (ModelPublishedMaterializedTableRespDTO table : target.getTables()) {
                if (model.getId().equals(table.getModelId()) && "SOURCE".equals(table.getTableRole())
                        && Boolean.TRUE.equals(table.getEnabledFlag())) {
                    sourceCountMap.put(model.getId(), sourceDataTableRepository.countLatestRecords(target, table));
                    break;
                }
            }
        }
        List<SourceDataReferenceRow> models = modelRows.stream()
                .map(this::toModelReference)
                .collect(Collectors.toList());
        Map<Long, List<SourceDataReferenceRow>> modelsByLayerId = models.stream()
                .filter(model -> model.getLayerId() != null)
                .collect(Collectors.groupingBy(SourceDataReferenceRow::getLayerId));
        Map<Long, List<SourceDataReferenceRow>> modelsByProjectId = models.stream()
                .filter(model -> model.getLayerId() == null && model.getProjectId() != null)
                .collect(Collectors.groupingBy(SourceDataReferenceRow::getProjectId));

        List<SourceDataReferenceRow> layers = modelLayerService.getModelLayerList().stream()
                .map(this::toLayerReference)
                .collect(Collectors.toList());
        Map<Long, List<SourceDataReferenceRow>> layersByProjectId = layers.stream()
                .filter(layer -> layer.getProjectId() != null)
                .collect(Collectors.groupingBy(SourceDataReferenceRow::getProjectId));

        return modelProjectService.getModelProjectList().stream()
                .map(this::toProjectReference)
                .map(project -> buildProjectNode(project, layersByProjectId.get(project.getId()),
                        modelsByLayerId, modelsByProjectId.get(project.getId()), sourceCountMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 业务列使用成功物化快照，避免未发布的模型变更影响来源表展示。
     */
    @Override
    public List<SourceDataModelColumnRespVO> getModelColumns(Long modelId) {
        ModelPublishedMaterializedTableRespDTO table = sourceTable(materialization(modelId), modelId);
        Long nameFieldId = modelPublishedApi.getPublishedModelStructure(modelId).getFields().stream()
                .filter(field -> Boolean.TRUE.equals(field.getNameFlag()))
                .map(ModelPublishedColumnRespDTO::getId)
                .findFirst().orElse(null);
        return table.getColumns().stream()
                .filter(column -> !Boolean.TRUE.equals(column.getSystemFlag())
                        && Boolean.TRUE.equals(column.getEnabledFlag()))
                .map(column -> toModelColumn(column, modelId, nameFieldId)).collect(Collectors.toList());
    }

    private SourceDataModelTreeRespVO buildProjectNode(
            SourceDataReferenceRow project,
            List<SourceDataReferenceRow> projectLayers,
            Map<Long, List<SourceDataReferenceRow>> modelsByLayerId,
            List<SourceDataReferenceRow> directModels,
            Map<Long, Long> sourceCountMap) {
        List<SourceDataModelTreeRespVO> children = new ArrayList<>();
        if (projectLayers != null) {
            children.addAll(buildLayerNodes(projectLayers, null, modelsByLayerId, sourceCountMap));
        }
        if (directModels != null) {
            children.addAll(buildModelNodes(directModels, sourceCountMap));
        }
        if (children.isEmpty()) {
            return null;
        }
        SourceDataModelTreeRespVO node = new SourceDataModelTreeRespVO();
        node.setId("project-" + project.getId());
        node.setType("project");
        node.setCode(project.getCode());
        node.setLabel(project.getName());
        node.setChildren(children);
        return node;
    }

    private List<SourceDataModelTreeRespVO> buildLayerNodes(List<SourceDataReferenceRow> projectLayers, Long parentId,
                                                             Map<Long, List<SourceDataReferenceRow>> modelsByLayerId,
                                                             Map<Long, Long> sourceCountMap) {
        return projectLayers.stream()
                .filter(layer -> isSameLayer(parentId, layer.getParentId()))
                .map(layer -> buildLayerNode(layer, projectLayers, modelsByLayerId, sourceCountMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isSameLayer(Long expectedParentId, Long actualParentId) {
        if (expectedParentId == null) {
            return actualParentId == null || actualParentId == 0L;
        }
        return expectedParentId.equals(actualParentId);
    }

    private SourceDataModelTreeRespVO buildLayerNode(
            SourceDataReferenceRow layer,
            List<SourceDataReferenceRow> projectLayers,
            Map<Long, List<SourceDataReferenceRow>> modelsByLayerId,
            Map<Long, Long> sourceCountMap) {
        List<SourceDataModelTreeRespVO> children = new ArrayList<>();
        children.addAll(buildLayerNodes(projectLayers, layer.getId(), modelsByLayerId, sourceCountMap));
        Collection<SourceDataReferenceRow> models = modelsByLayerId.get(layer.getId());
        if (models != null) {
            children.addAll(buildModelNodes(models, sourceCountMap));
        }
        if (children.isEmpty()) {
            return null;
        }
        SourceDataModelTreeRespVO node = new SourceDataModelTreeRespVO();
        node.setId("layer-" + layer.getId());
        node.setType("layer");
        node.setCode(layer.getCode());
        node.setLabel(layer.getName());
        node.setChildren(children);
        return node;
    }

    private List<SourceDataModelTreeRespVO> buildModelNodes(Collection<SourceDataReferenceRow> models,
                                                             Map<Long, Long> sourceCountMap) {
        return models.stream()
                .map(model -> buildModelNode(model, sourceCountMap.get(model.getId())))
                .collect(Collectors.toList());
    }

    private SourceDataModelTreeRespVO buildModelNode(SourceDataReferenceRow model, Long count) {
        SourceDataModelTreeRespVO node = new SourceDataModelTreeRespVO();
        node.setId("model-" + model.getId());
        node.setType("model");
        node.setCode(model.getCode());
        node.setModelId(model.getId());
        node.setLabel(model.getName());
         // 尚无来源记录的模型数量显示为 0。
        node.setCount(count == null ? 0L : count);
        return node;
    }

    @Override
    public SourceDataDetailRespVO getDetail(Long modelId, Long id) {
        ModelPublishedMaterializationRespDTO target = materialization(modelId);
        ModelPublishedMaterializedTableRespDTO table = sourceTable(target, modelId);
        Map<String, Object> data = sourceDataTableRepository.getRecord(target, table, id);
        SourceDataPageRespVO row = toPageRow(data, table, modelPublishedApi.getPublishedModel(modelId));
        fillSourceSystemNames(Collections.singletonList(row));
        SourceDataDetailRespVO detail = BeanUtils.toBean(row, SourceDataDetailRespVO.class);
        ModelBaseDO modelBase = modelBaseService.getModelBaseById(modelId);
        if (modelBase != null) {
            detail.setModelIcon(modelBase.getIcon());
            detail.setModelType(modelBase.getType());
            if (modelBase.getReleaseVersion() != null) {
                detail.setModelVersionNo(modelBase.getReleaseVersion().getVersionNo());
            } else if (modelBase.getVersion() != null) {
                detail.setModelVersionNo(modelBase.getVersion().getVersionNo());
            }
            if (modelBase.getVersion() != null) {
                detail.setModelVersionStatus(modelBase.getVersion().getStatus());
            }
        }
        detail.setTaskId(longValue(value(data, "access_task_id")));
        detail.setBatchNo(text(value(data, "batch_id")));
        detail.setDelFlag(text(value(data, "del_flag")));
        detail.setReceiveTime(formatDateTime(value(data, "collection_time")));
        if (detail.getTaskId() != null) {
            try {
                detail.setTaskName(ingestTaskService.detail(detail.getTaskId()).getName());
            } catch (RuntimeException ignored) {
                 // 历史来源数据仍可查看，即使关联任务已删除。
            }
        }
        return detail;
    }

    @Override
    public void deleteRecord(Long modelId, Long id) {
        ModelPublishedMaterializationRespDTO target = materialization(modelId);
        sourceDataTableRepository.deleteRecord(target, sourceTable(target, modelId), id);
    }

    private ModelPublishedMaterializationRespDTO materialization(Long modelId) {
        if (modelId == null) throw new ServiceException("请选择主数据模型");
        ModelPublishedMaterializationRespDTO target = modelPublishedApi.getPublishedModelMaterialization(modelId);
        if (target == null || target.getDatasourceId() == null || target.getTables() == null) {
            throw new ServiceException("模型未配置已发布的来源表");
        }
        return target;
    }

    private ModelPublishedMaterializedTableRespDTO sourceTable(ModelPublishedMaterializationRespDTO target, Long modelId) {
        return target.getTables().stream().filter(table -> modelId.equals(table.getModelId())
                        && ModelTableRole.SOURCE.name().equals(table.getTableRole())
                        && Boolean.TRUE.equals(table.getEnabledFlag()))
                .findFirst().orElseThrow(() -> new ServiceException("模型没有已启用的来源表"));
    }

    private SourceDataPageRespVO toPageRow(Map<String, Object> data,
            ModelPublishedMaterializedTableRespDTO table, ModelPublishedRespDTO model) {
        SourceDataPageRespVO row = new SourceDataPageRespVO();
        row.setId(longValue(value(data, "id")));
        row.setModelId(table.getModelId());
        if (model != null) {
            row.setModelName(model.getName());
            row.setModelCode(model.getCode());
        }
        row.setSourceSystemId(longValue(value(data, "source_system_id")));
        row.setTaskId(longValue(value(data, "access_task_id")));
        row.setSourceRecordKey(text(value(data, "source_record_id")));
        row.setStableRecordId(row.getSourceRecordKey());
        row.setBatchNo(text(value(data, "batch_id")));
        row.setGovernanceStatus(text(value(data, "status")));
        Object collected = value(data, "collection_time");
        row.setLastReceiveTime(toDate(collected));
        Map<String, Object> business = new LinkedHashMap<>();
        table.getColumns().stream().filter(column -> !Boolean.TRUE.equals(column.getSystemFlag())
                && Boolean.TRUE.equals(column.getEnabledFlag())).forEach(column ->
                business.put(column.getColumnName(), value(data, column.getColumnName())));
        row.setModelData(JSON.toJSONString(business, "yyyy-MM-dd HH:mm:ss", JSONWriter.Feature.WriteLongAsString));
        return row;
    }

    private Date toDate(Object value) {
        if (value instanceof Date) return (Date) value;
        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof OffsetDateTime) return Date.from(((OffsetDateTime) value).toInstant());
        if (value instanceof Instant) return Date.from((Instant) value);
        String text = text(value);
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return Date.from(LocalDateTime.parse(text.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(ZoneId.systemDefault()).toInstant());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String formatDateTime(Object value) {
        Date date = toDate(value);
        if (date != null) return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        String text = text(value);
        if (text == null) return null;
        return text.replace('T', ' ');
    }

    private Object value(Map<String, Object> data, String name) {
        return data.entrySet().stream().filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Long longValue(Object value) {
        return value == null ? null : new java.math.BigDecimal(value.toString()).longValueExact();
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }

    private SourceDataReferenceRow toModelReference(ModelBaseDO model) {
        SourceDataReferenceRow row = new SourceDataReferenceRow();
        row.setId(model.getId());
        row.setCode(model.getCode());
        row.setName(model.getName());
        row.setProjectId(model.getProjectId());
        row.setLayerId(model.getLayerId());
        return row;
    }

    private SourceDataReferenceRow toLayerReference(ModelLayerDO layer) {
        SourceDataReferenceRow row = new SourceDataReferenceRow();
        row.setId(layer.getId());
        row.setCode(layer.getCode());
        row.setName(layer.getName());
        row.setProjectId(layer.getProjectId());
        row.setParentId(layer.getParentId());
        return row;
    }

    private SourceDataReferenceRow toProjectReference(ModelProjectDO project) {
        SourceDataReferenceRow row = new SourceDataReferenceRow();
        row.setId(project.getId());
        row.setCode(project.getCode());
        row.setName(project.getName());
        return row;
    }

    private SourceDataModelColumnRespVO toModelColumn(ModelMaterializedColumnRespDTO column, Long modelId, Long nameFieldId) {
        SourceDataModelColumnRespVO field = new SourceDataModelColumnRespVO();
        field.setId(column.getModelFieldId());
        field.setModelId(modelId);
        field.setNameFlag(nameFieldId != null && nameFieldId.equals(column.getModelFieldId()));
        field.setColumnName(column.getColumnComment() == null ? column.getColumnName() : column.getColumnComment());
        field.setColumnCode(column.getColumnName());
        field.setDataType(column.getLogicalType());
        field.setDataLength(column.getCharacterLength() == null ? null : column.getCharacterLength().intValue());
        field.setDataScale(column.getNumericScale());
        field.setPrimaryKeyFlag(column.getPrimaryKeyFlag());
        field.setRequiredFlag(Boolean.FALSE.equals(column.getNullable()));
        return field;
    }
}
