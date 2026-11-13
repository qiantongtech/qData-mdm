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

package tech.qiantong.qdata.mdm.merge.service.source.impl;

import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceTableService;

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.repository.ingest.SourceDataTableRepository;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import javax.annotation.Resource;
import java.util.*;

/** 读取模型发布的动态来源表，并维护来源行的治理状态。 */
@Service
public class MergeSourceTableServiceImpl implements IMergeSourceTableService {
    @Resource
    private ModelPublishedApi modelPublishedApi;
    @Resource
    private SourceDataTableRepository sourceDataTableRepository;

    @Override
    public List<MergeSourceRecordBO> load(Long modelId, Collection<Long> ids) {
        return load(modelId, ids, false);
    }

    @Override
    public MergeSourceRecordBO lock(Long modelId, Long id) {
        List<MergeSourceRecordBO> rows = load(modelId, Collections.singletonList(id), true);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 按已发布来源表读取指定采集行，编号先去重再每批最多读取 500 个。 */
    private List<MergeSourceRecordBO> load(Long modelId, Collection<Long> ids, boolean lock) {
        ModelPublishedMaterializationRespDTO target = target(modelId);
        ModelPublishedMaterializedTableRespDTO table = sourceTable(target, modelId);
        List<MergeSourceRecordBO> result = new ArrayList<>();
        if (ids != null && ids.isEmpty()) {
            return result;
        }
        List<Long> selected = ids == null ? null : new ArrayList<>(new LinkedHashSet<>(ids));
        int batches = selected == null ? 1 : (selected.size() + 499) / 500;
        for (int batch = 0; batch < batches; batch++) {
            Collection<Long> part = selected == null ? null
                    : selected.subList(batch * 500, Math.min(selected.size(), (batch + 1) * 500));
            for (Map<String, Object> row : sourceDataTableRepository.getFusionRecords(target, table, part, lock)) {
                result.add(toSource(modelId, row, table));
            }
        }
        return result;
    }

    @Override
    public void transition(Long modelId, Collection<Long> ids, String expected, String status) {
        if (ids.isEmpty()) return;
        ModelPublishedMaterializationRespDTO target = target(modelId);
        ModelPublishedMaterializedTableRespDTO table = sourceTable(target, modelId);
        List<Long> selected = new ArrayList<>(new LinkedHashSet<>(ids));
        Collections.sort(selected);
        for (int start = 0; start < selected.size(); start += 500) {
            sourceDataTableRepository.transitionFusionStatus(target, table,
                    selected.subList(start, Math.min(selected.size(), start + 500)), expected, status);
        }
    }

    /** 取得已发布的物化配置，缺少数据源或表结构时拒绝读取。 */
    private ModelPublishedMaterializationRespDTO target(Long modelId) {
        ModelPublishedMaterializationRespDTO target = modelPublishedApi.getPublishedModelMaterialization(modelId);
        if (target == null || target.getDatasourceId() == null || target.getTables() == null) {
            throw new ServiceException("模型未配置已发布的来源实体表");
        }
        return target;
    }

    /** 仅选择当前模型已启用且角色为 SOURCE 的实体表。 */
    private ModelPublishedMaterializedTableRespDTO sourceTable(ModelPublishedMaterializationRespDTO target, Long modelId) {
        return target.getTables().stream().filter(table -> modelId.equals(table.getModelId())
                && "SOURCE".equals(table.getTableRole()) && Boolean.TRUE.equals(table.getEnabledFlag()))
                .findFirst().orElseThrow(() -> new ServiceException("模型没有已启用的来源实体表"));
    }

    /** 读取来源行系统字段，业务数据仅包含发布配置中已启用的非系统列。 */
    private MergeSourceRecordBO toSource(Long modelId, Map<String, Object> row,
            ModelPublishedMaterializedTableRespDTO table) {
        MergeSourceRecordBO source = new MergeSourceRecordBO();
        source.setId(number(value(row, "id")));
        source.setModelId(modelId);
        source.setSourceSystemId(number(value(row, "source_system_id")));
        source.setTaskId(number(value(row, "access_task_id")));
        source.setSourceRecordKey(text(value(row, "source_record_id")));
        source.setGovernanceStatus(text(value(row, "status")));
        Object collected = value(row, "collection_time");
        if (collected instanceof Date) source.setReceiveTime((Date) collected);
        else if (collected instanceof java.time.LocalDateTime) source.setReceiveTime(Date.from(
                ((java.time.LocalDateTime) collected).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        Map<String, Object> business = new LinkedHashMap<>();
        table.getColumns().stream().filter(column -> !Boolean.TRUE.equals(column.getSystemFlag())
                && Boolean.TRUE.equals(column.getEnabledFlag())).forEach(column ->
                business.put(column.getColumnName(), value(row, column.getColumnName())));
        source.setModelData(MergeJson.toJson(business));
        return source;
    }

    /** 忽略 JDBC 返回列名的大小写差异。 */
    private Object value(Map<String, Object> row, String name) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** 精确转换来源编号，遇到小数或溢出时报错，避免静默截断。 */
    private Long number(Object value) {
        return value == null ? null : new java.math.BigDecimal(value.toString()).longValueExact();
    }

    /** 保留数据库空值，其余值转换为文本。 */
    private String text(Object value) {
        return value == null ? null : value.toString();
    }
}
