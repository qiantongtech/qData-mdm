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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditMasterChangeDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelInstanceDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperatorDO;
import tech.qiantong.qdata.mdm.audit.dal.dto.AuditOperationFactQuery;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditModelInstanceMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditOperatorMapper;

/**
 * 从已存在的业务事实读取操作事件，不写入或伪造审计日志。
 * 主数据变更一条记录对应一个事件，模型发布一条实例对应一次发布事件。
 * @author zyy
 */
@Service
public class AuditBusinessOperationTraceService extends AuditTraceReadService {
    @Resource private AuditModelInstanceMapper auditModelInstanceMapper;
    @Resource private AuditOperatorMapper auditOperatorMapper;
    @Resource private AuditOperationRelationService auditOperationRelationService;

    private static final List<String> CHANGE_TYPES = Arrays.asList(
            "CREATE", "UPDATE", "COPY", "RESTORE", "VERSION_RESTORE", "MERGE",
            "FREEZE", "UNFREEZE", "INVALIDATE", "RECOVER", "ARCHIVE");

    public AuditTracePageRespVO<AuditOperationTraceRespVO> operations(Map<String, String> filters) {
        String scope = hasText(filters.get("scope")) ? filters.get("scope") : "master";
        if (!"master".equals(scope) && !"model".equals(scope)) return emptyPage();
        AuditOperationFactQuery query = query(filters, scope);
        if (query == null) return emptyPage();
        int pageNum = bounded(filters.get("pageNum"), 1, 1, Integer.MAX_VALUE);
        int pageSize = bounded(filters.get("pageSize"), 10, 1, 200);
        List<AuditOperationTraceRespVO> rows;
        Map<Long, Long> actors = new HashMap<>();
        if ("model".equals(scope)) {
            Page<AuditModelInstanceDO> page = auditModelInstanceMapper.selectOperationPage(new Page<>(pageNum, pageSize), query);
            rows = page.getRecords().stream().map(this::publishRow).collect(Collectors.toList());
            page.getRecords().forEach(instance -> actors.put(instance.getId(), instance.getCreatorId()));
            complete(rows, actors);
            for (int i = 0; i < rows.size(); i++) completePublish(rows.get(i), page.getRecords().get(i));
            return new AuditTracePageRespVO<>(rows, page.getTotal());
        }
        // 实际变化需要比较快照，分批筛选后再计数和分页，避免过滤当前页导致总数错误。
        return actualChangePage(query, filters, pageNum, pageSize);
    }

    public AuditOperationTraceRespVO operation(long id, String source) {
        if (id <= 0) return null;
        Map<Long, Long> actors = new HashMap<>();
        AuditOperationTraceRespVO row;
        if ("change".equals(source)) {
            AuditMasterChangeDO change = auditMasterChangeMapper.selectById(id);
            if (change == null || !actualChange(change)) return null;
            row = changeRow(change);
            actors.put(id, change.getOperatorId());
            complete(Collections.singletonList(row), actors);
            return row;
        }
        if ("publish".equals(source)) {
            AuditModelInstanceDO instance = auditModelInstanceMapper.selectById(id);
            if (instance == null || !actualPublish(instance)) return null;
            row = publishRow(instance);
            actors.put(id, instance.getCreatorId());
            complete(Collections.singletonList(row), actors);
            completePublish(row, instance);
            return row;
        }
        return null;
    }

    private AuditTracePageRespVO<AuditOperationTraceRespVO> actualChangePage(
            AuditOperationFactQuery query, Map<String, String> filters, int pageNum, int pageSize) {
        final int batchSize = 500;
        long first = ((long) pageNum - 1) * pageSize;
        long total = 0;
        List<AuditOperationTraceRespVO> selected = new ArrayList<>();
        Map<Long, Long> actors = new HashMap<>();
        String wantedName = text(filters.get("objectName"));
        for (long batchNum = 1; ; batchNum++) {
            Page<AuditMasterChangeDO> batch = auditMasterChangeMapper.selectOperationPage(
                    new Page<>(batchNum, batchSize, false), query);
            List<AuditMasterChangeDO> changes = batch.getRecords().stream()
                    .filter(this::actualChange).collect(Collectors.toList());
            List<AuditOperationTraceRespVO> candidates = changes.stream().map(this::changeRow).collect(Collectors.toList());
            if (wantedName != null) completeRelations(candidates);
            for (int i = 0; i < candidates.size(); i++) {
                AuditOperationTraceRespVO candidate = candidates.get(i);
                if (wantedName != null && (!hasText(candidate.getMasterDataName())
                        || !candidate.getMasterDataName().contains(wantedName))) continue;
                if (total >= first && selected.size() < pageSize) {
                    selected.add(candidate);
                    actors.put(candidate.getId(), changes.get(i).getOperatorId());
                }
                total++;
            }
            if (batch.getRecords().size() < batchSize) break;
        }
        if (wantedName == null) completeRelations(selected);
        completePhones(selected, actors);
        return new AuditTracePageRespVO<>(selected, total);
    }

