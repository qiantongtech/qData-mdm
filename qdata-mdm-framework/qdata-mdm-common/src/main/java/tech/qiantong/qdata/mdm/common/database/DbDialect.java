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

package tech.qiantong.qdata.mdm.common.database;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbIndex;
import tech.qiantong.qdata.mdm.common.database.core.DbName;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Table data query interface
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
public interface DbDialect {
    /**
     * 将平台逻辑字段转成当前方言建表、加列可接收的独立副本，不修改原对象。
     */
    DbColumn convertLogicalColumn(DbColumn column);

    /** 驱动ResultSetMetaData未识别原生自增列时，从数据库目录补充读取；只返回自增列。 */
    default java.util.Map<String, Boolean> readIdentityColumns(java.sql.Connection connection,
            String schemaName, String tableName) throws java.sql.SQLException {
        return java.util.Collections.emptyMap();
    }

    /** 显式写入自增列是否需要会话开关。 */
    default boolean requiresIdentityInsert() { return false; }

    /** qualifiedTable由方言引用后传入，必须来自经过校验的SQL模板。 */
    default String identityInsertSql(String qualifiedTable, boolean enabled) {
        throw new tech.qiantong.qdata.mdm.common.exception.ServiceException("当前数据库不需要自增列显式写入开关");
    }

    /**
     * 生成普通或唯一索引SQL；主键继续由原建表方法处理。
     */
    String generateCreateIndexSQL(DbQueryProperty property, String tableName, DbIndex index);

    /**
     * 只生成撤销索引SQL，是否允许撤销由业务调用方判断。
     */
    String generateDropIndexSQL(DbQueryProperty property, String tableName, String indexName);

    /**
     * 根据写入端属性生成 INSERT OR UPDATE SQL（用于跨库写入场景）
     *
     * @param writerProperty  写入端数据库属性
     * @param tableName       表名
     * @param selectedColumns 用于匹配（ON 条件）的列
     * @param column          所有列
     * @return SQL 模板，通过 String.format(sql, tableName) 格式化后使用
     */
    default String getInsertOrUpdateSql(DbQueryProperty writerProperty, String tableName, List<String> selectedColumns, List<String> column) {
        tableName = getTableName(writerProperty, tableName);
        List<String> valueHolders = new ArrayList<>(column.size());
        for (int i = 0; i < column.size(); i++) {
            valueHolders.add("?");
        }
        List<String> quotedColumns = new ArrayList<>(column.size());
        for (String col : column) {
            quotedColumns.add(quoteIdentifier(col));
        }
        String writeDataSqlTemplate = new StringBuilder()
                .append(buildMergeIntoFromDual(selectedColumns, column))
                .append("INSERT (")
                .append(StringUtils.join(quotedColumns, ","))
                .append(") VALUES(")
                .append(StringUtils.join(valueHolders, ","))
                .append(")")
                .toString();
        return String.format(writeDataSqlTemplate, tableName);
    }

    /**
     * 构建 MERGE INTO ... USING (SELECT ... FROM DUAL) TMP ON ... 前缀
     * 供需要标准 MERGE 语法的方言复用
     */
    default String buildMergeIntoFromDual(List<String> selectedColumns, List<String> column) {
        StringBuilder sb = new StringBuilder();
        sb.append("MERGE INTO %s A USING ( SELECT ");

        boolean first = true;
        boolean first1 = true;
        StringBuilder str = new StringBuilder();
        StringBuilder update = new StringBuilder();
        for (String columnHolder : column) {
            if (selectedColumns.contains(columnHolder)) {
                if (!first) {
                    sb.append(",");
                    str.append(" AND ");
                } else {
                    first = false;
                }
                String quoted = quoteIdentifier(columnHolder);
                str.append("TMP.").append(quoted);
                sb.append("?");
                str.append(" = ");
                sb.append(" AS ");
                str.append("A.").append(quoted);
                sb.append(quoted);
            }
        }

        for (String columnHolder : column) {
            if (!selectedColumns.contains(columnHolder)) {
                if (!first1) {
                    update.append(",");
                } else {
                    first1 = false;
                }
                update.append(quoteIdentifier(columnHolder));
                update.append(" = ");
                update.append("?");
            }
        }

        sb.append(" FROM DUAL ) TMP ON (");
        sb.append(str);
        sb.append(" ) WHEN MATCHED THEN UPDATE SET ");
        sb.append(update);
        sb.append(" WHEN NOT MATCHED THEN ");
        return sb.toString();
    }

