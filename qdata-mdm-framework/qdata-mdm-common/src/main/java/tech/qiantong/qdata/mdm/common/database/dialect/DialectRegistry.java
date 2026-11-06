/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.common.database.dialect;

import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;

import java.util.EnumMap;
import java.util.Map;

public class DialectRegistry {

    private final Map<DbType, DbDialect> dialect_enum_map = new EnumMap<>(DbType.class);

    public DialectRegistry() {
        dialect_enum_map.put(DbType.MARIADB, new MariaDBDialect());
        dialect_enum_map.put(DbType.MYSQL, new MySqlDialect());
        dialect_enum_map.put(DbType.ORACLE_12C, new Oracle12cDialect());
        dialect_enum_map.put(DbType.ORACLE, new OracleDialect());
        dialect_enum_map.put(DbType.POSTGRE_SQL, new PostgreDialect());
        dialect_enum_map.put(DbType.SQL_SERVER2008, new SQLServer2008Dialect());
        dialect_enum_map.put(DbType.SQL_SERVER, new SQLServerDialect());
        dialect_enum_map.put(DbType.DM8, new DM8Dialect());
        dialect_enum_map.put(DbType.OTHER, new UnknownDialect());
        dialect_enum_map.put(DbType.KINGBASE8, new Kingbase8Dialect());
        dialect_enum_map.put(DbType.KAFKA, new KafkaDialect());
        dialect_enum_map.put(DbType.DORIS, new DorisDialect());
    }

    public DbDialect getDialect(DbType dbType) {
        return dialect_enum_map.get(dbType);
    }
}
