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
import java.io.OutputStream;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.*;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Table data query interface
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
public interface DbQuery {

    /** 在指定连接上执行参数化更新，连接和事务生命周期由调用方控制。 */
    int update(Connection connection, String sql, Object[] args);

    /** 使用调用方行映射，保留数值精度和 JDBC 时间类型。 */
    default <T> List<T> queryList(String sql, Object[] args, org.springframework.jdbc.core.RowMapper<T> mapper) {
        throw new UnsupportedOperationException("当前数据源不支持 JDBC 行映射查询");
    }

    /** 命名参数查询保留调用方行映射，支持集合参数。 */
    default <T> List<T> queryListWithMapper(String sql, Map<String, Object> params,
                                         org.springframework.jdbc.core.RowMapper<T> mapper) {
        throw new UnsupportedOperationException("当前数据源不支持 JDBC 命名参数行映射查询");
    }

    /**
     * 将平台逻辑字段转成当前方言建表、加列可接收的独立副本，不修改原对象。
     */
    default <T> T query(String sql, Object[] args, org.springframework.jdbc.core.ResultSetExtractor<T> extractor) {
        throw new UnsupportedOperationException("当前数据源不支持 JDBC 结果查询");
    }

    /**
     * 参数化更新支持 SqlParameterValue 指定实际 JDBC 类型，返回驱动报告的影响行数。
     * 不建立独立事务；多步事务由调用方保证操作绑定在同一事务连接。
     */
    default int update(String sql, Object[] args) {
        throw new UnsupportedOperationException("当前数据源不支持 JDBC 参数化更新");
    }

    DbColumn convertLogicalColumn(DbColumn column);

    /**
     * 生成普通或唯一索引SQL；主键继续由原建表方法处理。
     */
    String generateCreateIndexSQL(DbQueryProperty property, String tableName, DbIndex index);

    /**
     * 只生成撤销索引SQL，是否允许撤销由业务调用方判断。
     */
    String generateDropIndexSQL(DbQueryProperty property, String tableName, String indexName);

    /**
     * 读取真实主键；查询成功且没有主键时返回null。
     */
    DbIndex getTablePrimaryKey(DbQueryProperty property, String tableName);

    /**
     * 读取真实索引及有序列；读取失败抛异常，不返回伪造的空结果。
     */
    List<DbIndex> getTableIndexes(DbQueryProperty property, String tableName);

    /**
     * Get database connection
     */
    Connection getConnection();

    /**
     * Check connectivity
     */
    boolean valid();

    /**
     * Close data source
     */
    void close();

    /**
     * Get all column list of the specified table
     *
     * @param dbName
     * @param tableName
     * @return
     */
    List<DbColumn> getTableColumns(String dbName, String tableName);

    List<DbColumn> getTableColumns(DbQueryProperty dbQueryProperty, String tableName);

    /**
     * Get all column information under the database
     * @param dbQueryProperty
     * @return
     */
    List<DbColumn> getDbColumns(DbQueryProperty dbQueryProperty);

