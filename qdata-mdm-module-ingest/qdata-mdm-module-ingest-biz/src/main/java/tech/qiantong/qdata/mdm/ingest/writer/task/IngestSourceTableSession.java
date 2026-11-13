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

package tech.qiantong.qdata.mdm.ingest.writer.task;

import lombok.extern.slf4j.Slf4j;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.ingest.utils.task.IngestTaskExecutionUtils;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.GOVERNANCE_PENDING;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 来源实体表按来源记录唯一标识追加业务值、融合状态及采集版本字段。
 * 单条记录使用目标库本地事务，失败回滚，不依赖额外来源索引表或版本表。
 * 会话独占一条连接供一次任务顺序写入，不可跨线程共享；写入及提交使用同一连接。
 */
@Slf4j
public class IngestSourceTableSession implements AutoCloseable {
    private static final List<String> EXTENSION_COLUMNS = Arrays.asList("source_system_id", "access_task_id",
            "batch_id", "source_record_id", "collection_time", "del_flag");
    private static final int QUERY_TIMEOUT_SECONDS = 120;

    private final DbQuery query;
    private final Connection connection;
    private final String tableName;
    private final String schemaName;
    private final DbDialect dialect;
    private final List<String> mappedColumns;
    /**
     * 小写逻辑列名到已按方言引用的真实列名，兼容驱动返回的大写列名。
     */
    private final Map<String, String> physicalColumns = new LinkedHashMap<>();
    /**
     * 实际 JDBC 类型用于绑定参数，避免依赖数据库隐式转换。
     */
    private final Map<String, Integer> columnTypes = new LinkedHashMap<>();
    private String insertSql;

    private IngestSourceTableSession(DbQuery query, Connection connection, DbDialect dialect, String schemaName, String tableName, List<String> columns) {
        this.query = query;
        this.connection = connection;
        this.tableName = tableName;
        this.schemaName = schemaName;
        this.dialect = dialect;
        this.mappedColumns = columns.stream().filter(column -> !"id".equalsIgnoreCase(column) && !"status".equalsIgnoreCase(column))
                .collect(Collectors.toList());
    }

    /**
     * 建立运行级会话并关闭自动提交，由调用方逐行提交或回滚；初始化失败立即释放连接和查询器。
     *
     * @param query 本次运行独占的公共数据源查询器，成功后由会话负责关闭
     * @param dialect 与目标数据源匹配的数据库方言
     * @param schemaName 模型发布的目标模式，可为空；与表名分段校验并引用
     * @param tableName 模型生成的来源表名，不包含模式前缀
     * @param columns 模型业务字段，id 和 status 由接入逻辑管理
     * @return 已校验物理列和写入约束的会话
     */
    static IngestSourceTableSession open(DbQuery query, DbDialect dialect, String schemaName, String tableName, List<String> columns) {
        Connection connection = null;
        try {
            connection = query.getConnection();
            IngestSourceTableSession session = new IngestSourceTableSession(query, connection, dialect, schemaName, tableName, columns);
            session.prepare();
            connection.setAutoCommit(false);
            return session;
        } catch (Exception ex) {
            closeResources(query, connection);
            if (ex instanceof ServiceException) {
                throw (ServiceException) ex;
            }
            throw failure("来源表初始化失败，请检查目标数据连接和实体表", ex);
        }
    }