    /** 仅在记录能够证明成功且发生变化时，将其作为审计事实。 */
    private boolean actualChange(AuditMasterChangeDO change) {
        if (change == null || !"1".equals(change.getProcessResult())
                || !CHANGE_TYPES.contains(str(change.getType())) || change.getMasterRecordId() == null) return false;
        if (hasText(change.getBeforeStatus()) && hasText(change.getAfterStatus())
                && !change.getBeforeStatus().equals(change.getAfterStatus())) return true;
        JsonNode before = snapshot(change.getBeforeData());
        JsonNode after = snapshot(change.getAfterData());
        if (after == null) return false;
        if (("CREATE".equals(change.getType()) || "COPY".equals(change.getType()))
                && (before == null || before.size() == 0)) return after.size() > 0;
        if (before == null) return false;
        Iterator<String> names = before.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (after.has(field) && !Objects.equals(before.get(field), after.get(field))) return true;
        }
        return false;
    }

    private JsonNode snapshot(String value) {
        if (!hasText(value)) return null;
        try {
            JsonNode node = objectMapper.readTree(value);
            if (node == null || !node.isObject()) return null;
            JsonNode values = node.get("values");
            return values != null && values.isObject() ? values : node;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean actualPublish(AuditModelInstanceDO instance) {
        return "SUCCEEDED".equals(instance.getStatus()) && Arrays.asList(
                "FIRST_PUBLISH", "STRUCTURE_CHANGE", "METADATA_ONLY", "DISABLE", "MIXED")
                .contains(instance.getChangeStatus());
    }

    private void complete(List<AuditOperationTraceRespVO> rows, Map<Long, Long> actors) {
        completeRelations(rows);
        completePhones(rows, actors);
    }

    private void completeRelations(List<AuditOperationTraceRespVO> rows) {
        auditOperationRelationService.enrich(rows);
        rows.stream().filter(row -> "master".equals(row.getScope()))
                .forEach(row -> row.setObjectName(row.getMasterDataName()));
    }

    private void completePhones(List<AuditOperationTraceRespVO> rows, Map<Long, Long> actors) {
        List<Long> ids = rows.stream().map(row -> actors.get(row.getId())).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, String> phones = new HashMap<>();
        for (int start = 0; start < ids.size(); start += 500) {
            for (AuditOperatorDO user : auditOperatorMapper.selectCurrentPhones(ids.subList(start, Math.min(start + 500, ids.size())))) {
                phones.put(user.getUserId(), user.getPhoneNumber());
            }
        }
        rows.forEach(row -> row.setOperatorPhone(phones.get(actors.get(row.getId()))));
    }

    private AuditOperationTraceRespVO changeRow(AuditMasterChangeDO change) {
        AuditOperationTraceRespVO row = event(change.getId(), "change", "master");
        row.setChangeId(change.getId());
        row.setModelId(change.getModelId());
        row.setModelVersionId(change.getModelVersionId());
        row.setMasterRecordId(change.getMasterRecordId());
        row.setObjectId(change.getMasterRecordId() == null ? null : str(change.getMasterRecordId()));
        row.setObjectType("主数据记录");
        row.setType(type(str(change.getType())));
        row.setOperatorName(change.getOperatorName());
        row.setOperateResult(changeResult(change.getProcessResult()));
        row.setOperateTime(date(change.getChangeTime()));
        row.setFunctionName("主数据变更");
        row.setOperateDesc(change.getChangeReason());
        row.setAffectCount(change.getMasterRecordId() == null ? null : 1);
        return row;
    }

    private AuditOperationTraceRespVO publishRow(AuditModelInstanceDO instance) {
        AuditOperationTraceRespVO row = event(instance.getId(), "publish", "model");
        row.setModelId(instance.getModelId());
        row.setModelVersionId(instance.getModelVersionId());
        row.setObjectId(instance.getModelId() == null ? null : str(instance.getModelId()));
        row.setObjectType("主数据模型");
        row.setType("DISABLE".equals(instance.getChangeStatus()) ? "停用模型" : "发布模型");
        row.setOperatorName(instance.getCreateBy());
        row.setOperateTime(date(instance.getFinishedTime()));
        row.setOperateResult(publishResult(instance.getStatus()));
        row.setFunctionName("模型发布");
        row.setOperateDesc(instance.getFailureReason());
        row.setAffectCount(1);
        return row;
    }

    private AuditOperationTraceRespVO event(Long id, String source, String scope) {
        AuditOperationTraceRespVO row = new AuditOperationTraceRespVO();
        row.setId(id);
        row.setSourceId(id);
        row.setSourceType(source);
        row.setEventKey(source + "-" + id);
        row.setOperateId(source + "-" + id);
        row.setScope(scope);
        row.setBusinessScope("master".equals(scope) ? "主数据管理" : "模型管理");
        row.setAffectedRows(Collections.emptyList());
        return row;
    }

    /** 当前模型被删除或改名时，发布实例中的配置仍可提供模型名称等展示值。 */
    private void completePublish(AuditOperationTraceRespVO row, AuditModelInstanceDO instance) {
        Object raw = json(instance.getModelSnapshot()).get("model");
        if (raw instanceof Map) {
            Map<?, ?> model = (Map<?, ?>) raw;
            if (Objects.equals(row.getModelId(), number(model.get("id")))) {
                if (!hasText(row.getModelDisplayName())) row.setModelDisplayName(text(model.get("name")));
                if (!hasText(row.getModelBusinessCode())) row.setModelBusinessCode(text(model.get("code")));
                if (!hasText(row.getModelType())) row.setModelType(text(model.get("type")));
                if (!hasText(row.getModelDescription())) row.setModelDescription(text(model.get("description")));
                if (!hasText(row.getModelIcon())) row.setModelIcon(text(model.get("icon")));
            }
        }
        row.setModelName(row.getModelDisplayName());
        row.setModelCode(row.getModelBusinessCode());
        row.setObjectName(row.getModelDisplayName());
        if (!hasText(row.getOperateDesc())) {
            row.setOperateDesc(row.getType() + "，" + row.getOperateResult());
        }
    }

    private AuditOperationFactQuery query(Map<String, String> filters, String scope) {
        AuditOperationFactQuery query = new AuditOperationFactQuery();
        query.setModelName(text(filters.get("modelName")));
        if ("master".equals(scope)) query.setModelName(text(filters.get("objectType")));
        query.setModelCode(text(filters.get("modelCode")));
        query.setOperatorName(text(filters.get("operatorName")));
        query.setOrderBy(filters.get("orderByColumn"));
        query.setAscending("ascending".equals(filters.get("isAsc")));
        if (hasText(filters.get("treeModelIds"))) {
            List<Long> ids = new ArrayList<>();
            for (String value : filters.get("treeModelIds").split(",")) {
                Long id = number(value.trim());
                if (id == null || id <= 0) return null;
                ids.add(id);
            }
            query.setModelIds(ids);
        } else if (hasText(filters.get("treeModelNames"))) {
            return null;
        }
        if ("master".equals(scope) && hasText(filters.get("objectId"))) {
            Long id = number(filters.get("objectId").trim());
            if (id == null || id <= 0) return null;
            query.setRecordId(id);
        }
        if (hasText(filters.get("type"))) {
            String selected = filters.get("type").trim();
            if ("master".equals(scope)) {
                query.setTypes(CHANGE_TYPES.stream().filter(code -> selected.equals(code) || selected.equals(type(code)))
                        .collect(Collectors.toList()));
                if (query.getTypes().isEmpty()) return null;
            } else if ("停用模型".equals(selected)) {
                query.setTypes(Collections.singletonList("DISABLE"));
            } else if ("发布模型".equals(selected)) {
                query.setTypes(Collections.singletonList("PUBLISH"));
            } else return null;
        }
        query.setBeginTime(parseTime(filters.get("beginTime"), false));
        query.setEndTime(parseTime(filters.get("endTime"), true));
        if (hasText(filters.get("beginTime")) && query.getBeginTime() == null) return null;
        if (hasText(filters.get("endTime")) && query.getEndTime() == null) return null;
        return query;
    }

    private Date parseTime(String input, boolean end) {
        if (!hasText(input)) return null;
        String value = input.trim();
        String pattern = value.length() == 10 ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        try {
            Date date = format.parse(value);
            if (!format.format(date).equals(value)) return null;
            if (end && value.length() == 10) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date); calendar.add(Calendar.DATE, 1);
                return calendar.getTime();
            }
            if (end) return new Date(date.getTime() + 1000L);
            return date;
        } catch (ParseException exception) {
            return null;
        }
    }

    private String text(Object value) {
        String text = value == null ? null : str(value).trim();
        return hasText(text) ? text : null;
    }

    private String changeResult(String code) {
        if ("1".equals(code)) return "成功";
        if ("4".equals(code)) return "失败";
        if ("0".equals(code)) return "处理中";
        if ("2".equals(code)) return "已驳回";
        if ("3".equals(code)) return "已撤回";
        return hasText(code) ? code : "未知";
    }

    private String publishResult(String status) {
        if ("SUCCEEDED".equals(status)) return "成功";
        if ("FAILED".equals(status)) return "失败";
        if ("PARTIAL_FAILED".equals(status)) return "部分失败";
        if ("PREPARING".equals(status)) return "准备中";
        if ("EXECUTING".equals(status)) return "执行中";
        if ("COMPENSATING".equals(status)) return "补偿中";
        if ("COMMIT_PENDING".equals(status)) return "待提交";
        if ("UNKNOWN".equals(status)) return "未知";
        return hasText(status) ? status : "未知";
    }

    private AuditTracePageRespVO<AuditOperationTraceRespVO> emptyPage() {
        return new AuditTracePageRespVO<>(Collections.emptyList(), 0L);
    }
}
