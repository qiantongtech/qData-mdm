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

package tech.qiantong.qdata.mdm.ingest.dal.repository.ingest;

/**
 * 来源实体版本查询的方言。
 */
final class SourceDataVersionSql {

    private SourceDataVersionSql() {
    }

    static String latestVersionPredicate(String table, String idColumn, String sourceSystemColumn,
                                         String sourceRecordColumn, String collectionTimeColumn) {
        return "NOT EXISTS (SELECT 1 FROM " + table + " newer WHERE newer." + sourceSystemColumn
                + " = current_row." + sourceSystemColumn + " AND newer." + sourceRecordColumn
                + " = current_row." + sourceRecordColumn + " AND (newer." + collectionTimeColumn
                + " > current_row." + collectionTimeColumn + " OR (newer." + collectionTimeColumn
                + " = current_row." + collectionTimeColumn + " AND newer." + idColumn
                + " > current_row." + idColumn + ")))";
    }
}
