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

package tech.qiantong.qdata.mdm.common.database.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DbColumn {

    /**
     * Table name
     */
    private String tableName;

    /**
     * List
     */
    private String colName;

    /**
     * Data type
     */
    private String dataType;

    /** JDBC 元数据类型，用于来源表筛选和参数绑定。 */
    private Integer jdbcType;

    /**
     * Data length
     */
    private String dataLength;

    /**
     * Data accuracy
     */
    private String dataPrecision;

    /**
     * Data decimal places
     */
    private String dataScale;

    /**
     * Whether the primary key is true is the primary key false is not the primary key
     */
    private Boolean colKey;

    /**
     * 是否为数据库自增列；null/false保持普通列行为。
     */
    private Boolean identityFlag;

    /**
     * Whether empty is allowed true, empty is allowed, false is not allowed to be empty
     */
    private Boolean nullable;

    /**
     * Column number
     */
    private Integer colPosition;

    /**
     * Column default value
     */
    private String dataDefault;

    /**
     * Column comments
     */
    private String colComment;
    /** 兼容增加自增属性之前的全参构造器。 */
    public DbColumn(String tableName, String colName, String dataType, Integer jdbcType, String dataLength,
                    String dataPrecision, String dataScale, Boolean colKey, Boolean nullable,
                    Integer colPosition, String dataDefault, String colComment) {
        this(tableName, colName, dataType, jdbcType, dataLength, dataPrecision, dataScale,
                colKey, null, nullable, colPosition, dataDefault, colComment);
    }
}
