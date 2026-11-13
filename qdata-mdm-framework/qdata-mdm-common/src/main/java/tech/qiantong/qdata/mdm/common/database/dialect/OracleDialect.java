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

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbName;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import tech.qiantong.qdata.mdm.common.database.exception.DataQueryException;
import tech.qiantong.qdata.mdm.common.database.utils.DatabaseUtil;
import tech.qiantong.qdata.mdm.common.utils.MessageUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Oracle Oracle11g and below database dialect
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
public class OracleDialect extends AbstractDbDialect {

    @Override
    public String columns(String dbName, String tableName) {
        if (StringUtils.isNotBlank(dbName)) {
            return "SELECT\n" +
                    "\tc.table_name,\n" +
                    "\tc.column_name AS colName,\n" +
                    "\tc.data_type AS DATATYPE,\n" +
                    "\tc.data_length AS DATALENGTH,\n" +
                    "\tc.data_precision AS DATAPRECISION,\n" +
                    "\tc.data_scale AS DATASCALE,\n" +
                    "\tc.nullable AS NULLABLE,\n" +
                    "\tc.column_id AS COLPOSITION,\n" +
                    "\tc.data_default AS DATADEFAULT,\n" +
                    "\tcm.comments AS COLCOMMENT,\n" +
                    "CASE WHEN t.column_name IS NULL THEN 0 ELSE 1 END AS COLKEY " +
                    "FROM\n" +
                    "\tall_tab_columns c\n" +
                    "\tLEFT JOIN all_col_comments cm ON cm.OWNER = '" + dbName + "' AND c.table_name = cm.table_name AND c.column_name = cm.column_name\n" +
                    "\tLEFT JOIN ( " +
                    "SELECT a.table_name, a.column_name FROM all_constraints b JOIN all_cons_columns a ON b.owner = a.owner AND b.constraint_name = a.constraint_name\n" +
                    "WHERE b.owner ='" + dbName + "' AND b.constraint_type = 'P' AND b.table_name ='" + tableName + "' " +
                    ") t on t.table_name = c.table_name and c.column_name = t.column_name " +
                    "WHERE\n" +
                    "\t c.OWNER = '" + dbName + "' \n" +
                    "\tAND c.Table_Name = '" + tableName + "'";
        } else {
            return "select columns.column_name AS colName, columns.data_type AS DATATYPE, columns.data_length AS DATALENGTH, columns.data_precision AS DATAPRECISION, " +
                    "columns.data_scale AS DATASCALE, columns.nullable AS NULLABLE, columns.column_id AS COLPOSITION, columns.data_default AS DATADEFAULT, comments.comments AS COLCOMMENT," +
                    "case when t.column_name is null then 0 else 1 end as COLKEY " +
                    "from sys.user_tab_columns columns LEFT JOIN sys.user_col_comments comments ON columns.table_name = comments.table_name AND columns.column_name = comments.column_name " +
                    "left join ( " +
                    "select col.column_name as column_name, con.table_name as table_name from user_constraints con, user_cons_columns col " +
                    "where con.constraint_name = col.constraint_name and con.constraint_type = 'P' " +
                    ") t on t.table_name = columns.table_name and columns.column_name = t.column_name " +
                    "where columns.table_name = '" + tableName + "' order by columns.column_id ";
        }
    }

