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

import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 已有列静态默认值适配：标识符由方言引用，值始终编码为字面量，不执行用户表达式。
 */
public final class ColumnDefaultSql {
    private ColumnDefaultSql() {
    }

    public static List<String> generate(DbDialect dialect, DbQueryProperty property, String table,
                                        DbColumn column, Connection connection) throws SQLException {
        String full = dialect.getTableName(property, table);
        String name = dialect.quoteIdentifier(column.getColName());
        String literal = literal(dialect, column);
        if (dialect instanceof SQLServer2008Dialect) {
            List<String> sql = new ArrayList<>();
            // SQL Server DEFAULT 是具名约束，只定位本表本列的默认约束，不猜名称或删除其他约束。
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT d.name FROM sys.default_constraints d JOIN sys.columns c "
                            + "ON d.parent_object_id=c.object_id AND d.parent_column_id=c.column_id "
                            + "WHERE d.parent_object_id=OBJECT_ID(?) AND c.name=?")) {
                query.setString(1, full);
                query.setString(2, column.getColName());
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) {
                        sql.add("ALTER TABLE " + full + " DROP CONSTRAINT " + dialect.quoteIdentifier(rows.getString(1)));
                        if (rows.next()) throw new SQLException("默认约束不唯一，无法安全修改");
                    }
                }
            }
            if (literal != null) sql.add("ALTER TABLE " + full + " ADD DEFAULT " + literal + " FOR " + name);
            return sql;
        }
        if (dialect instanceof OracleDialect || dialect instanceof DM8Dialect) {
            return Collections.singletonList("ALTER TABLE " + full + " MODIFY (" + name + " DEFAULT "
                    + (literal == null ? "NULL" : literal) + ")");
        }
        // PostgreSQL/openGauss/GBase8c/DWS、MySQL/MariaDB/TiDB/OceanBase MySQL、DB2 等。
        if (literal != null && dialect instanceof MySqlDialect && "TEXT".equalsIgnoreCase(column.getDataType())) {
            literal = "(" + literal + ")";
        }
        return Collections.singletonList("ALTER TABLE " + full + " ALTER COLUMN " + name
                + (literal == null ? " DROP DEFAULT" : " SET DEFAULT " + literal));
    }

    private static String literal(DbDialect dialect, DbColumn column) {
        String value = column.getDataDefault();
        if (value == null || value.isEmpty()) return null;
        String type = column.getDataType().toUpperCase(Locale.ROOT);
        if ("INTEGER".equals(type)) return new java.math.BigInteger(value).toString();
        if ("DECIMAL".equals(type)) {
            if (!value.matches("[+-]?\\d+(?:\\.\\d+)?")) throw new IllegalArgumentException("默认值必须是静态数字");
            return new java.math.BigDecimal(value).toPlainString();
        }
        if ("BOOLEAN".equals(type)) {
            if (!Arrays.asList("0", "1", "true", "false").contains(value.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("布尔默认值不合法");
            String converted = dialect.convertLogicalColumn(column).getDataDefault();
            return "true".equalsIgnoreCase(converted) ? "TRUE" : "false".equalsIgnoreCase(converted) ? "FALSE" : converted;
        }
        if ("DATE".equals(type)) java.time.LocalDate.parse(value);
        if ("DATETIME".equals(type)) java.time.LocalDateTime.parse(value.replace(' ', 'T'));
        String quoted = "'" + value.replace("'", "''") + "'";
        if (dialect instanceof OracleDialect && "DATE".equals(type)) return "DATE " + quoted;
        if (dialect instanceof OracleDialect && "DATETIME".equals(type)) return "TIMESTAMP " + quoted;
        return dialect instanceof SQLServer2008Dialect ? "N" + quoted : quoted;
    }
}
