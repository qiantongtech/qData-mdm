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
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditMasterChangeDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestItemDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelInstanceDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperatorDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditChangeRequestMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelInstanceMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditOperatorMapper;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import tech.qiantong.qdata.mdm.audit.service.audit.IAuditChangeTraceService;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditChangeTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditChangeTraceListRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationTraceRespVO;

@Service
public class AuditChangeTraceServiceImpl extends AuditTraceReadService implements IAuditChangeTraceService {
    @Resource private AuditChangeRequestMapper auditChangeRequestMapper;
    @Resource private AuditModelInstanceMapper auditModelInstanceMapper;
    @Resource private AuditOperatorMapper auditOperatorMapper;
    @Resource private AuditOperationRelationService auditOperationRelationService;

    @Override
    public AuditTracePageRespVO<AuditChangeTraceListRespVO> changes(Map<String, String> filters) {
        List<AuditChangeTraceListRespVO> result = new ArrayList<>();
        List<AuditMasterChangeDO> changes = auditMasterChangeMapper.selectTraceChanges();
        Map<Long, String> changeModels = modelNamesByIds(changes.stream().map(AuditMasterChangeDO::getModelId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        List<Map<String, Object>> masterChanges = changes.stream()
                .map(item -> changeData(item, changeModels.get(item.getModelId()))).collect(Collectors.toList());
        // 申请明细不是历史事实；仅补充已执行且尚未写入 MASTER_CHANGE 的生命周期结果。
        List<Map<String, Object>> lifecycleItems = lifecycleItems(null);
        List<Map<String, Object>> schemaRows = new ArrayList<>(masterChanges);
        schemaRows.addAll(lifecycleItems);
        Map<Long, List<Map<String, Object>>> columnsByVersion = publishedModelColumns(schemaRows);
        for (Map<String, Object> raw : masterChanges) {
            result.add(changeRow(raw, columnsByVersion.get(number(raw.get("MODEL_VERSION_ID"))), new AuditChangeTraceListRespVO()));
        }
        for (Map<String, Object> raw : lifecycleItems) {
            result.add(lifecycleRow(raw, columnsByVersion.get(number(raw.get("MODEL_VERSION_ID"))), new AuditChangeTraceListRespVO()));
        }
        Comparator<AuditChangeTraceListRespVO> changeOrder = numericSortValue(filters, "recordId", AuditChangeTraceListRespVO::getRecordId);
        List<AuditChangeTraceListRespVO> filtered = result.stream().filter(row -> matchesValues(filters,
                "model", row.getModel(), "name", row.getName(), "code", row.getCode(), "operateId", row.getOperateId(),
                "type", row.getType(), "category", row.getCategory(), "result", row.getResult()) && inTime(row.getTime(), filters))
                .sorted(changeOrder == null ? descendingValue(AuditChangeTraceListRespVO::getTime) : changeOrder).collect(Collectors.toList());
        int pageNum = bounded(filters.get("pageNum"), 1, 1, Integer.MAX_VALUE);
        int pageSize = bounded(filters.get("pageSize"), 10, 1, 200);
        int from = (int) Math.min((long) (pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<AuditChangeTraceListRespVO> currentPage = filtered.subList(from, to);
        enrichChangeRows(currentPage);
        enrichOperators(currentPage);
        return page(filtered, filters);
    }

    @Override
    public AuditChangeTraceRespVO change(String id) {
        if (id == null || id.length() < 4) return null;
        Long key = number(id.substring(3));
        if (key == null) return null;
        if (id.startsWith("MC-")) {
            AuditMasterChangeDO item = auditMasterChangeMapper.selectById(key);
            if (item == null || !"0".equals(item.getDelFlag())) return null;
            AuditModelNameDO model = item.getModelId() == null ? null : auditModelNameMapper.selectById(item.getModelId());
            List<Map<String, Object>> rows = Collections.singletonList(changeData(item, model == null ? null : model.getName()));
            AuditChangeTraceRespVO result = changeRow(rows.get(0), publishedModelColumns(rows).get(item.getModelVersionId()), new AuditChangeTraceRespVO());
            enrichOperators(Collections.singletonList(result));
            return result;
        }
        if (id.startsWith("RI-")) {
            List<Map<String, Object>> rows = lifecycleItems(key);
            return rows.isEmpty() ? null : lifecycleRow(rows.get(0), publishedModelColumns(rows).get(number(rows.get(0).get("MODEL_VERSION_ID"))), new AuditChangeTraceRespVO());
        }
        return null;
    }

    private Map<String, Object> changeData(AuditMasterChangeDO item, String modelName) {
        Map<String, Object> row = new LinkedHashMap<>();
        put(row, "ID", item.getId(), "MODEL_ID", item.getModelId(), "MODEL_VERSION_ID", item.getModelVersionId(),
                "MASTER_RECORD_ID", item.getMasterRecordId(), "MODEL_NAME", modelName, "TYPE", item.getType(),
                "BEFORE_DATA", item.getBeforeData(), "PROPOSED_DATA", item.getProposedData(), "AFTER_DATA", item.getAfterData(),
                "PROCESS_RESULT", item.getProcessResult(), "BEFORE_STATUS", item.getBeforeStatus(),
                "AFTER_STATUS", item.getAfterStatus(), "CHANGE_TIME", item.getChangeTime(), "CHANGE_REASON", item.getChangeReason(),
                "OPERATOR_ID", item.getOperatorId(), "OPERATOR_NAME", item.getOperatorName());
        return row;
    }

    private List<Map<String, Object>> lifecycleItems(Long id) {
        List<AuditChangeRequestItemDO> items = auditChangeRequestItemMapper.selectExecutedLifecycleItems(id);
        List<Long> itemIds = items.stream().map(AuditChangeRequestItemDO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> alreadyRecorded = new HashSet<>();
        for (int start = 0; start < itemIds.size(); start += 500) {
            List<Long> batch = itemIds.subList(start, Math.min(start + 500, itemIds.size()));
            for (AuditMasterChangeDO change : auditMasterChangeMapper.selectByRequestItemIds(batch)) alreadyRecorded.add(change.getRequestItemId());
        }
        List<Long> requestIds = items.stream().filter(item -> !alreadyRecorded.contains(item.getId()))
                .map(AuditChangeRequestItemDO::getRequestId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, AuditChangeRequestDO> requests = new HashMap<>();
        for (int start = 0; start < requestIds.size(); start += 500) {
            for (AuditChangeRequestDO request : auditChangeRequestMapper.selectBatchIds(requestIds.subList(start, Math.min(start + 500, requestIds.size())))) {
                requests.put(request.getId(), request);
            }
        }
        Map<Long, String> names = modelNamesByIds(requests.values().stream().map(AuditChangeRequestDO::getModelId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditChangeRequestItemDO item : items) {
            AuditChangeRequestDO request = requests.get(item.getRequestId());
            if (request == null || alreadyRecorded.contains(item.getId())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "ID", item.getId(), "ITEM_NO", item.getItemNo(), "REQUEST_NO", request.getRequestNo(),
                    "MODEL_ID", request.getModelId(), "MODEL_VERSION_ID", request.getModelVersionId(),
                    "MODEL_NAME", names.get(request.getModelId()), "MASTER_RECORD_ID", item.getMasterRecordId(),
                    "BEFORE_DATA", item.getBeforeData(), "BEFORE_STATUS", item.getBeforeStatus(), "TARGET_STATUS", item.getTargetStatus(),
                    "EXECUTE_STATUS", item.getExecuteStatus(), "CHANGE_TYPE", item.getChangeType(),
                    "EXECUTE_TIME", item.getExecuteTime(), "UPDATE_TIME", item.getUpdateTime(), "EXECUTE_MESSAGE", item.getExecuteMessage());
            result.add(row);
        }
        return result;
    }

    private <T extends AuditChangeTraceRespVO> T changeRow(Map<String, Object> raw, List<Map<String, Object>> modelColumns, T row) {
        Long modelId = number(raw.get("MODEL_ID"));
        Map<String, Object> before = json(raw.get("BEFORE_DATA"));
        Map<String, Object> proposed = json(raw.get("PROPOSED_DATA"));
        Map<String, Object> after = json(raw.get("AFTER_DATA"));
        String result = processResult(str(raw.get("PROCESS_RESULT")));
        String changeType = str(raw.get("TYPE"));
        boolean lifecycleChange = isLifecycleChange(changeType);
        Map<String, Object> identity = changeIdentity(modelColumns, after, before, proposed);
        row.setId("MC-" + raw.get("ID"));
        row.setRecordId(number(raw.get("ID")));
        row.setOperateId("MC-" + raw.get("ID"));
        row.setModelId(modelId);
        row.setMasterRecordId(number(raw.get("MASTER_RECORD_ID")));
        row.setModelVersionId(number(raw.get("MODEL_VERSION_ID")));
        row.setModel((String) raw.get("MODEL_NAME"));
        row.setCode(str(raw.get("MASTER_RECORD_ID")));
        row.setName((String) identity.get("name"));
        row.setBusinessCode((String) identity.get("businessCode"));
        row.setType(type(changeType));
        row.setCategory(lifecycleChange ? "生命周期变化" : "数据内容变化");
        row.setResult(result);
        row.setTime(date(raw.get("CHANGE_TIME")));
        row.setSummary((String) raw.get("CHANGE_REASON"));
        row.setOperatorId(number(raw.get("OPERATOR_ID")));
        row.setOperatorName((String) raw.get("OPERATOR_NAME"));
        // 列表不展示字段级差异；详情单独计算，避免为分页前的全部记录逐条查询字段配置。
        row.setFields(lifecycleChange || row instanceof AuditChangeTraceListRespVO
                ? Collections.emptyList() : diff(modelId, before, proposed, after, "已生效".equals(result)));
        if (lifecycleChange) {
            String beforeStatus = lifecycleStatus(raw.get("BEFORE_STATUS"));
            String targetStatus = lifecycleStatus(raw.get("AFTER_STATUS"));
            Map<String, Object> lifecycle = new LinkedHashMap<>();
            put(lifecycle, "before", beforeStatus, "proposed", targetStatus,
                    "final", "已生效".equals(result) ? targetStatus : beforeStatus);
            row.setLifecycle(lifecycle);
        } else {
            row.setLifecycle(Collections.emptyMap());
        }
        return row;
    }

    private <T extends AuditChangeTraceRespVO> T lifecycleRow(Map<String, Object> raw, List<Map<String, Object>> modelColumns, T row) {
        Long modelId = number(raw.get("MODEL_ID"));
        String result = itemResult(str(raw.get("EXECUTE_STATUS")));
        String before = lifecycleStatus(raw.get("BEFORE_STATUS"));
        String target = lifecycleStatus(raw.get("TARGET_STATUS"));
        Map<String, Object> identity = changeIdentity(modelColumns, json(raw.get("BEFORE_DATA")));
        row.setId("RI-" + raw.get("ID"));
        row.setRecordId(number(raw.get("ID")));
        row.setOperateId(str(raw.get("REQUEST_NO")) + "/" + str(raw.get("ITEM_NO")));
        row.setModelId(modelId);
        row.setModelVersionId(number(raw.get("MODEL_VERSION_ID")));
        row.setMasterRecordId(number(raw.get("MASTER_RECORD_ID")));
        row.setModel((String) raw.get("MODEL_NAME"));
        row.setCode(str(raw.get("MASTER_RECORD_ID")));
        row.setName((String) identity.get("name"));
        row.setBusinessCode((String) identity.get("businessCode"));
        row.setType(type(str(raw.get("CHANGE_TYPE"))));
        row.setCategory("生命周期变化");
        row.setResult(result);
        row.setTime(date(raw.get("EXECUTE_TIME") != null ? raw.get("EXECUTE_TIME") : raw.get("UPDATE_TIME")));
        row.setSummary((String) raw.get("EXECUTE_MESSAGE"));
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        put(lifecycle, "before", before, "proposed", target, "final", "已生效".equals(result) ? target : before);
        row.setLifecycle(lifecycle);
        row.setFields(Collections.emptyList());
        return row;
    }

    private void enrichChangeRows(List<AuditChangeTraceListRespVO> rows) {
        List<AuditOperationTraceRespVO> modelRows = new ArrayList<>();
        for (AuditChangeTraceListRespVO row : rows) {
            AuditOperationTraceRespVO model = new AuditOperationTraceRespVO();
            model.setModelId(row.getModelId());
            model.setModelVersionId(row.getModelVersionId());
            modelRows.add(model);
        }
        auditOperationRelationService.enrichModelInfo(modelRows);
        for (int i = 0; i < rows.size(); i++) {
            AuditChangeTraceListRespVO row = rows.get(i);
            AuditOperationTraceRespVO model = modelRows.get(i);
            row.setModel(model.getModelDisplayName());
            row.setModelCode(model.getModelBusinessCode());
            row.setModelType(model.getModelType());
            row.setModelIcon(model.getModelIcon());
            row.setModelDescription(model.getModelDescription());
            row.setProjectName(model.getProjectName());
            row.setProjectIcon(model.getProjectIcon());
            row.setLayerName(model.getLayerName());
            row.setModelVersionNo(model.getModelVersionNo());
        }
    }

    /** 姓名取本次变更保存值，仅按明确的操作人ID批量读取当前电话。 */
    private void enrichOperators(List<? extends AuditChangeTraceRespVO> rows) {
        List<Long> userIds = rows.stream().map(AuditChangeTraceRespVO::getOperatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> phones = new HashMap<>();
        for (int start = 0; start < userIds.size(); start += 500) {
            for (AuditOperatorDO user : auditOperatorMapper.selectCurrentPhones(
                    userIds.subList(start, Math.min(start + 500, userIds.size())))) {
                phones.put(user.getUserId(), user.getPhoneNumber());
            }
        }
        rows.forEach(row -> row.setOperatorPhone(phones.get(row.getOperatorId())));
    }

    Map<Long, List<Map<String, Object>>> publishedModelColumns(List<Map<String, Object>> auditRows) {
        Map<Long, Long> modelByVersion = new LinkedHashMap<>();
        for (Map<String, Object> row : auditRows) {
            Long modelVersionId = number(row.get("MODEL_VERSION_ID"));
            Long modelId = number(row.get("MODEL_ID"));
            if (modelVersionId != null && modelId != null) modelByVersion.putIfAbsent(modelVersionId, modelId);
        }
        if (modelByVersion.isEmpty()) return Collections.emptyMap();

        Map<Long, List<Map<String, Object>>> result = new HashMap<>();
        List<Long> versionIds = new ArrayList<>(modelByVersion.keySet());
        for (int start = 0; start < versionIds.size(); start += 500) {
            List<Long> batch = versionIds.subList(start, Math.min(start + 500, versionIds.size()));
            for (AuditModelInstanceDO instance : auditModelInstanceMapper.selectPublishedSnapshots(batch)) {
                Long modelVersionId = instance.getModelVersionId();
                Long modelId = instance.getModelId();
                if (modelVersionId == null || result.containsKey(modelVersionId)
                        || !Objects.equals(modelByVersion.get(modelVersionId), modelId)) continue;

                Map<String, Object> publishSnapshot = json(instance.getModelSnapshot());
                Map<String, Object> model = objectMap(publishSnapshot.get("model"));
                if (!Objects.equals(modelId, number(model.get("id")))) continue;
                Object rawFields = model.get("fields");
                if (!(rawFields instanceof List)) continue;

                List<Map<String, Object>> columns = new ArrayList<>();
                for (Object rawField : (List<?>) rawFields) {
                    Map<String, Object> field = objectMap(rawField);
                    if (field.isEmpty() || isTrue(field.get("delFlag"))) continue;
                    columns.add(field);
                }
                result.put(modelVersionId, columns);
            }
        }
        return result;
    }

    @SafeVarargs
    final Map<String, Object> changeIdentity(List<Map<String, Object>> columns, Map<String, Object>... snapshots) {
        Map<String, Object> identity = new LinkedHashMap<>();
        put(identity, "name", "—", "businessCode", null);
        if (columns == null || columns.isEmpty()) return identity;

        List<Map<String, Object>> nameColumns = columns.stream()
                .filter(column -> isTrue(column.get("nameFlag")))
                .collect(Collectors.toList());
        if (nameColumns.size() == 1) {
            String value = snapshotValue(str(nameColumns.get(0).get("code")), snapshots);
            if (!value.isEmpty()) identity.put("name", value);
        }

        Map<String, Object> codeColumn = codeColumn(columns);
        if (codeColumn != null) {
            String value = snapshotValue(str(codeColumn.get("code")), snapshots);
            if (!value.isEmpty()) identity.put("businessCode", value);
        }
        return identity;
    }

    private Map<String, Object> codeColumn(List<Map<String, Object>> columns) {
        List<Map<String, Object>> boundEncodingColumns = columns.stream()
                .filter(column -> "AUTOMATIC".equalsIgnoreCase(str(column.get("encodingMode")))
                        && hasText(str(column.get("encodingVersionId"))))
                .collect(Collectors.toList());
        return boundEncodingColumns.size() == 1 ? boundEncodingColumns.get(0) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private boolean isTrue(Object value) {
        String flag = str(value).trim();
        return "1".equals(flag) || "true".equalsIgnoreCase(flag);
    }

    @SafeVarargs
    private final String snapshotValue(String fieldCode, Map<String, Object>... snapshots) {
        if (fieldCode.isEmpty()) return "";
        for (Map<String, Object> snapshot : snapshots) {
            for (Map.Entry<String, Object> field : snapshot.entrySet()) {
                if (fieldCode.equalsIgnoreCase(field.getKey())) {
                    String value = str(field.getValue()).trim();
                    if (!value.isEmpty()) return value;
                }
            }
        }
        return "";
    }

    private List<Map<String, Object>> diff(Long modelId, Map<String, Object> before, Map<String, Object> proposed, Map<String, Object> after, boolean effective) {
        Map<String, String> names = labels(modelId);
        Map<String, Object> keys = new LinkedHashMap<>();
        before.keySet().forEach(key -> keys.put(key, null));
        proposed.keySet().forEach(key -> keys.put(key, null));
        after.keySet().forEach(key -> keys.put(key, null));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : keys.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "field", names.getOrDefault(key, key), "before", display(before.get(key)), "proposed", display(proposed.get(key)),
                    "after", display(after.get(key)), "final", display(effective ? after.get(key) : before.get(key)));
            result.add(row);
        }
        return result;
    }

    private static String processResult(String code) {
        switch (code) { case "1": return "已生效"; case "2": return "已驳回"; case "3": return "已撤回"; case "4": return "执行失败"; default: return "处理中"; }
    }

    private static String itemResult(String code) {
        switch (code) { case "2": return "已生效"; case "3": return "执行失败"; case "4": return "已跳过"; case "1": return "执行中"; default: return "待执行"; }
    }

    private static boolean isLifecycleChange(String type) {
        switch (type) {
            case "FREEZE": case "UNFREEZE": case "INVALIDATE": case "RECOVER": case "ARCHIVE": return true;
            default: return false;
        }
    }
}
