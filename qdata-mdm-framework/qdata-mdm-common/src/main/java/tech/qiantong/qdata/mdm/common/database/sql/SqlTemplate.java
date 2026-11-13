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

package tech.qiantong.qdata.mdm.common.database.sql;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 通用参数化 SQL 模板：按表元数据生成 INSERT、UPDATE 和聚合 SELECT。
 * 此类只生成 SQL 与有序参数，执行由 DbQuery 负责，事务由调用方控制。
 */
public final class SqlTemplate {
    private static final String INSERT_SQL = "INSERT INTO %s (%s) VALUES (%s)";
    private static final String UPDATE_SQL = "UPDATE %s SET %s%s";
    private static final String SELECT_SQL = "SELECT %s FROM %s%s";

    private final String sql;
    private final List<Object> parameters;

    private SqlTemplate(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public static SqlTemplate build(Table table, Operation operation) {
        if (operation == null) throw new ServiceException("SQL 操作不能为空");
        return operation.insert ? insert(table, operation.values)
                : update(table, operation.values, operation.conditions);
    }

    /** 表达式由调用方通过已校验字段生成；表名与写入、聚合统一使用方言限定名。 */
    public static SqlTemplate selectExpression(Table table, String expression) {
        if (table == null || expression == null || expression.trim().isEmpty())
            throw new ServiceException("查询表和字段表达式不能为空");
        return new SqlTemplate(String.format(Locale.ROOT, SELECT_SQL,
                expression, table.qualifiedTable(), ""), Collections.emptyList());
    }

    public static SqlTemplate insert(Table table, Map<String, Object> values) {
        if (table == null || values == null || values.isEmpty())
            throw new ServiceException("新增表和字段不能为空");
        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            columns.add(column(table, entry.getKey()));
            placeholders.add("?");
            parameters.add(table.jdbcParameter(entry.getKey(), entry.getValue()));
        }
        return new SqlTemplate(String.format(Locale.ROOT, INSERT_SQL,
                table.qualifiedTable(), columns, placeholders), parameters);
    }

    /** 只更新提交字段；强制提供条件，避免整表更新。 */
    public static SqlTemplate update(Table table, Map<String, Object> values, Map<String, Object> conditions) {
        if (table == null || values == null || values.isEmpty())
            throw new ServiceException("修改表和字段不能为空");
        if (conditions == null || conditions.isEmpty()) throw new ServiceException("更新条件不能为空");
        StringJoiner assignments = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            assignments.add(column(table, entry.getKey()) + " = ?");
            parameters.add(table.jdbcParameter(entry.getKey(), entry.getValue()));
        }
        String where = where(table, conditions, parameters);
        return new SqlTemplate(String.format(Locale.ROOT, UPDATE_SQL,
                table.qualifiedTable(), assignments, where), parameters);
    }

    /** 条件计数值与 WHERE 值均使用占位符；空表计数返回 0，MAX 返回 NULL。 */
    public static SqlTemplate aggregate(Table table, List<Aggregate> aggregates, Map<String, Object> conditions) {
        if (table == null || aggregates == null || aggregates.isEmpty())
            throw new ServiceException("聚合表和统计项不能为空");
        StringJoiner projections = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        for (Aggregate aggregate : aggregates) {
            String expression;
            switch (aggregate.type) {
                case COUNT:
                    expression = "COUNT(1)";
                    break;
                case COUNT_WHEN:
                    String comparison = aggregate.value == null ? " IS NULL" : " = ?";
                    expression = "COUNT(CASE WHEN " + column(table, aggregate.field) + comparison + " THEN 1 END)";
                    if (aggregate.value != null) parameters.add(table.jdbcParameter(aggregate.field, aggregate.value));
                    break;
                case COUNT_NON_BLANK:
                    expression = "COUNT(CASE WHEN " + table.trimExpression(column(table, aggregate.field))
                            + " LIKE ? THEN 1 END)";
                    // 至少匹配一个字符，兼容Oracle将空字符串视为NULL的行为。
                    parameters.add("_%");
                    break;
                case MAX:
                    expression = "MAX(" + column(table, aggregate.field) + ")";
                    break;
                default:
                    throw new ServiceException("不支持的聚合类型");
            }
            projections.add(expression + " AS " + table.quoteIdentifier(aggregate.alias));
        }
        String where = where(table, conditions, parameters);
        return new SqlTemplate(String.format(Locale.ROOT, SELECT_SQL,
                projections, table.qualifiedTable(), where), parameters);
    }

    private static String where(Table table, Map<String, Object> conditions, List<Object> parameters) {
        if (conditions == null || conditions.isEmpty()) return "";
        StringJoiner predicates = new StringJoiner(" AND ");
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String name = column(table, entry.getKey());
            predicates.add(name + (entry.getValue() == null ? " IS NULL" : " = ?"));
            if (entry.getValue() != null) parameters.add(table.jdbcParameter(entry.getKey(), entry.getValue()));
        }
        return " WHERE " + predicates;
    }

    private static String column(Table table, String field) {
        if (!table.contains(field)) throw new ServiceException("模型字段不存在：" + field);
        return table.column(field);
    }

    public String getSql() { return sql; }
    public List<Object> getParameters() { return parameters; }

    /** 表、字段从元数据解析，标识符引用交给数据库方言。 */
    public interface Table {
        String qualifiedTable();
        boolean contains(String field);
        String column(String field);
        String quoteIdentifier(String identifier);

        /** 对已校验并引用的字段表达式去除首尾空格，可按数据库方言适配。 */
        default String trimExpression(String expression) { return "TRIM(" + expression + ")"; }

        /** 按真实列类型适配绑定值，默认保留调用方传入值；不改变 SQL 占位符。 */
        default Object jdbcParameter(String field, Object value) { return value; }
    }

    /** 延迟执行的写操作描述，供主表、子表统一编排。 */
    public static final class Operation {
        private final boolean insert;
        private final Map<String, Object> values;
        private final Map<String, Object> conditions;

        private Operation(boolean insert, Map<String, Object> values, Map<String, Object> conditions) {
            this.insert = insert;
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            this.conditions = Collections.unmodifiableMap(new LinkedHashMap<>(conditions));
        }

        public static Operation insert(Map<String, Object> values) {
            return new Operation(true, values, Collections.emptyMap());
        }

        public static Operation update(Map<String, Object> values, Map<String, Object> conditions) {
            return new Operation(false, values, conditions);
        }

        public boolean isInsert() { return insert; }
        public Map<String, Object> getValues() { return values; }
        public Map<String, Object> getConditions() { return conditions; }
    }

    /** 业务只描述统计字段和条件，SQL 语法由通用模板负责。 */
    public static final class Aggregate {
        private enum Type { COUNT, COUNT_WHEN, COUNT_NON_BLANK, MAX }
        private final Type type;
        private final String field;
        private final Object value;
        private final String alias;

        private Aggregate(Type type, String field, Object value, String alias) {
            this.type = type;
            this.field = field;
            this.value = value;
            this.alias = alias;
        }

        public static Aggregate count(String alias) { return new Aggregate(Type.COUNT, null, null, alias); }
        public static Aggregate countWhen(String field, Object value, String alias) {
            return new Aggregate(Type.COUNT_WHEN, field, value, alias);
        }
        /** 统计排除NULL、空串及仅含空格值的文本字段记录数。 */
        public static Aggregate countNonBlank(String field, String alias) {
            return new Aggregate(Type.COUNT_NON_BLANK, field, null, alias);
        }
        public static Aggregate max(String field, String alias) { return new Aggregate(Type.MAX, field, null, alias); }
    }
}
