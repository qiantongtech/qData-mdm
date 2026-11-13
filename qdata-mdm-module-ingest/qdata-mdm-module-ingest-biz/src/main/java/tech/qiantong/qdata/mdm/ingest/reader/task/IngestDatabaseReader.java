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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskDbConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceDatasourceRelMapper;
import tech.qiantong.qdata.mdm.ingest.service.datasource.impl.IngestDatasourceServiceImpl;

import javax.annotation.Resource;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.DATABASE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.INCREMENTAL;

/**
 * 数据库读取策略，复用数据连接服务创建连接，校验来源字段后逐条回调。
 * 负责查询投影、增量筛选和读取资源的关闭，不承担字段转换或目标入库。
 *
 * @author chentangwen
 */
@Component
public class IngestDatabaseReader implements IngestReader {
    private static final int QUERY_TIMEOUT_SECONDS = 120;
    private static final int FETCH_SIZE = 500;
    @Resource
    private IngestDatasourceServiceImpl datasourceService;
    @Resource
    private IngestSourceDatasourceRelMapper sourceDatasourceRelMapper;

    @Override
    public String getType() {
        return DATABASE;
    }

    /**
     * 兼容只消费字段值的读取入口。
     *
     * @param task 接入任务配置
     * @param limit 最大回调条数，小于或等于 0 时不限制
     * @param consumer 来源字段值回调
     * @return 来源对象的完整字段列表
     */
    public List<String> read(IngestTaskSaveReqVO task, int limit, Consumer<Map<String, Object>> consumer) {
        return read(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 校验来源系统与连接的关联，按字段配置生成只读查询并同步回调。
     * JDBC 连接、语句、结果集以及 DbQuery 均在读取结束或异常时释放。
     *
     * @param context 本次读取配置与进度
     * @param consumer 来源记录回调
     * @return 来源对象的完整字段列表
     */
    @Override
    public List<String> read(IngestReadContext context, Consumer<IngestRow> consumer) {
        IngestTaskSaveReqVO task = context.getTask();
        IngestTaskDbConfigVO config = task.getDbConfig();
        if (config == null || config.getSourceObject() == null
                || task.getSourceId() == null || config.getDatasourceId() == null) {
            throw new ServiceException("请选择来源系统、数据库连接和数据对象");
        }
        boolean linked = sourceDatasourceRelMapper.selectBySourceIds(Collections.singletonList(task.getSourceId()))
                .stream().anyMatch(r -> config.getDatasourceId().equals(r.getDatasourceId()));
        if (!linked) {
            throw new ServiceException("数据库连接未关联当前来源系统");
        }
        DbQuery query = datasourceService.buildDbQuery(config.getDatasourceId());
        try (Connection connection = query.getConnection()) {
            connection.setReadOnly(true);
            Map<String, Integer> fieldTypes = getFieldTypes(connection, config);
            List<String> fields = new ArrayList<>(fieldTypes.keySet());
            context.validateFields(fields);
            boolean incremental = INCREMENTAL.equals(task.getIngestMode());
            Set<String> selected = context.requiredFields();
            // 预览返回全部来源列；执行只读取映射、标识及增量列。
            if (context.getLimit() > 0 || selected.isEmpty()) {
                selected = new LinkedHashSet<>(fields);
            }
            String sql = buildSelectSql(connection, task, selected, incremental, context.getCurrentIncrementValue() != null);
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            boolean transactionCursor = product.contains("postgresql");
            if (transactionCursor) {
                connection.setAutoCommit(false);
            }
            try (PreparedStatement statement = connection.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                // MySQL 使用驱动流式模式；PostgreSQL 使用事务游标。
                statement.setFetchSize(product.contains("mysql") ? Integer.MIN_VALUE : FETCH_SIZE);
                if (context.getLimit() > 0) {
                    statement.setMaxRows(context.getLimit());
                }
                if (incremental) {
                    // 按元数据类型绑定，避免数字、日期列与 VARCHAR 参数比较时出现驱动兼容问题。
                    Object start = context.getCurrentIncrementValue() == null
                            ? config.getInitialIncrementValue() : context.getCurrentIncrementValue();
                    statement.setObject(1, start, fieldTypes.get(config.getIncrementField()));
                }
                try (ResultSet rows = statement.executeQuery()) {
                    ResultSetMetaData metadata = rows.getMetaData();
                    long position = 0;
                    while (!context.reachedLimit() && rows.next()) {
                        context.emit(readRow(rows, metadata), config.getSourceObject() + " 第" + (++position) + "条", consumer);
                    }
                }
            } finally {
                if (transactionCursor) {
                    // 该事务仅用于 PostgreSQL 游标读取，结束后回滚以释放事务资源。
                    connection.rollback();
                }
            }
            return fields;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("数据库读取失败，请检查连接、数据对象和增量配置");
            failure.initCause(ex);
            throw failure;
        } finally {
            query.close();
        }
    }

    /**
     * 校验表或视图存在性，获取来源列名及 JDBC 类型，供字段校验与增量参数绑定使用。
     */
    private Map<String, Integer> getFieldTypes(Connection connection, IngestTaskDbConfigVO config) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String quote = metadata.getIdentifierQuoteString().trim();
        quoteIdentifier(config.getSourceObject(), quote);
        String catalog = connection.getCatalog();
        String schema = config.getSourceSchema();
        if (schema != null && !schema.trim().isEmpty()) {
            quoteIdentifier(schema, quote);
            if (metadata.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql")) {
                // MySQL 的库名通过 catalog 查询，不能直接作为 JDBC schema 条件。
                catalog = schema;
                schema = null;
            }
        } else {
            schema = connection.getSchema();
        }
        String escape = metadata.getSearchStringEscape();
        String pattern = escapePattern(config.getSourceObject(), escape);
        String schemaPattern = schema == null ? null : escapePattern(schema, escape);
        boolean exists = false;
        try (ResultSet tables = metadata.getTables(catalog, schemaPattern, pattern, new String[]{"TABLE", "VIEW"})) {
            while (tables.next()) {
                if (config.getSourceObject().equals(tables.getString("TABLE_NAME"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            throw new ServiceException("来源表或视图不存在");
        }
        Map<String, Integer> fields = new LinkedHashMap<>();
        try (ResultSet columns = metadata.getColumns(catalog, schemaPattern, pattern, null)) {
            while (columns.next()) {
                fields.put(columns.getString("COLUMN_NAME"), columns.getInt("DATA_TYPE"));
            }
        }
        if (fields.isEmpty()) {
            throw new ServiceException("来源对象没有可读取字段");
        }
        return fields;
    }

    /**
     * 元数据查询使用模式匹配，转义名称中的通配符，避免下划线匹配到其他表。
     */
    private String escapePattern(String name, String escape) {
        if (escape == null || escape.isEmpty()) {
            return name;
        }
        return name.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
    }

    /**
     * 仅接受单段标识符，并使用驱动提供的引号；模式名和表名分别处理。
     */
    private String quoteIdentifier(String text, String quote) {
        if (text == null || !text.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
            throw new ServiceException("数据对象或字段名称无效");
        }
        return quote + text + quote;
    }

    /**
     * 首次包含配置起点，后续排除已完整处理的增量位置；相同增量值在同一轮全部读取。
     */
    private String buildSelectSql(Connection connection, IngestTaskSaveReqVO task,
                                  Set<String> fields, boolean incremental, boolean resumed) throws SQLException {
        IngestTaskDbConfigVO config = task.getDbConfig();
        String quote = connection.getMetaData().getIdentifierQuoteString().trim();
        String table = quoteIdentifier(config.getSourceObject(), quote);
        if (config.getSourceSchema() != null && !config.getSourceSchema().trim().isEmpty()) {
            table = quoteIdentifier(config.getSourceSchema(), quote) + "." + table;
        }
        String sql = "SELECT " + fields.stream().map(field -> quoteIdentifier(field, quote))
                .collect(Collectors.joining(", ")) + " FROM " + table;
        Set<String> order = new LinkedHashSet<>();
        if (incremental) {
            if (!fields.contains(config.getIncrementField()) || config.getInitialIncrementValue() == null) {
                throw new ServiceException("增量字段或首次起始值无效");
            }
            sql += " WHERE " + quoteIdentifier(config.getIncrementField(), quote) + (resumed ? " > ?" : " >= ?");
            order.add(config.getIncrementField());
        }
        if (task.getUniqueFields() != null) {
            order.addAll(task.getUniqueFields());
        }
        if (!order.isEmpty()) {
            sql += " ORDER BY " + order.stream().map(field -> quoteIdentifier(field, quote))
                    .collect(Collectors.joining(", "));
        }
        return sql;
    }

    /**
     * 保留 JDBC 返回值类型；CLOB 在连接关闭前转为字符串并释放底层资源。
     */
    private Map<String, Object> readRow(ResultSet rows, ResultSetMetaData metadata) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            Object value = rows.getObject(i);
            if (value instanceof Clob) {
                Clob clob = (Clob) value;
                try {
                    if (clob.length() > Integer.MAX_VALUE) {
                        throw new ServiceException("来源CLOB字段过大");
                    }
                    value = clob.getSubString(1, (int) clob.length());
                } finally {
                    clob.free();
                }
            }
            row.put(metadata.getColumnLabel(i), value);
        }
        return row;
    }
}
