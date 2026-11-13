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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelColumnDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditMasterChangeDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestItemDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelNameMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelColumnMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditMasterChangeMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditChangeRequestItemMapper;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** 审计追溯的只读查询。历史事实必须由对应业务操作写入。 */
public abstract class AuditTraceReadService {
    @Resource protected ObjectMapper objectMapper;
    @Resource protected AuditModelNameMapper auditModelNameMapper;
    @Resource protected AuditModelColumnMapper auditModelColumnMapper;
    @Resource protected AuditMasterChangeMapper auditMasterChangeMapper;
    @Resource protected AuditChangeRequestItemMapper auditChangeRequestItemMapper;

    protected static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    protected Map<Long, String> modelNamesByIds(List<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        for (int start = 0; start < ids.size(); start += 500) {
            for (AuditModelNameDO model : auditModelNameMapper.selectBatchIds(ids.subList(start, Math.min(start + 500, ids.size())))) {
                result.put(model.getId(), model.getName());
            }
        }
        return result;
    }

    protected String latestState(Long modelId, Long recordId) {
        if (modelId == null || recordId == null) return "—";
        List<AuditMasterChangeDO> content = auditMasterChangeMapper.selectEffectiveStates(modelId, recordId);
        List<AuditChangeRequestItemDO> lifecycle = auditChangeRequestItemMapper.selectEffectiveStates(modelId, recordId);
        if (content.isEmpty() && lifecycle.isEmpty()) return "—";
        if (content.isEmpty()) return lifecycleStatus(lifecycle.get(0).getTargetStatus());
        if (lifecycle.isEmpty() || date(content.get(0).getChangeTime()).compareTo(date(lifecycle.get(0).getExecuteTime())) >= 0)
            return lifecycleStatus(content.get(0).getAfterStatus());
        return lifecycleStatus(lifecycle.get(0).getTargetStatus());
    }

    protected String lifecycleStatus(Object value) {
        String status = str(value);
        switch (status) {
            case "1": return "生效";
            case "2": return "冻结";
            case "3": return "失效";
            case "4": return "归档";
            default: return status;
        }
    }

    protected String name(Long modelId, Map<String, Object> snapshot) {
        if (modelId == null) return "—";
        for (AuditModelColumnDO column : auditModelColumnMapper.selectNameColumns(modelId)) {
            String result = str(snapshot.get(str(column.getCode())));
            if (!result.isEmpty()) return result;
        }
        return "—";
    }

