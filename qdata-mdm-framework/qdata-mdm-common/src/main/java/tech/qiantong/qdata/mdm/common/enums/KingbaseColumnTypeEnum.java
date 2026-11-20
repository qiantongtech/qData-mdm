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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.common.enums;

import lombok.Getter;
import tech.qiantong.qdata.mdm.common.database.utils.MD5Util;

/**
 * NPC Jincang database field type enumeration
 */
@Getter
public enum KingbaseColumnTypeEnum {
    INT2("INT2", DmColumnTypeEnum.INTEGER),
    INT4("INT4", DmColumnTypeEnum.INTEGER),
    INT8("INT8", DmColumnTypeEnum.BIGINT),
    TINYINT("TINYINT", DmColumnTypeEnum.TINYINT),
    SMALLINT("SMALLINT", DmColumnTypeEnum.INTEGER),
    INTEGER("INTEGER", DmColumnTypeEnum.INTEGER),
    INT("INT", DmColumnTypeEnum.INTEGER),
    BIGINT("BIGINT", DmColumnTypeEnum.BIGINT),
    BIGSERIAL("BIGSERIAL", DmColumnTypeEnum.BIGINT),
    DECIMAL("DECIMAL", DmColumnTypeEnum.DECIMAL),
    NUMERIC("NUMERIC", DmColumnTypeEnum.DECIMAL),
    FLOAT("FLOAT", DmColumnTypeEnum.FLOAT),
    DOUBLE("DOUBLE", DmColumnTypeEnum.DOUBLE),
    NUMBER("NUMBER", DmColumnTypeEnum.NUMBER),
    CHAR("CHAR", DmColumnTypeEnum.CHAR),
    VARCHAR("VARCHAR", DmColumnTypeEnum.VARCHAR),
    VARCHAR2("VARCHAR2", DmColumnTypeEnum.VARCHAR2),
    TEXT("TEXT", DmColumnTypeEnum.TEXT),
    CLOB("CLOB", DmColumnTypeEnum.TEXT),
    DATE("DATE", DmColumnTypeEnum.DATE),
    TIMESTAMP("TIMESTAMP", DmColumnTypeEnum.TIMESTAMP),
    DATETIME("DATETIME", DmColumnTypeEnum.DATETIME);

    private final String type;
    private final DmColumnTypeEnum dmType;

    KingbaseColumnTypeEnum(String type, DmColumnTypeEnum dmType) {
        this.type = type;
        this.dmType = dmType;
    }

    /**
     * Convert Renmin University Jincang type to Dameng type
     */
    public static String convertToDmType(String type) {
        String kingbaseType = MD5Util.convertIfLowercase(type);
        kingbaseType = kingbaseType.replaceAll("\\(.*\\)", "").trim().toUpperCase();
        for (KingbaseColumnTypeEnum typeEnum : values()) {
            if (typeEnum.getType().equals(kingbaseType)) {
                return typeEnum.getDmType().getType();
            }
        }
        return kingbaseType;
// return DmColumnTypeEnum.VARCHAR.getType(); // Convert to VARCHAR by default
    }
}
