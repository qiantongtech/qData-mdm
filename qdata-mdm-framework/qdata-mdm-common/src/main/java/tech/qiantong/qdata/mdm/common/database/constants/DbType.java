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

package tech.qiantong.qdata.mdm.common.database.constants;

import tech.qiantong.qdata.mdm.common.database.exception.DataQueryException;

/**
 * Database type
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
public enum DbType {

    /**
     * MYSQL
     */
    MYSQL("MySql",
            "MySql数据库",
            "jdbc:mysql://${host}:${port}/${dbName}?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT ({pageNo}-1)*{pageSize},{pageSize}"),
    /**
     * MARIADB
     */
    MARIADB("2",
            "MariaDB数据库",
            "jdbc:mariadb://${host}:${port}/${dbName}",
            "CHAR_LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT ({pageNo}-1)*{pageSize},{pageSize}"),
    /**
     * ORACLE
     */
    ORACLE("Oracle11",
            "Oracle11g及以下数据库",
            "jdbc:oracle:thin:@${host}:${port}:${sid}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT *  FROM (SELECT a.*,ROWNUM rnum FROM(SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC) a WHERE ROWNUM <= ( {pageNo}- 1 ) * {pageSize} + {pageSize}) WHERE rnum > ( {pageNo} - 1 ) * {pageSize}"),
    /**
     * oracle12c new pagination
     */
    ORACLE_12C("Oracle",
            "Oracle12c+数据库",
            "jdbc:oracle:thin:@${host}:${port}:${sid}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC OFFSET {pageNo} ROWS FETCH NEXT {pageSize} ROWS ONLY"),
    /**
     * POSTGRESQL
     */
    POSTGRE_SQL("PostgreSQL",
            "PostgreSQL数据库",
            "jdbc:postgresql://${host}:${port}/${dbName}?stringtype=unspecified",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT {pageSize} OFFSET ({pageNo}-1)*{pageSize}"),
    /**
     * SQLServer 2008 and below
     */
    SQL_SERVER2008("SQL_Server2008",
            "SQLServer2008及以下数据库",
            "jdbc:jtds:sqlserver://${host}:${port}/${dbName};ssl=off",
            "LEN",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC OFFSET {pageNo} ROWS FETCH NEXT {pageSize} ROWS ONLY"),
    /**
     * SQLSERVER
     */
    SQL_SERVER("SQL_Server",
            "SQLServer2012+数据库",
            "jdbc:sqlserver://${host}:${port};DatabaseName=${dbName};encrypt=false;trustServerCertificate=true",
            "LEN",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC OFFSET {pageNo} ROWS FETCH NEXT {pageSize} ROWS ONLY"),

    /**
     * DM8 (Dameng8)
     */
    DM8("DM8",
            "达梦8",
            "jdbc:dm://${host}:${port}?schema=${dbName}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} LIMIT ({pageNo}-1)*{pageSize},{pageSize}"),
    /**
     * KingbaseES database
     */
    KINGBASE8("Kingbase8",
            "人大金仓数据库",
            "jdbc:kingbase8://${host}:${port}/${dbName}?stringtype=unspecified",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT {pageSize} OFFSET ({pageNo}-1)*{pageSize} "),

    /**
     * OSCAR database (official Java Driver)
     */
    OSCAR("OSCAR",
            "神通数据库",
            "jdbc:oscar://${host}:${port}/${dbName}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT ({pageNo}-1)*{pageSize},{pageSize}"),

    /**
     * DB2 idm
     */
    DB2("DB2",
            "DB2",
            "jdbc:db2://${host}:${port}/${dbName}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (ORDER BY 1) AS rownum FROM {tableName} ORDER BY {orderBy} DESC) AS temp_table WHERE rownum BETWEEN  ( {pageNo} - 1 ) * {pageSize} AND ( {pageNo} - 1 ) * {pageSize} + {pageSize})"),

    OPEN_GAUSS("openGauss",
            "openGauss数据库",
            "jdbc:opengauss://${host}:${port}/${dbName}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT {pageSize} OFFSET ({pageNo}-1)*{pageSize}"),
    GBASE8C("GBase 8c",
            "GBase 8c数据库",
            "jdbc:gbase8c://${host}:${port}/${dbName}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT {pageSize} OFFSET ({pageNo}-1)*{pageSize}"),
    GAUSS_DB("GaussDB",
            "GaussDB(DWS)数据库",
            "jdbc:gaussdb://${host}:${port}/${dbName}",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT {pageSize} OFFSET ({pageNo}-1)*{pageSize}"),
    GBase("GBase",
            "GBase 8s",
            "jdbc:gbasedbt-sqli://${host}:${port}/${dbName}:GBASEDBTSERVER=gbase01;DB_LOCALE=zh_CN.utf8;CLIENT_LOCALE=zh_CN.utf8;DELIMIDENT=y;IGNORECASE=TRUE",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} SKIP ({pageNo}-1)*{pageSize} LIMIT {pageSize} "),

    OCEAN_BASE("OCEANBASE",
            "OceanBase分布式数据库【MySQL兼容租户】",
            "jdbc:oceanbase://${host}:${port}/${dbName}?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useSSL=false",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT ({pageNo}-1)*{pageSize},{pageSize}"),
    OCEAN_BASE_ORACLE("OCEANBASE_ORACLE",
            "OceanBase分布式数据库【Oracle兼容租户】",
            "jdbc:oceanbase:oracle://${host}:${port}/${dbName}?useUnicode=true&characterEncoding=utf8&useSSL=false",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC OFFSET {pageNo} ROWS FETCH NEXT {pageSize} ROWS ONLY"),
    TIDB("TIDB",
            "TiDB分布式数据库",
            "jdbc:mysql://${host}:${port}/${dbName}?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useSSL=false&serverTimezone=Asia/Shanghai",
            "LENGTH",
            "SELECT COUNT(1) FROM {tableName}",
            "SELECT {tableFieldName} FROM {tableName} ORDER BY {orderBy} DESC LIMIT ({pageNo}-1)*{pageSize},{pageSize}");

    /**
     * Database name
     */
    private final String db;

    /**
     * Description
     */
    private final String desc;

    /**
     * url
     */
    private final String url;

    /**
     * lengthFun
     */
    private final String lengthFun;

    /**
     * Count query
     */
    private String selectCount;

    /**
     * Pagination query
     */
    private String selectPage;

    public String getLengthFun() {
        return lengthFun;
    }

    public String getDb() {
        return this.db;
    }

    public String getDesc() {
        return this.desc;
    }

    public String getUrl() {
        return this.url;
    }

    public String getSelectCount() {
        return this.selectCount;
    }

    public String getSelectPage() {
        return this.selectPage;
    }

    DbType(String db, String desc, String url, String lengthFun, String selectCount, String selectPage) {
        this.db = db;
        this.desc = desc;
        this.url = url;
        this.lengthFun = lengthFun;
        this.selectCount = selectCount;
        this.selectPage = selectPage;
    }

    /**
     * Get database type
     *
     * @param dbType Database type string
     */
    public static DbType getDbType(String dbType) {
        for (DbType type : DbType.values()) {
            if (type.db.equals(dbType)) {
                return type;
            }
        }
        throw new DataQueryException("db.error.unsupported.dbtype", "Unsupported database type");
    }
}