    @Override
    public String columns(DbQueryProperty dbQueryProperty, String tableName) {
        // 按输入原样查询目录，与建表时加引号保留大小写的规则一致。
        tableName = normalizeOracleIdentifier(tableName);
        String owner = getSchemaName(dbQueryProperty);
        if (StringUtils.isNotBlank(owner)) {
            return "SELECT\n" +
                    "\tc.table_name,\n" +
                    "\tc.column_name AS colName,\n" +
                    "\tc.data_type AS DATATYPE,\n" +
                    "\tc.data_length AS DATALENGTH,\n" +
                    "\tc.data_precision AS DATAPRECISION,\n" +
                    "\tc.data_scale AS DATASCALE,\n" +
                    "\tc.nullable AS NULLABLE,\n" +
                    "\tc.column_id AS COLPOSITION,\n" +
                    "\tc.data_default AS DATADEFAULT,\n" +
                    "\tcm.comments AS COLCOMMENT,\n" +
                    "CASE WHEN t.column_name IS NULL THEN 0 ELSE 1 END AS COLKEY " +
                    "FROM\n" +
                    "\tall_tab_columns c\n" +
                    "\tLEFT JOIN all_col_comments cm ON cm.OWNER = '" + DatabaseUtil.escapeSingleQuotes(owner) + "' AND c.table_name = cm.table_name AND c.column_name = cm.column_name\n" +
                    "\tLEFT JOIN ( " +
                    "SELECT a.table_name, a.column_name FROM all_constraints b JOIN all_cons_columns a ON b.owner = a.owner AND b.constraint_name = a.constraint_name\n" +
                    "WHERE b.owner ='" + DatabaseUtil.escapeSingleQuotes(owner) + "' AND b.constraint_type = 'P' AND b.table_name ='" + DatabaseUtil.escapeSingleQuotes(tableName) + "' " +
                    ") t on t.table_name = c.table_name and c.column_name = t.column_name " +
                    "WHERE\n" +
                    "\t c.OWNER = '" + DatabaseUtil.escapeSingleQuotes(owner) + "' \n" +
                    "\tAND c.Table_Name = '" + DatabaseUtil.escapeSingleQuotes(tableName) + "'";
        } else {
            return "select columns.column_name AS colName, columns.data_type AS DATATYPE, columns.data_length AS DATALENGTH, columns.data_precision AS DATAPRECISION, " +
                    "columns.data_scale AS DATASCALE, columns.nullable AS NULLABLE, columns.column_id AS COLPOSITION, columns.data_default AS DATADEFAULT, comments.comments AS COLCOMMENT," +
                    "case when t.column_name is null then 0 else 1 end as COLKEY " +
                    "from sys.user_tab_columns columns LEFT JOIN sys.user_col_comments comments ON columns.table_name = comments.table_name AND columns.column_name = comments.column_name " +
                    "left join ( " +
                    "select col.column_name as column_name, con.table_name as table_name from user_constraints con, user_cons_columns col " +
                    "where con.constraint_name = col.constraint_name and con.constraint_type = 'P' " +
                    ") t on t.table_name = columns.table_name and columns.column_name = t.column_name " +
                    "where columns.table_name = '" + DatabaseUtil.escapeSingleQuotes(tableName) + "' order by columns.column_id ";
        }
    }

