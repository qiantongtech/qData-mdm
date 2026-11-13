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
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperatorDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditOperationLogMapper;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditOperatorMapper;
import tech.qiantong.qdata.mdm.audit.config.AuditOperationCodeProperties;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import tech.qiantong.qdata.mdm.audit.service.audit.IAuditOperationTraceService;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationTraceRespVO;

@Service
public class AuditOperationTraceServiceImpl extends AuditTraceReadService implements IAuditOperationTraceService {
    @Resource private AuditOperationLogMapper auditOperationLogMapper;
    @Resource private AuditOperatorMapper auditOperatorMapper;
    @Resource private AuditOperationRelationService auditOperationRelationService;
    @Resource private AuditOperationCodeProperties auditOperationCodeProperties;
    @Resource private AuditBusinessOperationTraceService auditBusinessOperationTraceService;

    @Override
    public AuditTracePageRespVO<AuditOperationTraceRespVO> operations(Map<String, String> filters) {
        // 默认展示已经存在的业务事件；原日志查询保留为 source=log，不与业务事件混合重复计数。
        if (!"log".equals(filters.get("source"))) return auditBusinessOperationTraceService.operations(filters);
        String scope = filters.get("scope");
        if (hasText(scope) && !"master".equals(scope) && !"model".equals(scope)) {
            return page(Collections.emptyList(), filters);
        }

        Date begin = parseOperationTime(filters.get("beginTime"), false);
        Date end = parseOperationTime(filters.get("endTime"), true);
        if ((hasTime(filters.get("beginTime")) && begin == null) || (hasTime(filters.get("endTime")) && end == null)) {
            return page(Collections.emptyList(), filters);
        }
        if (hasText(filters.get("operateId")) && number(filters.get("operateId")) == null) {
            return page(Collections.emptyList(), filters);
        }
        Map<String, String> query = new HashMap<>(filters);
        query.remove("_modelIds");
        query.put("_moduleCodes", String.join(",", auditOperationCodeProperties.moduleCodes(scope == null ? "" : scope)));
        query.put("_typeCodes", String.join(",", auditOperationCodeProperties.typeCodes(scope, str(filters.get("type")))));
        String modelName = "master".equals(scope) ? filters.get("objectType") : filters.get("modelName");
        if (hasText(modelName) || hasText(filters.get("modelCode"))) {
            List<AuditModelNameDO> models = auditModelNameMapper.selectOperationModels(modelName, filters.get("modelCode"));
            if (models.isEmpty()) return page(Collections.emptyList(), filters);
            query.put("_modelIds", models.stream().map(model -> str(model.getId())).collect(Collectors.joining(",")));
        }
        int pageNum = bounded(filters.get("pageNum"), 1, 1, Integer.MAX_VALUE);
        int pageSize = bounded(filters.get("pageSize"), 10, 1, 200);
        Page<AuditOperationLogDO> result = auditOperationLogMapper.selectTracePage(new Page<>(pageNum, pageSize), query, begin, end);
        List<AuditOperationTraceRespVO> rows = result.getRecords().stream().map(this::operationRow).collect(Collectors.toList());
        enrichOperatorPhones(rows, result.getRecords());
        auditOperationRelationService.enrich(rows);
        return new AuditTracePageRespVO<>(rows, result.getTotal());
    }

    @Override
    public AuditOperationTraceRespVO operation(long id, String source) {
        if ("change".equals(source) || "publish".equals(source)) {
            return auditBusinessOperationTraceService.operation(id, source);
        }
        return "log".equals(source) ? operation(id) : null;
    }

    @Override
    public AuditOperationTraceRespVO operation(long id) {
        AuditOperationLogDO log = auditOperationLogMapper.selectById(id);
        if (log == null) return null;
        AuditOperationTraceRespVO row = operationRow(log);
        enrichOperatorPhones(Collections.singletonList(row), Collections.singletonList(log));
        auditOperationRelationService.enrich(Collections.singletonList(row));
        return row;
    }

    /** 只为当前页批量读取用户当前电话；日志未记录历史电话。 */
    private void enrichOperatorPhones(List<AuditOperationTraceRespVO> rows, List<AuditOperationLogDO> logs) {
        if (rows.isEmpty()) return;
        Set<Long> rowIds = rows.stream().map(AuditOperationTraceRespVO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> operatorsByLogId = new HashMap<>();
        for (AuditOperationLogDO log : logs) {
            if (rowIds.contains(log.getId()) && log.getOperatorId() != null) {
                operatorsByLogId.put(log.getId(), log.getOperatorId());
            }
        }
        List<Long> operatorIds = new ArrayList<>(new HashSet<>(operatorsByLogId.values()));
        Map<Long, String> phonesByOperatorId = new HashMap<>();
        for (int start = 0; start < operatorIds.size(); start += 500) {
            List<Long> batch = operatorIds.subList(start, Math.min(start + 500, operatorIds.size()));
            for (AuditOperatorDO operator : auditOperatorMapper.selectCurrentPhones(batch)) {
                phonesByOperatorId.put(operator.getUserId(), operator.getPhoneNumber());
            }
        }
        for (AuditOperationTraceRespVO row : rows) {
            row.setOperatorPhone(phonesByOperatorId.get(operatorsByLogId.get(row.getId())));
        }
    }

    private AuditOperationTraceRespVO operationRow(AuditOperationLogDO log) {
        AuditOperationTraceRespVO row = new AuditOperationTraceRespVO();
        String scope = auditOperationCodeProperties.scope(log.getModuleCode());
        row.setId(log.getId());
        row.setSourceType("log");
        row.setSourceId(log.getId());
        row.setEventKey("log-" + log.getId());
        row.setScope(scope);
        row.setOperateId(str(log.getId()));
        row.setObjectType(log.getObjectType());
        row.setObjectId(log.getObjectId());
        row.setObjectName(log.getObjectName());
        row.setModelId(log.getModelId());
        row.setModelVersionId(log.getModelVersionId());
        row.setMasterRecordId(log.getMasterRecordId());
        row.setChangeId(log.getChangeId());
        if ("master".equals(scope)) row.setMasterDataName(log.getObjectName());
        if ("model".equals(scope)) row.setModelDisplayName(log.getObjectName());
        row.setType(auditOperationCodeProperties.type(scope, log.getType()));
        row.setOperatorName(log.getOperatorName());
        row.setOperateResult(auditOperationCodeProperties.result(log.getOperateResult()));
        row.setOperateTime(date(log.getOperateTime()));
        row.setBusinessScope("model".equals(scope) ? "模型管理" : ("master".equals(scope) ? "主数据管理" : null));
        row.setFunctionName(log.getFunctionCode());
        row.setModuleCode(log.getModuleCode());
        // 现有表未保存这些事实；不以假数据填充批量明细、模型版本和描述。
        row.setAffectedRows(Collections.emptyList());
        return row;
    }

    private static boolean hasTime(String value) {
        return value != null && !value.isEmpty();
    }

    private static Date parseOperationTime(String value, boolean endOfDay) {
        if (!hasTime(value)) return null;
        if (endOfDay && value.length() != 10) return null;
        String text = value;
        String pattern = text.length() == 10 ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        try {
            Date parsed = format.parse(text);
            if (!format.format(parsed).equals(text)) return null;
            if (endOfDay) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsed);
                calendar.add(Calendar.DATE, 1);
                return calendar.getTime();
            }
            return parsed;
        } catch (ParseException ignored) {
            return null;
        }
    }
}
