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

package tech.qiantong.qdata.mdm.neo4j.rel;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import tech.qiantong.qdata.mdm.neo4j.node.TaskNode;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 14:02
 **/
@RelationshipProperties
@Data
@Builder
public class TableToTaskRel {
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务编码
     */
    private String taskCode;

    /**
     * 全限定表名 例如：sales_db.dwh.orders
     */
    private String tableName;
//    /**
//     * 数据源 id
//     */
//    private Long datasourceId;

    /**
     * 数据源ip:port
     */
    private String datasourceHostPort;

    @TargetNode
    private TaskNode task;
}