    int generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName);

    List<String> generateCreateTableSQL(DbQueryProperty dbQueryProperty, String tableName, String tableComment,
                                        List<DbColumn> dbColumnList);

    int createCollectionWithSchema(DbQueryProperty dbQueryProperty, String tableName, String tableComment,
                                   List<DbColumn> dbColumnList);

    /**
     * Get all table information in the specified database
     *
     * @param dbName
     * @return
     */
    List<DbTable> getTables(String dbName);

    List<DbTable> getTables(DbQueryProperty dbQueryProperty);

    List<DbName> getDbNames(DbName dbName);

    /**
     * Get total count
     *
     * @param sql
     * @return
     */
    int count(String sql);

    /**
     * Execute SQL directly to get statistics count
     *
     * @param sql
     * @return
     */
    int executeCountSql(String sql);

    /**
     * Get total count with query parameters
     *
     * @param sql
     * @return
     */
    int count(String sql, Object[] args);

    /**
     * Get total count with query parameters using NamedParameterJdbcTemplate
     *
     * @param sql
     * @return
     */
    int count(String sql, Map<String, Object> params);

    int countNew(String sql, Map<String, Object> params);

    int countNew(String tableName, DbQueryProperty dbQueryProperty, String where);

    /**
     * Query result list
     *
     * @param sql
     * @return
     */
    List<Map<String, Object>> queryList(String sql);

    List<Map<String, Object>> queryDbColumnByList(List<DbColumn> columns, String tableName, DbQueryProperty dbQueryProperty, long offset, long size);

    List<Map<String, Object>> queryDbColumnByList(
            List<DbColumn> columns
            , String tableName
            , DbQueryProperty dbQueryProperty
            , String where
            , List<Map> orderByList
            , long offset
            , long size
    );

    /**
     * Query result list with query parameters
     *
     * @param sql
     * @param args
     * @return
     */
    List<Map<String, Object>> queryList(String sql, Object[] args);

    /**
     * Query result list with query parameters
     *
     * @param sql
     * @param params
     * @param cache  Whether to enable cache 0 No 1 Yes
     * @return
     */
    List<Map<String, Object>> queryList(String sql, Map<String, Object> params, Integer cache);

    /**
     * Query detail result with query parameters
     *
     * @param sql
     * @param params
     * @param cache  Whether to enable cache 0 No 1 Yes
     * @return
     */
    Map<String, Object> queryOne(String sql, Map<String, Object> params, Integer cache);

    /**
     * Paginated query results
     *
     * @param sql
     * @param offset
     * @param size
     * @return
     */
    PageResult<Map<String, Object>> queryByPage(String sql, long offset, long size);

    /**
     * Paginated query results with query parameters
     *
     * @param sql
     * @param args
     * @param offset
     * @param size
     * @return
     */
    PageResult<Map<String, Object>> queryByPage(String sql, Object[] args, long offset, long size);

    /**
     * Paginated query results with query parameters using NamedParameterJdbcTemplate
     *
     * @param sql
     * @param params
     * @param offset
     * @param size
     * @param cache  Whether to enable cache 0 No 1 Yes
     * @return
     */
    PageResult<Map<String, Object>> queryByPage(String sql, Map<String, Object> params, long offset, long size,
                                                Integer cache);

    int update(String sql);

    int addTableData(String tableName, Map<String, Object> after);

    int updateTableData(Map<String, Object> after,
                        List<String> setCols,
                        List<String> whereCols,
                        String tableName);

    void execute(String sql);

    int[] batchUpdate(String sql);

    int isTableExists(String sql);

    /**
     * Get storage size
     *
     * @return
     */
    Integer getDataStorageSize();

    /**
     * Create a new table based on an existing table
     *
     * @param dbQueryProperty
     * @param tableName
     * @param newTableName
     * @return
     */
    Boolean copyTable(Connection conn, DbQueryProperty dbQueryProperty, String tableName, String newTableName);

    /**
     * Create a table from one table to another database
     *
     * @param otherDbQueryProperty
     * @param tableName
     * @param newTableName
     * @param addColumn            Additional columns, nullable
     * @return
     */

    String getInsertOrUpdateSql(DbQueryProperty property, String tableName, List<String> selectedColumns, List<String> column);

    /**
     * Query columns by SQL
     *
     * @param querySql
     * @return
     */
    List<DbColumn> getColumnsByQuerySql(String querySql);

    default void downloadFile(String path, OutputStream outputStream) {
        throw new ServiceException("当前数据源不支持下载文件");
    }

    default void deleteFile(String path, Boolean directory) {
        throw new ServiceException("当前数据源不支持删除文件");
    }

    default void renameFile(String oldPath, String newPath) {
        throw new ServiceException("当前数据源不支持重命名文件");
    }

    default void createDirectory(String path) {
        throw new ServiceException("当前数据源不支持新建文件夹");
    }

    default Long getRemainingStorage() {
        return null;
    }

    /**
     * Get Flink fields
     *
     * @param dbQueryProperty Database connection information
     * @param config          Additional extension parameters
     * @param tableName
     * @param column
     * @param querySql        When this parameter is not empty, tableName is ignored
     * @return
     */
    String generateFlinkFields(DbQueryProperty dbQueryProperty, JSONObject config, String tableName, List<String> column, String querySql);

    /**
     * Get Flink SQL
     *
     * @param taskExecuteType Execution type: STREAM streaming mode, BATCH batch processing
     * @param flinkTableName  Flink table name
     * @param tableName       Real table name
     * @param column          Columns
     * @param querySql
     * @return
     */
    String getFlinkSQL(JSONObject config, String taskExecuteType, String flinkTableName, String tableName, List<String> column, String querySql);

    /**
     * Get Flink CDC SQL
     *
     * @param flinkTableName Flink table name
     * @param tableName      Real table name
     * @param column         Columns
     * @param querySql
     * @return
     */
    String getFlinkCDCSQL(JSONObject config, String flinkTableName, String tableName, List<String> column, String querySql);

    /**
     * Get Flink sink SQL
     *
     * @param taskExecuteType Execution type: STREAM streaming mode, BATCH batch processing
     * @param flinkTableName  Flink table name
     * @param tableName       Real table name
     * @param column          Columns
     * @return
     */
    String getFlinkSinkSQL(JSONObject config, String taskExecuteType, String flinkTableName, String tableName, List<String> column);

    /**
     * Delete table
     *
     * @param dbQueryProperty
     * @param tableName
     * @return
     */
    Boolean deleteTable(DbQueryProperty dbQueryProperty, String tableName);

    /**
     * Update table SQL
     * @param dbQueryProperty
     * @param tableName
     * @param tableComment
     * @param dbColumnList
     * @return
     */
    List<String> generateUpdateTableSQL(DbQueryProperty dbQueryProperty, String tableName, String tableComment,
                                        List<DbColumn> dbColumnList);
}