    /**
     * 自增声明由方言生成；未启用自增时返回空串。
     */
    default String identityColumnClause(DbColumn column) {
        if (Boolean.TRUE.equals(column.getIdentityFlag())) {
            throw new tech.qiantong.qdata.mdm.common.exception.ServiceException("当前目标数据库不支持自增列");
        }
        return "";
    }

    RowMapper<DbTable> tableMapper();

    RowMapper<DbColumn> columnMapper();

    /**
     * 将当前方言的原生字段类型转换为平台标准字段类型.
     */
    DbColumn toStandardColumn(DbColumn column);

    /**
     * Get all columns of the specified table
     *
     * @param dbName
     * @param tableName
     * @return
     */
    String columns(String dbName, String tableName);

    String columns(DbQueryProperty dbQueryProperty, String tableName);

    /**
     * Get all columns under the specified database
     *
     * @param dbQueryProperty
     * @return
     */
    String getDbColumns(DbQueryProperty dbQueryProperty);

    String getPkColumnNames(DbQueryProperty dbQueryProperty, String tableName);

    String getPkColumnNames(DbQueryProperty dbQueryProperty);

    String generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName);

    List<String> someInternalSqlGenerator(DbQueryProperty dbQueryProperty, String tableName, String tableComment, List<DbColumn> dbColumnList);

    /**
     * Validate whether the table and column information conforms to DM8 specification requirements.
     *
     * @param tableName    Table name.
     * @param tableComment Table comment.
     * @param columns      Column definition list.
     * @return Returns a list of error messages. If the list is empty, all inputs conform to the specification.
     */
    List<String> validateSpecification(String tableName, String tableComment, List<DbColumn> columns);

    /**
     * Get all tables in the database
     *
     * @param dbName
     * @return
     */
    String tables(String dbName);

    String tables(DbQueryProperty dbQueryProperty);

    String buildTableNameByDbType(DbQueryProperty dbQueryProperty, String tableName);

    /**
     * @param columns
     * @param tableName
     * @param dbQueryProperty
     * @return
     */
    String buildQuerySqlFields(List<DbColumn> columns, String tableName, DbQueryProperty dbQueryProperty);

    /**
     * Build pagination SQL
     *
     * @param sql
     * @param offset
     * @param count
     * @return
     */
    String buildPaginationSql(String sql, long offset, long count);

    /**
     * Wrap count SQL
     *
     * @param sql
     * @return
     */
    String count(String sql);

    default String buildSelectForUpdateSql(String table, String columns, String predicate) {
        return "SELECT " + columns + " FROM " + table + " WHERE " + predicate + " FOR UPDATE";
    }

    /**
     * 将可信列引用转成 LIKE 表达式；默认文本列直接使用，其他类型委托具体方言转换。
     * jdbcType 来自物理表元数据；转换仅用于查询，不修改存储类型或数据。
     */
    default String textSearchExpression(String column, int jdbcType) {
        switch (jdbcType) {
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.NCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
            case java.sql.Types.CLOB:
            case java.sql.Types.NCLOB:
                return column;
            default:
                return castToText(column);
        }
    }

    /**
     * 转换可信 SQL 表达式为文本；未适配的方言明确失败，不使用可能不兼容的默认语法。
     */
    default String castToText(String expression) {
        throw new UnsupportedOperationException("当前数据库尚未适配文本转换");
    }

    /**
     * 生成包含匹配参数，将输入的 !、% 和 _ 作为普通字符处理；特殊通配符由具体方言补充。
     * 调用方必须使用 LIKE ? ESCAPE '!' 并绑定返回值，不直接拼入 SQL。
     */
    default String containsPattern(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    String countNew(String tableName, Map<String, Object> params);

    /**
     * Oracle has a bug where reading long type causes stream closure, requiring special handling
     *
     * @return
     */
    default RowMapper<DbColumn> columnLongMapper() {
        return null;
    }

    /**
     * Get storage size
     *
     * @return
     */
    String getDataStorageSize(String dbName);

    /**
     * Get database name or schema name
     *
     * @return
     */
    String getDbName();

    /**
     * Get database name or schema name
     *
     * @param dbName
     * @return
     */
    String getDbName(DbName dbName);

    /**
     * First-level RowMapper:
     * Maps DBNAME/TOTALLEVELS to DbName
     */
    default RowMapper<DbName> firstLevelMapper(int level) {
        return (ResultSet rs, int rowNum) -> DbName.builder()
                .dbName(rs.getString("DBNAME"))
                .level(level)
                .totalLevels(rs.getInt("TOTALLEVELS"))
                .children(null)
                .build();
    }

//    String getDatabasePhysicalInfo(DbName dbName);

    /**
     * Validate connection
     *
     * @param dataSource
     * @param dbQueryProperty
     * @return
     */
    Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty);

    String getInsertOrUpdateSql(String tableName, String where, String tableFieldName, String tableFieldValue, String setValue);

    String getFlinkSQL(DbQueryProperty property, String flinkTableName, String tableName, String tableFieldName);

    String getFlinkCDCSQL(DbQueryProperty property, String flinkTableName, String tableName, String tableFieldName);

    String getTableName(DbQueryProperty property, String tableName);

    String getFlinkSinkSQL(DbQueryProperty dbQueryProperty, JSONObject config, String flinkTableName, String tableName, String tableFieldName);

    /**
     * Convert DbQueryProperty to jdbcUrl
     *
     * @param property
     * @return
     */
    String trainToJdbcUrl(DbQueryProperty property);

    /**
     * Get table information SQL
     *
     * @param dbQueryProperty
     * @param tableName
     * @return
     */
    String table(DbQueryProperty dbQueryProperty, String tableName);

    /**
     * Get SQL for updating comments
     *
     * @param dbQueryProperty
     * @param tableName
     * @param tableComment
     * @return
     */
    String updateTableComment(DbQueryProperty dbQueryProperty, String tableName, String tableComment);

    /**
     * Modify column
     *
     * @param dbQueryProperty
     * @param tableName
     * @param column
     * @return
     */
    List<String> modifyColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column);

    /**
     * Add column
     *
     * @param dbQueryProperty
     * @param tableName
     * @param column
     * @return
     */
    List<String> addColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column);

    /**
     * Drop column
     *
     * @param dbQueryProperty
     * @param tableName
     * @param colName
     * @return
     */
    String dropColumn(DbQueryProperty dbQueryProperty, String tableName, String colName);

    /**
     * Update primary key
     *
     * @param dbQueryProperty
     * @param tableName
     * @param colKeyDbColumnList
     * @return
     */
    List<String> updateColKey(DbQueryProperty dbQueryProperty, String tableName, List<DbColumn> colKeyDbColumnList);

    /**
     * Get column type
     *
     * @param column
     * @return
     */
    String getColumnType(DbColumn column);
    /**
     * 仅变更已有列的静态DEFAULT，不更新历史行；多条SQL必须由调用方逐步记录和核验。
     */
    default List<String> alterColumnDefault(DbQueryProperty property, String table, DbColumn logicalColumn,
                                            java.sql.Connection connection) throws java.sql.SQLException {
        return tech.qiantong.qdata.mdm.common.database.dialect.ColumnDefaultSql.generate(
                this, property, table, logicalColumn, connection);
    }

    /**
     * 当前连接配置中的表命名空间（MySQL等数据库返回库名），不包含标识符引号。
     * Oracle的sid是连接服务，不能作为Schema；独立Schema的数据库由各自方言解析。
     */
    default String getSchemaName(DbQueryProperty property) {
        return property.getDbName();
    }

    /**
     * 生成删表 SQL
     */
    String generateDropTableSQL(DbQueryProperty dbQueryProperty, String tableName);

    /**
     * 根据当前真实连接解析建表后的物理类型；无兼容模式差异的方言保持原行为。
     */
    default String resolveColumnType(DbColumn column, java.sql.Connection connection) throws java.sql.SQLException {
        return getColumnType(column);
    }

    /**
     * 类型别名交给方言统一；字段长度、数字精度和可空约束仍由调用方独立核验。
     */
    default boolean matchesColumnType(DbColumn expected, DbColumn actual) {
        String left = toStandardColumn(expected).getDataType();
        String right = toStandardColumn(actual).getDataType();
        return (left == null ? "" : left.replaceAll("\\(.*", "").trim())
                .equalsIgnoreCase(right == null ? "" : right.replaceAll("\\(.*", "").trim());
    }

    /**
     * 结合真实目录读取可空约束，默认保持 JDBC 行元数据行为。
     */
    default Boolean observedColumnNullable(DbColumn jdbcColumn, DbColumn catalogColumn) {
        return jdbcColumn.getNullable();
    }

    /**
     * 将已读到的真实元数据表示为可保存的物理类型，不使用模型目标代替实际值。
     */
    default String observedColumnType(DbColumn column) {
        return column.getDataType();
    }

    /**
     * Quote one identifier using the database dialect's delimiters.
     */
    /** 去除字段表达式首尾空格，由具体数据库方言适配。 */
    default String trimExpression(String expression) {
        return "TRIM(" + expression + ")";
    }

    /** Quote one identifier using the database dialect's delimiters. */
    default String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
