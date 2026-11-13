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

package tech.qiantong.qdata.mdm.common.database.dialect;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbName;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import tech.qiantong.qdata.mdm.common.database.exception.DataQueryException;
import tech.qiantong.qdata.mdm.common.database.utils.DatabaseUtil;
import tech.qiantong.qdata.mdm.common.utils.MessageUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQLServer 2005 database dialect
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
@Slf4j
public class SQLServer2008Dialect extends AbstractDbDialect {
    private static final long MAX_VARCHAR_LENGTH = 8000L;

    /** 兼容不支持 TRIM 的 SQL Server 版本以及旧式 TEXT/NTEXT 字段。 */
    @Override
    public String trimExpression(String expression) {
        return "LTRIM(RTRIM(CAST(" + expression + " AS NVARCHAR(MAX))))";
    }

    @Override
    public String columns(String dbName, String tableName) {
        return "select columns.name AS colName, columns.column_id AS COLPOSITION, columns.max_length AS DATALENGTH, columns.precision AS DATAPRECISION, columns.scale AS DATASCALE, " +
                "columns.is_nullable AS NULLABLE, types.name AS DATATYPE, CAST(ep.value  AS NVARCHAR(128)) AS COLCOMMENT, (CASE WHEN e.text LIKE '((%)' AND e.text NOT LIKE '%)%''%' THEN SUBSTRING(e.text, 3, LEN(e.text) - 4) WHEN e.text LIKE '(%' THEN SUBSTRING(e.text, 2, LEN(e.text) - 2) ELSE e.text END) AS DATADEFAULT, " +
                "(CASE WHEN (SELECT ic.column_id FROM sys.indexes idx INNER JOIN sys.index_columns ic ON idx.object_id = ic.object_id AND idx.index_id = ic.index_id WHERE idx.is_primary_key = 1 AND columns.column_id = ic.column_id AND columns.object_id = ic.object_id)  IS NOT NULL THEN '1' ELSE '0' END) AS COLKEY " +
                "from sys.tables tables " +
                "JOIN sys.columns columns ON tables.object_id = columns.object_id " +
                "LEFT JOIN sys.types types ON columns.user_type_id = types.user_type_id " +
                "LEFT JOIN syscomments e ON columns.default_object_id= e.id " +
                "LEFT JOIN sys.extended_properties ep ON ep.major_id = columns.object_id AND ep.minor_id = columns.column_id AND ep.name = 'MS_Description' " +
                "where tables.name = '" + tableName + "' " +
                "order by columns.column_id ";
    }

    @Override
    public String columns(DbQueryProperty dbQueryProperty, String tableName) {
        return "select columns.name AS colName, columns.column_id AS COLPOSITION, columns.max_length AS DATALENGTH, columns.precision AS DATAPRECISION, columns.scale AS DATASCALE, " +
                "columns.is_nullable AS NULLABLE, types.name AS DATATYPE, CAST(ep.value  AS NVARCHAR(128)) AS COLCOMMENT, (CASE WHEN e.text LIKE '((%)' AND e.text NOT LIKE '%)%''%' THEN SUBSTRING(e.text, 3, LEN(e.text) - 4) WHEN e.text LIKE '(%' THEN SUBSTRING(e.text, 2, LEN(e.text) - 2) ELSE e.text END) AS DATADEFAULT, " +
                "(CASE WHEN (SELECT ic.column_id FROM sys.indexes idx INNER JOIN sys.index_columns ic ON idx.object_id = ic.object_id AND idx.index_id = ic.index_id WHERE idx.is_primary_key = 1 AND columns.column_id = ic.column_id AND columns.object_id = ic.object_id)  IS NOT NULL THEN '1' ELSE '0' END) AS COLKEY " +
                "from sys.tables tables " +
                "JOIN sys.columns columns ON tables.object_id = columns.object_id " +
                "LEFT JOIN sys.types types ON columns.user_type_id = types.user_type_id " +
                "LEFT JOIN syscomments e ON columns.default_object_id= e.id " +
                "LEFT JOIN sys.extended_properties ep ON ep.major_id = columns.object_id AND ep.minor_id = columns.column_id AND ep.name = 'MS_Description' " +
                "where tables.name = '" + tableName + "' " +
                "AND SCHEMA_NAME(tables.schema_id) = '" + DatabaseUtil.escapeSingleQuotes(getSchemaName(dbQueryProperty)) + "' " +
                "order by columns.column_id ";
    }

