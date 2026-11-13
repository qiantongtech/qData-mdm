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
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbIndex;
import tech.qiantong.qdata.mdm.common.database.core.DbName;
import tech.qiantong.qdata.mdm.common.database.exception.DataQueryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialect abstract class
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
@Slf4j
public abstract class AbstractDbDialect implements DbDialect {

    /**
     * 逻辑类型仅在这个新入口转换，原来的原生INTEGER仍按原方法处理。
     * 旧方言生成器使用dataLength保存小数总位数，因此在副本中同步精度。
     */
    @Override
    public DbColumn convertLogicalColumn(DbColumn column) {
        if (column == null || column.getDataType() == null) {
            throw new IllegalArgumentException("逻辑字段和类型不能为空");
        }
        DbColumn converted = copyColumn(column);
        String type = column.getDataType().trim().toUpperCase(Locale.ROOT);
        switch (type) {
            case "STRING":
                converted.setDataType("VARCHAR");
                break;
            case "INTEGER":
                converted.setDataType("BIGINT");
                converted.setDataLength(null);
                converted.setDataPrecision(null);
                converted.setDataScale(null);
                break;
            case "DECIMAL":
                converted.setDataType("DECIMAL");
                if (column.getDataPrecision() != null && !column.getDataPrecision().isEmpty()) {
                    converted.setDataLength(column.getDataPrecision());
                }
                break;
            case "BOOLEAN":
                converted.setDataType("SMALLINT");
                // 使用数字布尔列的方言需要数字默认值，业务含义保持不变。
                if ("true".equalsIgnoreCase(column.getDataDefault())) {
                    converted.setDataDefault("1");
                } else if ("false".equalsIgnoreCase(column.getDataDefault())) {
                    converted.setDataDefault("0");
                }
                converted.setDataLength(null);
                converted.setDataPrecision(null);
                converted.setDataScale(null);
                break;
            case "TEXT":
                converted.setDataType(type);
                if (column.getDataDefault() != null && !column.getDataDefault().isEmpty()) {
                    converted.setDataDefault("'" + column.getDataDefault().replace("'", "''") + "'");
                }
                break;
            case "DATE":
            case "DATETIME":
                converted.setDataType(type);
                break;
            default:
                throw new IllegalArgumentException("无效逻辑字段类型：" + column.getDataType());
        }
        return converted;
    }

    @Override
    public String identityColumnClause(DbColumn column) {
        if (!Boolean.TRUE.equals(column.getIdentityFlag())) return "";
        DbColumn normalized = toStandardColumn(column);
        String type = normalizeTypeName(normalized.getDataType());
        if (!type.matches("TINYINT|SMALLINT|INT|INTEGER|BIGINT|NUMBER|NUMERIC|DECIMAL")
                || (normalized.getDataScale() != null && !normalized.getDataScale().trim().matches("0+"))) {
            throw new tech.qiantong.qdata.mdm.common.exception.ServiceException("自增列必须使用整数类型");
        }
        if (Boolean.TRUE.equals(column.getNullable())
                || (column.getDataDefault() != null && !column.getDataDefault().trim().isEmpty())) {
            throw new tech.qiantong.qdata.mdm.common.exception.ServiceException("自增列不能允许空值或指定默认值");
        }
        return identityColumnClause();
    }

    /**
     * 子类只提供本数据库的原生自增语法。
     */
    protected String identityColumnClause() {
        throw new tech.qiantong.qdata.mdm.common.exception.ServiceException("当前目标数据库不支持自增列");
    }

    @Override
    public String columns(String dbName, String tableName) {
        return "select column_name AS COLNAME, ordinal_position AS COLPOSITION, column_default AS DATADEFAULT, is_nullable AS NULLABLE, data_type AS DATATYPE, " +
                "character_maximum_length AS DATALENGTH, numeric_precision AS DATAPRECISION, numeric_scale AS DATASCALE, column_key AS COLKEY, column_comment AS COLCOMMENT " +
                "from information_schema.columns where table_schema = '" + dbName + "' and table_name = '" + tableName + "' order by ordinal_position ";
    }

    @Override
    public String tables(String dbName) {
        return "SELECT table_name AS TABLENAME, table_comment AS TABLECOMMENT FROM information_schema.tables where table_schema = '" + dbName + "' ";
    }

    @Override
    public String getPkColumnNames(DbQueryProperty dbQueryProperty, String tableName) {
        return "";
    }

    @Override
    public String getPkColumnNames(DbQueryProperty dbQueryProperty) {
        return "";
    }

    @Override
    public String buildTableNameByDbType(DbQueryProperty dbQueryProperty, String tableName) {
        return tableName;
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long count) {
        // Get the actual number of pagination items
        StringBuilder sqlBuilder = new StringBuilder(originalSql);
        sqlBuilder.append(" LIMIT ").append(offset).append(" , ").append(count);
        return sqlBuilder.toString();
    }