    /** 展示业务编码；内部 MASTER_RECORD_ID 仍用于详情查询，不把各模型重复的记录号当作业务编码。 */
    protected String businessCode(Long modelId, Map<String, Object> snapshot, Long masterRecordId) {
        Map<String, String> names = labels(modelId);
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            String label = names.getOrDefault(entry.getKey(), entry.getKey());
            if (("编码".equals(label) || label.endsWith("编码") || label.endsWith("编号"))
                    && !label.contains("信用代码")) {
                String value = str(entry.getValue());
                if (!value.isEmpty()) return value;
            }
        }
        return String.valueOf(masterRecordId);
    }

    protected Map<String, String> labels(Long modelId) {
        if (modelId == null) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>();
        for (AuditModelColumnDO raw : auditModelColumnMapper.selectLabels(modelId)) result.put(str(raw.getCode()), str(raw.getName()));
        return result;
    }

    protected List<Map<String, Object>> fields(Long modelId, Map<String, Object> snapshot) {
        Map<String, String> names = labels(modelId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "code", entry.getKey(), "field", names.getOrDefault(entry.getKey(), entry.getKey()), "value", display(entry.getValue()));
            result.add(row);
        }
        return result;
    }

    protected Map<String, Object> json(Object value) {
        if (value == null) return Collections.emptyMap();
        try {
            Map<String, Object> result = objectMapper.readValue(value.toString(), new TypeReference<Map<String, Object>>() {});
            return result == null ? Collections.emptyMap() : result;
        } catch (Exception ignored) { return Collections.emptyMap(); }
    }

    protected <T> AuditTracePageRespVO<T> page(List<T> rows, Map<String, String> filters) {
        int pageNum = bounded(filters.get("pageNum"), 1, 1, Integer.MAX_VALUE);
        int pageSize = bounded(filters.get("pageSize"), 10, 1, 200);
        int from = (int) Math.min((long) (pageNum - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new AuditTracePageRespVO<>(rows.subList(from, to), rows.size());
    }

    /** 只为查询条件组装键值，不作为接口返回对象。 */
    protected boolean matchesValues(Map<String, String> filters, Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        String[] keys = new String[entries.length / 2];
        for (int i = 0; i < entries.length; i += 2) {
            keys[i / 2] = (String) entries[i];
            values.put(keys[i / 2], entries[i + 1]);
        }
        return matches(values, filters, keys);
    }

    protected <T> Comparator<T> descendingValue(Function<T, ?> value) {
        return (a, b) -> str(value.apply(b)).compareTo(str(value.apply(a)));
    }

    protected <T> Comparator<T> numericSortValue(Map<String, String> filters, String sortKey, Function<T, ?> value) {
        if (!sortKey.equals(filters.get("orderByColumn"))) return null;
        String order = filters.get("isAsc");
        if (!"ascending".equals(order) && !"descending".equals(order)) return null;
        Comparator<T> comparator = Comparator.comparingLong(row -> {
            Long number = number(value.apply(row));
            return number == null ? Long.MIN_VALUE : number;
        });
        return "ascending".equals(order) ? comparator : comparator.reversed();
    }

    protected boolean matches(Map<String, Object> row, Map<String, String> filters, String... keys) {
        for (String key : keys) {
            String wanted = filters.get(key);
            if (wanted == null || wanted.trim().isEmpty()) continue;
            String actual = str(row.get(key));
            if ("scope".equals(key) || "type".equals(key) || "category".equals(key) || "result".equals(key)
                    || "state".equals(key) || "mode".equals(key) || "objectType".equals(key) || "model".equals(key)) {
                if (!actual.equals(wanted)) return false;
            } else if (!actual.toLowerCase().contains(wanted.trim().toLowerCase())) return false;
        }
        return true;
    }

    protected boolean inTime(Object time, Map<String, String> filters) {
        String actual = str(time);
        String begin = filters.get("beginTime");
        String end = filters.get("endTime");
        return (begin == null || begin.isEmpty() || actual.compareTo(begin) >= 0)
                && (end == null || end.isEmpty() || actual.compareTo(end + " 23:59:59") <= 0);
    }

    protected static String type(String code) {
        switch (code) {
            case "CREATE": return "新增"; case "UPDATE": return "修改"; case "COPY": return "复制";
            case "RESTORE": case "VERSION_RESTORE": return "历史版本恢复"; case "MERGE": return "多源融合";
            case "FREEZE": return "冻结"; case "UNFREEZE": return "解冻"; case "INVALIDATE": return "失效";
            case "RECOVER": return "恢复"; case "ARCHIVE": return "归档"; case "INITIAL": return "初始版本";
            default: return code;
        }
    }

    protected static int bounded(String value, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(value))); } catch (Exception ignored) { return fallback; }
    }
    protected static Long number(Object value) {
        try { return value == null ? null : Long.valueOf(value.toString()); } catch (Exception ignored) { return null; }
    }
    protected static String date(Object value) {
        return value instanceof Date ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value) : str(value);
    }
    protected static String display(Object value) { return value == null ? "—" : value.toString(); }
    protected static String str(Object value) { return Objects.toString(value, ""); }
    protected static void put(Map<String, Object> target, Object... entries) {
        for (int i = 0; i < entries.length; i += 2) target.put((String) entries[i], entries[i + 1]);
    }
}