    @Override
    public String generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName) {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_CATALOG = '" + dbQueryProperty.getDbName() + "' AND TABLE_SCHEMA = '" + DatabaseUtil.escapeSingleQuotes(getSchemaName(dbQueryProperty)) + "' AND TABLE_NAME = '" + tableName + "';";
    }

    @Override
    public List<String> someInternalSqlGenerator(DbQueryProperty dbQueryProperty, String tableName, String tableComment, List<DbColumn> dbColumnList) {
        List<String> sqlList = new ArrayList<>();
        String fullTableName = getTableName(dbQueryProperty, tableName);

        List<String> primaryKeys = new ArrayList<>();
        {
            StringBuilder sql = new StringBuilder();
            // 生成CREATE TABLE语句
            sql.append("CREATE TABLE ").append(fullTableName).append(" (\n");

            for (DbColumn column : dbColumnList) {
                String columnType = column.getDataType().toUpperCase();
                sql.append("  ").append(quoteIdentifier(column.getColName())).append(" ");

                // 转换数据类型为SQL Server支持的类型
                switch (columnType) {
                    case "NVARCHAR":
                    case "NVARCHAR(MAX)":
                        sql.append(unicodeColumnType(column));
                        break;
                    case "VARCHAR":
                    case "VARCHAR2": // SQL Server不支持VARCHAR2，映射为VARCHAR
                        sql.append("VARCHAR");
                        if (StringUtils.isNotEmpty(column.getDataLength())) {
                            sql.append("(").append(column.getDataLength()).append(")");
                        } else {
                            sql.append("(MAX)"); // SQL Server中的VARCHAR默认支持最大长度
                        }
                        break;
                    case "CHAR":
                        sql.append("CHAR");
                        if (StringUtils.isNotEmpty(column.getDataLength())) {
                            sql.append("(").append(column.getDataLength()).append(")");
                        }
                        break;
                    case "TEXT":
                        sql.append("TEXT");
                        break;
                    case "INT":
                    case "INTEGER":
                        sql.append("INT");
                        break;
                    case "BIGINT":
                        sql.append("BIGINT");
                        break;
                    case "TINYINT":
                        sql.append("SMALLINT");
                        break;
                    case "DECIMAL":
                        sql.append("DECIMAL");
                        if (StringUtils.isNotEmpty(column.getDataLength())) {
                            sql.append("(").append(column.getDataLength());
                            if (StringUtils.isNotEmpty(column.getDataScale())) {
                                sql.append(", ").append(column.getDataScale());
                            }
                            sql.append(")");
                        }
                        break;
                    case "FLOAT":
                        sql.append("FLOAT");
                        break;
                    case "DOUBLE":
                        sql.append("FLOAT"); // SQL Server中没有DOUBLE，使用FLOAT
                        break;
                    case "DATE":
                        sql.append("DATE");
                        break;
                    case "DATETIME":
                        sql.append("DATETIME");
                        break;
                    case "TIME":
                        sql.append("TIME");
                        break;
                    default:
                        sql.append(columnType); // 默认处理未知类型
                        break;
                }

                // 检查是否必填
                sql.append(identityColumnClause(column));
                if (!column.getNullable()) {
                    sql.append(" NOT NULL");
                }

                // 默认值处理
                appendSqlServerDefaultClause(sql, columnType, column.getDataDefault());

                // 加入字段到主键列表，如果是主键
                if (column.getColKey()) {
                    primaryKeys.add(column.getColName());
                }

                sql.append(",\n");
            }

            // 移除最后的逗号和换行
            sql.setLength(sql.length() - 2);
            sql.append("\n");

            // 添加主键约束
            if (!primaryKeys.isEmpty()) {
                sql.append(", PRIMARY KEY (");
                for (String pk : primaryKeys) {
                    sql.append(quoteIdentifier(pk)).append(", ");
                }
                sql.setLength(sql.length() - 2); // 移除最后的逗号和空格
                sql.append(")");
            }

            sql.append("\n)\n");
            sqlList.add(sql.toString());
        }

        // 旧驱动分支也要与实际建表Schema保持一致。
        String commentSchema = DatabaseUtil.escapeSingleQuotes(StringUtils.defaultIfBlank(dbQueryProperty.getSid(), "dbo"));
        // 添加表备注（SQL Server不直接支持表备注，但可以使用扩展属性等方式）
        if (StringUtils.isNotEmpty(tableComment)) {
            StringBuilder sql = new StringBuilder();
            sql.append("EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'")
                    .append(DatabaseUtil.escapeSingleQuotes(tableComment))
                    .append("', @level0type = N'SCHEMA', @level0name = N'")
                    .append(commentSchema).append("', @level1type = N'TABLE', @level1name = N'")
                    .append(tableName)
                    .append("'\n");
            sqlList.add(sql.toString());
        }

        // 添加字段备注
        for (DbColumn column : dbColumnList) {
            if (StringUtils.isNotEmpty(column.getColComment())) {
                StringBuilder sql = new StringBuilder();
                sql.append("EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'")
                        .append(DatabaseUtil.escapeSingleQuotes(column.getColComment()))
                        .append("', @level0type = N'SCHEMA', @level0name = N'")
                    .append(commentSchema).append("', @level1type = N'TABLE', @level1name = N'")
                        .append(tableName)
                        .append("', @level2type = N'COLUMN', @level2name = N'")
                        .append(column.getColName())
                        .append("'\n");
                sqlList.add(sql.toString());
            }
        }

        return sqlList;
    }

    // SQL 关键字
    private static final String[] RESERVED_WORDS = {
            "ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE",
            "BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE",
            "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION",
            "CONSTRAINT", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME",
            "CURRENT_TIMESTAMP", "CURRENT_USER", "DATABASE", "DATABASES", "DAY_HOUR",
            "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL", "DEFAULT",
            "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW",
            "DIV", "DOUBLE", "DROP", "DUAL", "ELSE", "ELSEIF", "EXISTS", "EXPLAIN", "FALSE",
            "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FROM", "GROUP", "HAVING", "HIGH_PRIORITY",
            "IF", "IGNORE", "IN", "INDEX", "INNER", "INSERT", "INT", "INT1", "INT2", "INT3",
            "INT4", "INT8", "INTEGER", "INTERVAL", "INTO", "IS", "JOIN", "KEY", "KEYS",
            "LEADING", "LEFT", "LIKE", "LIMIT", "LINES", "LOAD", "LOCK", "LONG", "LONGBLOB",
            "LONGTEXT", "LOW_PRIORITY", "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT",
            "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES",
            "NATURAL", "NOT", "NULL", "NUMERIC", "ON", "OPTIMIZE", "OPTION", "OR", "ORDER",
            "OUTER", "PARTITION", "PRECISION", "PRIMARY", "RANGE", "READ", "REGEXP",
            "RELEASE", "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESTRICT", "RETURN", "RIGHT",
            "RLIKE", "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SET", "SHOW",
            "SMALLINT", "SQL", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT",
            "STARTING", "STORED", "STRAIGHT_JOIN", "TABLE", "TERMINATED", "THEN", "TINYBLOB",
            "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRUE", "UNION", "UNIQUE", "UNLOCK",
            "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME",
            "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING",
            "VIRTUAL", "WHEN", "WHERE", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL"
    };

    public static String escapeReservedKeyword(String colName) {
        if (colName == null || colName.isEmpty()) {
            return colName;
        }
        for (String reserved : RESERVED_WORDS) {
            if (reserved.equalsIgnoreCase(colName)) {
                return "`" + colName + "`";
            }
        }
        return colName;
    }

    @Override
    public List<String> validateSpecification(String tableName, String tableComment, List<DbColumn> columns) {
        return null;
    }

    @Override
    public String tables(String dbName) {
        return "select tables.name AS TABLENAME, CAST(ep.value AS NVARCHAR(128)) AS TABLECOMMENT " +
                "from sys.tables tables LEFT JOIN sys.extended_properties ep ON ep.major_id = tables.object_id AND ep.minor_id = 0";
    }

    @Override
    public String tables(DbQueryProperty dbQueryProperty) {
        return "select tables.name AS TABLENAME, CAST(ep.value AS NVARCHAR(128)) AS TABLECOMMENT " +
                "from sys.tables tables LEFT JOIN sys.extended_properties ep ON ep.major_id = tables.object_id AND ep.minor_id = 0";
    }

    @Override
    public String buildQuerySqlFields(List<DbColumn> columns, String tableName, DbQueryProperty dbQueryProperty) {
        // If no fields are passed in, * will be used by default to query all fields.
        if (columns == null || columns.isEmpty()) {
            return "SELECT * FROM " + tableName;
        }

        // Get all field names based on the passed in DbColumn list, separated by commas
        String fields = columns.stream()
                .map(DbColumn::getColName)
                .collect(Collectors.joining(", "));

        // Construct the final SQL query statement
        return "SELECT " + fields + " FROM " + dbQueryProperty.getDbName() + "." + dbQueryProperty.getSid() + "." + tableName;
    }

    private static String getOrderByPart(String sql) {
        String loweredString = sql.toLowerCase();
        int orderByIndex = loweredString.indexOf("order by");
        if (orderByIndex != -1) {
            return sql.substring(orderByIndex);
        } else {
            return "";
        }
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long count) {
        StringBuilder pagingBuilder = new StringBuilder();
        String orderby = getOrderByPart(originalSql);
        String distinctStr = "";

        String loweredString = originalSql.toLowerCase();
        String sqlPartString = originalSql;
        if (loweredString.trim().startsWith("select")) {
            int index = 6;
            if (loweredString.startsWith("select distinct")) {
                distinctStr = "DISTINCT ";
                index = 15;
            }
            sqlPartString = sqlPartString.substring(index);
        }
        pagingBuilder.append(sqlPartString);

        // if no ORDER BY is specified use fake ORDER BY field to avoid errors
        if (StringUtils.isEmpty(orderby)) {
            orderby = "ORDER BY CURRENT_TIMESTAMP";
        }
        StringBuilder sql = new StringBuilder();
        sql.append("WITH selectTemp AS (SELECT ").append(distinctStr).append("TOP 100 PERCENT ")
                .append(" ROW_NUMBER() OVER (").append(orderby).append(") as __row_number__, ").append(pagingBuilder)
                .append(") SELECT * FROM selectTemp WHERE __row_number__ BETWEEN ")
                //FIX#299: Reason: limit 10 (offset, size) in mysql starts from the 10th (excluding 10); and the BETWEEN used here includes both sides, so it is changed to offset+1
                .append(offset + 1)
                .append(" AND ")
                .append(offset + count).append(" ORDER BY __row_number__");
        return sql.toString();
    }

    @Override
    public String getDataStorageSize(String dbName) {
        return "SELECT SUM(size/128.0) AS \"usedSizeMb\" FROM sys.master_files WHERE DB_NAME(database_id) = '" + dbName + "'";
    }

    @Override
    public String getDbName() {
        return "SELECT DB_NAME() AS \"databaseName\"";
    }

    @Override
    public String getDbName(DbName dbNameVO) {
        int level = dbNameVO == null ? 1 : dbNameVO.getLevel() + 1;

        if (level == 1) {
            // First time: list all databases
            return "SELECT name AS DBNAME, 2 AS TOTALLEVELS " +
                    "FROM sys.databases " +
                    "WHERE name NOT IN ('master','tempdb','model','msdb') " +
                    "ORDER BY name";
        } else if (level == 2) {
            // The second time: List all schemas under a database
            String dbName = dbNameVO.getDbName();
            if (dbName == null || dbName.trim().isEmpty()) {
                throw new IllegalArgumentException(MessageUtils.messageWithFallback(
                        "sys.error.database.sqlserver.parent.required",
                        "SQLServer level=2 requires a parent dbName"));
            }
            return "SELECT name AS DBNAME,  2 AS TOTALLEVELS " +
                    "FROM [" + dbName + "].sys.schemas " +
                    "WHERE principal_id <> 1 " +
                    "ORDER BY name";
        }

        throw new UnsupportedOperationException(MessageUtils.messageWithFallback(
                "sys.error.database.sqlserver.level.unsupported", "SQLServer supports only levels 1~2"));
    }

    @Override
    public String getInsertOrUpdateSql(String tableName, String where, String tableFieldName, String tableFieldValue, String setValue) {
        String sql = "MERGE INTO {tableName} USING (SELECT COUNT(1) count FROM {tableName}  WHERE {where}) c ON (c.count > 0) WHEN MATCHED THEN UPDATE SET {setValue} WHERE {where} WHEN NOT MATCHED THEN INSERT ({tableFieldName}) VALUES ({tableFieldValue})";
        sql = StringUtils
                .replace(sql, "{tableName}", tableName)
                .replace("{where}", where)
                .replace("{tableFieldName}", tableFieldName)
                .replace("{tableFieldValue}", tableFieldValue)
                .replace("{setValue}", setValue);
        return sql;
    }

    @Override
    public RowMapper<DbColumn> columnMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbColumn entity = new DbColumn();
            if (DatabaseUtil.hasColumn(rs, "TABLENAME")) {
                entity.setTableName(rs.getString("TABLENAME"));
            }
            entity.setColName(rs.getString("COLNAME"));
            entity.setDataType(rs.getString("DATATYPE"));
            entity.setDataLength(rs.getString("DATALENGTH"));
            entity.setDataPrecision(rs.getString("DATAPRECISION"));
            entity.setDataScale(rs.getString("DATASCALE"));
            entity.setColKey("1".equals(rs.getString("COLKEY")) ? true : false);
            entity.setNullable("1".equals(rs.getString("NULLABLE")) ? true : false);
            entity.setColPosition(rs.getInt("COLPOSITION"));
            entity.setDataDefault(rs.getString("DATADEFAULT"));
            entity.setColComment(rs.getString("COLCOMMENT"));
            return entity;
        };
    }

    @Override
    public RowMapper<DbTable> tableMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbTable entity = new DbTable();
            entity.setTableName(rs.getString("TABLENAME"));
            entity.setTableComment(rs.getString("TABLECOMMENT"));
            return entity;
        };
    }

    @Override
    public String getTableName(DbQueryProperty property, String tableName) {
        return quoteIdentifier(property.getDbName()) + "." + quoteIdentifier(getSchemaName(property)) + "." + quoteIdentifier(tableName);
    }

    @Override
    public Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(trainToJdbcUrl(dbQueryProperty), dbQueryProperty.getUsername(),
                    dbQueryProperty.getPassword());

            // Method 2: Execute a test query (two-factor authentication)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new DataQueryException("db.error.connection.retry", "Database connection failed, please try again later");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new DataQueryException("db.error.close.connection", "Failed to close database connection");
                }
            }
        }
        return false;
    }

    @Override
    public String updateTableComment(DbQueryProperty dbQueryProperty, String tableName, String tableComment) {
        String schema = StringUtils.isNotEmpty(dbQueryProperty.getSid()) ? dbQueryProperty.getSid() : "dbo";
        return "EXEC sys.sp_updateextendedproperty @name = N'MS_Description', @value = N'" + DatabaseUtil.escapeSingleQuotes(tableComment) + "', @level0type = N'SCHEMA', @level0name = N'" + schema + "', @level1type = N'TABLE', @level1name = N'" + tableName + "'";
    }

    // ... existing code ...
    @Override
    public String dropColumn(DbQueryProperty dbQueryProperty, String tableName, String colName) {
        String fullTableName = getTableName(dbQueryProperty, tableName);
        return "ALTER TABLE " + fullTableName + " DROP COLUMN " + colName;
    }

    @Override
    public List<String> modifyColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column) {
        List<String> sqlList = new ArrayList<>();
        String fullTableName = getTableName(dbQueryProperty, tableName);

        if (Boolean.TRUE.equals(column.getColKey())) {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE ").append(fullTableName).append(" ADD CONSTRAINT ").append(quoteIdentifier("PK_" + tableName + "_" + column.getColName()));
            sql.append(" PRIMARY KEY (").append(quoteIdentifier(column.getColName())).append(")");
            sqlList.add(sql.toString());
        } else {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE ").append(fullTableName).append(" ALTER COLUMN ");
            sql.append(quoteIdentifier(column.getColName())).append(" ");

            String columnType = column.getDataType().toUpperCase();
            switch (columnType) {
                case "NVARCHAR":
                case "NVARCHAR(MAX)":
                    sql.append(unicodeColumnType(column));
                    break;
                case "VARCHAR":
                case "VARCHAR2":
                    sql.append("VARCHAR");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength()).append(")");
                    } else {
                        sql.append("(MAX)");
                    }
                    break;
                case "CHAR":
                    sql.append("CHAR");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength()).append(")");
                    }
                    break;
                case "TEXT":
                    sql.append("TEXT");
                    break;
                case "INT":
                case "INTEGER":
                    sql.append("INT");
                    break;
                case "BIGINT":
                    sql.append("BIGINT");
                    break;
                case "TINYINT":
                    sql.append("SMALLINT");
                    break;
                case "DECIMAL":
                    sql.append("DECIMAL");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength());
                        if (StringUtils.isNotEmpty(column.getDataScale())) {
                            sql.append(", ").append(column.getDataScale());
                        }
                        sql.append(")");
                    }
                    break;
                case "FLOAT":
                    sql.append("FLOAT");
                    break;
                case "DOUBLE":
                    sql.append("FLOAT");
                    break;
                case "DATE":
                    sql.append("DATE");
                    break;
                case "DATETIME":
                    sql.append("DATETIME");
                    break;
                default:
                    sql.append(columnType);
                    break;
            }

            if (!column.getNullable()) {
                sql.append(" NOT NULL");
            }

            sqlList.add(sql.toString());
        }

        if (StringUtils.isNotEmpty(column.getDataDefault())) {
            String defaultSql = "ALTER TABLE " + fullTableName + " ADD CONSTRAINT DF_" + tableName + "_" + column.getColName()
                    + " DEFAULT " + sqlServerDefaultExpression(column.getDataType(), column.getDataDefault()) + " FOR " + quoteIdentifier(column.getColName());
            sqlList.add(defaultSql);
        }

        if (StringUtils.isNotEmpty(column.getColComment())) {
            String schema = StringUtils.isNotEmpty(dbQueryProperty.getSid()) ? dbQueryProperty.getSid() : "dbo";
            sqlList.add("EXEC sys.sp_updateextendedproperty @name = N'MS_Description', @value = N'"
                    + DatabaseUtil.escapeSingleQuotes(column.getColComment())
                    + "', @level0type = N'SCHEMA', @level0name = N'" + schema
                    + "', @level1type = N'TABLE', @level1name = N'" + tableName
                    + "', @level2type = N'COLUMN', @level2name = N'" + column.getColName() + "'");
        }

        return sqlList;
    }

    @Override
    public List<String> addColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column) {
        List<String> sqlList = new ArrayList<>();
        String fullTableName = getTableName(dbQueryProperty, tableName);

        if (Boolean.TRUE.equals(column.getColKey())) {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE ").append(fullTableName).append(" ADD ");
            sql.append(quoteIdentifier(column.getColName())).append(" ");

            String columnType = column.getDataType().toUpperCase();
            switch (columnType) {
                case "NVARCHAR":
                case "NVARCHAR(MAX)":
                    sql.append(unicodeColumnType(column));
                    break;
                case "VARCHAR":
                case "VARCHAR2":
                    sql.append("VARCHAR");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength()).append(")");
                    } else {
                        sql.append("(MAX)");
                    }
                    break;
                case "CHAR":
                    sql.append("CHAR");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength()).append(")");
                    }
                    break;
                case "TEXT":
                    sql.append("TEXT");
                    break;
                case "INT":
                case "INTEGER":
                    sql.append("INT");
                    break;
                case "BIGINT":
                    sql.append("BIGINT");
                    break;
                case "TINYINT":
                    sql.append("SMALLINT");
                    break;
                case "DECIMAL":
                    sql.append("DECIMAL");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength());
                        if (StringUtils.isNotEmpty(column.getDataScale())) {
                            sql.append(", ").append(column.getDataScale());
                        }
                        sql.append(")");
                    }
                    break;
                case "FLOAT":
                    sql.append("FLOAT");
                    break;
                case "DOUBLE":
                    sql.append("FLOAT");
                    break;
                case "DATE":
                    sql.append("DATE");
                    break;
                case "DATETIME":
                    sql.append("DATETIME");
                    break;
                default:
                    sql.append(columnType);
                    break;
            }

            sql.append(identityColumnClause(column));

            if (!column.getNullable()) {
                sql.append(" NOT NULL");
            }

            if (StringUtils.isNotEmpty(column.getDataDefault())) {
                appendSqlServerDefaultClause(sql, columnType, column.getDataDefault());
            }

            sqlList.add(sql.toString());

            sql = new StringBuilder();
            sql.append("ALTER TABLE ").append(fullTableName).append(" ADD CONSTRAINT ").append(quoteIdentifier("PK_" + tableName + "_" + column.getColName()));
            sql.append(" PRIMARY KEY (").append(quoteIdentifier(column.getColName())).append(")");
            sqlList.add(sql.toString());
        } else {
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE ").append(fullTableName).append(" ADD ");
            sql.append(quoteIdentifier(column.getColName())).append(" ");

            String columnType = column.getDataType().toUpperCase();
            switch (columnType) {
                case "NVARCHAR":
                case "NVARCHAR(MAX)":
                    sql.append(unicodeColumnType(column));
                    break;
                case "VARCHAR":
                case "VARCHAR2":
                    sql.append("VARCHAR");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength()).append(")");
                    } else {
                        sql.append("(MAX)");
                    }
                    break;
                case "CHAR":
                    sql.append("CHAR");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength()).append(")");
                    }
                    break;
                case "TEXT":
                    sql.append("TEXT");
                    break;
                case "INT":
                case "INTEGER":
                    sql.append("INT");
                    break;
                case "BIGINT":
                    sql.append("BIGINT");
                    break;
                case "TINYINT":
                    sql.append("SMALLINT");
                    break;
                case "DECIMAL":
                    sql.append("DECIMAL");
                    if (StringUtils.isNotEmpty(column.getDataLength())) {
                        sql.append("(").append(column.getDataLength());
                        if (StringUtils.isNotEmpty(column.getDataScale())) {
                            sql.append(", ").append(column.getDataScale());
                        }
                        sql.append(")");
                    }
                    break;
                case "FLOAT":
                    sql.append("FLOAT");
                    break;
                case "DOUBLE":
                    sql.append("FLOAT");
                    break;
                case "DATE":
                    sql.append("DATE");
                    break;
                case "DATETIME":
                    sql.append("DATETIME");
                    break;
                default:
                    sql.append(columnType);
                    break;
            }

            sql.append(identityColumnClause(column));

            if (!column.getNullable()) {
                sql.append(" NOT NULL");
            }

            if (StringUtils.isNotEmpty(column.getDataDefault())) {
                appendSqlServerDefaultClause(sql, columnType, column.getDataDefault());
            }

            sqlList.add(sql.toString());
        }

        if (StringUtils.isNotEmpty(column.getColComment())) {
            String schema = StringUtils.isNotEmpty(dbQueryProperty.getSid()) ? dbQueryProperty.getSid() : "dbo";
            sqlList.add("EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'"
                    + DatabaseUtil.escapeSingleQuotes(column.getColComment())
                    + "', @level0type = N'SCHEMA', @level0name = N'" + schema
                    + "', @level1type = N'TABLE', @level1name = N'" + tableName
                    + "', @level2type = N'COLUMN', @level2name = N'" + column.getColName() + "'");
        }

        return sqlList;
    }

    @Override
    public String generateDropIndexSQL(DbQueryProperty property, String tableName, String indexName) {
        requireIndexIdentifier(tableName);
        requireIndexIdentifier(indexName);
        return "DROP INDEX " + quoteIdentifier(indexName) + " ON " + getTableName(property, tableName);
    }

    @Override
    public DbColumn convertLogicalColumn(DbColumn column) {
        DbColumn converted = super.convertLogicalColumn(column);
        if ("STRING".equalsIgnoreCase(column.getDataType())) {
            converted.setDataType("NVARCHAR");
            if (StringUtils.isBlank(converted.getDataLength())) converted.setDataLength("255");
        } else if ("TEXT".equalsIgnoreCase(column.getDataType())) {
            converted.setDataType("NVARCHAR(MAX)");
            converted.setDataLength(null);
        } else if ("DATETIME".equalsIgnoreCase(column.getDataType())) {
            converted.setDataType("DATETIME2(3)");
            converted.setDataLength(null);
            converted.setDataPrecision(null);
            converted.setDataScale("3");
        }
        return converted;
    }

    protected String unicodeColumnType(DbColumn column) {
        if (column.getDataType().toUpperCase(java.util.Locale.ROOT).contains("(MAX)")) return "NVARCHAR(MAX)";
        long length = DatabaseUtil.getStringToLong(column.getDataLength());
        if (length <= 0) return "NVARCHAR(MAX)";
        if (length > 4000) throw new IllegalArgumentException("SQL Server Unicode STRING长度不能超过4000，请使用TEXT");
        return "NVARCHAR(" + length + ")";
    }

    @Override
    public String getColumnType(DbColumn column) {
        if (column == null) {
            return null;
        }
        String type = normalizeTypeName(column.getDataType());
        switch (type) {
            case "NVARCHAR":
                String unicodeType = unicodeColumnType(column);
                return unicodeType.equals("NVARCHAR(MAX)") ? unicodeType : "NVARCHAR";
            case "NUMBER":
            case "NUMERIC":
            case "DECIMAL":
                return "DECIMAL";
            case "TIMESTAMP":
                return "DATETIME";
            case "VARCHAR":
            case "VARCHAR2":
                long varcharLength = DatabaseUtil.getStringToLong(column.getDataLength());
                return varcharLength <= 0 || varcharLength > MAX_VARCHAR_LENGTH ? "VARCHAR(MAX)" : "VARCHAR";
            default:
                return column.getDataType();
        }
    }

    @Override
    public DbColumn toStandardColumn(DbColumn column) {
        String type = normalizeTypeName(column == null ? null : column.getDataType());
        if ("NVARCHAR".equals(type) && (column.getDataType().toUpperCase(java.util.Locale.ROOT).contains("(MAX)")
                || "-1".equals(column.getDataLength()))) {
            return toStandardColumn(column, "TEXT");
        }
        switch (type) {
            case "CHAR":
            case "NCHAR":
                return toStandardColumn(column, "CHAR");
            case "VARCHAR":
            case "NVARCHAR":
                return toStandardColumn(column, "VARCHAR2");
            case "TEXT":
            case "NTEXT":
            case "XML":
                return toStandardColumn(column, "TEXT");
            case "TINYINT":
            case "SMALLINT":
            case "INT":
            case "INTEGER":
            case "BIGINT":
            case "DECIMAL":
            case "NUMERIC":
            case "MONEY":
            case "SMALLMONEY": {
                DbColumn standardColumn = toStandardColumn(column, "NUMBER");
                if (StringUtils.isNotBlank(standardColumn.getDataPrecision())) {
                    // SQL Server max_length is stored in bytes; numeric DDL requires decimal precision.
                    standardColumn.setDataLength(standardColumn.getDataPrecision());
                }
                return standardColumn;
            }
            case "REAL":
                return toStandardColumn(column, "FLOAT");
            case "FLOAT":
                return toStandardColumn(column, "DOUBLE");
            case "BIT":
            case "BOOLEAN":
                return toStandardColumn(column, "TINYINT");
            case "DATE":
                return toStandardColumn(column, "DATE");
            case "TIME":
                return toStandardColumn(column, "TIMESTAMP");
            case "DATETIME":
            case "DATETIME2":
            case "SMALLDATETIME":
            case "DATETIMEOFFSET":
                return toStandardColumn(column, "DATETIME");
            case "TIMESTAMP":
            case "ROWVERSION":
                return toStandardColumn(column, "BLOB");
            case "BINARY":
            case "VARBINARY":
            case "IMAGE":
                return toStandardColumn(column, "BLOB");
            case "UNIQUEIDENTIFIER":
                return toStandardColumn(column, "VARCHAR2");
            default:
                return super.toStandardColumn(column);
        }
    }

    @Override
    public String getSchemaName(DbQueryProperty property) {
        return StringUtils.defaultIfBlank(property.getSid(), "dbo");
    }

    protected void appendSqlServerDefaultClause(StringBuilder sql, String dataType, String defaultValue) {
        if (StringUtils.isBlank(defaultValue)) {
            return;
        }
        sql.append(" DEFAULT ").append(sqlServerDefaultExpression(dataType, defaultValue));
    }

    protected String sqlServerDefaultExpression(String dataType, String defaultValue) {
        String def = normalizeDefaultValue(defaultValue);
        String normalizedType = normalizeTypeName(dataType);
        if ("NVARCHAR".equals(normalizedType) || "NCHAR".equals(normalizedType) || "NTEXT".equals(normalizedType)) {
            if (def.matches("(?is)^N'.*'$")) return def;
            if (isQuotedDefault(def)) return "N" + def;
            if (!StringUtils.isBlank(def) && !isSqlServerFunctionDefault(def) && !isNumericDefault(def)) {
                return "N'" + DatabaseUtil.escapeSingleQuotes(def) + "'";
            }
        }
        if (StringUtils.isBlank(def)
                || isQuotedDefault(def)
                || isSqlServerFunctionDefault(def)
                || isNumericDefault(def)) {
            return def;
        }
        String type = dataType == null ? "" : dataType.trim().toUpperCase();
        if (isSqlServerStringOrTemporalType(type)) {
            return "'" + DatabaseUtil.escapeSingleQuotes(def) + "'";
        }
        return def;
    }

    private String normalizeDefaultValue(String defaultValue) {
        String def = defaultValue == null ? "" : defaultValue.trim();
        while (def.length() >= 2 && def.startsWith("(") && def.endsWith(")")) {
            def = def.substring(1, def.length() - 1).trim();
        }
        return def;
    }

    private boolean isQuotedDefault(String defaultValue) {
        return defaultValue.length() >= 2 && defaultValue.startsWith("'") && defaultValue.endsWith("'");
    }

    private boolean isNumericDefault(String defaultValue) {
        return defaultValue.matches("^-?\\d+(\\.\\d+)?$");
    }

    private boolean isSqlServerFunctionDefault(String defaultValue) {
        String def = defaultValue.trim().toUpperCase();
        return "CURRENT_TIMESTAMP".equals(def)
                || "GETDATE()".equals(def)
                || "SYSDATETIME()".equals(def)
                || "NEWID()".equals(def)
                || "NULL".equals(def);
    }

    private boolean isSqlServerStringOrTemporalType(String dataType) {
        return dataType.contains("CHAR")
                || dataType.contains("TEXT")
                || dataType.contains("DATE")
                || dataType.contains("TIME");
    }

    @Override
    protected String identityColumnClause() {
        return " IDENTITY(1,1)";
    }

    @Override
    public String castToText(String expression) {
        return "CAST(" + expression + " AS NVARCHAR(MAX))";
    }

    @Override
    public String containsPattern(String value) {
        return super.containsPattern(value).replace("[", "![");
    }

    @Override
    public String buildSelectForUpdateSql(String table, String columns, String predicate) {
        return "SELECT " + columns + " FROM " + table + " WITH (UPDLOCK, HOLDLOCK) WHERE " + predicate;
    }

    @Override
    public boolean requiresIdentityInsert() { return true; }

    @Override
    public String identityInsertSql(String qualifiedTable, boolean enabled) {
        return "SET IDENTITY_INSERT " + qualifiedTable + (enabled ? " ON" : " OFF");
    }
}