    @Override
    public String generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName) {
        return buildOracleCheckTableExistsSQL(dbQueryProperty, tableName);
    }

    @Override
    public String buildTableNameByDbType(DbQueryProperty dbQueryProperty, String tableName) {
        if (StringUtils.isNotEmpty(dbQueryProperty.getDbName())) {
            return dbQueryProperty.getDbName() + "." + tableName;
        }

        return tableName;
    }

    @Override
    public List<String> someInternalSqlGenerator(DbQueryProperty dbQueryProperty, String tableName, String tableComment, List<DbColumn> dbColumnList) {
        List<String> sqlList = generateOracleCreateSql(getTableName(dbQueryProperty, tableName), tableComment, dbColumnList);

        return sqlList;
    }

    @Override
    public List<String> validateSpecification(String tableName, String tableComment, List<DbColumn> columns) {
        return null;
    }

    private List<String> generateOracleCreateSql(String tableName, String tableComment, List<DbColumn> columns) {
        List<String> sqlList = new ArrayList<>();
        StringBuilder createSql = new StringBuilder();

        createSql.append("CREATE TABLE ").append(tableName).append(" (");
        List<String> pkList = new ArrayList<>();
        for (DbColumn col : columns) {
            createSql.append("\n  ").append(quoteIdentifier(col.getColName())).append(" ");
            createSql.append(mapOracleColumnType(col));

            if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(col.getDataDefault())) {
                appendDefaultClause(createSql, col.getDataType(), col.getDataDefault());
            }
            // VARCHAR、日期等字段也支持NOT NULL，不能只给少数类型保留必填约束。
            createSql.append(identityColumnClause(col));
            if (Boolean.FALSE.equals(col.getNullable())) {
                createSql.append(" NOT NULL");
            }
            if (col.getColKey()) {
                pkList.add(col.getColName());
            }
            createSql.append(",");
        }
        // 去逗号
        if (createSql.lastIndexOf(",") == createSql.length() - 1) {
            createSql.deleteCharAt(createSql.length() - 1);
        }
        // 主键
        if (!pkList.isEmpty()) {
            createSql.append(",\n  PRIMARY KEY(");
            for (String pk : pkList) {
                createSql.append(quoteIdentifier(pk)).append(",");
            }
            createSql.deleteCharAt(createSql.length() - 1);
            createSql.append(")");
        }
        createSql.append("\n)");
        sqlList.add(createSql.toString());

        // 表注释
        if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(tableComment)) {
            String tableCmt = "COMMENT ON TABLE " + tableName + " IS '" + DatabaseUtil.escapeSingleQuotes(tableComment) + "'";
            sqlList.add(tableCmt);
        }
        // 字段注释
        for (DbColumn col : columns) {
            if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(col.getColComment())) {
                String colCmt = "COMMENT ON COLUMN " + tableName + "." + quoteIdentifier(col.getColName())
                        + " IS '" + DatabaseUtil.escapeSingleQuotes(col.getColComment()) + "'";
                sqlList.add(colCmt);
            }
        }

        return sqlList;
    }

    private static boolean isStringTypeSwitchDEFAULT(String columnType) {
        switch (columnType) {
            case "VARCHAR":
            case "VARCHAR2":
            case "CHAR":
            case "CLOB":
            case "TEXT":
                return true;
            default:
                return false;
        }
    }

    private static String mapOracleColumnType(DbColumn col) {
        String type = normalizeOracleTypeName(col.getDataType());
        Long length = DatabaseUtil.getStringToLong(col.getDataLength());
        Long scale = DatabaseUtil.getStringToLong(col.getDataScale());

        switch (type) {
            case "VARCHAR":
            case "VARCHAR2":
                return "VARCHAR2(" + (length != null ? length : 255) + ")";
            case "CHAR":
                return "CHAR(" + (length != null ? length : 1) + ")";
            case "TINYINT":
                return "NUMBER(3)";
            case "SMALLINT":
                return "NUMBER(5)";
            case "INT":
            case "INTEGER":
                String resultINT = generateColumnDefinitionOracle(
                        length
                        , 10
                        , false
                        , scale
                );
                return new StringBuilder("NUMBER").append(resultINT).toString();
            case "BIGINT":
                String resultBIGINT = generateColumnDefinitionOracle(
                        length
                        , 19
                        , false
                        , scale
                );
                return new StringBuilder("NUMBER").append(resultBIGINT).toString();
            case "NUMBER":
            case "DECIMAL":
            case "NUMERIC":
                return "NUMBER(" + (length != null ? length : 10) + "," + (scale != null ? scale : 0) + ")";
            case "FLOAT":
                return "BINARY_FLOAT";
            case "DOUBLE":
                return "BINARY_DOUBLE";
            case "DATE":
                return "DATE";
            case "DATETIME":
            case "TIMESTAMP":
                return "TIMESTAMP";
            case "TEXT":
            case "CLOB":
                return "CLOB";
            default:
                return type;
        }
    }

    /**
     * Generate SQL string for concatenation based on column length and scale
     *
     * @param columnLength column length (string representation)
     * @param maxLength The maximum value of the length limit (e.g. 38)
     * @param includeScale Whether to splice decimal places
     * @param columnScale The number of decimal places in the column (string representation, may be empty)
     * @return generated SQL string for concatenation
     */
    public static String generateColumnDefinitionOracle(Long columnLength, long maxLength, boolean includeScale, Long columnScale) {
        StringBuilder sql = new StringBuilder("");

        if (columnLength == null) {
            throw new UnsupportedOperationException(MessageUtils.messageWithFallback(
                    "sys.error.database.numeric.length.missing",
                    "Invalid attribute type format: numeric type length is missing"));
        }

        // If columnLength is empty, maxLength is used as the default value
        long length = columnLength;

        if (length > maxLength) {
            length = maxLength;
        }

        // Splicing length
        sql.append("(").append(length);

        // Determine whether the number of decimal places needs to be spliced based on includeScale and columnScale
        if (includeScale && columnScale != 0) {
            sql.append(", ").append(columnScale);
        }

        sql.append(")");

        return sql.toString();
    }

    @Override
    public String tables(String dbName) {
        if (StringUtils.isNotBlank(dbName)) {
            return "SELECT DISTINCT t.TABLE_NAME AS TABLENAME,c.COMMENTS AS TABLECOMMENT FROM ALL_TAB_COMMENTS c JOIN ALL_TABLES t ON c.TABLE_NAME = t.TABLE_NAME WHERE t.OWNER = '" + dbName + "' AND c.OWNER = '" + dbName + "'";
        } else {
            return "select tables.table_name AS TABLENAME, comments.comments AS TABLECOMMENT from sys.user_tables tables " +
                    "LEFT JOIN sys.user_tab_comments comments ON tables.table_name = comments.table_name ";
        }
    }

    @Override
    public String tables(DbQueryProperty dbQueryProperty) {
        if (StringUtils.isNotBlank(dbQueryProperty.getDbName())) {
            return "SELECT DISTINCT t.TABLE_NAME AS TABLENAME,c.COMMENTS AS TABLECOMMENT FROM ALL_TAB_COMMENTS c JOIN ALL_TABLES t ON c.TABLE_NAME = t.TABLE_NAME WHERE t.OWNER = '" + dbQueryProperty.getDbName() + "' AND c.OWNER = '" + dbQueryProperty.getDbName() + "'";
        } else {
            return "select tables.table_name AS TABLENAME, comments.comments AS TABLECOMMENT from sys.user_tables tables " +
                    "LEFT JOIN sys.user_tab_comments comments ON tables.table_name = comments.table_name ";
        }
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
        return "SELECT " + fields + " FROM " + dbQueryProperty.getDbName() + "." + tableName;
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long count) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( ");
        sqlBuilder.append(originalSql).append(" ) TMP WHERE ROWNUM <=").append((offset >= 1) ? (offset + count) : count);
        sqlBuilder.append(") WHERE ROW_ID > ").append(offset);
        return sqlBuilder.toString();
    }

    @Override
    public RowMapper<DbColumn> columnLongMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbColumn entity = new DbColumn();
            entity.setDataDefault(rs.getString("DATADEFAULT"));
            return entity;
        };
    }

    @Override
    public String getDataStorageSize(String dbName) {
        return "SELECT ROUND(SUM(bytes) / 1024 / 1024, 2) AS \"usedSizeMb\" FROM dba_segments WHERE owner = '" + dbName + "' GROUP BY owner";
    }

    @Override
    public String getDbName() {
        return "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')  AS \"databaseName\" FROM DUAL";
    }

    @Override
    public String getDbName(DbName dbName) {
        int level = dbName == null ? 1 : dbName.getLevel() + 1;
        // Only one level is used: database
        if (level == 1) {
            return "SELECT USERNAME AS DBNAME,1 AS TOTALLEVELS \n" +
                    "FROM ALL_USERS\n" +
                    "WHERE USERNAME NOT IN (\n" +
                    "  'SYS','SYSTEM','OUTLN','MDSYS','XDB','WMSYS','CTXSYS','DBSNMP',\n" +
                    "  'APPQOSSYS','OLAPSYS','OWBSYS','ORDSYS','ORDDATA','ORDPLUGINS',\n" +
                    "  'SI_INFORMTN_SCHEMA','ORACLE_OCM','SYSMAN','MDDATA','ANONYMOUS',\n" +
                    "  'XS$NULL','DIP','MGMT_VIEW','APEX_PUBLIC_USER'\n" +
                    ")\n" +
                    "ORDER BY DBNAME";
        }
        // No second layer
        throw new UnsupportedOperationException("Oracle11g only has one level");
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
            entity.setColName(rs.getString("COLNAME"));
            entity.setDataType(rs.getString("DATATYPE"));
            entity.setDataLength(rs.getString("DATALENGTH"));
            entity.setDataPrecision(rs.getString("DATAPRECISION"));
            if (rs.getString("DATAPRECISION") != null) {
                entity.setDataLength(rs.getString("DATAPRECISION"));
            }
            entity.setDataScale(rs.getString("DATASCALE"));
            entity.setColKey("1".equals(rs.getString("COLKEY")));
            entity.setNullable("Y".equals(rs.getString("NULLABLE")));
            //long type, processed separately
            //entity.setDataDefault(rs.getString("DATADEFAULT"));
            entity.setColPosition(rs.getInt("COLPOSITION"));
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
    public String getFlinkCDCSQL(DbQueryProperty property, String flinkTableName, String tableName, String tableFieldName) {
        String sql = "CREATE TABLE ${flinkTableName} (${tableFieldName}) " +
                "WITH ( 'connector' = 'oracle-cdc'," +
                " 'hostname' = '${host}' ," +
                "'port' = '${port}' ," +
                "'username' = '${username}' ," +
                "'password' = '${password}'," +
                "'database-name' = '${sid}' ," +
                "'schema-name' = '${dbName}' ," +
                "'table-name' = '${tableName}' ," +
                "'scan.startup.mode' = 'initial' ," +
                "'scan.incremental.snapshot.enabled' = 'true'," +
                "'debezium.database.connection.adapter'='logminer'," +
                "'debezium.log.mining.strategy'='online_catalog'," +
                "'debezium.log.mining.continuous.mine'='true')";
        sql = StringUtils
                .replace(sql, "${flinkTableName}", flinkTableName)
                .replace("${tableName}", tableName)
                .replace("${host}", property.getHost())
                .replace("${tableFieldName}", tableFieldName)
                .replace("${port}", String.valueOf(property.getPort()))
                .replace("${dbName}", property.getDbName())
                .replace("${sid}", property.getSid().toUpperCase(Locale.ROOT))
                .replace("${username}", property.getUsername())
                .replace("${password}", property.getPassword());
        return sql;
    }

    @Override
    public String getFlinkSQL(DbQueryProperty property, String flinkTableName, String tableName, String tableFieldName) {
        String sql = "CREATE TABLE ${flinkTableName} (${tableFieldName}) " +
                "WITH ( 'connector' = 'jdbc'," +
                "'url' = 'jdbc:oracle:thin:@${host}:${port}:${sid}'," +
                "'table-name' = '${dbName}.${tableName}'," +
                "'username' = '${username}'," +
                "'password' = '${password}')";

        sql = StringUtils
                .replace(sql, "${flinkTableName}", flinkTableName)
                .replace("${tableName}", tableName)
                .replace("${host}", property.getHost())
                .replace("${tableFieldName}", tableFieldName)
                .replace("${port}", String.valueOf(property.getPort()))
                .replace("${dbName}", property.getDbName())
                .replace("${sid}", property.getSid())
                .replace("${username}", property.getUsername())
                .replace("${password}", property.getPassword());
        return sql;
    }

    @Override
    public String getFlinkSinkSQL(DbQueryProperty property, JSONObject config, String flinkTableName, String tableName, String tableFieldName) {
        String sql = "CREATE TABLE ${flinkTableName} (${tableFieldName}) " +
                "WITH ( 'connector' = 'jdbc'," +
                "'url' = 'jdbc:oracle:thin:@${host}:${port}:${sid}'," +
                "'table-name' = '${dbName}.${tableName}'," +
                "'username' = '${username}'," +
                "'password' = '${password}'," +
                "'sink.buffer-flush.max-rows' = '${batchSize}'," +
                "'sink.buffer-flush.interval' = '1s'" +
                ")";

        sql = StringUtils
                .replace(sql, "${flinkTableName}", flinkTableName)
                .replace("${tableName}", tableName)
                .replace("${host}", property.getHost())
                .replace("${tableFieldName}", tableFieldName)
                .replace("${port}", String.valueOf(property.getPort()))
                .replace("${dbName}", property.getDbName())
                .replace("${username}", property.getUsername())
                .replace("${password}", property.getPassword())
                .replace("${batchSize}", String.valueOf(config.getIntValue("batchSize", 100)));
        return sql;
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        String url = DbType.getDbType(property.getDbType()).getUrl();
        if (org.springframework.util.StringUtils.isEmpty(url)) {
            throw new DataQueryException("db.error.invalid.dbtype", "Invalid database type");
        }
        url = url.replace("${host}", property.getHost());
        url = url.replace("${port}", String.valueOf(property.getPort()));
        url = url.replace("${sid}", property.getSid());
        return url;
    }

    @Override
    public String updateTableComment(DbQueryProperty dbQueryProperty, String tableName, String tableComment) {
        String fullTableName = getTableName(dbQueryProperty, tableName);
        return "COMMENT ON TABLE " + fullTableName + " IS '" + DatabaseUtil.escapeSingleQuotes(tableComment) + "'";
    }

    // ... existing code ...
    @Override
    public String dropColumn(DbQueryProperty dbQueryProperty, String tableName, String colName) {
        String fullTableName = getTableName(dbQueryProperty, tableName);
        return "ALTER TABLE " + fullTableName + " DROP COLUMN " + quoteIdentifier(colName);
    }

    @Override
    public List<String> modifyColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column) {
        return modifyColumn(dbQueryProperty, tableName, column, null);
    }

    /**
     * 修改 Oracle 字段。仅在源、目标字段的可空性不一致时修改 NULL/NOT NULL，
     * 避免对已经是 NOT NULL 的字段重复设置并触发 ORA-01442。
     */
    public List<String> modifyColumn(DbQueryProperty dbQueryProperty, String tableName,
                                     DbColumn column, DbColumn targetColumn) {
        List<String> sqlList = new ArrayList<>();
        String fullTableName = getTableName(dbQueryProperty, tableName);

        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(fullTableName).append(" MODIFY ");
        sql.append(quoteIdentifier(column.getColName())).append(" ");
        sql.append(mapOracleColumnType(column));

        if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(column.getDataDefault())) {
            appendDefaultClause(sql, column.getDataType(), column.getDataDefault());
        }

        if (targetColumn != null
                && !java.util.Objects.equals(column.getNullable(), targetColumn.getNullable())) {
            if (Boolean.FALSE.equals(column.getNullable())) {
                sql.append(" NOT NULL");
            } else if (Boolean.TRUE.equals(column.getNullable())) {
                sql.append(" NULL");
            }
        }

        sqlList.add(sql.toString());

        if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(column.getColComment())) {
            sqlList.add("COMMENT ON COLUMN " + fullTableName + "." + quoteIdentifier(column.getColName())
                    + " IS '" + DatabaseUtil.escapeSingleQuotes(column.getColComment()) + "'");
        }

        return sqlList;
    }

    @Override
    public List<String> addColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column) {
        List<String> sqlList = new ArrayList<>();
        String fullTableName = getTableName(dbQueryProperty, tableName);

        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(fullTableName).append(" ADD ");
        sql.append(quoteIdentifier(column.getColName())).append(" ");
        sql.append(mapOracleColumnType(column));

        if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(column.getDataDefault())) {
            appendDefaultClause(sql, column.getDataType(), column.getDataDefault());
        }

        // 新增字段与首次建表使用相同的必填约束规则。
        sql.append(identityColumnClause(column));
        if (Boolean.FALSE.equals(column.getNullable())) {
            sql.append(" NOT NULL");
        }

        sqlList.add(sql.toString());

        if (tech.qiantong.qdata.mdm.common.utils.StringUtils.hasText(column.getColComment())) {
            sqlList.add("COMMENT ON COLUMN " + fullTableName + "." + quoteIdentifier(column.getColName())
                    + " IS '" + DatabaseUtil.escapeSingleQuotes(column.getColComment()) + "'");
        }

        return sqlList;
    }

    protected static void appendDefaultClause(StringBuilder sql, String dataType, String defaultValue) {
        if (StringUtils.isBlank(defaultValue)) {
            return;
        }
        sql.append(" DEFAULT ").append(defaultExpression(dataType, defaultValue));
    }

    protected static String defaultExpression(String dataType, String defaultValue) {
        String def = normalizeDefaultValue(defaultValue);
        if (StringUtils.isBlank(def)
                || isQuotedDefault(def)
                || isNumericDefault(def)
                || isKeywordOrFunctionDefault(def)) {
            return def;
        }
        if (isStringOrTemporalType(dataType)) {
            return "'" + DatabaseUtil.escapeSingleQuotes(def) + "'";
        }
        return def;
    }
    private static String normalizeDefaultValue(String defaultValue) {
        String def = defaultValue == null ? "" : defaultValue.trim();
        while (def.length() >= 2 && def.startsWith("(") && def.endsWith(")")) {
            def = def.substring(1, def.length() - 1).trim();
        }
        return def;
    }

    private static boolean isQuotedDefault(String defaultValue) {
        return defaultValue.length() >= 2 && defaultValue.startsWith("'") && defaultValue.endsWith("'");
    }

    private static boolean isNumericDefault(String defaultValue) {
        return defaultValue.matches("^-?\\d+(\\.\\d+)?$");
    }

    private static boolean isKeywordOrFunctionDefault(String defaultValue) {
        String def = defaultValue.trim().toUpperCase(Locale.ROOT);
        return "CURRENT_TIMESTAMP".equals(def)
                || "CURRENT_DATE".equals(def)
                || "CURRENT_TIME".equals(def)
                || "SYSDATE".equals(def)
                || "SYSTIMESTAMP".equals(def)
                || "NULL".equals(def)
                || "TRUE".equals(def)
                || "FALSE".equals(def)
                || (def.endsWith(")") && def.contains("("));
    }

    private static boolean isStringOrTemporalType(String dataType) {
        String type = dataType == null ? "" : dataType.trim().toUpperCase(Locale.ROOT);
        return type.contains("CHAR")
                || type.contains("TEXT")
                || type.contains("CLOB")
                || type.contains("STRING")
                || type.contains("DATE")
                || type.contains("TIME");
    }

    @Override
    public List<String> updateColKey(DbQueryProperty dbQueryProperty, String tableName, List<DbColumn> colKeyDbColumnList) {
        List<String> sqlList = new ArrayList<>();
        String fullTableName = getTableName(dbQueryProperty, tableName);

        // 首先删除现有的主键约束
        sqlList.add("ALTER TABLE " + fullTableName + " DROP PRIMARY KEY CASCADE");

        // 如果提供了新的主键字段列表，则添加新的主键约束
        if (colKeyDbColumnList != null && !colKeyDbColumnList.isEmpty()) {
            StringBuilder addSql = new StringBuilder();
            addSql.append("ALTER TABLE ").append(fullTableName).append(" ADD CONSTRAINT ");

            // 生成约束名：表名_主键字段组合
            StringBuilder constraintName = new StringBuilder();
            constraintName.append(tableName);
            for (DbColumn col : colKeyDbColumnList) {
                constraintName.append("_").append(col.getColName());
            }
            // 达梦约束名长度限制为128个字符
            String finalConstraintName = constraintName.length() > 128 ?
                    constraintName.substring(0, 128) : constraintName.toString();

            addSql.append(quoteIdentifier(finalConstraintName));
            addSql.append(" PRIMARY KEY (");
            for (int i = 0; i < colKeyDbColumnList.size(); i++) {
                if (i > 0) {
                    addSql.append(", ");
                }
                addSql.append(quoteIdentifier(colKeyDbColumnList.get(i).getColName()));
            }
            addSql.append(")");
            sqlList.add(addSql.toString());
        }

        return sqlList;
    }

    @Override
    public String getColumnType(DbColumn column) {
        String columnType = mapOracleColumnType(column);
        if (columnType.indexOf("(") > 0) {
            return columnType.substring(0, columnType.lastIndexOf("("));
        }
        return columnType;
    }

    @Override
    public String getTableName(DbQueryProperty property, String tableName) {
        if (!org.springframework.util.StringUtils.isEmpty(property.getDbName())) {
            return quoteIdentifier(normalizeOracleIdentifier(property.getDbName()))
                    + "." + quoteIdentifier(normalizeOracleIdentifier(tableName));
        }
        return quoteIdentifier(normalizeOracleIdentifier(tableName));
    }

    @Override
    public DbColumn convertLogicalColumn(DbColumn column) {
        DbColumn converted = super.convertLogicalColumn(column);
        if ("TEXT".equalsIgnoreCase(column.getDataType())) {
            converted.setDataType("CLOB");
        }
        if ("INTEGER".equalsIgnoreCase(column.getDataType())) {
            converted.setDataType("NUMBER");
            converted.setDataLength("19");
            converted.setDataScale("0");
        } else if ("BOOLEAN".equalsIgnoreCase(column.getDataType())) {
            converted.setDataType("NUMBER");
            converted.setDataLength("1");
            converted.setDataScale("0");
        }
        return converted;
    }

    @Override
    public DbColumn toStandardColumn(DbColumn column) {
        String type = normalizeTypeName(column == null ? null : column.getDataType());
        switch (type) {
            case "CHAR":
            case "NCHAR":
                return toStandardColumn(column, "CHAR");
            case "VARCHAR":
            case "VARCHAR2":
            case "NVARCHAR2":
                return toStandardColumn(column, "VARCHAR2");
            case "CLOB":
            case "NCLOB":
            case "LONG":
            case "XMLTYPE":
                return toStandardColumn(column, "TEXT");
            case "NUMBER":
            case "DECIMAL":
            case "NUMERIC":
            case "INTEGER":
            case "INT":
            case "SMALLINT":
                return toStandardColumn(column, "NUMBER");
            case "FLOAT":
            case "BINARY_FLOAT":
                return toStandardColumn(column, "FLOAT");
            case "BINARY_DOUBLE":
            case "DOUBLE":
                return toStandardColumn(column, "DOUBLE");
            case "DATE":
                return toStandardColumn(column, "DATETIME");
            case "TIMESTAMP":
            case "TIMESTAMP WITH TIME ZONE":
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                return toStandardColumn(column, "DATETIME");
            case "RAW":
            case "LONG RAW":
            case "BLOB":
                return toStandardColumn(column, "BLOB");
            default:
                return super.toStandardColumn(column);
        }
    }

    protected static String buildOracleCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName) {
        String owner = resolveOracleSchema(dbQueryProperty);
        String normalizedTableName = normalizeOracleIdentifier(tableName);
        String escapedTableName = DatabaseUtil.escapeSingleQuotes(normalizedTableName);
        if (StringUtils.isBlank(owner)) {
            return "SELECT COUNT(*) FROM user_tables WHERE table_name = '" + escapedTableName + "'";
        }
        String escapedOwner = DatabaseUtil.escapeSingleQuotes(owner);
        return "SELECT COUNT(*) FROM all_tables WHERE owner = '" + escapedOwner
                + "' AND table_name = '" + escapedTableName + "'";
    }

    private static String normalizeOracleIdentifier(String identifier) {
        if (identifier == null) {
            return "";
        }
        String value = identifier.trim();
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex > -1) {
            value = value.substring(dotIndex + 1).trim();
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalizeOracleTypeName(String dataType) {
        if (dataType == null) {
            return "";
        }
        String type = dataType.trim();
        int argumentIndex = type.indexOf('(');
        if (argumentIndex > -1) {
            type = type.substring(0, argumentIndex);
        }
        return type.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    @Override
    public String getSchemaName(DbQueryProperty property) {
        return resolveOracleSchema(property);
    }

    private static String resolveOracleSchema(DbQueryProperty property) {
        // 未指定owner时沿用Oracle当前会话Schema，不把连接服务或登录名当成显式owner。
        return StringUtils.isBlank(property.getDbName()) ? null
                : normalizeOracleIdentifier(property.getDbName());
    }

    @Override
    public String castToText(String expression) {
        return "TO_CHAR(" + expression + ")";
    }
}
