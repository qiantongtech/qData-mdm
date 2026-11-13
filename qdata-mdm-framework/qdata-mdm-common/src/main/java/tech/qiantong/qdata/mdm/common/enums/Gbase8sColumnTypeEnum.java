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

package tech.qiantong.qdata.mdm.common.enums;

import lombok.Getter;
import tech.qiantong.qdata.mdm.common.database.utils.DatabaseUtil;

/**
 * GBase 8s（Informix 体系）数据库字段类型枚举（物理类型 → DM 逻辑类型）。
 *
 * <p>供反向导入（物理表 → 逻辑模型，DaDatasourceServiceImpl#getColumnsList）使用，
 * 与 ClickHouseColumnTypeEnum / DorisColumnTypeEnum 等结构一致。
 */
@Getter
public enum Gbase8sColumnTypeEnum {
    CHAR("CHAR", DmColumnTypeEnum.CHAR),
    VARCHAR("VARCHAR", DmColumnTypeEnum.VARCHAR),
    LVARCHAR("LVARCHAR", DmColumnTypeEnum.TEXT),
    TEXT("TEXT", DmColumnTypeEnum.TEXT),
    CLOB("CLOB", DmColumnTypeEnum.TEXT),
    INTEGER("INTEGER", DmColumnTypeEnum.INTEGER),
    INT("INT", DmColumnTypeEnum.INTEGER),
    SMALLINT("SMALLINT", DmColumnTypeEnum.INTEGER),
    SERIAL("SERIAL", DmColumnTypeEnum.INTEGER),
    BIGINT("BIGINT", DmColumnTypeEnum.BIGINT),
    SERIAL8("SERIAL8", DmColumnTypeEnum.BIGINT),
    INT8("INT8", DmColumnTypeEnum.BIGINT),
    FLOAT("FLOAT", DmColumnTypeEnum.FLOAT),
    SMALLFLOAT("SMALLFLOAT", DmColumnTypeEnum.FLOAT),
    DOUBLE("DOUBLE", DmColumnTypeEnum.DOUBLE),
    DECIMAL("DECIMAL", DmColumnTypeEnum.DECIMAL),
    NUMERIC("NUMERIC", DmColumnTypeEnum.DECIMAL),
    MONEY("MONEY", DmColumnTypeEnum.DECIMAL),
    DATE("DATE", DmColumnTypeEnum.DATE),
    DATETIME("DATETIME", DmColumnTypeEnum.DATETIME),
    BYTE("BYTE", DmColumnTypeEnum.TEXT);

    private final String type;
    private final DmColumnTypeEnum dmType;

    Gbase8sColumnTypeEnum(String type, DmColumnTypeEnum dmType) {
        this.type = type;
        this.dmType = dmType;
    }

    public static String convertToDmType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return type;
        }
        String gbaseType = DatabaseUtil.convertIfLowercase(type);
        gbaseType = gbaseType.replaceAll("\\(.*\\)", "").trim().toUpperCase();
        // 剥离 DATETIME 限定符（如 DATETIME YEAR TO SECOND → DATETIME）
        if (gbaseType.startsWith("DATETIME ")) {
            gbaseType = "DATETIME";
        }
        for (Gbase8sColumnTypeEnum typeEnum : values()) {
            if (typeEnum.getType().equals(gbaseType)) {
                return typeEnum.getDmType().getType();
            }
        }
        return gbaseType;
    }
}
