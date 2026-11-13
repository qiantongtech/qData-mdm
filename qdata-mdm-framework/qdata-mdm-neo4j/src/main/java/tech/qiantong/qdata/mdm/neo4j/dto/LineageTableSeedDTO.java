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

package tech.qiantong.qdata.mdm.neo4j.dto;

import lombok.Data;

@Data
public class LineageTableSeedDTO {

    /**
     * 数据源连接标识，对应 Neo4j Table.datasourceHostPort。
     */
    private String datasourceHostPort;

    /**
     * Neo4j 中保存的全限定表名。
     */
    private String tableName;

    /**
     * 以下字段来自元数据树，用于补齐图谱表节点展示信息。
     */
    private Long datasourceId;
    private String datasourceName;
    private String datasourceType;
    private String dbName;
    private String dbComment;
    private String schemaName;
    private Long tableId;
    private String tableComment;
}
