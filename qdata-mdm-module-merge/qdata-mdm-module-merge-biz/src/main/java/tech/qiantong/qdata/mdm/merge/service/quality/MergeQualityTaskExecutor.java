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

package tech.qiantong.qdata.mdm.merge.service.quality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataConnectionProvider;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergeQualityCheckItemBO;
import tech.qiantong.qdata.mdm.merge.support.MergeJson;
import tech.qiantong.qdata.mdm.merge.support.MergeQualityChecker;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelMaterializedColumnDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelMaterializedTableDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelInstanceMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelMaterializedColumnMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelMaterializedTableMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelVersionMapper;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance.QualityTaskExecutor;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.RuleQualityMapper;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 按已发布权威主表执行质量任务，汇总筛选范围内的记录检测结果。
 */
@Service
public class MergeQualityTaskExecutor implements QualityTaskExecutor {
    private static final int PAGE_SIZE = 500;

    @Resource
    private ModelInstanceMapper instanceMapper;
    @Resource
    private ModelMaterializedTableMapper tableMapper;
    @Resource
    private ModelMaterializedColumnMapper columnMapper;
    @Resource
    private ModelVersionMapper versionMapper;
    @Resource
    private ModelPublishedApi modelPublishedApi;
    @Resource
    private RuleQualityMapper ruleMapper;
    @Resource
    private MasterDataConnectionProvider connectionProvider;

    /**
     * 兼容规则表中的字段编号配置，仅在 JSON 未声明相应字段时补入。
     */
    private static JsonNode ruleConfig(RuleQualityDO rule) {
        JsonNode config = readConfig(rule.getRuleConfig());
        if (rule.getModelFieldId() == null || rule.getModelFieldId().trim().isEmpty()) {
            return config;
        }
        String[] ids = rule.getModelFieldId().split(",");
        ObjectNode normalized = config.deepCopy();
        String type = rule.getRuleType() == null ? "" : rule.getRuleType().toUpperCase(Locale.ROOT);
        if ("UNIQUE".equals(type) && !normalized.has("fieldList") && !normalized.has("fieldIdList")) {
            ArrayNode fields = normalized.putArray("fieldList");
            for (String id : ids) if (!id.trim().isEmpty()) fields.add(id.trim());
        } else if ("FIELD_LOGIC".equals(type)) {
            if (!normalized.has("modelFieldId1") && ids.length > 0) normalized.put("modelFieldId1", ids[0].trim());
            if (!normalized.has("modelFieldId2") && ids.length > 1) normalized.put("modelFieldId2", ids[1].trim());
        } else if (!normalized.has("modelFieldId") && !normalized.has("fieldId") && ids.length > 0) {
            normalized.put("modelFieldId", ids[0].trim());
        }
        return normalized;
    }

    /**
     * 收集单字段、双字段和组合规则所需字段，供查询投影及发布结构校验使用。
     */
    private static void collectRuleFields(JsonNode config, Set<String> fields) {
        for (String key : new String[]{"modelFieldId", "fieldId", "modelFieldId1", "fieldId1", "modelFieldId2", "fieldId2"}) {
            JsonNode value = config.get(key);
            if (value != null && !value.isNull() && !value.asText().trim().isEmpty()) fields.add(value.asText().trim());
        }
        fields.addAll(listFields(config));
    }

