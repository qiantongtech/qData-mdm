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

import java.util.HashMap;
import java.util.Map;

@Data
public class LineageGraphEdgeDTO {

    /**
     * 前端使用的稳定边 ID。
     */
    private String id;

    /**
     * Neo4j 内部关系 ID，仅用于调试和后端排查。
     */
    private Long neo4jId;

    /**
     * 起点节点 ID。
     */
    private String source;

    /**
     * 终点节点 ID。
     */
    private String target;

    /**
     * 关系类型：TABLE_TO_TASK、TASK_TO_TABLE。
     */
    private String type;

    /**
     * 边展示文案，默认使用关系类型。
     */
    private String label;

    /**
     * Neo4j 原始关系属性。
     */
    private Map<String, Object> properties = new HashMap<>();
}
