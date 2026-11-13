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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 已完成业务节点解析的血缘维护数据。
 * DA 模块负责把 treeId 转成稳定图节点，Neo4j 模块只处理关系差异和事务写入。
 */
@Data
public class LineageMaintenanceDTO {

    private List<Graph> graphs = new ArrayList<>();

    @Data
    public static class Graph {
        private Integer graphIndex;
        private List<Long> originalRelIds = new ArrayList<>();
        private List<Edge> edges = new ArrayList<>();
    }

    @Data
    public static class Edge {
        private Long originalRelId;
        private Node source;
        private Node target;
        private String type;
    }

    @Data
    public static class Node {
        private String nodeType;
        private String graphId;
        private Long neo4jId;
        private Map<String, Object> properties = new HashMap<>();
    }
}