    @Override
    public String count(String sql) {
        return "SELECT COUNT(*) FROM ( " + sql + " ) TEMP";
    }

    @Override
    public String countNew(String tableName, Map<String, Object> params) {
        // Dynamically constructing WHERE clauses
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);
        if (params != null && !params.isEmpty()) {
            countSql.append(buildWhereClause(params));
        }
        return countSql.toString();
    }

    /**
     * Verify connection
     *
     * @param dataSource
     * @param dbQueryProperty
     * @return
     */
    @Override
    public Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(0);
        } catch (SQLException e) {
            log.error("Database connection failed; retry later", e);
            throw new DataQueryException("db.error.connection.retry", "Database connection failed, please try again later");
        }
    }

    /**
     * Dynamically constructing WHERE clauses
     *
     * @param params parameter Map
     * @return WHERE clause string
     */
    private static String buildWhereClause(Map<String, Object> params) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        for (String key : params.keySet()) {
            whereClause.append(" AND ").append(key).append(" = :").append(key);
        }
        return whereClause.toString();
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        String url = DbType.getDbType(property.getDbType()).getUrl();
        if (StringUtils.isEmpty(url)) {
            throw new DataQueryException("db.error.invalid.dbtype", "Invalid database type");
        }
        url = url.replace("${host}", property.getHost());
        url = url.replace("${port}", String.valueOf(property.getPort()));
        url = url.replace("${dbName}", property.getDbName());
        return url;
    }

    @Override
    public String getFlinkCDCSQL(DbQueryProperty property, String flinkTableName, String tableName, String tableFieldName) {
        return null;
    }

    @Override
    public String getFlinkSQL(DbQueryProperty property, String flinkTableName, String tableName, String tableFieldName) {
        return null;
    }

    @Override
    public String getTableName(DbQueryProperty property, String tableName) {
        if (!StringUtils.isEmpty(property.getDbName())) {
            return property.getDbName() + "." + tableName;
        }
        return tableName;
    }

    @Override
    public String getFlinkSinkSQL(DbQueryProperty property, JSONObject config, String flinkTableName, String tableName, String tableFieldName) {
        return null;
    }

    @Override
    public String getDbName(DbName dbName) {
        return null;
    }

    @Override
    public String getDbColumns(DbQueryProperty property) {
        return null;
    }

    /**
     * 通用关系型索引语法，表名和每一列分别交给当前方言处理。
     */
    @Override
    public String generateCreateIndexSQL(DbQueryProperty property, String tableName, DbIndex index) {
        if (index == null || Boolean.TRUE.equals(index.getPrimaryKey())
                || index.getColumnNames() == null || index.getColumnNames().isEmpty()) {
            throw new IllegalArgumentException("请提供普通或唯一索引及索引列，主键由建表处理");
        }
        requireIndexIdentifier(index.getIndexName());
        requireIndexIdentifier(tableName);
        List<String> columns = new ArrayList<>();
        for (String column : index.getColumnNames()) {
            requireIndexIdentifier(column);
            columns.add(quoteIdentifier(column));
        }
        return "CREATE " + (Boolean.TRUE.equals(index.getUnique()) ? "UNIQUE " : "")
                + "INDEX " + quoteIdentifier(index.getIndexName()) + " ON "
                + getTableName(property, tableName) + " (" + String.join(", ", columns) + ")";
    }

    /**
     * 独立索引按所属表的Schema定位，防止撤销到其他Schema的同名索引。
     */
    @Override
    public String generateDropIndexSQL(DbQueryProperty property, String tableName, String indexName) {
        requireIndexIdentifier(tableName);
        requireIndexIdentifier(indexName);
        String table = getTableName(property, tableName);
        int separator = table.lastIndexOf('.');
        String prefix = separator < 0 ? "" : table.substring(0, separator + 1);
        return "DROP INDEX " + prefix + quoteIdentifier(indexName);
    }

    @Override
    public String generateDropTableSQL(DbQueryProperty dbQueryProperty, String tableName) {
        return "DROP TABLE " + tableName;
    }

    /**
     * Check if SSL is used
     *
     * @param property
     * @return
     */
    protected Boolean checkUseSSL(DbQueryProperty property) {
        if (property.getDatasourceConfig().containsKey("useSSL")) {
            Integer useSSL = (Integer) property.getDatasourceConfig().get("useSSL");
            if (useSSL == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if Kerberos is used
     *
     * @param property
     * @return
     */
    protected static Boolean checkUseKerberos(DbQueryProperty property) {
        if (property.getDatasourceConfig().containsKey("useKerberos")) {
            Integer useSSL = (Integer) property.getDatasourceConfig().get("useKerberos");
            if (useSSL == 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String table(DbQueryProperty dbQueryProperty,String tableName) {
        return null;
    }

    @Override
    public DbColumn toStandardColumn(DbColumn column) {
        if (column == null) {
            return null;
        }
        DbColumn standardColumn = copyColumn(column);
        fillTypeArgumentsFromExpression(standardColumn);
        normalizeTypeArguments(standardColumn);
        return standardColumn;
    }
    @Override
    public String updateTableComment(DbQueryProperty dbQueryProperty, String tableName, String tableComment) {
        return null;
    }

    @Override
    public List<String> addColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column) {
        return null;
    }

    @Override
    public List<String> modifyColumn(DbQueryProperty dbQueryProperty, String tableName, DbColumn column) {
        return null;
    }

    @Override
    public String dropColumn(DbQueryProperty dbQueryProperty, String tableName, String colName) {
        return null;
    }

    @Override
    public List<String> updateColKey(DbQueryProperty dbQueryProperty, String tableName, List<DbColumn> colKeyDbColumnList) {
        return null;
    }

    @Override
    public String getColumnType(DbColumn column) {
        return null;
    }

    protected DbColumn toStandardColumn(DbColumn column, String standardType) {
        if (column == null) {
            return null;
        }
        DbColumn standardColumn = copyColumn(column);
        fillTypeArgumentsFromExpression(standardColumn);
        standardColumn.setDataType(standardType);
        normalizeTypeArguments(standardColumn);
        return standardColumn;
    }

    protected DbColumn copyColumn(DbColumn sourceColumn) {
        if (sourceColumn == null) {
            return null;
        }
        DbColumn column = new DbColumn();
        column.setTableName(sourceColumn.getTableName());
        column.setColName(sourceColumn.getColName());
        column.setDataType(sourceColumn.getDataType());
        column.setDataLength(sourceColumn.getDataLength());
        column.setDataPrecision(sourceColumn.getDataPrecision());
        column.setDataScale(sourceColumn.getDataScale());
        column.setColKey(sourceColumn.getColKey());
        column.setIdentityFlag(sourceColumn.getIdentityFlag());
        column.setNullable(sourceColumn.getNullable());
        column.setDataDefault(sourceColumn.getDataDefault());
        column.setColComment(sourceColumn.getColComment());
        column.setColPosition(sourceColumn.getColPosition());
        return column;
    }

    protected void fillTypeArgumentsFromExpression(DbColumn column) {
        if (column == null || column.getDataType() == null) {
            return;
        }
        String dataType = column.getDataType();
        int startIndex = dataType.indexOf('(');
        int endIndex = dataType.lastIndexOf(')');
        if (startIndex < 0 || endIndex <= startIndex) {
            return;
        }
        String[] arguments = dataType.substring(startIndex + 1, endIndex).split(",");
        if (arguments.length > 0 && !org.apache.commons.lang3.StringUtils.isNotBlank(column.getDataLength())) {
            column.setDataLength(arguments[0].trim());
        }
        if (arguments.length > 1 && !org.apache.commons.lang3.StringUtils.isNotBlank(column.getDataScale())) {
            column.setDataScale(arguments[1].trim());
        }
    }

    protected void normalizeTypeArguments(DbColumn column) {
        if (column == null) {
            return;
        }
        String standardType = normalizeTypeName(column.getDataType());
        if ("NUMBER".equals(standardType) || "DECIMAL".equals(standardType) || "NUMERIC".equals(standardType)) {
            if (!org.apache.commons.lang3.StringUtils.isNotBlank(column.getDataLength())
                    && org.apache.commons.lang3.StringUtils.isNotBlank(column.getDataPrecision())) {
                column.setDataLength(column.getDataPrecision());
            }
        }
        if (("TEXT".equals(standardType) || "CLOB".equals(standardType) || "BLOB".equals(standardType))
                && !org.apache.commons.lang3.StringUtils.isNotBlank(column.getDataLength())) {
            column.setDataLength(null);
        }
    }

    protected String normalizeTypeName(String dataType) {
        if (dataType == null) {
            return "";
        }
        String normalized = dataType.trim();
        int argumentIndex = normalized.indexOf('(');
        if (argumentIndex > -1) {
            normalized = normalized.substring(0, argumentIndex);
        }
        return normalized.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /**
     * 新索引接口只接收单个标识符，不能混入SQL表达式或已拼好的限定名称。
     */
    protected void requireIndexIdentifier(String identifier) {
        if (identifier == null || !identifier.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
            throw new IllegalArgumentException("索引、表或列名称不合法：" + identifier);
        }
    }
}
