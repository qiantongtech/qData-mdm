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
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.RequestContext;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider.TableAccess;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDictUsageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDictUsageRespDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterEncodingUsageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterEncodingUsageRespDTO;
import tech.qiantong.qdata.mdm.master.service.change.maintenance.MaintenanceData;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.service.data.service.MasterFieldUsageApi;
import tech.qiantong.qdata.mdm.master.util.MasterPublishedModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 模块间字段统计：请求内按模型复用成功发布定义，按数据源复用连接，按字段合并数据项。 */
@Service
@RequiredArgsConstructor
public class MasterFieldUsageApiImpl implements MasterFieldUsageApi {
    private static final int MAX_REQUESTS = 100;
    private static final int MAX_ITEM_CODES = 500;

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository repository;

    @Override
    // 统计失败不影响调用方事务。
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<MasterDictUsageRespDTO> countDictUsageBatch(List<MasterDictUsageReqDTO> requests) {
        if (requests == null || requests.isEmpty()) return Collections.emptyList();
        List<MasterDictUsageRespDTO> results = new ArrayList<>();
        Map<Long, Map<ReferenceField, Map<String, MasterDictUsageRespDTO>>> grouped = new LinkedHashMap<>();
        long itemCount = 0;
        for (MasterDictUsageReqDTO request : requests) {
            if (request == null || request.getItemCodes() == null || request.getItemCodes().isEmpty()) continue;
            itemCount += request.getItemCodes().size();
            Map<String, MasterDictUsageRespDTO> rows = grouped
                    .computeIfAbsent(request.getModelId(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(new ReferenceField(request.getFieldId(), request.getDictTypeId()),
                            ignored -> new LinkedHashMap<>());
            for (String code : request.getItemCodes()) {
                rows.computeIfAbsent(code, ignored -> {
                    MasterDictUsageRespDTO row = new MasterDictUsageRespDTO();
                    row.setModelId(request.getModelId());
                    row.setFieldId(request.getFieldId());
                    row.setDictTypeId(request.getDictTypeId());
                    row.setItemCode(code);
                    row.setUsageCount(0L);
                    results.add(row);
                    return row;
                });
            }
        }
        if (results.isEmpty() || requests.size() > MAX_REQUESTS || itemCount > MAX_ITEM_CODES) return results;

        try (RequestContext context = repository.openQuietRequest()) {
            for (Map.Entry<Long, Map<ReferenceField, Map<String, MasterDictUsageRespDTO>>> entry : grouped.entrySet()) {
                try {
                    MasterModelRespDTO model = metadataService.getPublishedDefinition(null, entry.getKey(), true);
                    entry.getValue().values().forEach(rows -> rows.values()
                            .forEach(row -> row.setModelVersionId(model.getModelVersionId())));
                    TableAccess access = authorityTable(context, model);
                    entry.getValue().forEach((field, rows) -> countField(model, access, field, rows));
                } catch (RuntimeException ignored) {
                    // 当前模型保留0值，继续统计其他模型。
                }
            }
        } catch (RuntimeException ignored) {
            // 上下文失败时保留已有结果。
        }
        return results;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<MasterEncodingUsageRespDTO> countEncodingUsageBatch(List<MasterEncodingUsageReqDTO> requests) {
        if (requests == null || requests.isEmpty()) return Collections.emptyList();
        List<MasterEncodingUsageRespDTO> results = new ArrayList<>();
        Map<Long, Map<ReferenceField, MasterEncodingUsageRespDTO>> grouped = new LinkedHashMap<>();
        for (MasterEncodingUsageReqDTO request : requests) {
            if (request == null) continue;
            grouped.computeIfAbsent(request.getModelId(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(new ReferenceField(request.getFieldId(), request.getEncodingVersionId()), ignored -> {
                        MasterEncodingUsageRespDTO row = new MasterEncodingUsageRespDTO();
                        row.setModelId(request.getModelId());
                        row.setFieldId(request.getFieldId());
                        row.setEncodingVersionId(request.getEncodingVersionId());
                        row.setUsageCount(0L);
                        results.add(row);
                        return row;
                    });
        }
        if (results.isEmpty() || requests.size() > MAX_REQUESTS) return results;

        try (RequestContext context = repository.openQuietRequest()) {
            for (Map.Entry<Long, Map<ReferenceField, MasterEncodingUsageRespDTO>> entry : grouped.entrySet()) {
                try {
                    MasterModelRespDTO model = metadataService.getPublishedDefinition(null, entry.getKey(), true);
                    entry.getValue().values().forEach(row -> row.setModelVersionId(model.getModelVersionId()));
                    TableAccess access = authorityTable(context, model);
                    entry.getValue().values().forEach(row -> countEncodingField(model, access, row));
                } catch (RuntimeException ignored) {
                    // 当前模型保留0值，继续统计其他模型。
                }
            }
        } catch (RuntimeException ignored) {
            // 上下文失败时保留已有结果。
        }
        return results;
    }

    private TableAccess authorityTable(RequestContext context, MasterModelRespDTO model) {
        String table = MasterPublishedModelUtils.qualifiedTable(model.getSchemaName(), model.getPhysicalTableName());
        return context.forTable(model.getDatasourceId(), table, model.getColumnMappings());
    }

    private void countField(MasterModelRespDTO model, TableAccess access, ReferenceField reference,
            Map<String, MasterDictUsageRespDTO> rows) {
        try {
            MasterFieldRespDTO field = model.getFields().stream()
                    .filter(item -> Objects.equals(reference.fieldId, item.getId()) && item.getDictTypeId() != null
                            && Objects.equals(reference.referenceId, item.getDictTypeId()))
                    .findFirst().orElse(null);
            if (field == null) return;
            Map<String, Object> values = new LinkedHashMap<>();
            rows.keySet().forEach(code -> values.put(code, MaintenanceData.convert(code, field.getDataType())));
            Map<String, Long> counts = repository.countFieldValues(access, field.getCode(), values);
            rows.forEach((code, row) -> row.setUsageCount(counts.getOrDefault(code, 0L)));
        } catch (RuntimeException ignored) {
            // 当前字段保留0值，继续统计其他字段。
        }
    }

    private void countEncodingField(MasterModelRespDTO model, TableAccess access, MasterEncodingUsageRespDTO row) {
        try {
            MasterFieldRespDTO field = model.getFields().stream()
                    .filter(item -> Objects.equals(row.getFieldId(), item.getId()) && item.getEncodingVersionId() != null
                            && Objects.equals(row.getEncodingVersionId(), item.getEncodingVersionId()))
                    .findFirst().orElse(null);
            if (field != null) row.setUsageCount(repository.countNonBlankFieldValues(access, field.getCode()));
        } catch (RuntimeException ignored) {
            // 当前字段保留0值，继续统计其他字段。
        }
    }

    @Value
    private static class ReferenceField {
        Long fieldId;
        Long referenceId;
    }
}
