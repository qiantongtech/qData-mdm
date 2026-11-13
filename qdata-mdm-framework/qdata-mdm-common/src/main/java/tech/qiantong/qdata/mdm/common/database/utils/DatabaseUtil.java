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

package tech.qiantong.qdata.mdm.common.database.utils;

import org.apache.commons.collections4.MapUtils;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.MessageUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * Data connection tool methods
 */
public class DatabaseUtil {

    /** 数字物理列的 Boolean 参数转为 0/1，原生布尔列、未知类型及其他值保持原样。 */
    public static Object convertBooleanParameter(String physicalType, Object value) {
        if (!(value instanceof Boolean) || physicalType == null) {
            return value;
        }
        String type = physicalType.trim();
        int argumentIndex = type.indexOf('(');
        if (argumentIndex >= 0) {
            type = type.substring(0, argumentIndex);
        }
        switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "TINYINT":
            case "SMALLINT":
            case "INT2":
            case "INT":
            case "INTEGER":
            case "INT4":
            case "BIGINT":
            case "INT8":
            case "NUMBER":
            case "NUMERIC":
            case "DECIMAL":
                return (short) ((Boolean) value ? 1 : 0);
            default:
                return value;
        }
    }

    /**
     * Convert a string to type Long. If the string is empty or cannot be converted, 0L is returned.
     *
     * @param dataLength The string to be converted
     * @return converted Long type value
     */
    public static Long getStringToLong(String dataLength) {
        if (StringUtils.isEmpty(dataLength)) {
            return 0L;
        }
        try {
            return Long.parseLong(dataLength);
        } catch (NumberFormatException e) {
            // If the conversion fails, 0L is returned
            return 0L;
        }
    }

    /**
     * Escape single quotes in strings to avoid errors when splicing SQL
     */
    public static String escapeSingleQuotes(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "''");
    }

    /**
     * If the input string is all lowercase, it will be converted to uppercase and returned; otherwise, the original string will be returned directly.
     *
     * @param input input string
     * @return If it is all lowercase, return all uppercase string; otherwise return the original string
     */
    public static String convertIfLowercase(String input) {
        if (input == null) {
            return null;
        }
        // If the string is the same as its lowercase counterpart, it means all lowercase
        if (input.equals(input.toLowerCase())) {
            return input.toUpperCase();
        }
        return input;
    }

    /**
     * Normalized database type
     *
     * @param dbType database type, supports: MySql, Oracle11, Oracle, DM8, Kingbase8
     * @return Standardized database type, return value is MYSQL, ORACLE, DM8, KINGBASE, Oracle11 also returns ORACLE
     * @throws IllegalArgumentException thrown when dbType is null or empty string
     */
    public static String getNormalizedDbType(String dbType) {
        if (dbType == null || dbType.isEmpty()) {
            throw new IllegalArgumentException(MessageUtils.messageWithFallback(
                    "sys.error.database.type.empty", "Database type cannot be empty"));
        }
        if (DbType.MYSQL.getDb().equals(dbType)) {
            return "MYSQL";
        } else if (DbType.ORACLE.getDb().equals(dbType) || DbType.ORACLE_12C.getDb().equals(dbType)) {
            return "ORACLE";
        } else if (DbType.DM8.getDb().equals(dbType)) {
            return "DAMENG";
        } else if (DbType.KINGBASE8.getDb().equals(dbType)) {
            return "KINGBASE";
        }
        // Returns primitive type value by default
        return dbType;
    }

    /**
     * Create object
     *
     * @param datasource
     * @return
     */
    public static DbQueryProperty buildJobDatasource(Map<String, Object> datasource) {
        String ip = MapUtils.getString(datasource, "ip");
        Long port = MapUtils.getLong(datasource, "port");
        String datasourceConfig = MapUtils.getString(datasource, "datasourceConfig");
        String datasourceType = MapUtils.getString(datasource, "datasourceType");
        DbQueryProperty dbQueryProperty = new DbQueryProperty(datasourceType, ip, port, datasourceConfig);

        return dbQueryProperty;
    }

    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            if (columnName.equalsIgnoreCase(meta.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
}
