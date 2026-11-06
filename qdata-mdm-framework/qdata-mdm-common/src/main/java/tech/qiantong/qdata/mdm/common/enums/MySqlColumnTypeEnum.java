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

package tech.qiantong.qdata.mdm.common.enums;

import lombok.Getter;
import tech.qiantong.qdata.mdm.common.database.utils.MD5Util;

/**
 * MySQL database field type enumeration
 */
@Getter
public enum MySqlColumnTypeEnum {
    TINYINT("TINYINT", DmColumnTypeEnum.TINYINT),
    SMALLINT("SMALLINT", DmColumnTypeEnum.TINYINT),
    MEDIUMINT("MEDIUMINT", DmColumnTypeEnum.INTEGER),
    INT("INT", DmColumnTypeEnum.INTEGER),
    INTEGER("INTEGER", DmColumnTypeEnum.INTEGER),
    BIGINT("BIGINT", DmColumnTypeEnum.BIGINT),
    DECIMAL("DECIMAL", DmColumnTypeEnum.DECIMAL),
    NUMERIC("NUMERIC", DmColumnTypeEnum.NUMERIC),
    FLOAT("FLOAT", DmColumnTypeEnum.FLOAT),
    DOUBLE("DOUBLE", DmColumnTypeEnum.DOUBLE),
    REAL("REAL", DmColumnTypeEnum.DOUBLE),
    CHAR("CHAR", DmColumnTypeEnum.CHAR),
    VARCHAR("VARCHAR", DmColumnTypeEnum.VARCHAR2),
    TINYTEXT("TINYTEXT", DmColumnTypeEnum.TEXT),
    TEXT("TEXT", DmColumnTypeEnum.TEXT),
    MEDIUMTEXT("MEDIUMTEXT", DmColumnTypeEnum.TEXT),
    LONGTEXT("LONGTEXT", DmColumnTypeEnum.TEXT),
    DATE("DATE", DmColumnTypeEnum.DATE),
    TIME("TIME", DmColumnTypeEnum.TIMESTAMP),
    TIMESTAMP("TIMESTAMP", DmColumnTypeEnum.TIMESTAMP),
    DATETIME("DATETIME", DmColumnTypeEnum.DATETIME);

    private final String type;
    private final DmColumnTypeEnum dmType;

    MySqlColumnTypeEnum(String type, DmColumnTypeEnum dmType) {
        this.type = type;
        this.dmType = dmType;
    }

    /**
     * Convert MySQL type to Damon type
     */
    public static String convertToDmType(String type) {
        String mysqlType = MD5Util.convertIfLowercase(type);
        mysqlType = mysqlType.replaceAll("\\(.*\\)", "").trim().toUpperCase();
        for (MySqlColumnTypeEnum typeEnum : values()) {
            if (typeEnum.getType().equals(mysqlType)) {
                return typeEnum.getDmType().getType();
            }
        }
        return mysqlType;
// return DmColumnTypeEnum.VARCHAR.getType(); // Convert to VARCHAR by default
    }
}
