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

package tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance;

import lombok.Value;
import tech.qiantong.qdata.mdm.common.database.sql.SqlTemplate.Operation;
import java.util.Map;

/** 已发布子表及其参数化维护操作，与主表共用一个连接提交。 */
@Value
public class TableMutation {
    String tableName;
    Operation operation;
    boolean generatedId;
    boolean bindMainRecordId;
    Map<String, Object> result;

    public TableMutation(String tableName, Operation operation) {
        this(tableName, operation, false, false, null);
    }

    private TableMutation(String tableName, Operation operation, boolean generatedId,
            boolean bindMainRecordId, Map<String, Object> result) {
        this.tableName = tableName;
        this.operation = operation;
        this.generatedId = generatedId;
        this.bindMainRecordId = bindMainRecordId;
        this.result = result;
    }

    public static TableMutation generatedInsert(String tableName, Operation operation,
            boolean bindMainRecordId, Map<String, Object> result) {
        return new TableMutation(tableName, operation, true, bindMainRecordId, result);
    }
}