    /**
     * 收集记录命中的全部失败规则；组合唯一性中的空字段交由非空规则约束。
     */
    static List<QualityIssueInfoDO> failures(Map<String, Object> row, List<RuleQualityDO> rules,
                                           Map<Long, Set<String>> duplicates, Map<String, Set<String>> referenceValues) {
        Map<String, String> values = new HashMap<>();
        row.forEach((key, value) -> values.put(key, value == null ? "" : value.toString()));
        List<MergeQualityCheckItemBO> results = MergeQualityChecker.check(values, rules,
                Collections.emptyList(), Collections.emptySet(), referenceValues);
        List<QualityIssueInfoDO> failures = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            RuleQualityDO rule = rules.get(i);
            String type = rule.getRuleType() == null ? "" : rule.getRuleType().toUpperCase(Locale.ROOT);
            MergeQualityCheckItemBO result = results.get(i);
            if (!result.isExecuted()) {
                throw new IllegalArgumentException(rule.getRuleName() + "：" + result.getMessage());
            }
            List<String> fields = MergeQualityChecker.fieldKeys(rule);
            boolean passed = result.isPassed();
            String message = result.getMessage();
            if ("UNIQUE".equals(type)) {
                if (fields.stream().anyMatch(field -> values.getOrDefault(field, "").trim().isEmpty())) continue;
                Set<String> keys = duplicates.getOrDefault(rule.getId(), Collections.emptySet());
                passed = !keys.contains(signature(row, fields));
                message = "字段组合与检测范围内其他数据重复";
            }
            if (!passed) {
                QualityIssueInfoDO detail = new QualityIssueInfoDO();
                detail.setQualityRuleId(rule.getId());
                detail.setQualityRuleName(rule.getRuleName());
                detail.setQualityRuleType(type);
                if (!fields.isEmpty()) detail.setModelColumnId(Long.valueOf(fields.get(0)));
                String reason = rule.getRuleName() + "：" + message;
                detail.setFailReason(reason.substring(0, Math.min(reason.length(), 500)));
                failures.add(detail);
            }
        }
        return failures;
    }

    /**
     * 在完整筛选范围内预查重复组合，避免逐页检测遗漏跨页重复记录。
     */
    private static Map<Long, Set<String>> findDuplicates(MasterDataConnectionProvider.TableAccess table,
                                                        List<RuleQualityDO> rules, String baseSql, Map<String, Object> params) {
        Map<Long, Set<String>> result = new HashMap<>();
        for (RuleQualityDO rule : rules) {
            if (!"UNIQUE".equalsIgnoreCase(rule.getRuleType())) continue;
            List<String> fields = MergeQualityChecker.fieldKeys(rule);
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("唯一性规则没有配置字段");
            }
            String columns = fields.stream().map(field -> "QDATA_SCOPE." + table.quoteIdentifier(field.toUpperCase(Locale.ROOT)))
                    .collect(Collectors.joining(", "));
            String sql = "SELECT " + columns + " FROM (" + baseSql + ") QDATA_SCOPE GROUP BY " + columns
                    + " HAVING COUNT(*) > 1";
            Set<String> signatures = new HashSet<>();
            for (Map<String, Object> row : table.query(sql, params)) signatures.add(signature(row, fields));
            result.put(rule.getId(), signatures);
        }
        return result;
    }

    /**
     * 兼容两种字段列表配置及对象、标量两种列表元素，保留配置顺序。
     */
    private static List<String> listFields(JsonNode config) {
        List<String> fields = new ArrayList<>();
        JsonNode list = config.has("fieldList") ? config.get("fieldList") : config.get("fieldIdList");
        if (list != null && list.isArray()) for (JsonNode value : list) {
            String field = value.isObject() ? value.path("modelFieldId").asText(value.path("fieldId").asText("")) : value.asText("");
            if (!field.trim().isEmpty()) fields.add(field.trim());
        }
        return fields;
    }

    /**
     * 以长度前缀编码字段组合，避免字段值包含分隔符时出现拼接歧义。
     */
    private static String signature(Map<String, Object> row, List<String> fields) {
        StringBuilder key = new StringBuilder();
        for (String field : fields) {
            Object value = row.get(field.toUpperCase(Locale.ROOT));
            if (value == null) value = row.get(field);
            String text = value == null ? "" : value.toString().trim();
            key.append(text.length()).append(':').append(text).append('|');
        }
        return key.toString();
    }

    /**
     * 仅条件筛选模式解析范围配置，并将筛选字段加入查询投影。
     */
    private static Map<String, Object> buildScope(QualityTaskDO task, Map<String, String> columns, Set<String> selected) {
        if (!"2".equals(task.getDataScopeType())) {
            return Collections.emptyMap();
        }
        JsonNode root = readConfig(task.getDataScopeConfig());
        JsonNode conditions = root.path("conditions");
        if (!conditions.isArray() || conditions.isEmpty()) {
            throw new IllegalArgumentException("条件筛选范围未配置条件");
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (JsonNode condition : conditions) {
            String field = condition.path("fieldId").asText("").trim();
            if (!columns.containsKey(field)) {
                throw new IllegalArgumentException("数据范围引用了未发布字段：" + field);
            }
            selected.add(field);
            String operator = condition.path("operator").asText("").toUpperCase(Locale.ROOT);
            JsonNode value = condition.get("value");
            Map<String, Object> item = new HashMap<>();
            item.put("field", field);
            item.put("operator", operator);
            Object filterValue = "";
            if (value != null && !value.isNull()) {
                if (value.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode element : value) {
                        values.add(element.asText());
                    }
                    filterValue = values;
                } else {
                    filterValue = value.asText(value.toString());
                }
            }
            item.put("value", filterValue);
            normalized.add(item);
        }
        Map<String, Object> scope = new HashMap<>();
        scope.put("logic", "OR".equalsIgnoreCase(root.path("logic").asText()) ? "OR" : "AND");
        scope.put("conditions", normalized);
        return scope;
    }

    /**
     * 将筛选条件转换为参数化 SQL，并把加工后的绑定值写回 scope。
     */
    @SuppressWarnings("unchecked")
    private static String scopeSql(MasterDataConnectionProvider.TableAccess table, Map<String, Object> scope) {
        if (scope.isEmpty()) {
            return "";
        }
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) scope.get("conditions");
        List<String> sql = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> condition : conditions) {
            String column = table.column((String) condition.get("field"));
            String operator = (String) condition.get("operator");
            Object value = condition.get("value");
            String parameter = "scope" + index++;
            switch (operator) {
                case "IS_NULL":
                    sql.add(column + " IS NULL");
                    break;
                case "IS_NOT_NULL":
                    sql.add(column + " IS NOT NULL");
                    break;
                case "EQ":
                case "NE":
                    sql.add(column + ("EQ".equals(operator) ? " = :" : " <> :") + parameter);
                    break;
                case "GT":
                case "GE":
                case "LT":
                case "LE":
                    String comparison;
                    if ("GT".equals(operator)) {
                        comparison = ">";
                    } else if ("GE".equals(operator)) {
                        comparison = ">=";
                    } else if ("LT".equals(operator)) {
                        comparison = "<";
                    } else {
                        comparison = "<=";
                    }
                    sql.add(column + " " + comparison + " :" + parameter);
                    break;
                case "STARTS_WITH":
                    sql.add(column + " LIKE :" + parameter);
                    value = escapeLike(value.toString()) + "%";
                    break;
                case "ENDS_WITH":
                    sql.add(column + " LIKE :" + parameter);
                    value = "%" + escapeLike(value.toString());
                    break;
                case "CONTAINS":
                case "NOT_CONTAINS":
                    sql.add(column + ("CONTAINS".equals(operator) ? " LIKE :" : " NOT LIKE :") + parameter);
                    value = "%" + escapeLike(value.toString()) + "%";
                    break;
                case "IN":
                case "NOT_IN":
                    if (!(value instanceof Collection) || ((Collection<?>) value).isEmpty()) {
                        throw new IllegalArgumentException("数据范围的IN筛选值不能为空");
                    }
                    sql.add(column + ("IN".equals(operator) ? " IN (:" : " NOT IN (:") + parameter + ")");
                    break;
                default:
                    throw new IllegalArgumentException("不支持的数据范围运算符：" + operator);
            }
            if (!"IS_NULL".equals(operator) && !"IS_NOT_NULL".equals(operator)) {
                scope.put(parameter, value);
            }
        }
        return "(" + String.join("OR".equals(scope.get("logic")) ? " OR " : " AND ", sql) + ")";
    }

    /**
     * 转义用户值中的反斜杠及 LIKE 通配符，再由调用方补入匹配通配符。
     */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 执行规则并返回总数、通过数和问题数；同一记录命中多条规则只计一次。 */
    @Override
    public long[] execute(QualityTaskDO task, Consumer<QualityIssueDO> issueConsumer) {
        if (task == null || task.getModelId() == null) {
            throw new IllegalArgumentException("质量任务未绑定主数据模型");
        }
        ModelInstanceDO instance = instanceMapper.selectPublishedStructure(task.getModelId(), true);
        if (instance == null || instance.getAuthorityTableId() == null || instance.getAuthorityTableName() == null
                || instance.getDatasourceId() == null) {
            throw new IllegalStateException("模型没有可用的已发布权威主表");
        }
        ModelVersionDO version = versionMapper.selectById(instance.getModelVersionId());
        if (version == null || !Objects.equals(task.getModelVersion(), version.getVersionNo())) {
            throw new IllegalStateException("任务模型版本与已发布权威主表版本不一致，请重新选择模型版本");
        }
        List<ModelMaterializedTableDO> tables = tableMapper.selectPublishedTables(instance.getId());
        ModelMaterializedTableDO authority = tables.stream().filter(t -> instance.getAuthorityTableId().equals(t.getId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("已发布权威主表结构缺失"));
        List<ModelMaterializedColumnDO> columns = columnMapper.selectPublishedColumns(Collections.singleton(authority.getId()));
        Map<String, String> publishedColumns = new LinkedHashMap<>();
        for (ModelMaterializedColumnDO column : columns) {
            if (column.getModelFieldId() != null && column.getColumnName() != null)
                publishedColumns.put(column.getModelFieldId().toString(), column.getColumnName());
            if (column.getSystemFieldKey() != null && column.getColumnName() != null)
                publishedColumns.put(column.getSystemFieldKey(), column.getColumnName());
        }
        if (!publishedColumns.containsKey("ID")) {
            throw new IllegalStateException("权威主表缺少系统主键字段");
        }
        List<RuleQualityDO> rules = ruleMapper.selectModelRules(Collections.singleton(task.getModelId())).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getValidFlag()) && !Boolean.TRUE.equals(rule.getDelFlag())
                        && !"1".equals(rule.getAbnormalFlag()))
                .filter(rule -> rule.getModelVersion() == null || rule.getModelVersion().trim().isEmpty()
                        || rule.getModelVersion().equals(task.getModelVersion()))
                .collect(Collectors.toList());
        if (rules.isEmpty()) {
            throw new IllegalStateException("该模型版本暂无可执行质量规则");
        }
        // 配置只在开始执行时归一化，逐条记录直接使用融合质检的规则判断。
        rules.forEach(rule -> rule.setRuleConfig(ruleConfig(rule).toString()));
        Map<String, Set<String>> referenceValues = rules.stream()
                .anyMatch(rule -> "REF_DATA_VALID".equalsIgnoreCase(rule.getRuleType()))
                ? MergeQualityChecker.referenceValues(modelPublishedApi.getPublishedModelStructure(task.getModelId()))
                : Collections.emptyMap();
        Set<String> selected = new LinkedHashSet<>();
        selected.add("ID");
        if (publishedColumns.containsKey("DEL_FLAG")) selected.add("DEL_FLAG");
        for (RuleQualityDO rule : rules) collectRuleFields(readConfig(rule.getRuleConfig()), selected);
        Map<String, Object> scope = buildScope(task, publishedColumns, selected);
        Map<String, String> bindings = new LinkedHashMap<>();
        for (String field : selected) {
            String physical = publishedColumns.get(field);
            if (physical == null) {
                throw new IllegalStateException("质量规则或筛选范围引用了未发布字段：" + field);
            }
            bindings.put(field, physical);
        }
        try (MasterDataConnectionProvider.RequestContext request = connectionProvider.openRequest()) {
            MasterDataConnectionProvider.TableAccess table = request.forTable(instance.getDatasourceId(),
                    instance.getAuthorityTableName(), publishedColumns).withColumns(bindings);
            String where = scopeSql(table, scope);
            Map<String, Object> params = new HashMap<>(scope);
            params.remove("logic");
            params.remove("conditions");
            if (publishedColumns.containsKey("DEL_FLAG")) {
                where = (where.isEmpty() ? "" : where + " AND ") + table.column("DEL_FLAG") + " = :qualityDeleted";
                params.put("qualityDeleted", "0");
            }
            String baseSql = table.select(selected) + (where.isEmpty() ? "" : " WHERE " + where);

            Map<Long, Set<String>> duplicates = findDuplicates(table, rules, baseSql, params);
            long total = 0, failed = 0, offset = 0;
            String sql = baseSql + " ORDER BY " + table.column("ID");
            while (true) {
                List<Map<String, Object>> rows = table.page(sql, params, offset, PAGE_SIZE);
                if (rows.isEmpty()) break;
                for (Map<String, Object> row : rows) {
                    total++;
                    List<QualityIssueInfoDO> details = failures(row, rules, duplicates, referenceValues);
                    if (!details.isEmpty()) {
                        QualityIssueDO issue = new QualityIssueDO();
                        issue.setModelId(task.getModelId());
                        issue.setMasterRecordId(new BigDecimal(String.valueOf(row.get("ID"))).longValueExact());
                        issue.setQualityIssueInfoList(details);
                        issueConsumer.accept(issue);
                        failed++;
                    }
                }
                offset += rows.size();
                if (rows.size() < PAGE_SIZE) break;
            }
            return new long[]{total, total - failed, failed};
        }
    }

    private static JsonNode readConfig(String json) {
        JsonNode config = MergeJson.readStrict(json == null || json.trim().isEmpty() ? "{}" : json);
        if (!config.isObject()) throw new IllegalArgumentException("质量规则和检测范围配置必须为JSON对象");
        return config;
    }
}

