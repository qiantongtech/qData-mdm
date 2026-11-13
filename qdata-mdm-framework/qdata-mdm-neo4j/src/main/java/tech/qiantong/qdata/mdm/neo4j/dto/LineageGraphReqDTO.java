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

@Data
public class LineageGraphReqDTO {

    /**
     * 查询场景：SOURCE_SYSTEM、INTEGRATION_TASK、DEVELOPMENT_TASK、ALL_DB_SYNC_TASK、API_SERVICE
     */
    private String sceneType;

    /**
     * 元数据/树节点类型：DATASOURCE、DATABASE、SCHEMA、TABLE、CAT、TASK。
     */
    private String metadataType;

    /**
     * 图节点类型，预留给前端按节点继续展开时使用。
     */
    private String nodeType;

    /**
     * 无法通过表连接信息或任务ID定位时，使用 Neo4j 内部节点ID作为兼容方案。
     */
    private Long rootNeo4jId;

    /**
     * 数据源在 Neo4j 中的唯一连接标识，格式由 DbQueryProperty#trainToHostPort 生成。
     */
    private String datasourceHostPort;

    /**
     * 数据库名称，用于库级筛选。
     */
    private String dbName;

    /**
     * 模式名称，用于 Schema 级筛选。
     */
    private String sid;

    /**
     * Neo4j 中保存的全限定表名。
     */
    private String tableName;

    /**
     * 批量表种子。类目、库、Schema 点击时会先解析成一批叶子表，再批量查血缘。
     */
    private List<LineageTableSeedDTO> tableSeeds = new ArrayList<>();

    /**
     * 任务 ID，用于以单个任务节点为根查询血缘。
     */
    private Long taskId;

    /**
     * 批量任务种子。点击任务类目时会先分页解析成一批任务，再批量查血缘。
     */
    private List<Long> taskIds = new ArrayList<>();

    /**
     * 是否启用全局有血缘种子查询。
     * 首屏空入参、画布拖动加载时使用，不再依赖默认数据源。
     */
    private Boolean globalLineageSeed;

    /**
     * 全局血缘种子游标，使用 Neo4j 节点 id 向后翻页。
     */
    private Long globalSeedCursor;

    /**
     * 每批全局血缘种子数量。
     */
    private Integer seedLimit;

    /**
     * 查询方向：UP 上游、DOWN 下游、ALL 全链路。
     */
    private String direction;

    /**
     * 查询深度，服务层会限制最大值，避免一次拉取过大的图。
     */
    private Integer depth;

    /**
     * 返回路径数量上限。
     */
    private Integer limit;

    /**
     * 是否计算每个节点的上下游数量。
     */
    private Boolean includeCounts;

    /**
     * 动态加载游标。业务服务生成，Neo4j 服务负责透传到返回结果。
     */
    private String cursor;

    /**
     * 是否还有下一批叶子种子或图谱数据。
     */
    private Boolean hasMore = false;

    /**
     * 下一次动态加载时前端原样回传的游标。
     */
    private String nextCursor;

    /**
     * 前端已经加载过的节点，后端用于增量返回时去重。
     */
    private List<String> excludeNodeIds = new ArrayList<>();

    /**
     * 前端已经加载过的边，后端用于增量返回时去重。
     */
    private List<String> excludeEdgeIds = new ArrayList<>();

    /**
     * 回传给前端的查询上下文，如 datasourceId、sceneType、nodeType、treeId。
     */
    private Map<String, Object> scope = new HashMap<>();
}