    /**
     * 校验实际列名及扩展字段，业务列按模型编码忽略大小写匹配；SQL 标识符和锁查询使用公共方言。
     */
    private void prepare() throws SQLException {
        String table = quoteIdentifier(tableName);
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            table = quoteIdentifier(schemaName) + "." + table;
        }
        /*
         * 零行查询读取真实列信息，不依赖发布快照是否列出全部采集字段。
         */
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE 1=0")) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rows = statement.executeQuery()) {
                ResultSetMetaData metadata = rows.getMetaData();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    String column = metadata.getColumnName(i);
                    String key = column.toLowerCase(Locale.ROOT);
                    physicalColumns.put(key, quoteIdentifier(column));
                    columnTypes.put(key, metadata.getColumnType(i));
                }
            }
        }
        if (mappedColumns.isEmpty()) {
            throw new ServiceException("来源表没有可写入的映射字段");
        }
        Set<String> selected = new HashSet<>();
        for (String column : mappedColumns) {
            quoteIdentifier(column);
            String key = column.toLowerCase(Locale.ROOT);
            if (EXTENSION_COLUMNS.contains(key)) {
                throw new ServiceException("采集扩展字段由系统生成，不能配置业务映射：" + column);
            }
            if (!selected.add(key)) {
                throw new ServiceException("来源表映射字段重复：" + column);
            }
            requireColumn(column);
        }
        EXTENSION_COLUMNS.forEach(this::requireColumn);
        requireColumn("status");
        List<String> columns = new ArrayList<>(mappedColumns);
        columns.add("status");
        columns.addAll(EXTENSION_COLUMNS);
        insertSql = "INSERT INTO " + table + " (" + columns.stream().map(this::physicalColumn)
                .collect(Collectors.joining(",")) + ") VALUES ("
                + String.join(",", Collections.nCopies(columns.size(), "?")) + ")";
        validateConstraints(columns);
    }

    /**
     * 为每次成功采集追加一个来源数据版本，不在本方法中提交。
     * 最终失败交由执行服务回滚；调用方 commitRow 成功后才能计入成功数。
     */
    public void writeRecord(IngestTaskRespVO task, IngestTaskInstanceDO instance,
                            Map<String, Object> identityValues, Map<String, Object> modelData) {
        String recordKey = IngestTaskExecutionUtils.stableRecordId(identityValues);
        Object batch = numericColumn("batch_id") ? instance.getId() : instance.getBatchNo();
        insertVersion(modelData, task.getSourceId(), recordKey, task.getId(), batch,
                new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 每个版本的主键由数据库自增生成，并置为待融合，便于按批次追溯本次采集值。
     */
    private void insertVersion(Map<String, Object> data, Long sourceId, Object recordKey,
                               Long taskId, Object batch, Timestamp receivedAt) {
        /*
         * 保持与 prepare 中占位符一致的列顺序，避免业务值与采集字段错位。
         */
        Map<String, Object> values = new LinkedHashMap<>();
        for (String column : mappedColumns) {
            values.put(column, data.get(column));
        }
        values.put("status", GOVERNANCE_PENDING);
        values.put("source_system_id", sourceId);
        values.put("access_task_id", taskId);
        values.put("batch_id", batch);
        values.put("source_record_id", recordKey);
        values.put("collection_time", receivedAt);
        values.put("del_flag", activeFlag());
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            int index = 1;
            for (Map.Entry<String, Object> value : values.entrySet()) {
                bindValue(statement, index++, value.getKey(), value.getValue());
            }
            int affected = statement.executeUpdate();
            if (affected != 1) {
                throw new ServiceException("来源表写入影响行数不为1，本条写入已终止");
            }
        } catch (SQLException ex) {
            throw sqlFailure(ex);
        }
    }

    /**
     * 按物理列类型绑定参数；日期字符串先转为 JDBC 日期或时间戳，不将值拼入 SQL。
     */
    private void bindValue(PreparedStatement statement, int index, String column, Object value) throws SQLException {
        int type = columnTypes.get(column.toLowerCase(Locale.ROOT));
        if (value instanceof String && type == Types.DATE) {
            value = java.sql.Date.valueOf((String) value);
        } else if (value instanceof String && type == Types.TIMESTAMP) {
            String text = ((String) value).replace(' ', 'T');
            // 模型 DATE 可能发布为物理 TIMESTAMP 列，仅在此处将纯日期转为当天零点。
            LocalDateTime dateTime = text.length() == 10
                    ? LocalDate.parse(text).atStartOfDay() : LocalDateTime.parse(text);
            value = Timestamp.valueOf(dateTime);
        }
        statement.setObject(index, value, type);
    }

    /**
     * 按 del_flag 实际类型使用 false、数字0或字符串0表示正常。
     */
    private Object activeFlag() {
        int type = columnTypes.get("del_flag");
        if (type == Types.BOOLEAN || type == Types.BIT) return false;
        return numericColumn("del_flag") ? 0 : "0";
    }

    /**
     * 识别整数或精确数值列，用于批次号和逻辑删除标识的值类型选择。
     */
    private boolean numericColumn(String column) {
        int type = columnTypes.get(column);
        return type == Types.BIGINT || type == Types.INTEGER || type == Types.SMALLINT || type == Types.TINYINT
                || type == Types.NUMERIC || type == Types.DECIMAL;
    }

    /**
     * 提交目标实体行；调用方提交成功后才增加运行成功数。
     */
    public void commitRow() {
        try {
            connection.commit();
        } catch (SQLException ex) {
            throw sqlFailure(ex);
        }
    }

    /**
     * 单行任一阶段失败时释放目标锁并回滚暂存写入，不让半条记录进入下一行。
     */
    public void rollbackRow() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            throw failure("来源表事务回滚失败，必须终止本次运行", ex);
        }
    }

    /**
     * 仅检查未提供值的必填列；不要求改动既有主键、索引或业务列类型。
     */
    private void validateConstraints(List<String> writtenColumns) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = schemaName;
        if ((schema == null || schema.trim().isEmpty()) && metadata.supportsSchemasInTableDefinitions()) {
            try {
                schema = connection.getSchema();
            } catch (SQLFeatureNotSupportedException | AbstractMethodError ignored) {
                /*
                 * 老版本 JDBC 驱动不支持 getSchema，元数据查询使用连接默认位置。
                 */
            }
        }
        /*
         * 以 catalog 表示数据库的驱动使用发布目标限定元数据位置。
         */
        if (!metadata.supportsSchemasInTableDefinitions()) {
            if (schema != null && !schema.trim().isEmpty() && metadata.supportsCatalogsInTableDefinitions()) {
                catalog = schema;
            }
            schema = null;
        }
        /*
         * getColumns 接收的是表名匹配模式，需转义真实名称中的下划线和百分号。
         */
        String escape = metadata.getSearchStringEscape();
        if (escape == null) escape = "";
        String pattern = tableName.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
        Set<String> written = writtenColumns.stream().map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        try (ResultSet columns = metadata.getColumns(catalog, schema, pattern, null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (!written.contains(name.toLowerCase(Locale.ROOT))
                        && columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls
                        && columns.getString("COLUMN_DEF") == null
                        && !"YES".equalsIgnoreCase(columns.getString("IS_AUTOINCREMENT"))) {
                    throw new ServiceException("来源表字段「" + name + "」必填且无默认值，请核对字段映射");
                }
            }
        }
    }

    /**
     * 仅回显数据库错误码和安全的字段名称，不把驱动消息中的业务值、连接信息带到页面。
     */
    private ServiceException sqlFailure(SQLException ex) {
        String message = "来源表 " + tableName + " 写入失败（SQLState=" + ex.getSQLState()
                + "，错误码=" + ex.getErrorCode() + "）";
        java.util.regex.Matcher field = java.util.regex.Pattern
                .compile("(?i)(?:Field|Column|column) ['`]([\\p{L}\\p{N}_]+)['`]")
                .matcher(ex.getMessage() == null ? "" : ex.getMessage());
        if (field.find()) {
            message += "，字段「" + field.group(1) + "」不符合表约束";
        }
        return failure(message, ex);
    }

    /**
     * 必需字段以物理表为准，缺失时在执行写入前终止。
     */
    private void requireColumn(String column) {
        if (!physicalColumns.containsKey(column.toLowerCase(Locale.ROOT))) {
            throw new ServiceException("来源表 " + tableName + " 缺少字段：" + column);
        }
    }

    /**
     * 取初始化时已经校验和引用的真实列名，供 SQL 组装使用。
     */
    private String physicalColumn(String column) {
        return physicalColumns.get(column.toLowerCase(Locale.ROOT));
    }

    /**
     * 名称只接受单段标识符；业务值始终通过预编译参数绑定。
     */
    private String quoteIdentifier(String name) {
        if (name == null || !name.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
            throw new ServiceException("来源表或字段名称无效：" + name);
        }
        return dialect.quoteIdentifier(name);
    }

    /**
     * 页面使用业务提示，原始异常保留为 cause 便于服务端定位。
     */
    private static ServiceException failure(String message, Exception cause) {
        ServiceException failure = new ServiceException(message);
        failure.initCause(cause);
        return failure;
    }

    /**
     * 关闭运行级连接与连接池；关闭失败仅记录服务日志，不覆盖原来的执行结果。
     */
    @Override
    public void close() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            log.warn("关闭来源表会话前回滚失败", ex);
        } finally {
            closeResources(query, connection);
        }
    }

    /**
     * 先关闭本次连接，再释放查询器资源；清理异常仅记录，不覆盖主异常。
     */
    private static void closeResources(DbQuery query, Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                log.warn("关闭来源表写入连接失败", ex);
            }
        }
        try {
            query.close();
        } catch (Exception ex) {
            log.warn("关闭来源表写入连接池失败", ex);
        }
    }
}
