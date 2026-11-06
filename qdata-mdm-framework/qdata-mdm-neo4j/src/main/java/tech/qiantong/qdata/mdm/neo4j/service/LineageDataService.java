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

package tech.qiantong.qdata.mdm.neo4j.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.UnresolvedRelation;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Relationship;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.neo4j.config.Neo4jProperties;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageGraphDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageGraphEdgeDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageGraphNodeDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageGraphReqDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageMaintenanceDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageMaintenanceResultDTO;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageTableSeedDTO;
import tech.qiantong.qdata.mdm.neo4j.node.TableNode;
import tech.qiantong.qdata.mdm.neo4j.node.TaskNode;
import tech.qiantong.qdata.mdm.neo4j.rel.TableToTaskRel;
import tech.qiantong.qdata.mdm.neo4j.rel.TaskToTableRel;
import tech.qiantong.qdata.mdm.neo4j.repository.TableRepository;
import tech.qiantong.qdata.mdm.neo4j.repository.TaskRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 13:46
 **/
@Service
public class LineageDataService {

    @Resource
    private TableRepository tableRepository;

    @Resource
    private TaskRepository taskRepository;

    @Resource
    private Neo4jProperties neo4jProperties;

    /**
     * Spring Data Neo4j 客户端。与 {@code neo4jTransactionManager} 事务绑定，
     * 在 {@code @Transactional} 方法内查询时可读到本事务尚未提交的写入（读己之写），
     * 因此适合在 {@link #saveTable(TaskNode, String, String)} 写入血缘边后立即查询链路。
     */
    @Resource
    private Neo4jClient neo4jClient;

    /**
     * 用于血缘解析的 SparkSession（延迟初始化，复用避免重复创建开销）
     */
    private volatile SparkSession sparkSession;

    private volatile Driver lineageDriver;

    @Transactional("neo4jTransactionManager")
    public LineageDTO lineage(String datasourceHostPort, String tableName) {
        LineageDTO lineageDto = tableRepository.findLineage(datasourceHostPort, tableName).orElse(null);
        if (lineageDto == null) {
            return new LineageDTO();
        }

        //查询关系数据
        getRels(lineageDto, datasourceHostPort, tableName);
        return lineageDto;
    }

    /**
     * 获取血缘信息
     *
     * @param taskId
     * @param type   任务类型;1：离线任务(数据集成) 2：实时任务 3：数据开发任务 4：作业任务  5:整库同步 6:数据服
     * @return
     */
    @Transactional("neo4jTransactionManager")
    public LineageDTO lineage(Long taskId, String type) {
        LineageDTO lineageDto = tableRepository.findLineage(taskId, type).orElse(null);
        if (lineageDto == null) {
            return new LineageDTO();
        }

        //查询关系数据
        getRels(lineageDto, taskId, type);
        return lineageDto;
    }

    public LineageGraphDTO lineageGraph(LineageGraphReqDTO req) {
        LineageGraphDTO graph = new LineageGraphDTO();
        long startTime = System.currentTimeMillis();
        fillScope(graph, req);
        if (req != null) {
            graph.setHasMore(Boolean.TRUE.equals(req.getHasMore()));
            graph.setNextCursor(req.getNextCursor());
        }

        int depth = normalizeDepth(req == null ? null : req.getDepth());
        int limit = normalizeLimit(req == null ? null : req.getLimit());
        String direction = normalizeDirection(req == null ? null : req.getDirection());
        Set<String> excludeNodeIds = new HashSet<>(emptyIfNull(req == null ? null : req.getExcludeNodeIds()));
        Set<String> excludeEdgeIds = new HashSet<>(emptyIfNull(req == null ? null : req.getExcludeEdgeIds()));

        try (Session session = getLineageDriver().session(SessionConfig.forDatabase("neo4j"))) {
            Map<Long, LineageGraphNodeDTO> nodeMap = new LinkedHashMap<>();
            Map<Long, LineageGraphNodeDTO> allNodeMap = new LinkedHashMap<>();
            Map<Long, LineageGraphEdgeDTO> edgeMap = new LinkedHashMap<>();
            List<Long> rootNodeIds = collectRootNodes(session, req, limit, excludeNodeIds, nodeMap, allNodeMap);
            long rootEndTime = System.currentTimeMillis();
            syncPageCursor(graph, req);
            if (rootNodeIds.isEmpty()) {
                graph.setMessage("\u6682\u65e0\u53ef\u67e5\u8be2\u7684\u8840\u7f18\u8303\u56f4");
                if (req != null && Boolean.TRUE.equals(req.getGlobalLineageSeed())) {
                    graph.getScope().put("initialStrategy", "NO_LINEAGE_DATA");
                }
                graph.getScope().put("rootQueryMs", rootEndTime - startTime);
                return graph;
            }

            if ("ALL".equals(direction)) {
                // 全链路是“持续向上游”和“持续向下游”两部分的并集。
                // 两个方向独立遍历，避免到达父节点后再转向其其他子分支，带出平级图谱。
                collectLineageByLayer(session, rootNodeIds, "UP", depth, limit,
                        excludeNodeIds, excludeEdgeIds, nodeMap, allNodeMap, edgeMap);
                collectLineageByLayer(session, rootNodeIds, "DOWN", depth, limit,
                        excludeNodeIds, excludeEdgeIds, nodeMap, allNodeMap, edgeMap);
            } else {
                collectLineageByLayer(session, rootNodeIds, direction, depth, limit,
                        excludeNodeIds, excludeEdgeIds, nodeMap, allNodeMap, edgeMap);
            }
            long expandEndTime = System.currentTimeMillis();

            if (!nodeMap.isEmpty()) {
                if (req == null || !Boolean.FALSE.equals(req.getIncludeCounts())) {
                    fillCounts(session, nodeMap);
                }
                long countEndTime = System.currentTimeMillis();
                markRoot(graph, req, nodeMap);
                graph.setNodes(new ArrayList<>(nodeMap.values()));
                graph.setEdges(new ArrayList<>(edgeMap.values()));
                graph.setEmpty(false);
                graph.getScope().put("rootQueryMs", rootEndTime - startTime);
                graph.getScope().put("expandQueryMs", expandEndTime - rootEndTime);
                graph.getScope().put("countQueryMs", countEndTime - expandEndTime);
                graph.getScope().put("neo4jTotalMs", countEndTime - startTime);
                graph.getScope().put("resultNodeCount", nodeMap.size());
                graph.getScope().put("resultEdgeCount", edgeMap.size());
            }
            return graph;
        }
    }

    private Driver getLineageDriver() {
        if (lineageDriver == null) {
            synchronized (this) {
                if (lineageDriver == null) {
                    lineageDriver = GraphDatabase.driver(neo4jProperties.getUri(),
                            AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword()));
                }
            }
        }
        return lineageDriver;
    }

    public List<Long> filterLineageTaskIds(Collection<Long> taskIds, int limit) {
        if (taskIds == null || taskIds.isEmpty() || limit < 1) {
            return Collections.emptyList();
        }
        try (Session session = getLineageDriver().session(SessionConfig.forDatabase("neo4j"))) {
            Result result = session.run("UNWIND $taskIds AS taskId "
                            + "MATCH (root:Task {taskId: taskId})-[:TABLE_TO_TASK|TASK_TO_TABLE]-() "
                            + "RETURN DISTINCT root.taskId AS taskId LIMIT $limit",
                    Values.parameters("taskIds", new ArrayList<>(taskIds), "limit", limit));
            List<Long> lineageTaskIds = new ArrayList<>();
            while (result.hasNext()) {
                lineageTaskIds.add(result.next().get("taskId").asLong());
            }
            return lineageTaskIds;
        }
    }

    /**
     * 按“编辑前关系 ID + 编辑后最终关系”维护血缘。
     * 所有图在同一个 Neo4j 事务中处理，任意一项失败都会整体回滚。
     */
    public LineageMaintenanceResultDTO maintainLineage(LineageMaintenanceDTO request) {
        if (request == null || request.getGraphs() == null || request.getGraphs().isEmpty()) {
            throw new IllegalArgumentException("graphs不能为空");
        }

        validateMaintenanceRequest(request);
        try (Session session = getLineageDriver().session(SessionConfig.forDatabase("neo4j"));
             Transaction tx = session.beginTransaction()) {
            try {
                LineageMaintenancePlan plan = buildMaintenancePlan(tx, request);

                // 先统一删除，再创建新关系，避免两个更新关系互换端点时相互覆盖。
                deleteMaintenanceRelationships(tx, plan.deleteRelIds);
                createMaintenanceRelationships(tx, plan);
                fillMaintenanceRefreshNodes(tx, plan);
                tx.commit();
                return plan.result;
            } catch (RuntimeException ex) {
                if (tx.isOpen()) {
                    tx.rollback();
                }
                throw ex;
            }
        }
    }

    private void validateMaintenanceRequest(LineageMaintenanceDTO request) {
        Set<Long> allOriginalRelIds = new HashSet<>();
        Set<String> allFinalEdgeKeys = new HashSet<>();
        for (int graphIndex = 0; graphIndex < request.getGraphs().size(); graphIndex++) {
            LineageMaintenanceDTO.Graph graph = request.getGraphs().get(graphIndex);
            if (graph == null) {
                throw new IllegalArgumentException("graphs[" + graphIndex + "]不能为空");
            }
            List<Long> originalRelIds = graph.getOriginalRelIds() == null
                    ? Collections.emptyList() : graph.getOriginalRelIds();
            for (Long relId : originalRelIds) {
                if (relId == null || !allOriginalRelIds.add(relId)) {
                    throw new IllegalArgumentException("originalRelIds存在空值或重复关系ID：" + relId);
                }
            }

            Set<Long> graphFinalOriginalIds = new HashSet<>();
            List<LineageMaintenanceDTO.Edge> edges = graph.getEdges() == null
                    ? Collections.emptyList() : graph.getEdges();
            for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
                LineageMaintenanceDTO.Edge edge = edges.get(edgeIndex);
                validateMaintenanceEdge(edge, graphIndex, edgeIndex);
                if (edge.getOriginalRelId() != null) {
                    if (!originalRelIds.contains(edge.getOriginalRelId())) {
                        throw new IllegalArgumentException("graphs[" + graphIndex + "].edges[" + edgeIndex
                                + "].originalRelId不在originalRelIds中");
                    }
                    if (!graphFinalOriginalIds.add(edge.getOriginalRelId())) {
                        throw new IllegalArgumentException("同一个originalRelId不能在最终关系中出现多次："
                                + edge.getOriginalRelId());
                    }
                }
                String edgeKey = maintenanceEdgeKey(edge.getSource().getGraphId(), edge.getTarget().getGraphId(), edge.getType());
                if (!allFinalEdgeKeys.add(edgeKey)) {
                    throw new IllegalArgumentException("最终关系重复：" + edgeKey);
                }
            }
        }
    }

    private void validateMaintenanceEdge(LineageMaintenanceDTO.Edge edge, int graphIndex, int edgeIndex) {
        String path = "graphs[" + graphIndex + "].edges[" + edgeIndex + "]";
        if (edge == null || edge.getSource() == null || edge.getTarget() == null) {
            throw new IllegalArgumentException(path + "的起点和终点不能为空");
        }
        if (!isMaintenanceNode(edge.getSource()) || !isMaintenanceNode(edge.getTarget())) {
            throw new IllegalArgumentException(path + "包含无效节点");
        }
        if (!"TABLE_TO_TASK".equals(edge.getType()) && !"TASK_TO_TABLE".equals(edge.getType())
                && !"TABLE_TO_TABLE".equals(edge.getType())) {
            throw new IllegalArgumentException(path + "关系类型无效");
        }
        if ("TABLE_TO_TASK".equals(edge.getType())
                && !("TABLE".equals(edge.getSource().getNodeType()) && "TASK".equals(edge.getTarget().getNodeType()))) {
            throw new IllegalArgumentException(path + "必须是TABLE -> TASK");
        }
        if ("TASK_TO_TABLE".equals(edge.getType())
                && !("TASK".equals(edge.getSource().getNodeType()) && "TABLE".equals(edge.getTarget().getNodeType()))) {
            throw new IllegalArgumentException(path + "必须是TASK -> TABLE");
        }
        if ("TABLE_TO_TABLE".equals(edge.getType())
                && !("TABLE".equals(edge.getSource().getNodeType()) && "TABLE".equals(edge.getTarget().getNodeType()))) {
            throw new IllegalArgumentException(path + "必须是TABLE -> TABLE");
        }
    }

    private boolean isMaintenanceNode(LineageMaintenanceDTO.Node node) {
        return node != null && isNotBlank(node.getGraphId())
                && ("TABLE".equals(node.getNodeType()) || "TASK".equals(node.getNodeType()));
    }

    private LineageMaintenancePlan buildMaintenancePlan(Transaction tx, LineageMaintenanceDTO request) {
        Set<Long> allOriginalRelIds = request.getGraphs().stream()
                .filter(Objects::nonNull)
                .flatMap(graph -> emptyIfNull(graph.getOriginalRelIds()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MaintenanceOriginalRel> originalRelMap = loadMaintenanceRelationships(tx, allOriginalRelIds);
        if (originalRelMap.size() != allOriginalRelIds.size()) {
            Set<Long> missingIds = new LinkedHashSet<>(allOriginalRelIds);
            missingIds.removeAll(originalRelMap.keySet());
            throw new IllegalArgumentException("原血缘关系不存在或已发生变化：" + missingIds);
        }

        LineageMaintenancePlan plan = new LineageMaintenancePlan();
        for (int graphIndex = 0; graphIndex < request.getGraphs().size(); graphIndex++) {
            LineageMaintenanceDTO.Graph graph = request.getGraphs().get(graphIndex);
            LineageMaintenanceResultDTO.GraphResult graphResult = new LineageMaintenanceResultDTO.GraphResult();
            graphResult.setGraphIndex(graph.getGraphIndex() == null ? graphIndex : graph.getGraphIndex());
            plan.result.getGraphs().add(graphResult);

            Set<Long> finalOriginalIds = new HashSet<>();
            for (LineageMaintenanceDTO.Edge edge : emptyIfNull(graph.getEdges())) {
                if (edge.getOriginalRelId() == null) {
                    plan.creates.add(new MaintenanceCreate(edge, null, graphResult));
                    continue;
                }

                finalOriginalIds.add(edge.getOriginalRelId());
                MaintenanceOriginalRel original = originalRelMap.get(edge.getOriginalRelId());
                if (original.matches(edge)) {
                    graphResult.setUnchangedCount(graphResult.getUnchangedCount() + 1);
                } else {
                    plan.deleteRelIds.add(edge.getOriginalRelId());
                    plan.creates.add(new MaintenanceCreate(edge, edge.getOriginalRelId(), graphResult));
                }
            }

            for (Long originalRelId : emptyIfNull(graph.getOriginalRelIds())) {
                if (!finalOriginalIds.contains(originalRelId)) {
                    plan.deleteRelIds.add(originalRelId);
                    graphResult.getDeletedRelIds().add(originalRelId);
                    graphResult.setDeletedCount(graphResult.getDeletedCount() + 1);
                }
            }
            collectMaintenanceRefreshCandidates(plan, graphResult, graph, originalRelMap, graphIndex);
        }
        return plan;
    }

    /**
     * 优先从保存后的最终关系中选节点；最终关系全部删除时，再使用删除前关系的端点。
     */
    private void collectMaintenanceRefreshCandidates(LineageMaintenancePlan plan,
                                                       LineageMaintenanceResultDTO.GraphResult graphResult,
                                                       LineageMaintenanceDTO.Graph graph,
                                                       Map<Long, MaintenanceOriginalRel> originalRelMap,
                                                       int graphIndex) {
        List<LineageMaintenanceDTO.Node> candidates = new ArrayList<>();
        List<LineageMaintenanceDTO.Edge> finalEdges = emptyIfNull(graph.getEdges());
        if (!finalEdges.isEmpty()) {
            for (LineageMaintenanceDTO.Edge edge : finalEdges) {
                candidates.add(edge.getSource());
                candidates.add(edge.getTarget());
            }
        } else {
            for (Long relId : emptyIfNull(graph.getOriginalRelIds())) {
                MaintenanceOriginalRel originalRel = originalRelMap.get(relId);
                if (originalRel != null) {
                    candidates.add(toMaintenanceRefreshCandidate(originalRel.sourceNode));
                    candidates.add(toMaintenanceRefreshCandidate(originalRel.targetNode));
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("graphs[" + graphIndex + "]没有可保存的关系，无法确定刷新节点");
        }

        // 有任务时优先返回任务，方便前端继续以业务任务为中心查询；纯表血缘则返回表节点。
        candidates.sort(Comparator.comparing((LineageMaintenanceDTO.Node node) ->
                "TASK".equals(node.getNodeType()) ? 0 : 1));
        Map<String, LineageMaintenanceDTO.Node> distinctCandidates = new LinkedHashMap<>();
        for (LineageMaintenanceDTO.Node candidate : candidates) {
            if (candidate != null && isNotBlank(candidate.getGraphId())) {
                distinctCandidates.putIfAbsent(candidate.getNodeType() + "|" + candidate.getGraphId(), candidate);
            }
        }
        plan.refreshPlans.add(new MaintenanceRefreshPlan(graphResult,
                new ArrayList<>(distinctCandidates.values())));
    }

    private LineageMaintenanceDTO.Node toMaintenanceRefreshCandidate(LineageGraphNodeDTO graphNode) {
        if (graphNode == null) {
            return null;
        }
        LineageMaintenanceDTO.Node candidate = new LineageMaintenanceDTO.Node();
        candidate.setGraphId(graphNode.getId());
        candidate.setNodeType(graphNode.getNodeType());
        candidate.setNeo4jId(graphNode.getNeo4jId());
        if (graphNode.getProperties() != null) {
            candidate.setProperties(new HashMap<>(graphNode.getProperties()));
        }
        return candidate;
    }

    private Map<Long, MaintenanceOriginalRel> loadMaintenanceRelationships(Transaction tx, Collection<Long> relIds) {
        Map<Long, MaintenanceOriginalRel> resultMap = new HashMap<>();
        if (relIds == null || relIds.isEmpty()) {
            return resultMap;
        }
        Result result = tx.run("UNWIND $relIds AS relId "
                        + "MATCH (source)-[rel:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]->(target) "
                        + "WHERE id(rel) = relId "
                        + "RETURN id(rel) AS relId, type(rel) AS relType, source, target",
                Values.parameters("relIds", new ArrayList<>(relIds)));
        while (result.hasNext()) {
            Record row = result.next();
            org.neo4j.driver.types.Node source = row.get("source").asNode();
            org.neo4j.driver.types.Node target = row.get("target").asNode();
            MaintenanceOriginalRel rel = new MaintenanceOriginalRel();
            rel.id = row.get("relId").asLong();
            rel.type = row.get("relType").asString();
            rel.sourceNode = toGraphNode(source);
            rel.targetNode = toGraphNode(target);
            rel.sourceGraphId = rel.sourceNode.getId();
            rel.targetGraphId = rel.targetNode.getId();
            resultMap.put(rel.id, rel);
        }
        return resultMap;
    }

    private void deleteMaintenanceRelationships(Transaction tx, Collection<Long> relIds) {
        if (relIds == null || relIds.isEmpty()) {
            return;
        }
        tx.run("UNWIND $relIds AS relId "
                        + "MATCH ()-[rel:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]->() "
                        + "WHERE id(rel) = relId DELETE rel",
                Values.parameters("relIds", new ArrayList<>(new LinkedHashSet<>(relIds)))).consume();
    }

    private void createMaintenanceRelationships(Transaction tx, LineageMaintenancePlan plan) {
        for (MaintenanceCreate create : plan.creates) {
            long sourceNodeId = mergeMaintenanceNode(tx, create.edge.getSource());
            long targetNodeId = mergeMaintenanceNode(tx, create.edge.getTarget());
            MaintenanceCreatedRel createdRel = mergeMaintenanceRelationship(tx, sourceNodeId, targetNodeId, create.edge);

            if (create.oldRelId == null) {
                if (createdRel.created) {
                    create.graphResult.getAddedRelIds().add(createdRel.id);
                    create.graphResult.setAddedCount(create.graphResult.getAddedCount() + 1);
                } else {
                    create.graphResult.setUnchangedCount(create.graphResult.getUnchangedCount() + 1);
                }
            } else {
                LineageMaintenanceResultDTO.UpdatedRel updatedRel = new LineageMaintenanceResultDTO.UpdatedRel();
                updatedRel.setOldRelId(create.oldRelId);
                updatedRel.setNewRelId(createdRel.id);
                create.graphResult.getUpdatedRels().add(updatedRel);
                create.graphResult.setUpdatedCount(create.graphResult.getUpdatedCount() + 1);
            }
        }
    }

    private void fillMaintenanceRefreshNodes(Transaction tx, LineageMaintenancePlan plan) {
        for (MaintenanceRefreshPlan refreshPlan : plan.refreshPlans) {
            LineageMaintenanceResultDTO.RefreshNode refreshNode = null;
            for (LineageMaintenanceDTO.Node candidate : refreshPlan.candidates) {
                org.neo4j.driver.types.Node storedNode = findMaintenanceNode(tx, candidate);
                if (storedNode == null) {
                    continue;
                }
                LineageGraphNodeDTO graphNode = toGraphNode(storedNode);
                if (!isNotBlank(graphNode.getId()) || !isNotBlank(graphNode.getNodeType())) {
                    continue;
                }
                // 任务场景无法识别时继续尝试同图中的表节点，避免返回不可判断场景的参数。
                if ("TASK".equals(graphNode.getNodeType()) && !isNotBlank(graphNode.getSceneType())) {
                    continue;
                }
                refreshNode = new LineageMaintenanceResultDTO.RefreshNode();
                refreshNode.setGraphNodeId(graphNode.getId());
                refreshNode.setNodeType(graphNode.getNodeType());
                refreshNode.setSceneType(graphNode.getSceneType());
                break;
            }
            if (refreshNode == null) {
                throw new IllegalArgumentException("保存成功但无法确定可用于刷新的图谱节点");
            }
            refreshPlan.graphResult.setRefreshNode(refreshNode);
        }
    }

    private org.neo4j.driver.types.Node findMaintenanceNode(Transaction tx, LineageMaintenanceDTO.Node candidate) {
        if (candidate == null) {
            return null;
        }
        Map<String, Object> properties = candidate.getProperties() == null
                ? Collections.emptyMap() : candidate.getProperties();
        String label = "TASK".equals(candidate.getNodeType()) ? "Task" : "Table";
        Result result;
        if (candidate.getNeo4jId() != null) {
            result = tx.run("MATCH (node:" + label + ") WHERE id(node) = $nodeId RETURN node LIMIT 1",
                    Values.parameters("nodeId", candidate.getNeo4jId()));
        } else if ("TASK".equals(candidate.getNodeType())) {
            Long taskId = toLong(properties.get("taskId"));
            if (taskId == null) {
                return null;
            }
            result = tx.run("MATCH (node:Task {taskId: $taskId}) RETURN node LIMIT 1",
                    Values.parameters("taskId", taskId));
        } else {
            String datasourceHostPort = toStr(properties.get("datasourceHostPort"));
            String tableName = toStr(properties.get("tableName"));
            if (!isNotBlank(datasourceHostPort) || !isNotBlank(tableName)) {
                return null;
            }
            result = tx.run("MATCH (node:Table {datasourceHostPort: $datasourceHostPort, tableName: $tableName}) "
                            + "RETURN node LIMIT 1",
                    Values.parameters("datasourceHostPort", datasourceHostPort, "tableName", tableName));
        }
        return result.hasNext() ? result.next().get("node").asNode() : null;
    }

    private long mergeMaintenanceNode(Transaction tx, LineageMaintenanceDTO.Node node) {
        if (node.getNeo4jId() != null) {
            String label = "TABLE".equals(node.getNodeType()) ? "Table" : "Task";
            Result existingNode = tx.run("MATCH (node:" + label + ") WHERE id(node) = $nodeId "
                            + "RETURN id(node) AS nodeId",
                    Values.parameters("nodeId", node.getNeo4jId()));
            if (!existingNode.hasNext()) {
                throw new IllegalArgumentException("图谱节点不存在或已发生变化：" + node.getGraphId());
            }
            return existingNode.next().get("nodeId").asLong();
        }

        Map<String, Object> properties = new HashMap<>();
        if (node.getProperties() != null) {
            node.getProperties().forEach((key, value) -> {
                if (key != null && value != null) {
                    properties.put(key, value);
                }
            });
        }
        Result result;
        if ("TABLE".equals(node.getNodeType())) {
            String datasourceHostPort = toStr(properties.get("datasourceHostPort"));
            String tableName = toStr(properties.get("tableName"));
            if (!isNotBlank(datasourceHostPort) || !isNotBlank(tableName)) {
                throw new IllegalArgumentException("表节点缺少datasourceHostPort或tableName：" + node.getGraphId());
            }
            result = tx.run("MERGE (node:Table {datasourceHostPort: $datasourceHostPort, tableName: $tableName}) "
                            + "SET node += $properties RETURN id(node) AS nodeId",
                    Values.parameters("datasourceHostPort", datasourceHostPort, "tableName", tableName, "properties", properties));
        } else {
            Long taskId = toLong(properties.get("taskId"));
            if (taskId == null) {
                throw new IllegalArgumentException("任务节点缺少taskId：" + node.getGraphId());
            }
            result = tx.run("MERGE (node:Task {taskId: $taskId}) "
                            + "SET node += $properties RETURN id(node) AS nodeId",
                    Values.parameters("taskId", taskId, "properties", properties));
        }
        if (!result.hasNext()) {
            throw new IllegalArgumentException("无法创建或定位节点：" + node.getGraphId());
        }
        return result.next().get("nodeId").asLong();
    }

    private MaintenanceCreatedRel mergeMaintenanceRelationship(Transaction tx, long sourceNodeId, long targetNodeId,
                                                                 LineageMaintenanceDTO.Edge edge) {
        String token = UUID.randomUUID().toString();
        Map<String, Object> properties = buildMaintenanceRelProperties(edge);
        String cypher = "MATCH (source), (target) WHERE id(source) = $sourceNodeId AND id(target) = $targetNodeId "
                + "MERGE (source)-[rel:" + edge.getType() + "]->(target) "
                + "ON CREATE SET rel.__maintenanceToken = $token, rel.lineageSource = 'MANUAL', rel.createTime = timestamp() "
                + "WITH collect(rel) AS matchedRels WITH head(matchedRels) AS rel "
                + "SET rel += $properties, rel.updateTime = timestamp() "
                + "WITH rel, coalesce(rel.__maintenanceToken = $token, false) AS created "
                + "REMOVE rel.__maintenanceToken RETURN id(rel) AS relId, created";
        Record row = tx.run(cypher, Values.parameters(
                "sourceNodeId", sourceNodeId,
                "targetNodeId", targetNodeId,
                "token", token,
                "properties", properties)).single();
        return new MaintenanceCreatedRel(row.get("relId").asLong(), row.get("created").asBoolean());
    }

    private Map<String, Object> buildMaintenanceRelProperties(LineageMaintenanceDTO.Edge edge) {
        LineageMaintenanceDTO.Node table = "TABLE".equals(edge.getSource().getNodeType())
                ? edge.getSource() : edge.getTarget();
        Map<String, Object> properties = new HashMap<>();
        putIfNotNull(properties, "datasourceHostPort", table.getProperties().get("datasourceHostPort"));
        putIfNotNull(properties, "tableName", table.getProperties().get("tableName"));
        if (!"TABLE_TO_TABLE".equals(edge.getType())) {
            LineageMaintenanceDTO.Node task = "TASK".equals(edge.getSource().getNodeType())
                    ? edge.getSource() : edge.getTarget();
            putIfNotNull(properties, "taskId", task.getProperties().get("taskId"));
            putIfNotNull(properties, "taskCode", task.getProperties().get("taskCode"));
        } else {
            putIfNotNull(properties, "targetDatasourceHostPort",
                    edge.getTarget().getProperties().get("datasourceHostPort"));
            putIfNotNull(properties, "targetTableName", edge.getTarget().getProperties().get("tableName"));
        }
        return properties;
    }

    private void putIfNotNull(Map<String, Object> properties, String key, Object value) {
        if (value != null) {
            properties.put(key, value);
        }
    }

    private String maintenanceEdgeKey(String sourceGraphId, String targetGraphId, String type) {
        return sourceGraphId + "|" + type + "|" + targetGraphId;
    }

    private static class LineageMaintenancePlan {
        private final LineageMaintenanceResultDTO result = new LineageMaintenanceResultDTO();
        private final Set<Long> deleteRelIds = new LinkedHashSet<>();
        private final List<MaintenanceCreate> creates = new ArrayList<>();
        private final List<MaintenanceRefreshPlan> refreshPlans = new ArrayList<>();
    }

    private static class MaintenanceRefreshPlan {
        private final LineageMaintenanceResultDTO.GraphResult graphResult;
        private final List<LineageMaintenanceDTO.Node> candidates;

        private MaintenanceRefreshPlan(LineageMaintenanceResultDTO.GraphResult graphResult,
                                       List<LineageMaintenanceDTO.Node> candidates) {
            this.graphResult = graphResult;
            this.candidates = candidates;
        }
    }

    private static class MaintenanceOriginalRel {
        private Long id;
        private String sourceGraphId;
        private String targetGraphId;
        private String type;
        private LineageGraphNodeDTO sourceNode;
        private LineageGraphNodeDTO targetNode;

        private boolean matches(LineageMaintenanceDTO.Edge edge) {
            return Objects.equals(sourceGraphId, edge.getSource().getGraphId())
                    && Objects.equals(targetGraphId, edge.getTarget().getGraphId())
                    && Objects.equals(type, edge.getType());
        }
    }

    private static class MaintenanceCreate {
        private final LineageMaintenanceDTO.Edge edge;
        private final Long oldRelId;
        private final LineageMaintenanceResultDTO.GraphResult graphResult;

        private MaintenanceCreate(LineageMaintenanceDTO.Edge edge, Long oldRelId,
                                  LineageMaintenanceResultDTO.GraphResult graphResult) {
            this.edge = edge;
            this.oldRelId = oldRelId;
            this.graphResult = graphResult;
        }
    }

    private static class MaintenanceCreatedRel {
        private final long id;
        private final boolean created;

        private MaintenanceCreatedRel(long id, boolean created) {
            this.id = id;
            this.created = created;
        }
    }

    private void syncPageCursor(LineageGraphDTO graph, LineageGraphReqDTO req) {
        if (graph == null || req == null) {
            return;
        }
        graph.setHasMore(Boolean.TRUE.equals(req.getHasMore()));
        graph.setNextCursor(req.getNextCursor());
        graph.getScope().put("hasMore", graph.getHasMore());
        graph.getScope().put("nextCursor", graph.getNextCursor());
    }

    private List<Long> collectRootNodes(Session session, LineageGraphReqDTO req, int limit,
                                        Set<String> excludeNodeIds,
                                        Map<Long, LineageGraphNodeDTO> nodeMap,
                                        Map<Long, LineageGraphNodeDTO> allNodeMap) {
        String query = buildRootQuery(req);
        if (query == null) {
            return Collections.emptyList();
        }
        Result result = session.run(query, buildGraphParams(req, limit, rootQueryLimit(req, limit)));
        List<Long> rootNodeIds = new ArrayList<>();
        int seedLimit = normalizeSeedLimit(req == null ? null : req.getSeedLimit());
        Long lastAcceptedRootId = null;
        while (result.hasNext()) {
            org.neo4j.driver.types.Node root = result.next().get("root").asNode();
            if (Boolean.TRUE.equals(req.getGlobalLineageSeed()) && rootNodeIds.size() >= seedLimit) {
                req.setHasMore(Boolean.TRUE);
                req.setNextCursor(lastAcceptedRootId == null ? null : String.valueOf(lastAcceptedRootId));
                break;
            }
            collectGraphNode(root, excludeNodeIds, limit, nodeMap, allNodeMap);
            rootNodeIds.add(root.id());
            lastAcceptedRootId = root.id();
        }
        if (Boolean.TRUE.equals(req.getGlobalLineageSeed()) && !Boolean.TRUE.equals(req.getHasMore())) {
            req.setHasMore(Boolean.FALSE);
            req.setNextCursor(null);
        }
        return rootNodeIds;
    }

    private int rootQueryLimit(LineageGraphReqDTO req, int limit) {
        if (req != null && Boolean.TRUE.equals(req.getGlobalLineageSeed())) {
            return normalizeSeedLimit(req.getSeedLimit()) + 1;
        }
        return limit;
    }

    private String buildRootQuery(LineageGraphReqDTO req) {
        if (req == null) {
            return null;
        }
        if (Boolean.TRUE.equals(req.getGlobalLineageSeed())) {
            return "MATCH (root:Table)-[:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]-() "
                    + "WHERE id(root) > $globalSeedCursor "
                    + "AND ($datasourceHostPort IS NULL OR root.datasourceHostPort = $datasourceHostPort) "
                    + "AND ($dbName IS NULL OR root.dbName = $dbName) "
                    + "AND ($sid IS NULL OR root.sid = $sid) "
                    + "WITH DISTINCT root "
                    + "ORDER BY id(root) "
                    + "RETURN root LIMIT $rootLimit";
        }
        if (req.getRootNeo4jId() != null) {
            String rootLabel = "TASK".equalsIgnoreCase(req.getNodeType()) ? "Task" : "Table";
            String taskTypeFilter = "Task".equals(rootLabel) ? taskTypeFilter(req) : "";
            return "MATCH (root:" + rootLabel + ") WHERE id(root) = $rootNeo4jId"
                    + taskTypeFilter + " RETURN root LIMIT 1";
        }
        if (req.getTableSeeds() != null && !req.getTableSeeds().isEmpty()) {
            return "UNWIND $tableSeeds AS seed "
                    + "MATCH (root:Table {datasourceHostPort: seed.datasourceHostPort, tableName: seed.tableName}) "
                    + "RETURN DISTINCT root LIMIT $limit";
        }
        if (req.getTaskIds() != null && !req.getTaskIds().isEmpty()) {
            String taskTypeFilter = taskTypeFilter(req);
            return "UNWIND $taskIds AS taskId "
                    + "MATCH (root:Task {taskId: taskId})-[:TABLE_TO_TASK|TASK_TO_TABLE]-() "
                    + (taskTypeFilter.isEmpty() ? "" : "WHERE root.type IN $taskTypes ")
                    + "RETURN DISTINCT root LIMIT $limit";
        }
        if (isNotBlank(req.getTableName())) {
            return "MATCH (root:Table {datasourceHostPort: $datasourceHostPort, tableName: $tableName}) "
                    + "RETURN root LIMIT 1";
        }
        if (req.getTaskId() != null) {
            String taskTypeFilter = taskTypeFilter(req);
            return "MATCH (root:Task {taskId: $taskId})"
                    + (taskTypeFilter.isEmpty() ? "" : " WHERE root.type IN $taskTypes")
                    + " RETURN root LIMIT 1";
        }
        if (isNotBlank(req.getDatasourceHostPort())) {
            return "MATCH (root:Table) "
                    + "WHERE root.datasourceHostPort = $datasourceHostPort "
                    + "AND ($dbName IS NULL OR root.dbName = $dbName) "
                    + "AND ($sid IS NULL OR root.sid = $sid) "
                    + "RETURN DISTINCT root LIMIT $limit";
        }
        return null;
    }

    /**
     * 仅为任务场景添加任务类型过滤，避免不同场景的同一任务 ID 产生歧义。
     */
    private String taskTypeFilter(LineageGraphReqDTO req) {
        if (req == null || !isNotBlank(req.getSceneType())) {
            return "";
        }
        if ("INTEGRATION_TASK".equalsIgnoreCase(req.getSceneType())
                || "DEVELOPMENT_TASK".equalsIgnoreCase(req.getSceneType())
                || "API_SERVICE".equalsIgnoreCase(req.getSceneType())
                || "ALL_DB_SYNC_TASK".equalsIgnoreCase(req.getSceneType())) {
            return " AND root.type IN $taskTypes";
        }
        return "";
    }

    private List<String> taskTypesForScene(String sceneType) {
        if ("INTEGRATION_TASK".equalsIgnoreCase(sceneType)) {
            return Arrays.asList("1", "2");
        }
        if ("DEVELOPMENT_TASK".equalsIgnoreCase(sceneType)) {
            return Collections.singletonList("3");
        }
        if ("ALL_DB_SYNC_TASK".equalsIgnoreCase(sceneType)) {
            return Arrays.asList( "5");
        }
        if ("API_SERVICE".equalsIgnoreCase(sceneType)) {
            return Collections.singletonList("6");
        }
        return Collections.emptyList();
    }

    private void collectLineageByLayer(Session session, List<Long> rootNodeIds, String direction, int depth, int limit,
                                       Set<String> excludeNodeIds, Set<String> excludeEdgeIds,
                                       Map<Long, LineageGraphNodeDTO> nodeMap,
                                       Map<Long, LineageGraphNodeDTO> allNodeMap,
                                       Map<Long, LineageGraphEdgeDTO> edgeMap) {
        Set<Long> visitedNodeIds = new LinkedHashSet<>(rootNodeIds);
        List<Long> frontier = new ArrayList<>(rootNodeIds);
        int edgeLimit = Math.max(limit * 3, 100);
        int hopLimit = Math.min(Math.max(limit * 4, 200), 5000);
        for (int i = 0; i < depth && !frontier.isEmpty() && nodeMap.size() < limit && edgeMap.size() < edgeLimit; i++) {
            Result result = session.run(buildLayerQuery(direction),
                    Values.parameters("nodeIds", frontier, "hopLimit", hopLimit));
            List<Long> nextFrontier = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                org.neo4j.driver.types.Node source = record.get("source").asNode();
                org.neo4j.driver.types.Node target = record.get("target").asNode();
                Relationship rel = record.get("rel").asRelationship();
                collectGraphNode(source, excludeNodeIds, limit, nodeMap, allNodeMap);
                collectGraphNode(target, excludeNodeIds, limit, nodeMap, allNodeMap);
                collectGraphEdge(rel, excludeNodeIds, excludeEdgeIds, edgeLimit, nodeMap, allNodeMap, edgeMap);

                Long nextNodeId = record.get("nextNodeId").asLong();
                if (visitedNodeIds.add(nextNodeId)) {
                    nextFrontier.add(nextNodeId);
                }
            }
            frontier = nextFrontier;
        }
    }

    private String buildLayerQuery(String direction) {
        if ("UP".equals(direction)) {
            return "MATCH (target) "
                    + "WHERE id(target) IN $nodeIds "
                    + "MATCH (source)-[rel:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]->(target) "
                    + "RETURN source, target, rel, id(source) AS nextNodeId LIMIT $hopLimit";
        }
        if ("DOWN".equals(direction)) {
            return "MATCH (source) "
                    + "WHERE id(source) IN $nodeIds "
                    + "MATCH (source)-[rel:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]->(target) "
                    + "RETURN source, target, rel, id(target) AS nextNodeId LIMIT $hopLimit";
        }
        throw new IllegalArgumentException("分层血缘查询方向只支持UP或DOWN");
    }

    private void collectGraphNode(org.neo4j.driver.types.Node node, Set<String> excludeNodeIds, int limit,
                                  Map<Long, LineageGraphNodeDTO> nodeMap,
                                  Map<Long, LineageGraphNodeDTO> allNodeMap) {
        LineageGraphNodeDTO graphNode = allNodeMap.get(node.id());
        if (graphNode == null) {
            graphNode = toGraphNode(node);
            allNodeMap.put(node.id(), graphNode);
        }
        if (!excludeNodeIds.contains(graphNode.getId()) && (nodeMap.containsKey(node.id()) || nodeMap.size() < limit)) {
            nodeMap.putIfAbsent(node.id(), graphNode);
        }
    }

    private void collectGraphEdge(Relationship rel, Set<String> excludeNodeIds, Set<String> excludeEdgeIds, int edgeLimit,
                                  Map<Long, LineageGraphNodeDTO> nodeMap,
                                  Map<Long, LineageGraphNodeDTO> allNodeMap,
                                  Map<Long, LineageGraphEdgeDTO> edgeMap) {
        if (edgeMap.size() >= edgeLimit) {
            return;
        }
        LineageGraphEdgeDTO edge = toGraphEdge(rel, allNodeMap);
        if (edge.getSource() == null || edge.getTarget() == null || excludeEdgeIds.contains(edge.getId())) {
            return;
        }
        boolean sourceVisible = nodeMap.containsKey(rel.startNodeId()) || excludeNodeIds.contains(edge.getSource());
        boolean targetVisible = nodeMap.containsKey(rel.endNodeId()) || excludeNodeIds.contains(edge.getTarget());
        if (!sourceVisible || !targetVisible) {
            return;
        }
        edgeMap.putIfAbsent(rel.id(), edge);
    }

    private String buildGraphQuery(LineageGraphReqDTO req, String direction, int depth) {
        if (req == null) {
            return null;
        }
        String pathPattern = buildPathPattern(direction, depth);
        if (req.getRootNeo4jId() != null) {
            String rootLabel = "TASK".equalsIgnoreCase(req.getNodeType()) ? "Task" : "Table";
            return "MATCH (root:" + rootLabel + ") WHERE id(root) = $rootNeo4jId "
                    + "OPTIONAL MATCH p=" + pathPattern
                    + " RETURN CASE WHEN p IS NULL THEN [root] ELSE nodes(p) END AS nodes, "
                    + "CASE WHEN p IS NULL THEN [] ELSE relationships(p) END AS rels LIMIT $limit";
        }
        if (req.getTableSeeds() != null && !req.getTableSeeds().isEmpty()) {
            // 范围查询解析出一批表叶子后，以这些表作为根节点批量查询血缘。
            return "UNWIND $tableSeeds AS seed "
                    + "MATCH (root:Table {datasourceHostPort: seed.datasourceHostPort, tableName: seed.tableName}) "
                    + "OPTIONAL MATCH p=" + pathPattern
                    + " RETURN CASE WHEN p IS NULL THEN [root] ELSE nodes(p) END AS nodes, "
                    + "CASE WHEN p IS NULL THEN [] ELSE relationships(p) END AS rels "
                    + "LIMIT $limit";
        }
        if (req.getTaskIds() != null && !req.getTaskIds().isEmpty()) {
            // 点击任务类目时会解析成一批任务叶子，以任务 ID 批量查询血缘。
            return "UNWIND $taskIds AS taskId "
                    + "MATCH (root:Task {taskId: taskId}) "
                    + "OPTIONAL MATCH p=" + pathPattern
                    + " RETURN CASE WHEN p IS NULL THEN [root] ELSE nodes(p) END AS nodes, "
                    + "CASE WHEN p IS NULL THEN [] ELSE relationships(p) END AS rels "
                    + "LIMIT $limit";
        }
        if (isNotBlank(req.getTableName())) {
            // 表级查询是精确查询，即使没有任何关系，也返回当前表节点作为空链路根节点。
            return "MATCH (root:Table {datasourceHostPort: $datasourceHostPort, tableName: $tableName}) " + "OPTIONAL MATCH p=" + pathPattern + " RETURN CASE WHEN p IS NULL THEN [root] ELSE nodes(p) END AS nodes, " + "CASE WHEN p IS NULL THEN [] ELSE relationships(p) END AS rels " + "LIMIT $limit";
        }
        if (req.getTaskId() != null) {
            // 任务级查询用于点击任务节点后的继续展开。
            return "MATCH (root:Task {taskId: $taskId}) " + "OPTIONAL MATCH p=" + pathPattern + " RETURN CASE WHEN p IS NULL THEN [root] ELSE nodes(p) END AS nodes, " + "CASE WHEN p IS NULL THEN [] ELSE relationships(p) END AS rels " + "LIMIT $limit";
        }
        if (isNotBlank(req.getDatasourceHostPort())) {
            // 数据源、库、Schema 属于范围查询，只返回有关系的路径，避免首屏出现大量孤立表。
            return "MATCH (root:Table) " + "WHERE root.datasourceHostPort = $datasourceHostPort " + "AND ($dbName IS NULL OR root.dbName = $dbName) " + "AND ($sid IS NULL OR root.sid = $sid) " + "MATCH p=" + pathPattern + " RETURN nodes(p) AS nodes, relationships(p) AS rels " + "LIMIT $limit";
        }
        return null;
    }

    private String buildPathPattern(String direction, int depth) {
        if ("UP".equals(direction)) {
            // 上游：从当前节点沿入边方向查找。
            return "(root)<-[:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE*1.." + depth + "]-(n)";
        }
        if ("DOWN".equals(direction)) {
            // 下游：从当前节点沿出边方向查找。
            return "(root)-[:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE*1.." + depth + "]->(n)";
        }
        // 全链路：双向查询。
        return "(root)-[:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE*1.." + depth + "]-(n)";
    }

    private Value buildGraphParams(LineageGraphReqDTO req, int limit, int rootLimit) {
        return Values.parameters("datasourceHostPort", req.getDatasourceHostPort(),
                "tableName", req.getTableName(),
                "taskId", req.getTaskId(),
                "rootNeo4jId", req.getRootNeo4jId(),
                "taskIds", emptyIfNull(req.getTaskIds()),
                "taskTypes", taskTypesForScene(req.getSceneType()),
                "tableSeeds", toSeedParams(req),
                "dbName", blankToNull(req.getDbName()),
                "sid", blankToNull(req.getSid()),
                "globalSeedCursor", req.getGlobalSeedCursor() == null ? -1L : req.getGlobalSeedCursor(),
                "rootLimit", rootLimit,
                "limit", limit);
    }

    private List<Map<String, Object>> toSeedParams(LineageGraphReqDTO req) {
        List<Map<String, Object>> seeds = new ArrayList<>();
        if (req == null || req.getTableSeeds() == null) {
            return seeds;
        }
        for (LineageTableSeedDTO seed : req.getTableSeeds()) {
            if (seed == null || !isNotBlank(seed.getDatasourceHostPort()) || !isNotBlank(seed.getTableName())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("datasourceHostPort", seed.getDatasourceHostPort());
            item.put("tableName", seed.getTableName());
            seeds.add(item);
        }
        return seeds;
    }

    private LineageGraphNodeDTO toGraphNode(org.neo4j.driver.types.Node node) {
        Map<String, Object> properties = node.asMap(Value::asObject);
        LineageGraphNodeDTO graphNode = new LineageGraphNodeDTO();
        graphNode.setNeo4jId(node.id());
        graphNode.setProperties(properties);

        if (hasLabel(node, "Table")) {
            // Table 节点使用数据源连接标识 + 全限定表名生成前端稳定 ID。
            graphNode.setNodeType("TABLE");
            graphNode.setSceneType("SOURCE_SYSTEM");
            graphNode.setName(toStr(properties.get("name")));
            graphNode.setTableName(toStr(properties.get("tableName")));
            graphNode.setTableId(toLong(properties.get("tableId")));
            graphNode.setTableComment(toStr(properties.get("tableComment")));
            graphNode.setDatasourceHostPort(toStr(properties.get("datasourceHostPort")));
            graphNode.setDatasourceName(toStr(properties.get("datasourceName")));
            graphNode.setDatasourceType(toStr(properties.get("datasourceType")));
            graphNode.setDbName(toStr(properties.get("dbName")));
            graphNode.setDbComment(toStr(properties.get("dbComment")));
            graphNode.setSid(toStr(properties.get("sid")));
            graphNode.setLabel(firstNotBlank(graphNode.getName(), graphNode.getTableName()));
            graphNode.setSubLabel(graphNode.getDatasourceName());
            // 未关联到数据连接的表无法生成 host:port 业务键，降级使用 Neo4j 节点 ID。
            graphNode.setId(isNotBlank(graphNode.getDatasourceHostPort())
                    ? "table:" + graphNode.getDatasourceHostPort() + ":" + graphNode.getTableName()
                    : "tableNode:" + node.id());
        } else {
            // Task 节点优先使用业务任务 ID，缺失时退化为 Neo4j 节点 ID。
            graphNode.setNodeType("TASK");
            graphNode.setName(toStr(properties.get("name")));
            graphNode.setTaskId(toLong(properties.get("taskId")));
            graphNode.setTaskCode(toStr(properties.get("taskCode")));
            graphNode.setType(toStr(properties.get("type")));
            graphNode.setTaskType(toStr(properties.get("taskType")));
            graphNode.setSceneType(resolveTaskSceneType(graphNode.getType()));
            graphNode.setLabel(firstNotBlank(graphNode.getName(), graphNode.getTaskCode()));
            graphNode.setSubLabel(graphNode.getTaskType());
            graphNode.setId(graphNode.getTaskId() == null ? "taskNode:" + node.id() : "task:" + graphNode.getTaskId());
        }
        return graphNode;
    }

    private String resolveTaskSceneType(String taskType) {
        if ("1".equals(taskType) || "2".equals(taskType)) {
            return "INTEGRATION_TASK";
        }
        if ("3".equals(taskType)) {
            return "DEVELOPMENT_TASK";
        }
        if ("5".equals(taskType)) {
            return "ALL_DB_SYNC_TASK";
        }
        if ("6".equals(taskType)) {
            return "API_SERVICE";
        }
        return null;
    }

    private LineageGraphEdgeDTO toGraphEdge(Relationship rel, Map<Long, LineageGraphNodeDTO> nodeMap) {
        LineageGraphEdgeDTO edge = new LineageGraphEdgeDTO();
        edge.setNeo4jId(rel.id());
        edge.setId("edge:" + rel.id());
        edge.setType(rel.type());
        edge.setLabel(rel.type());
        edge.setProperties(rel.asMap(Value::asObject));
        LineageGraphNodeDTO source = nodeMap.get(rel.startNodeId());
        LineageGraphNodeDTO target = nodeMap.get(rel.endNodeId());
        edge.setSource(source == null ? null : source.getId());
        edge.setTarget(target == null ? null : target.getId());
        return edge;
    }

    private void fillCounts(Session session, Map<Long, LineageGraphNodeDTO> nodeMap) {
        List<Long> nodeIds = new ArrayList<>(nodeMap.keySet());
        // 逐节点统计直接上下游数量，前端据此展示“上游/下游”和是否可展开。
        Result result = session.run("UNWIND $nodeIds AS nodeId "
                + "MATCH (n) WHERE id(n) = nodeId "
                + "OPTIONAL MATCH (n)<-[inRel:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]-() "
                + "WITH nodeId, n, count(DISTINCT inRel) AS upstreamCount "
                + "OPTIONAL MATCH (n)-[outRel:TABLE_TO_TASK|TASK_TO_TABLE|TABLE_TO_TABLE]->() "
                + "RETURN nodeId, upstreamCount, count(DISTINCT outRel) AS downstreamCount", Values.parameters("nodeIds", nodeIds));
        while (result.hasNext()) {
            Record row = result.next();
            LineageGraphNodeDTO node = nodeMap.get(row.get("nodeId").asLong());
            if (node != null) {
                long upstreamCount = row.get("upstreamCount").asLong();
                long downstreamCount = row.get("downstreamCount").asLong();
                node.setUpstreamCount(upstreamCount);
                node.setDownstreamCount(downstreamCount);
                node.setHasUpstream(upstreamCount > 0);
                node.setHasDownstream(downstreamCount > 0);
                node.setExpandable(upstreamCount > 0 || downstreamCount > 0);
            }
        }
    }

    private void markRoot(LineageGraphDTO graph, LineageGraphReqDTO req, Map<Long, LineageGraphNodeDTO> nodeMap) {
        // 精确查询时优先高亮请求中的表或任务；范围查询则默认高亮返回的第一个节点。
        for (LineageGraphNodeDTO node : nodeMap.values()) {
            boolean current = false;
            if (req.getRootNeo4jId() != null) {
                current = Objects.equals(node.getNeo4jId(), req.getRootNeo4jId());
            } else if ("TABLE".equals(node.getNodeType()) && isNotBlank(req.getTableName())) {
                current = Objects.equals(node.getDatasourceHostPort(), req.getDatasourceHostPort()) && Objects.equals(node.getTableName(), req.getTableName());
            } else if ("TABLE".equals(node.getNodeType()) && req.getTableSeeds() != null && !req.getTableSeeds().isEmpty()) {
                LineageTableSeedDTO firstSeed = req.getTableSeeds().get(0);
                current = Objects.equals(node.getDatasourceHostPort(), firstSeed.getDatasourceHostPort()) && Objects.equals(node.getTableName(), firstSeed.getTableName());
            } else if ("TASK".equals(node.getNodeType()) && req.getTaskId() != null) {
                current = Objects.equals(node.getTaskId(), req.getTaskId());
            } else if ("TASK".equals(node.getNodeType()) && req.getTaskIds() != null && !req.getTaskIds().isEmpty()) {
                current = Objects.equals(node.getTaskId(), req.getTaskIds().get(0));
            }
            if (current) {
                node.setCurrent(true);
                graph.setRootNodeId(node.getId());
                graph.setRootType(node.getNodeType());
                return;
            }
        }
        if (graph.getRootNodeId() == null && !nodeMap.isEmpty()) {
            LineageGraphNodeDTO first = nodeMap.values().iterator().next();
            first.setCurrent(true);
            graph.setRootNodeId(first.getId());
            graph.setRootType(first.getNodeType());
        }
    }

    private void fillScope(LineageGraphDTO graph, LineageGraphReqDTO req) {
        if (req == null) {
            return;
        }
        graph.getScope().put("sceneType", req.getSceneType());
        graph.getScope().put("metadataType", req.getMetadataType());
        graph.getScope().put("nodeType", req.getNodeType());
        graph.getScope().put("rootNeo4jId", req.getRootNeo4jId());
        graph.getScope().put("datasourceHostPort", req.getDatasourceHostPort());
        graph.getScope().put("dbName", req.getDbName());
        graph.getScope().put("sid", req.getSid());
        graph.getScope().put("tableName", req.getTableName());
        graph.getScope().put("taskId", req.getTaskId());
        graph.getScope().put("taskIds", req.getTaskIds());
        graph.getScope().put("globalLineageSeed", req.getGlobalLineageSeed());
        graph.getScope().put("globalSeedCursor", req.getGlobalSeedCursor());
        graph.getScope().put("seedLimit", req.getSeedLimit());
        graph.getScope().put("cursor", req.getCursor());
        graph.getScope().put("nextCursor", req.getNextCursor());
        graph.getScope().put("hasMore", req.getHasMore());
        graph.getScope().put("direction", normalizeDirection(req.getDirection()));
        graph.getScope().put("depth", normalizeDepth(req.getDepth()));
        graph.getScope().put("limit", normalizeLimit(req.getLimit()));
        if (req.getScope() != null) {
            graph.getScope().putAll(req.getScope());
        }
    }

    private int normalizeDepth(Integer depth) {
        if (depth == null || depth < 1) {
            return 1;
        }
        return Math.min(depth, 6);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 100;
        }
        return Math.min(limit, 1000);
    }

    private int normalizeSeedLimit(Integer seedLimit) {
        if (seedLimit == null || seedLimit < 1) {
            return 20;
        }
        return Math.min(seedLimit, 100);
    }

    private String normalizeDirection(String direction) {
        if ("UP".equalsIgnoreCase(direction)) {
            return "UP";
        }
        if ("DOWN".equalsIgnoreCase(direction)) {
            return "DOWN";
        }
        // 兼容前端使用 FULL 表示完整上下游链路。
        if ("ALL".equalsIgnoreCase(direction) || "FULL".equalsIgnoreCase(direction)) {
            return "ALL";
        }
        return "ALL";
    }

    private <T> List<T> emptyIfNull(Collection<T> data) {
        return data == null ? Collections.emptyList() : new ArrayList<>(data);
    }

    private boolean hasLabel(org.neo4j.driver.types.Node node, String label) {
        for (String item : node.labels()) {
            if (Objects.equals(item, label)) {
                return true;
            }
        }
        return false;
    }

    private String firstNotBlank(String first, String second) {
        return isNotBlank(first) ? first : second;
    }

    private boolean isNotBlank(String value) {
        return value != null && value.trim().length() > 0;
    }

    private String blankToNull(String value) {
        return isNotBlank(value) ? value : null;
    }

    private String toStr(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 查询关系数据
     *
     * @param lineageDto
     * @param datasourceHostPort
     * @param tableName
     */
    void getRels(LineageDTO lineageDto, String datasourceHostPort, String tableName) {
        // 创建驱动
        Driver driver = GraphDatabase.driver(neo4jProperties.getUri(), AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword()));

        // 打开自动关闭的会话
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            // 3. 执行 Cypher，返回一条记录
            Result result = session.run("MATCH (currentTable:Table {datasourceHostPort: $datasourceHostPort,tableName: $tableName})   " +
                    "OPTIONAL MATCH (currentTable)<-[r1:TASK_TO_TABLE]-(sourceTask:Task)   " +
                    "OPTIONAL MATCH (sourceTask)<-[r2:TABLE_TO_TASK]-(sourceTable:Table)   " +
                    "OPTIONAL MATCH (currentTable)-[r3:TABLE_TO_TASK]->(targetTask:Task)   " +
                    "OPTIONAL MATCH (targetTask)-[r4:TASK_TO_TABLE]->(targetTable:Table)   " +
                    "RETURN     " +
                    "collect(DISTINCT sourceTask) + collect(DISTINCT targetTask) AS tasks,   " +
                    "currentTable + collect(DISTINCT sourceTable) + collect(DISTINCT targetTable)  AS tables,   " +
                    "collect(DISTINCT r1) + collect(DISTINCT r2) +collect(DISTINCT r3) + collect(DISTINCT r4) AS  rels ",
                    Values.parameters("datasourceHostPort", datasourceHostPort, "tableName", tableName));
            if (result.hasNext()) {
                Record row = result.single();
                List<Relationship> rels = row.get("rels").asList(v -> v.asRelationship());
                List<JSONObject> relsList = new ArrayList<>();
                for (Relationship rel : rels) {
                    JSONObject relObj = new JSONObject();
                    relObj.put("startNodeId", rel.startNodeId());
                    relObj.put("endNodeId", rel.endNodeId());
                    relObj.put("type", rel.type());
                    JSONObject properties = new JSONObject();
                    properties.put("taskId", rel.get("taskId").asLong());
                    properties.put("taskCode", rel.get("taskCode").asString());
                    properties.put("datasourceHostPort", rel.get("datasourceHostPort").asString());
                    properties.put("tableName", rel.get("tableName").asString());
                    relObj.put("properties", properties);
                    relsList.add(relObj);
                }
                lineageDto.setRels(relsList);
            }
        } finally {
            if (driver != null) {
                driver.close();
            }
        }
    }

    /**
     * 查询关系数据
     *
     * @param lineageDto
     * @param taskId
     * @param type 任务类型;1：离线任务(数据集成) 2：实时任务 3：数据开发任务 4：作业任务  5:整库同步 6:数据服务
     */
    void getRels(LineageDTO lineageDto, Long taskId,String type) {
        // 创建驱动
        Driver driver = GraphDatabase.driver(neo4jProperties.getUri(), AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword()));

        // 打开自动关闭的会话
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            // 3. 执行 Cypher，返回一条记录
            Result result = session.run(
                    "MATCH (currentTask:Task {taskId: $taskId,type: $type}) " +
                            " " +
                            "// 1. 先收集上游节点和关系\n" +
                            "OPTIONAL MATCH (currentTask)<-[r2:TABLE_TO_TASK]-(sourceTable:Table) " +
                            "OPTIONAL MATCH (sourceTable)<-[r1:TASK_TO_TABLE]-(sourceTask:Task) " +
                            "WITH currentTask, " +
                            "     collect(DISTINCT sourceTask) AS sTasks, " +
                            "     collect(DISTINCT sourceTable) AS sTables, " +
                            "     collect(DISTINCT r1) + collect(DISTINCT r2) AS upRels " +
                            " " +
                            "// 2. 再收集下游节点和关系\n" +
                            "OPTIONAL MATCH (currentTask)-[r3:TASK_TO_TABLE]->(targetTable:Table) " +
                            "OPTIONAL MATCH (targetTable)-[r4:TABLE_TO_TASK]->(targetTask:Task) " +
                            "WITH currentTask, sTasks, sTables, upRels, " +
                            "     collect(DISTINCT targetTask) AS tTasks, " +
                            "     collect(DISTINCT targetTable) AS tTables, " +
                            "     collect(DISTINCT r3) + collect(DISTINCT r4) AS downRels " +
                            " " +
                            "// 3. 把上下游的所有数据拼起来\n" +
                            "WITH [currentTask] + sTasks + tTasks AS allTasks, " +
                            "     sTables + tTables AS allTables, " +
                            "     upRels + downRels AS allRels " +
                            " " +
                            "// 4. 通过 UNWIND + DISTINCT 依次对三大集合做强行去重\n" +
                            "UNWIND allTasks AS tsk " +
                            "WITH allTables, allRels, collect(DISTINCT tsk) AS uniqueTasks " +
                            " " +
                            "UNWIND allTables AS tbl " +
                            "WITH uniqueTasks, allRels, collect(DISTINCT tbl) AS uniqueTables " +
                            " " +
                            "UNWIND allRels AS rel " +
                            "WITH uniqueTasks, uniqueTables, collect(DISTINCT rel) AS uniqueRels " +
                            " " +
                            "RETURN uniqueTasks AS tasks, uniqueTables AS tables, uniqueRels AS rels",
                    Values.parameters("taskId", taskId,"type", type)
            );
            if (result.hasNext()) {
                Record row = result.single();
                List<Relationship> rels = row.get("rels").asList(v -> v.asRelationship());
                List<JSONObject> relsList = new ArrayList<>();
                for (Relationship rel : rels) {
                    JSONObject relObj = new JSONObject();
                    relObj.put("startNodeId", rel.startNodeId());
                    relObj.put("endNodeId", rel.endNodeId());
                    relObj.put("type", rel.type());
                    JSONObject properties = new JSONObject();
                    properties.put("taskId", rel.get("taskId").asLong());
                    properties.put("taskCode", rel.get("taskCode").asString());
                    properties.put("datasourceHostPort", rel.get("datasourceHostPort").asString());
                    properties.put("tableName", rel.get("tableName").asString());
                    relObj.put("properties", properties);
                    relsList.add(relObj);
                }
                lineageDto.setRels(relsList);
            }
        } finally {
            if (driver != null) {
                driver.close();
            }
        }
    }

        /**
     * Delete a task using its legacy task-ID-only lookup.
     *
     * @param taskId task identifier
     * @deprecated Use {@link #deleteTaskByTaskIdAndType(Long, String)} when the task type
     * is known. This legacy method is retained for callers that only have a task ID.
     */
    @Deprecated
    @Transactional("neo4jTransactionManager")
    public void deleteTask(Long taskId) {
        TaskNode oldTaskNode = taskRepository.findByTaskId(taskId);
        if (oldTaskNode != null) {
            taskRepository.delete(oldTaskNode);
        }
    }

    /**
     * 删除任务
     *
     * @param taskId
     */
    @Transactional("neo4jTransactionManager")
    public void deleteTaskByTaskIdAndType(Long taskId, String type) {
        TaskNode oldTaskNode = taskRepository.findByTaskIdAndType(taskId, type);
        if (oldTaskNode != null) {
            taskRepository.delete(oldTaskNode);
        }
    }

    /**
     * 手动保存数据血缘（数据开发手动设置）。
     * <p>
     * 先删除当前任务的旧关系，再按 reader / writer 分别建立
     * {@code (table)-[:TABLE_TO_TASK]->(task)} 和 {@code (task)-[:TASK_TO_TABLE]->(table)} 关系。
     * 使用显式 Cypher MERGE，只操作当前任务的边，规避 SDN 对象图级联误删其它任务关系的问题。
     *
     * @param taskNode    任务节点
     * @param readerNodes 输入表列表（reader），每条链路第一个表
     * @param writerNodes 输出表列表（writer），每条链路后续表
     */
    @Transactional("neo4jTransactionManager")
    public void saveManualLineage(TaskNode taskNode, List<TableNode> readerNodes, List<TableNode> writerNodes) {
        // 删除当前任务的旧关系（仅按 taskId 删除本任务的边，不影响其它任务）
        taskRepository.deleteRelByTaskId(taskNode.getTaskId());

        // 幂等 MERGE 任务节点
        taskRepository.upsertTask(taskNode.getTaskId(), taskNode.getName(),
                taskNode.getTaskCode(), taskNode.getType(), taskNode.getTaskType());

        // MERGE reader 表并建立 (table)-[:TABLE_TO_TASK]->(task)
        if (readerNodes != null) {
            for (TableNode r : readerNodes) {
                tableRepository.mergeTableToTask(r.getTableName(), r.getDatasourceHostPort(),
                        r.getName(), r.getDatasourceName(), r.getDatasourceType(),
                        r.getDbName(), r.getSid(), taskNode.getTaskId(), taskNode.getTaskCode());
            }
        }

        // MERGE writer 表并建立 (task)-[:TASK_TO_TABLE]->(table)
        if (writerNodes != null) {
            for (TableNode w : writerNodes) {
                tableRepository.mergeTaskToTable(w.getTableName(), w.getDatasourceHostPort(),
                        w.getName(), w.getDatasourceName(), w.getDatasourceType(),
                        w.getDbName(), w.getSid(), taskNode.getTaskId(), taskNode.getTaskCode());
            }
        }
    }

    /**
     * 保存
     *
     * @param readerTableNodeList
     * @param writerTableNodeList
     * @param taskNode
     */
    @Transactional("neo4jTransactionManager")
    public void save(List<TableNode> readerTableNodeList, List<TableNode> writerTableNodeList, TaskNode taskNode) {
        TaskNode oldTaskNode = taskRepository.findByTaskId(taskNode.getTaskId());
        if (oldTaskNode != null) {
            taskRepository.delete(oldTaskNode);
        }

        for (TableNode writerTableNode : writerTableNodeList) {
            TableNode oldWriterTableNode = tableRepository.findByTableNameAndDatasourceHostPort(writerTableNode.getTableName(), writerTableNode.getDatasourceHostPort())
                    .orElse(null);
            if (oldWriterTableNode != null) {
                writerTableNode.setId(oldWriterTableNode.getId());
                writerTableNode.setTaskToTableRels(oldWriterTableNode.getTaskToTableRels());
                writerTableNode.setTableToTaskRels(oldWriterTableNode.getTableToTaskRels());
            }
            tableRepository.save(writerTableNode);
        }

        for (TableNode readerTableNode : readerTableNodeList) {
            TableNode oldReaderTableNode = tableRepository.findByTableNameAndDatasourceHostPort(readerTableNode.getTableName(), readerTableNode.getDatasourceHostPort())
                    .orElse(null);
            if (oldReaderTableNode != null) {
                readerTableNode.setId(oldReaderTableNode.getId());
                readerTableNode.setTableToTaskRels(Stream.concat(readerTableNode.getTableToTaskRels()
                                .stream(), oldReaderTableNode.getTableToTaskRels().stream())
                        .distinct()
                        .collect(Collectors.toList()));
            }
            tableRepository.save(readerTableNode);
        }
        taskRepository.save(taskNode);
    }

    /**
     * 保存节点信息
     *
     * @param tableNodeList
     */
    @Transactional("neo4jTransactionManager")
    public void saveTable(List<TableNode> tableNodeList) {
        for (TableNode writerTableNode : tableNodeList) {
            TableNode oldWriterTableNode = tableRepository.findByTableNameAndDatasourceHostPort(writerTableNode.getTableName(), writerTableNode.getDatasourceHostPort())
                    .orElse(null);
            if (oldWriterTableNode != null) {
                writerTableNode.setId(oldWriterTableNode.getId());
                writerTableNode.setTaskToTableRels(oldWriterTableNode.getTaskToTableRels());
                writerTableNode.setTableToTaskRels(oldWriterTableNode.getTableToTaskRels());
            }
            tableRepository.save(writerTableNode);
        }
    }

    /**
     * 解析数据血缘
     * <p>
     * 将 INSERT INTO ... SELECT ... FROM ... 格式的 SQL 解析为 TableNode 列表。
     * INSERT INTO 的目标表为 writer（输出表），FROM/JOIN 引用的表为 reader（输入表）。
     * <p>
     * <b>数据源元数据从 SQL 中的 CREATE TABLE DDL 自动解析：</b>
     * <ul>
     *   <li>JDBC URL → datasourceHostPort（如 {@code jdbc:mysql://host:port/db} → host:port）</li>
     *   <li>CDC connector → datasourceHostPort（如 {@code 'hostname'='x', 'port'='y'} → x:y）</li>
     *   <li>connector 类型 → datasourceType（如 mysql-cdc → MySql，jdbc + url → 从URL提取）</li>
     * </ul>
     * <p>
     * 解析后自动保存到 Neo4j，随后查询并返回<b>经过本任务的全链路</b>（从最上游源表到最下游汇表，
     * 表级有序链路，跨任务遍历整个图）。例如 {@code A表 -> B表 -> C表}、{@code A表 -> B表 -> D表}
     *
     * @param taskNode       任务
     * @param datasourceType 数据源类型，目前支持 FlinkBatch、FlinkStream、SparkSql
     * @param sql            SQL 语句（可含 CREATE TABLE DDL + INSERT INTO）
     * @return 经过本任务的全链路列表，每条链路是一组从源到汇有序排列的 {@link TableNode}
     */
    @Transactional("neo4jTransactionManager")
    public LineageResult saveTable(TaskNode taskNode, com.alibaba.fastjson2.JSONObject parameters, String datasourceType, String sql) {
        String effectiveDatasourceType = configuredDatasourceType(parameters, datasourceType);
        if (relationalDbType(effectiveDatasourceType) != null) {
            return saveRelationalTable(taskNode, parameters, datasourceType, sql);
        }
        return saveFlinkOrSparkTable(taskNode, parameters, datasourceType, sql);
    }

    @Transactional("neo4jTransactionManager")
    public LineageResult saveRelationalTable(TaskNode taskNode, com.alibaba.fastjson2.JSONObject parameters,
                                                   String datasourceType, String sql) {
        String effectiveDatasourceType = configuredDatasourceType(parameters, datasourceType);
        com.alibaba.druid.DbType relationalDbType = relationalDbType(effectiveDatasourceType);
        if (relationalDbType == null) {
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }
        taskRepository.deleteRelByTaskId(taskNode.getTaskId());
        try {
            TableMeta meta = relationalTableMeta(parameters, effectiveDatasourceType);
            LineageResult relationalResult = meta == null ? null : parseRelationalSql(sql, relationalDbType, meta);
            if (relationalResult == null || relationalResult.getWriterNodes().isEmpty()) {
                return new LineageResult(Collections.emptyList(), Collections.emptyList());
            }
            writeRelationalLineage(taskNode, relationalResult);
            relationalResult.setDatasourceId(parameters == null ? null : parameters.getLong("datasourceId"));
            return relationalResult;
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(LineageDataService.class).error("Operation failed", ignored);
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }
    }

    @Transactional("neo4jTransactionManager")
    public LineageResult saveFlinkOrSparkTable(TaskNode taskNode, com.alibaba.fastjson2.JSONObject parameters,
                                                     String datasourceType, String sql) {
        String effectiveDatasourceType = configuredDatasourceType(parameters, datasourceType);
        if (!"FlinkBatch".equals(effectiveDatasourceType)
                && !"FlinkStream".equals(effectiveDatasourceType)
                && !"SparkSql".equals(effectiveDatasourceType)) {
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }
        taskRepository.deleteRelByTaskId(taskNode.getTaskId());
        taskRepository.upsertTask(taskNode.getTaskId(), taskNode.getName(),
                taskNode.getTaskCode(), taskNode.getType(), taskNode.getTaskType());
        try {
        // 第一步：从 CREATE TABLE DDL 中提取表级元数据（host:port, connector 类型等）
        //        SparkSQL 使用 USING ... OPTIONS (...) 语法，需专用解析器
        Map<String, TableMeta> tableMetaMap;
        if ("SparkSql".equals(effectiveDatasourceType)) {
            tableMetaMap = extractSparkCreateTableMeta(sql);
            // 同时尝试 Flink 风格解析作为补充（混合 SQL 场景）
            Map<String, TableMeta> flinkMeta = extractCreateTableMeta(sql);
            for (Map.Entry<String, TableMeta> entry : flinkMeta.entrySet()) {
                tableMetaMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
        } else {
            tableMetaMap = extractCreateTableMeta(sql);
        }

        // 第二步：解析 INSERT INTO ... SELECT 血缘关系
        LineageResult result;
        switch (effectiveDatasourceType) {
            case "FlinkBatch":
            case "FlinkStream":
                result = parseFlinkSQL(sql, tableMetaMap);
                break;
            case "SparkSql":
                result = parseSparkSQL(sql, tableMetaMap);
                break;
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + datasourceType);
        }

        // 第五步：MERGE 每个 writer 表并建立 (task)-[:TASK_TO_TABLE]->(table)
        //        使用显式 Cypher MERGE，只操作当前任务的边，规避 SDN 对象图级联误删其它任务关系的问题
        for (TableNode w : result.writerNodes) {
            tableRepository.mergeTaskToTable(w.getTableName(), w.getDatasourceHostPort(),
                    w.getName(), w.getDatasourceName(), w.getDatasourceType(),
                    w.getDbName(), w.getSid(), taskNode.getTaskId(), taskNode.getTaskCode());
        }

        // 第六步：MERGE 每个 reader 表并建立 (table)-[:TABLE_TO_TASK]->(task)
        //        同名表若既是 reader 又是 writer，两条边各自 MERGE，互不冲突
        for (TableNode r : result.readerNodes) {
            tableRepository.mergeTableToTask(r.getTableName(), r.getDatasourceHostPort(),
                    r.getName(), r.getDatasourceName(), r.getDatasourceType(),
                    r.getDbName(), r.getSid(), taskNode.getTaskId(), taskNode.getTaskCode());
        }

        // 第七步：直接基于解析结果组装链路并返回。
        return result;
        } catch (Exception ignored) {
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }
    }

    private void writeRelationalLineage(TaskNode task, LineageResult result) {
        taskRepository.upsertTask(task.getTaskId(), task.getName(), task.getTaskCode(),
                task.getType(), task.getTaskType());
        for (TableNode node : result.getWriterNodes()) {
            tableRepository.mergeTaskToTable(node.getTableName(), node.getDatasourceHostPort(),
                    node.getName(), node.getDatasourceName(), node.getDatasourceType(),
                    node.getDbName(), node.getSid(), task.getTaskId(), task.getTaskCode());
        }
        for (TableNode node : result.getReaderNodes()) {
            tableRepository.mergeTableToTask(node.getTableName(), node.getDatasourceHostPort(),
                    node.getName(), node.getDatasourceName(), node.getDatasourceType(),
                    node.getDbName(), node.getSid(), task.getTaskId(), task.getTaskCode());
        }
    }

    private com.alibaba.druid.DbType relationalDbType(String value) {
        String type = normalizeRelationalType(value);
        if (type == null) {
            return null;
        }
        switch (type) {
            case "DM8":
                return com.alibaba.druid.DbType.dm;
            case "ORACLE":
                return com.alibaba.druid.DbType.oracle;
            case "MYSQL":
                return com.alibaba.druid.DbType.mysql;
            case "KINGBASE":
                return com.alibaba.druid.DbType.kingbase;
            case "DB2":
                return com.alibaba.druid.DbType.db2;
            case "OSCAR":
                return com.alibaba.druid.DbType.oscar;
            case "HIVE":
                return com.alibaba.druid.DbType.hive;
            case "ODPS":
                return com.alibaba.druid.DbType.odps;
            default:
                return null;
        }
    }

    private String normalizeRelationalType(String value) {
        if (value == null) {
            return null;
        }
        switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "DM":
            case "DM8":
                return "DM8";
            case "MYSQL":
                return "MYSQL";
            case "ORACLE":
                return "ORACLE";
            case "KINGBASE":
            case "KINGBASE8":
                return "KINGBASE";
            case "DB2":
                return "DB2";
            case "OSCAR":
                return "OSCAR";
            case "ODPS":
                return "ODPS";
            case "HIVE":
                return "HIVE";
            default:
                return null;
        }
    }

    private TableMeta relationalTableMeta(com.alibaba.fastjson2.JSONObject parameters, String datasourceType) {
        if (parameters == null || !parameters.containsKey("datasources")) {
            return null;
        }
        com.alibaba.fastjson2.JSONObject datasource = parameters.getJSONObject("datasources");
        String type = normalizeRelationalType(configuredDatasourceType(parameters, datasourceType));
        String dbName = firstNonBlank(datasource.getString("dbname"), datasource.getString("database"));
        if (dbName == null && datasource.getString("datasourceConfig") != null) {
            try {
                dbName = com.alibaba.fastjson2.JSON.parseObject(datasource.getString("datasourceConfig"))
                        .getString("dbname");
            } catch (Exception ignored) {
                return null;
            }
        }
        String host = "ODPS".equals(type)
                ? firstNonBlank(datasource.getString("endpoint"), parameters.getString("endpoint"))
                : firstNonBlank(datasource.getString("ip"), datasource.getString("host"));
        String port = datasource.getString("port");
        if (type == null || host == null || dbName == null || (!"ODPS".equals(type) && port == null)) {
            return null;
        }

        TableMeta meta = new TableMeta();
        meta.datasourceType = type;
        meta.datasourceHostPort = "ODPS".equals(type) ? host : host + ":" + port;
        meta.datasourceName = dbName;
        meta.dbName = dbName;
        return meta;
    }

    private LineageResult parseRelationalSql(String sql, com.alibaba.druid.DbType dbType, TableMeta meta) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        LinkedHashSet<String> readers = new LinkedHashSet<>();
        LinkedHashSet<String> writers = new LinkedHashSet<>();
        for (String statementSql : splitSqlStatements(sql)) {
            // DM 的 IDENTITY DDL 不能被当前 Druid 方言解析，且普通建表不产生表间血缘。
            if (isSkippableCreateTableStatement(statementSql)) {
                continue;
            }
            List<SQLStatement> statements = SQLUtils.parseStatements(statementSql, dbType);
            if (statements.isEmpty()) {
                continue;
            }
            SQLStatement statement = statements.get(0);
            Set<String> statementWriters = new LinkedHashSet<>();
            Set<String> statementReaders = new LinkedHashSet<>();
            if (statement instanceof SQLInsertStatement) {
                SQLInsertStatement s = (SQLInsertStatement) statement;
                String writer = druidTableName(s.getTableSource());
                statementWriters.add(writer != null ? writer : s.getTableName().toString());
                collectPhysicalTables(s.getQuery() == null ? statement : s.getQuery(), statementReaders);
            } else if (statement instanceof SQLCreateTableStatement) {
                SQLCreateTableStatement s = (SQLCreateTableStatement) statement;
                if (s.getSelect() == null) {
                    continue;
                }
                statementWriters.add(druidTableName(s.getTableSource()));
                collectPhysicalTables(s.getSelect(), statementReaders);
            } else if (statement instanceof SQLMergeStatement) {
                SQLMergeStatement s = (SQLMergeStatement) statement;
                statementWriters.add(druidTableName(s.getInto()));
                collectPhysicalTables(s.getUsing(), statementReaders);
            } else {
                continue;
            }
            statementWriters.remove(null);
            if (statementWriters.isEmpty()) {
                return null;
            }
            statementReaders.removeAll(statementWriters);
            readers.addAll(statementReaders);
            writers.addAll(statementWriters);
        }
        if (writers.isEmpty()) {
            return null;
        }
        List<TableNode> readerNodes = relationalNodes(readers, meta);
        List<TableNode> writerNodes = relationalNodes(writers, meta);
        return new LineageResult(writerNodes, readerNodes);
    }

    /**
     * 按分号拆分 SQL，忽略字符串字面量和注释中的分号，避免一个不支持的 DDL 阻断后续 DML。
     */
    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int index = 0; index < sql.length(); index++) {
            char currentChar = sql.charAt(index);
            char nextChar = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';

            if (lineComment) {
                current.append(currentChar);
                if (currentChar == '\n' || currentChar == '\r') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                current.append(currentChar);
                if (currentChar == '*' && nextChar == '/') {
                    current.append(nextChar);
                    index++;
                    blockComment = false;
                }
                continue;
            }
            if (!singleQuoted && !doubleQuoted && currentChar == '-' && nextChar == '-') {
                current.append(currentChar).append(nextChar);
                index++;
                lineComment = true;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && currentChar == '/' && nextChar == '*') {
                current.append(currentChar).append(nextChar);
                index++;
                blockComment = true;
                continue;
            }
            if (currentChar == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (currentChar == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
            }
            if (currentChar == ';' && !singleQuoted && !doubleQuoted) {
                addSqlStatement(statements, current);
                continue;
            }
            current.append(currentChar);
        }
        addSqlStatement(statements, current);
        return statements;
    }

    private void addSqlStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }

    private boolean isSkippableCreateTableStatement(String sql) {
        String statement = stripLeadingSqlComments(sql);
        return statement.regionMatches(true, 0, "CREATE TABLE", 0, "CREATE TABLE".length())
                && !statement.toUpperCase(Locale.ROOT).contains(" AS SELECT ");
    }

    private String stripLeadingSqlComments(String sql) {
        String statement = sql.trim();
        while (statement.startsWith("--") || statement.startsWith("/*")) {
            if (statement.startsWith("--")) {
                int lineEnd = statement.indexOf('\n');
                statement = lineEnd < 0 ? "" : statement.substring(lineEnd + 1).trim();
            } else {
                int commentEnd = statement.indexOf("*/");
                statement = commentEnd < 0 ? "" : statement.substring(commentEnd + 2).trim();
            }
        }
        return statement;
    }

    /** 从 Druid 查询树中提取物理表，过滤 CTE 别名，避免将中间结果写入血缘。 */
    private void collectPhysicalTables(SQLObject source, Set<String> tables) {
        if (source == null) {
            return;
        }
        Set<String> ctes = new HashSet<>();
        if (source instanceof SQLSelect && ((SQLSelect) source).getWithSubQuery() != null) {
            for (SQLWithSubqueryClause.Entry entry : ((SQLSelect) source).getWithSubQuery().getEntries()) {
                ctes.add(entry.getAlias());
            }
        }
        source.accept(new SQLASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource tableSource) {
                String tableName = druidTableName(tableSource);
                if (tableName != null && !ctes.contains(tableName)) {
                    tables.add(tableName);
                }
                return true;
            }
        });
    }

    private String druidTableName(com.alibaba.druid.sql.ast.statement.SQLTableSource source) {
        return source instanceof SQLExprTableSource ? druidTableName((SQLExprTableSource) source) : null;
    }

    private String druidTableName(SQLExprTableSource source) {
        if (source == null || source.getExpr() == null) {
            return null;
        }
        return source.getExpr().toString();
    }

    private String unquoteIdentifier(String tableName) {
        return tableName.replace("`", "").replace("\"", "");
    }

    private List<TableNode> relationalNodes(Collection<String> tables, TableMeta meta) {
        List<TableNode> nodes = new ArrayList<>();
        for (String table : tables) {
            nodes.add(relationalTableNode(table, meta));
        }
        return nodes;
    }

    /**
     * 为关系库表补齐全限定名，并构建与 Neo4j 表节点身份一致的元数据。
     */
    private TableNode relationalTableNode(String table, TableMeta meta) {
        String fullName = table;
        DbQueryProperty property = new DbQueryProperty(meta.datasourceType, "", "", "", 0, meta.dbName, null);
        DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(property.getDbType()));
        if (!table.contains(".")) {
            try {
                fullName = dialect.getTableName(property, table);
            } catch (Exception ignored) {
                fullName = meta.dbName + "." + table;
            }
        }
        String[] parts = fullName.split("\\.");
        fullName = quoteTableName(dialect, parts);
        return TableNode.builder()
                .name(unquoteIdentifier(parts[parts.length - 1]))
                .tableName(fullName)
                .datasourceHostPort(meta.datasourceHostPort)
                .datasourceType(meta.datasourceType)
                .datasourceName(meta.datasourceName)
                .dbName(parts.length > 1 ? unquoteIdentifier(parts[parts.length - 2]) : meta.dbName)
                .sid(parts.length > 2 ? unquoteIdentifier(parts[parts.length - 3]) : null)
                .build();
    }

    /** 对库、模式和表名逐段引用，保留 SQL 已提供的方言引用符。 */
    private String quoteTableName(DbDialect dialect, String[] identifierParts) {
        List<String> quotedParts = new ArrayList<>(identifierParts.length);
        for (String identifierPart : identifierParts) {
            quotedParts.add(isQuotedIdentifier(identifierPart)
                    ? identifierPart
                    : dialect.quoteIdentifier(identifierPart));
        }
        return String.join(".", quotedParts);
    }

    private boolean isQuotedIdentifier(String identifier) {
        return identifier.length() >= 2
                && ((identifier.startsWith("`") && identifier.endsWith("`"))
                || (identifier.startsWith("\"") && identifier.endsWith("\"")));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second != null && !second.trim().isEmpty() ? second : null;
    }

    /** 任务节点配置优先，缺失时从单数据源配置读取类型。 */
    private String configuredDatasourceType(com.alibaba.fastjson2.JSONObject parameters, String datasourceType) {
        if (datasourceType != null && !datasourceType.trim().isEmpty()) {
            return datasourceType;
        }
        if (parameters == null || !(parameters.get("datasources") instanceof com.alibaba.fastjson2.JSONObject)) {
            return datasourceType;
        }
        com.alibaba.fastjson2.JSONObject datasource = parameters.getJSONObject("datasources");
        return firstNonBlank(datasource.getString("datasourceType"), datasource.getString("type"));
    }

    private class ParsedLineageGraph {
        private final Map<String, TableNode> nodeMap = new LinkedHashMap<>();
        private final LinkedHashMap<String, TableNode> readerNodeMap = new LinkedHashMap<>();
        private final LinkedHashMap<String, TableNode> writerNodeMap = new LinkedHashMap<>();
        private final Map<String, LinkedHashSet<String>> outgoingMap = new LinkedHashMap<>();
        private final Map<String, Integer> indegreeMap = new LinkedHashMap<>();
        private final Map<String, TableMeta> tableMetaMap;

        private ParsedLineageGraph(Map<String, TableMeta> tableMetaMap) {
            this.tableMetaMap = tableMetaMap;
        }

        private void addStatement(Collection<String> readerTables, Collection<String> writerTables) {
            List<TableNode> readers = resolveNodes(readerTables);
            List<TableNode> writers = resolveNodes(writerTables);
            if (readers.isEmpty()) {
                for (TableNode writer : writers) {
                    rememberWriter(writer);
                    registerNode(writer);
                }
                return;
            }
            if (writers.isEmpty()) {
                for (TableNode reader : readers) {
                    rememberReader(reader);
                    registerNode(reader);
                }
                return;
            }

            for (TableNode reader : readers) {
                rememberReader(reader);
                registerNode(reader);
                String sourceKey = buildTableKey(reader);
                for (TableNode writer : writers) {
                    rememberWriter(writer);
                    registerNode(writer);
                    addEdge(sourceKey, buildTableKey(writer));
                }
            }
        }

        private List<TableNode> resolveNodes(Collection<String> tableNames) {
            if (tableNames == null || tableNames.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, TableNode> unique = new LinkedHashMap<>();
            for (String tableName : tableNames) {
                if (tableName == null || tableName.trim().isEmpty()) {
                    continue;
                }
                TableNode tableNode = buildTableNode(tableName, tableMetaMap);
                if (tableNode == null) {
                    return new ArrayList<>();
                }
                unique.putIfAbsent(buildTableKey(tableNode), tableNode);
            }
            return new ArrayList<>(unique.values());
        }

        private void rememberReader(TableNode tableNode) {
            readerNodeMap.putIfAbsent(buildTableKey(tableNode), tableNode);
        }

        private void rememberWriter(TableNode tableNode) {
            writerNodeMap.putIfAbsent(buildTableKey(tableNode), tableNode);
        }

        private void registerNode(TableNode tableNode) {
            String key = buildTableKey(tableNode);
            nodeMap.putIfAbsent(key, tableNode);
            outgoingMap.putIfAbsent(key, new LinkedHashSet<>());
            indegreeMap.putIfAbsent(key, 0);
        }

        private void addEdge(String sourceKey, String targetKey) {
            if (sourceKey.equals(targetKey)) {
                return;
            }
            LinkedHashSet<String> targets = outgoingMap.computeIfAbsent(sourceKey, key -> new LinkedHashSet<>());
            if (targets.add(targetKey)) {
                indegreeMap.put(targetKey, indegreeMap.getOrDefault(targetKey, 0) + 1);
            }
        }

        private LineageResult toLineageResult() {
            return new LineageResult(new ArrayList<>(writerNodeMap.values()), new ArrayList<>(readerNodeMap.values()));
        }
    }

    private String buildTableKey(TableNode tableNode) {
        if (tableNode == null) {
            return "";
        }
        String tableName = firstNotBlank(tableNode.getTableName(), tableNode.getName());
        return (tableNode.getDatasourceHostPort() == null ? "" : tableNode.getDatasourceHostPort()) + "@"
                + (tableName == null ? "" : tableName);
    }

    /**
     * 每条链路最多遍历的关系数（一次“表 -> 表”跳跃含 2 条关系），限制查询复杂度、规避异常图导致的超长遍历。
     */
    private static final int MAX_CHAIN_REL_DEPTH = 40;

    /**
     * 血缘解析结果：明确区分 reader（输入表）与 writer（输出表）
     * <p>
     * 可直接作为参数传给 {@link #save(List, List, TaskNode)}：
     * <pre>{@code
     *   LineageResult r = service.saveTable("FlinkBatch", sql);
     *   service.save(r.getReaderNodes(), r.getWriterNodes(), taskNode);
     * }</pre>
     */
    public static class LineageResult {
        private final List<TableNode> writerNodes;
        private final List<TableNode> readerNodes;
        private Long datasourceId;

        public LineageResult(List<TableNode> writerNodes, List<TableNode> readerNodes) {
            this.writerNodes = writerNodes != null ? writerNodes : Collections.emptyList();
            this.readerNodes = readerNodes != null ? readerNodes : Collections.emptyList();
        }

        /**
         * INSERT INTO 目标表（输出）
         */
        public List<TableNode> getWriterNodes() {
            return writerNodes;
        }

        /**
         * FROM/JOIN 源表（输入）
         */
        public List<TableNode> getReaderNodes() {
            return readerNodes;
        }

        public Long getDatasourceId() {
            return datasourceId;
        }

        public void setDatasourceId(Long datasourceId) {
            this.datasourceId = datasourceId;
        }

        /**
         * 所有 TableNode 的合并列表（writer 在前，reader 在后）
         */
        public List<TableNode> toList() {
            List<TableNode> all = new ArrayList<>(writerNodes.size() + readerNodes.size());
            all.addAll(writerNodes);
            all.addAll(readerNodes);
            return all;
        }
    }

    /**
     * 表级元数据（从 CREATE TABLE DDL 的 WITH 子句解析得到）
     */
    private static class TableMeta {
        String datasourceHostPort;
        String datasourceType;
        String datasourceName;
        String dbName;
        /**
         * CREATE TABLE DDL 中的库名（用于补全简单表名）
         */
        String dbNameFromDDL;
        /**
         * CREATE TABLE DDL 中的模式名（用于补全简单表名）
         */
        String sidFromDDL;
        /**
         * 'table-name' 属性指定的真实数据库表名，优先于 Flink 表名
         */
        String realTableName;
    }

    // ==================== 数据源元数据提取 ====================

    /**
     * 匹配 JDBC URL 中的 host:port，兼容 Oracle / MySQL / DM 等多种格式
     * <ul>
     *   <li>jdbc:mysql://host:port/db              → host:port</li>
     *   <li>jdbc:oracle:thin:@//host:port/service  → host:port</li>
     *   <li>jdbc:oracle:thin:@host:port:SID        → host:port</li>
     * </ul>
     */
    private static final Pattern JDBC_URL_HOST_PORT = Pattern.compile(
            "(?:@//|://|@)([^/@:\\s]+:\\d+)");

    /**
     * 匹配 CDC connector 中的 hostname + port
     * <p>'hostname' = 'x', 'port' = 'y' → x:y</p>
     */
    private static final Pattern CDC_HOST = Pattern.compile("'hostname'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern CDC_PORT = Pattern.compile("'port'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);

    /**
     * 从 SQL 中提取所有 CREATE TABLE DDL 的表级元数据
     * <p>
     * 匹配 Flink SQL 格式：
     * <pre>
     *   CREATE TABLE tableName ( ... ) WITH ( 'connector' = 'jdbc', 'url' = 'jdbc:mysql://host:port/db' ... )
     * </pre>
     *
     * @return tableName → TableMeta 映射
     */
    private Map<String, TableMeta> extractCreateTableMeta(String sql) {
        Map<String, TableMeta> result = new LinkedHashMap<>();
        if (sql == null || sql.isEmpty()) {
            return result;
        }

        // 先行清理，避免注释 / 多余空白干扰匹配
        String cleaned = cleanSQL(sql);

        // 匹配 CREATE TABLE name ( ... ) WITH ( ... )
        Pattern createPattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\S+)\\s*\\(", Pattern.CASE_INSENSITIVE);

        Matcher matcher = createPattern.matcher(cleaned);
        while (matcher.find()) {
            String rawName = matcher.group(1);
            // 去掉反引号 / 双引号
            String tableName = stripQuotes(rawName);

            // 从 CREATE TABLE 表名中预解析 dbName / sid，后续可用于补全简单表名
            String[] nameParts = tableName.split("\\.");
            String ddlDbName = null, ddlSid = null;
            if (nameParts.length == 2) {
                ddlDbName = nameParts[0];
            } else if (nameParts.length >= 3) {
                ddlSid = nameParts[nameParts.length - 3];
                ddlDbName = nameParts[nameParts.length - 2];
            }

            // 从列定义括号开始位置匹配到闭合括号
            int columnsStart = matcher.end() - 1; // '('
            int columnsEnd = findMatchingParen(cleaned, columnsStart);
            if (columnsEnd < 0 || columnsEnd >= cleaned.length()) {
                continue;
            }

            // 查找 WITH 子句
            String afterColumns = cleaned.substring(columnsEnd + 1).trim();
            if (!afterColumns.toUpperCase().startsWith("WITH")) {
                continue;
            }

            // 定位 WITH 后的括号
            int withParenIdx = afterColumns.indexOf('(');
            if (withParenIdx < 0) {
                continue;
            }

            int withStartInCleaned = columnsEnd + 1 + withParenIdx;
            int withEnd = findMatchingParen(cleaned, withStartInCleaned);
            if (withEnd < 0) {
                continue;
            }

            String withContent = cleaned.substring(withStartInCleaned + 1, withEnd);
            TableMeta meta = parseWithContent(withContent);
            if (meta != null) {
                meta.dbNameFromDDL = ddlDbName;
                meta.sidFromDDL = ddlSid;
                result.put(tableName, meta);
            }
        }

        return result;
    }

    /**
     * 去掉 SQL 标识符的引用字符（反引号 `` ` `` / 双引号 {@code "}）
     */
    private String stripQuotes(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String stripped = name;
        if ((stripped.startsWith("`") && stripped.endsWith("`")) || (stripped.startsWith("\"") && stripped.endsWith("\""))) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        return stripped;
    }

    /**
     * 从 WITH 子句内容解析 TableMeta
     */
    private TableMeta parseWithContent(String withContent) {
        TableMeta meta = new TableMeta();

        // 尝试从 JDBC URL 提取 host:port + datasourceType + datasourceName
        //    格式：'url' = 'jdbc:mysql://host:port/dbName...'
        Matcher urlMatcher = Pattern.compile("'url'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE).matcher(withContent);
        if (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            Matcher hostPortMatcher = JDBC_URL_HOST_PORT.matcher(url);
            if (hostPortMatcher.find()) {
                meta.datasourceHostPort = hostPortMatcher.group(1);
            }
            meta.datasourceType = extractDbTypeFromJdbcUrl(url);
            if (StringUtils.isNotBlank(meta.datasourceType)) {
                DbType dbType = DbType.getDbType(meta.datasourceType);
                if (dbType != null) {
                    meta.datasourceName = dbType.getDesc();
                }
            }
            meta.dbName = extractDbNameFromJdbcUrl(url);
        }

        // 尝试从 CDC connector 提取 host:port
        if (meta.datasourceHostPort == null) {
            Matcher hostMatcher = CDC_HOST.matcher(withContent);
            Matcher portMatcher = CDC_PORT.matcher(withContent);
            if (hostMatcher.find() && portMatcher.find()) {
                meta.datasourceHostPort = hostMatcher.group(1) + ":" + portMatcher.group(1);
            }
        }

        // 尝试从 StarRocks load-url 提取 host:port
        //    格式：'load-url' = '192.168.0.101:8030'
        if (meta.datasourceHostPort == null) {
            Matcher loadUrlMatcher = Pattern.compile("'jdbc-url'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE)
                    .matcher(withContent);
            if (loadUrlMatcher.find()) {
                String loadUrl = loadUrlMatcher.group(1);
                // load-url 格式通常为 host:port 或 http://host:port，提取 host:port 部分
                Matcher hostPortMatcher = JDBC_URL_HOST_PORT.matcher(loadUrl);
                if (hostPortMatcher.find()) {
                    meta.datasourceHostPort = hostPortMatcher.group(1);
                } else {
                    // 直接 host:port 格式
                    meta.datasourceHostPort = loadUrl;
                }
                meta.datasourceType = "StarRocks";
                meta.datasourceName = "StarRocks";
            }
        }

        // 尝试从 Kafka properties.bootstrap.servers 提取 host:port
        //    格式：'properties.bootstrap.servers' = 'localhost:9092'
        if (meta.datasourceHostPort == null) {
            Matcher kafkaServerMatcher = Pattern.compile(
                    "'properties\\.bootstrap\\.servers'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE)
                    .matcher(withContent);
            if (kafkaServerMatcher.find()) {
                meta.datasourceHostPort = kafkaServerMatcher.group(1);
            }
        }

        // 提取 CDC 的 database-name
        if (StringUtils.isBlank(meta.dbName)) {
            Matcher dbNameMatcher = Pattern.compile("'database-name'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE)
                    .matcher(withContent);
            if (dbNameMatcher.find()) {
                meta.dbNameFromDDL = dbNameMatcher.group(1);
                meta.dbName = dbNameMatcher.group(1);
            }
        }

        // 从 connector 提取 datasourceType（优先于 JDBC URL 推导的类型，
        // 因为 StarRocks 等 connector 使用 MySQL 协议但应识别为 StarRocks）
        String connectorType = extractDatasourceTypeFromConnector(withContent);
        if (connectorType != null) {
            meta.datasourceType = connectorType;
        }

        // 提取 'table-name' —— Flink 表名映射到的真实数据库表名
        //    例：'table-name' = 'TEST.TEST_MYSQL' → realTableName = TEST.TEST_MYSQL
        Matcher tableNameMatcher = Pattern.compile("'table-name'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE)
                .matcher(withContent);
        if (tableNameMatcher.find()) {
            meta.realTableName = tableNameMatcher.group(1);
        }

        // 提取 Kafka 'topic' 作为表名（Kafka 无 table-name，topic 即数据源表标识）
        //    例：'topic' = 'your_topic_name' → realTableName = your_topic_name
        if (StringUtils.isBlank(meta.realTableName)) {
            Matcher topicMatcher = Pattern.compile("'topic'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE)
                    .matcher(withContent);
            if (topicMatcher.find()) {
                meta.realTableName = topicMatcher.group(1);
            }
        }

        if (StringUtils.isNotBlank(meta.realTableName)) {
            // 从 realTableName 解析 dbName / sid，兼容 ` `` ` ` " " ` 引用
            String stripped = meta.realTableName.replace("`", "").replace("\"", "");
            String[] parts = stripped.split("\\.");
            if (parts.length == 2) {
                meta.dbNameFromDDL = parts[0];
                meta.dbName = parts[0];
            } else if (parts.length >= 3) {
                meta.sidFromDDL = parts[parts.length - 3];
                meta.dbNameFromDDL = parts[parts.length - 2];
                meta.dbName = parts[parts.length - 2];
            }
        }

        // 如仍未获得 datasourceName，从配置属性补充
        if (StringUtils.isNotBlank(meta.datasourceType)) {
            DbType dbType = DbType.getDbType(meta.datasourceType);
            if (dbType != null) {
                meta.datasourceName = DbType.getDbType(meta.datasourceType).getDesc();
            }
        }

        // 兜底：datasourceName = datasourceHostPort
        if (meta.datasourceName == null && meta.datasourceHostPort != null) {
            meta.datasourceName = meta.datasourceHostPort;
        }

        return (meta.datasourceHostPort != null || meta.datasourceType != null || meta.datasourceName != null || meta.realTableName != null) ? meta : null;
    }

    /**
     * 从 JDBC URL 提取数据库/Schema 名
     * <ul>
     *   <li>Oracle：优先取 {@code currentSchema} 参数，无则取 service-name</li>
     *   <li>MySQL / DM / Kingbase / PG 等：取最后一个 / 之后的段</li>
     * </ul>
     */
    private String extractDbNameFromJdbcUrl(String url) {
        if (url == null) {
            return null;
        }
        String lower = url.toLowerCase();

        // Oracle：优先 currentSchema 参数
        if (lower.contains(":oracle:")) {
            Matcher schemaMatcher = Pattern.compile("[?;&]currentSchema=([^&;]+)", Pattern.CASE_INSENSITIVE)
                    .matcher(url);
            if (schemaMatcher.find()) {
                return schemaMatcher.group(1);
            }
        }

        // 通用：提取最后一个 / 之后、? 或 ; 之前的片段
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0) {
            return null;
        }
        String tail = url.substring(lastSlash + 1);
        int end = tail.length();
        int q = tail.indexOf('?');
        if (q >= 0)
            end = Math.min(end, q);
        int s = tail.indexOf(';');
        if (s >= 0)
            end = Math.min(end, s);
        String dbName = tail.substring(0, end);
        return dbName.isEmpty() ? null : dbName;
    }

    /**
     * 从 JDBC URL 提取数据库类型名
     * <p>jdbc:mysql://... → MySql, jdbc:oracle:... → Oracle</p>
     */
    private String extractDbTypeFromJdbcUrl(String url) {
        if (url == null)
            return null;
        String lower = url.toLowerCase();
        if (lower.contains(":mysql:"))
            return "MySql";
        if (lower.contains(":oracle:"))
            return "Oracle";
        if (lower.contains(":dm:"))
            return "DM8";
        if (lower.contains(":kingbase8:"))
            return "Kingbase8";
        if (lower.contains(":hive2:"))
            return "Hive2";
        // 尝试从 jdbc:xxx: 模式提取
        Matcher m = Pattern.compile("jdbc:([^:]+):").matcher(lower);
        if (m.find()) {
            String db = m.group(1);
            if (!db.equals("mysql") && !db.equals("oracle")) {
                return db.substring(0, 1).toUpperCase() + db.substring(1);
            }
            return db.equals("mysql") ? "MySql" : "Oracle";
        }
        return null;
    }

    /**
     * 从 connector 配置提取数据源类型
     * <p>'connector' = 'mysql-cdc' → MySql, 'connector' = 'jdbc' → 从 url 判断</p>
     */
    private String extractDatasourceTypeFromConnector(String withContent) {
        Matcher m = Pattern.compile("'connector'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE).matcher(withContent);
        if (m.find()) {
            String connector = m.group(1).toLowerCase();
            if (connector.contains("mysql"))
                return "MySql";
            if (connector.contains("oracle"))
                return "Oracle";
            if (connector.contains("dm"))
                return "DM8";
            if (connector.contains("kingbase"))
                return "Kingbase";
            if (connector.contains("starrocks"))
                return "StarRocks";
            if (connector.contains("kafka"))
                return "Kafka";
        }
        return null;
    }

    /**
     * 从指定位置找到匹配的闭合括号（处理嵌套）
     */
    private int findMatchingParen(String sql, int openParenPos) {
        int depth = 0;
        char quote = 0;
        for (int i = openParenPos; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // JDBC URL、密码等字符串值可能包含括号，字符串中的括号不参与嵌套计算。
            if (quote != 0) {
                if (c == quote) {
                    // SQL 使用两个连续引号表示字符串中的引号，例如 'it''s'。
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                        i++;
                    } else {
                        quote = 0;
                    }
                } else if (c == '\\' && i + 1 < sql.length()) {
                    // 兼容常见 SQL 方言对反斜杠转义的处理。
                    i++;
                }
                continue;
            }

            if (c == '\'' || c == '"' || c == '`') {
                quote = c;
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1; // 未找到匹配
    }

    // ==================== Spark SQL CREATE TABLE 元数据提取 ====================

    /**
     * 从 Spark SQL 中提取所有 CREATE TABLE ... USING ... OPTIONS (...) 的表级元数据
     * <p>
     * 匹配 Spark SQL 格式（区别于 Flink SQL 的 WITH 格式）：
     * <pre>
     *   CREATE TABLE [IF NOT EXISTS] tableName
     *   [USING provider]
     *   [OPTIONS (
     *     key1 'value1',
     *     key2 'value2',
     *     ...
     *   )]
     * </pre>
     * <p>
     * 从 OPTIONS 中提取：
     * <ul>
     *   <li>{@code url} → datasourceHostPort / dbName / datasourceType</li>
     *   <li>{@code dbtable} → realTableName（映射到的真实数据库表名）</li>
     *   <li>{@code driver} → datasourceType（如果 url 无法确定）</li>
     * </ul>
     *
     * @return tableName（Spark 临时视图名）→ TableMeta 映射
     */
    private Map<String, TableMeta> extractSparkCreateTableMeta(String sql) {
        Map<String, TableMeta> result = new LinkedHashMap<>();
        if (sql == null || sql.isEmpty()) {
            return result;
        }

        String cleaned = cleanSQL(sql);

        // 匹配 CREATE TABLE [IF NOT EXISTS] name，name 之后可能是 USING、OPTIONS 或列定义
        // 先找到 CREATE TABLE 的起始位置
        Pattern createPattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\S+)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = createPattern.matcher(cleaned);
        while (matcher.find()) {
            String rawName = matcher.group(1);
            String tableName = stripQuotes(rawName);

            // 从 CREATE TABLE 表名中预解析 dbName / sid
            String[] nameParts = tableName.split("\\.");
            String ddlDbName = null, ddlSid = null;
            if (nameParts.length == 2) {
                ddlDbName = nameParts[0];
            } else if (nameParts.length >= 3) {
                ddlSid = nameParts[nameParts.length - 3];
                ddlDbName = nameParts[nameParts.length - 2];
            }

            // 在表名后查找 OPTIONS 子句
            int afterTableName = matcher.end();
            String remaining = cleaned.substring(afterTableName);

            // 跳过可能存在的列定义 ( ... ) 和 USING provider
            int optionsKeywordIdx = findKeywordOutsideParens(remaining, "OPTIONS");
            if (optionsKeywordIdx < 0) {
                continue;
            }

            // 定位 OPTIONS 后的 (
            int parenStart = remaining.indexOf('(', optionsKeywordIdx);
            if (parenStart < 0) {
                continue;
            }

            int optionsStartInCleaned = afterTableName + parenStart;
            int optionsEnd = findMatchingParen(cleaned, optionsStartInCleaned);
            if (optionsEnd < 0) {
                continue;
            }

            String optionsContent = cleaned.substring(optionsStartInCleaned + 1, optionsEnd);
            TableMeta meta = parseSparkOptions(optionsContent);
            if (meta != null) {
                meta.dbNameFromDDL = ddlDbName;
                meta.sidFromDDL = ddlSid;
                result.put(tableName, meta);
            }
        }

        return result;
    }

    /**
     * 在字符串中查找关键词（在括号外的位置）
     * <p>跳过可能存在的列定义括号和 USING 子句，找到 OPTIONS 关键字的位置。
     */
    private int findKeywordOutsideParens(String s, String keyword) {
        // 将括号内的内容替换为空格，然后在剩余部分中查找关键词
        StringBuilder masked = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                masked.append(' ');
            } else if (c == ')') {
                depth--;
                masked.append(' ');
            } else if (depth > 0) {
                masked.append(' ');
            } else {
                masked.append(c);
            }
        }
        String keywordUpper = keyword.toUpperCase();
        String maskedUpper = masked.toString().toUpperCase();
        return maskedUpper.indexOf(keywordUpper);
    }

    /**
     * 从 Spark OPTIONS 子句内容解析 TableMeta
     * <p>
     * Spark OPTIONS 格式不同于 Flink WITH 格式，使用 {@code key 'value'} 而非 {@code 'key' = 'value'}：
     * <pre>
     *   url 'jdbc:mysql://192.168.1.100:3306/source_db?useSSL=false',
     *   dbtable 'orders',
     *   driver 'com.mysql.cj.jdbc.Driver'
     * </pre>
     */
    private TableMeta parseSparkOptions(String optionsContent) {
        TableMeta meta = new TableMeta();

        // 1. 从 url 提取 host:port + datasourceType + dbName
        //    Spark 格式: url 'jdbc:mysql://host:port/db'
        Matcher urlMatcher = Pattern.compile("url\\s+'([^']+)'", Pattern.CASE_INSENSITIVE)
                .matcher(optionsContent);
        if (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            Matcher hostPortMatcher = JDBC_URL_HOST_PORT.matcher(url);
            if (hostPortMatcher.find()) {
                meta.datasourceHostPort = hostPortMatcher.group(1);
            }
            meta.datasourceType = extractDbTypeFromJdbcUrl(url);
            if (StringUtils.isNotBlank(meta.datasourceType)) {
                DbType dbType = DbType.getDbType(meta.datasourceType);
                if (dbType != null) {
                    meta.datasourceName = dbType.getDesc();
                }
            }
            meta.dbName = extractDbNameFromJdbcUrl(url);
        }

        // 2. 从 dbtable 提取真实表名
        //    Spark 格式: dbtable 'table_name' 或 dbtable 'schema.table_name'
        //              dbtable '(SELECT ...) AS alias' → 子查询，跳过
        Matcher dbtableMatcher = Pattern.compile("dbtable\\s+'([^']+)'", Pattern.CASE_INSENSITIVE)
                .matcher(optionsContent);
        if (dbtableMatcher.find()) {
            String dbtable = dbtableMatcher.group(1);
            // 跳过子查询格式
            if (!dbtable.trim().startsWith("(")
                    && !dbtable.trim().toUpperCase().startsWith("SELECT")) {
                meta.realTableName = dbtable;

                // 从 realTableName 解析 dbName / sid
                // 例: SCOTT.REGIONAL_SALES_ANALYTICS → dbNameFromDDL=SCOTT
                // 例: source_db.orders → dbNameFromDDL=source_db
                String stripped = dbtable.replace("`", "").replace("\"", "");
                String[] parts = stripped.split("\\.");
                if (parts.length == 2) {
                    meta.dbNameFromDDL = parts[0];
                    // dbtable 中已含库名，如果 URL 中也解析出了 dbName，保留 dbtable 的
                    if (meta.dbName == null) {
                        meta.dbName = parts[0];
                    }
                } else if (parts.length >= 3) {
                    meta.sidFromDDL = parts[parts.length - 3];
                    meta.dbNameFromDDL = parts[parts.length - 2];
                    if (meta.dbName == null) {
                        meta.dbName = parts[parts.length - 2];
                    }
                }
            }
        }

        // 3. 从 driver 提取 datasourceType（如果 url 未确定）
        if (meta.datasourceType == null) {
            Matcher driverMatcher = Pattern.compile("driver\\s+'([^']+)'", Pattern.CASE_INSENSITIVE)
                    .matcher(optionsContent);
            if (driverMatcher.find()) {
                String driver = driverMatcher.group(1).toLowerCase();
                if (driver.contains("mysql")) {
                    meta.datasourceType = "MySql";
                } else if (driver.contains("oracle")) {
                    meta.datasourceType = "Oracle";
                } else if (driver.contains("dm")) {
                    meta.datasourceType = "DM8";
                } else if (driver.contains("kingbase")) {
                    meta.datasourceType = "Kingbase8";
                }
            }
        }

        // 4. 兜底：datasourceName = datasourceHostPort
        if (meta.datasourceName == null && meta.datasourceHostPort != null) {
            meta.datasourceName = meta.datasourceHostPort;
        }

        return (meta.datasourceHostPort != null || meta.datasourceType != null
                || meta.datasourceName != null || meta.realTableName != null) ? meta : null;
    }

    // ==================== SQL 解析（基于 Apache Calcite） ====================

    /**
     * Calcite SQL 解析器配置：MySQL 词法（支持反引号引用）+ 反引号引用
     */
    private static final SqlParser.Config CALCITE_PARSER_CONFIG = SqlParser.config()
            .withLex(org.apache.calcite.config.Lex.MYSQL)
            .withQuoting(org.apache.calcite.avatica.util.Quoting.BACK_TICK);

    /**
     * 解析 Flink SQL（FlinkBatch / FlinkStream）
     * <p>
     * 使用 Calcite SqlParser 解析 SQL AST，精准提取表引用：
     * <pre>
     *   INSERT INTO catalog.db.table SELECT ... FROM source_table [JOIN ...]
     *   INSERT INTO catalog.db.table (col1, col2) SELECT ... FROM source1 t1 JOIN source2 t2 ON ...
     *   CREATE TABLE catalog.db.table AS SELECT ... FROM source_table  (CTAS)
     * </pre>
     */
    private LineageResult parseFlinkSQL(String sql, Map<String, TableMeta> tableMetaMap) {
        return parseWithCalcite(sql, tableMetaMap);
    }

    /**
     * 解析 SparkSQL
     * <p>
     * 使用 Spark SessionState 的 SQL 解析器（{@code spark.sessionState().sqlParser().parsePlan()}）
     * 解析 Spark SQL，精准提取 INSERT INTO/OVERWRITE 的目标表（writer）和 FROM/JOIN 的源表（reader）。
     * <p>
     * Spark 解析出的 LogicalPlan 树结构（以 {@code INSERT INTO t SELECT ... FROM s} 为例）:
     * <pre>
     *   InsertIntoStatement (UnaryNode)
     *   ├── table: UnresolvedRelation(t)    ← writer (独立字段, 反射 table() 获取, 不在 children 中)
     *   └── query: Project                  ← 唯一 child
     *       └── UnresolvedRelation(s)       ← reader
     * </pre>
     * <p>
     * 解析失败的语句降级为正则解析。
     */
    private LineageResult parseSparkSQL(String sql, Map<String, TableMeta> tableMetaMap) {
        SparkSession spark = getOrCreateSparkSession();
        String cleaned = cleanSQL(sql);
        if (cleaned.isEmpty()) {
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }

        ParsedLineageGraph graph = new ParsedLineageGraph(tableMetaMap);

        // 按 ; 拆分多条 SQL 语句，逐一解析
        String[] statements = cleaned.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                LogicalPlan plan = spark.sessionState().sqlParser().parsePlan(trimmed);
                Set<String> statementWriters = new LinkedHashSet<>();
                Set<String> statementReaders = new LinkedHashSet<>();
                extractSparkLineage(plan, statementWriters, statementReaders);
                graph.addStatement(statementReaders, statementWriters);
            } catch (Exception e) {
                // 单条解析失败则整体降级到正则
                return parseWithRegexFallback(sql, tableMetaMap);
            }
        }

        return graph.toLineageResult();
    }

    /**
     * 延迟初始化 SparkSession（local 模式，禁用 UI）
     * <p>双重检查锁定，确保整个应用生命周期内只创建一次，避免重复创建开销。
     */
    private SparkSession getOrCreateSparkSession() {
        if (sparkSession == null) {
            synchronized (this) {
                if (sparkSession == null) {
                    sparkSession = SparkSession.builder()
                            .config(new SparkConf().setAppName("spark-sql-lineage-parser")
                                    .setMaster("local[*]")
                                    .set("spark.executor.memory", "1g")
                                    .set("spark.driver.memory", "1g")
                                    .set("spark.driver.host", "localhost")
                                    .set("spark.driver.bindAddress", "localhost")
                                    .set("spark.sql.shuffle.partitions", "2")
                                    .set("spark.ui.enabled", "false"))
                            .getOrCreate();
                }
            }
        }
        return sparkSession;
    }

    /**
     * Spring Bean 销毁时关闭 SparkSession，释放资源
     */
    @PostConstruct
    public void warmUpLineageGraph() {
        Thread thread = new Thread(() -> {
            try (Session session = getLineageDriver().session(SessionConfig.forDatabase("neo4j"))) {
                session.run("RETURN 1").consume();
                session.run("MATCH (root:Table {datasourceHostPort: $datasourceHostPort, tableName: $tableName}) RETURN root LIMIT 1",
                        Values.parameters("datasourceHostPort", "__lineage_warmup__", "tableName", "__lineage_warmup__")).consume();
                session.run("MATCH (root:Task {taskId: $taskId}) RETURN root LIMIT 1",
                        Values.parameters("taskId", Long.MIN_VALUE)).consume();
                // 全链路由上游、下游两个定向查询组成，分别预热对应的执行计划。
                session.run(buildLayerQuery("UP"),
                        Values.parameters("nodeIds", Collections.emptyList(), "hopLimit", 1)).consume();
                session.run(buildLayerQuery("DOWN"),
                        Values.parameters("nodeIds", Collections.emptyList(), "hopLimit", 1)).consume();
            } catch (Exception ignored) {
                // 预热失败不影响业务查询，首次请求仍会按正常流程创建连接和执行查询。
            }
        }, "lineage-neo4j-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void destroySparkSession() {
        if (lineageDriver != null) {
            lineageDriver.close();
            lineageDriver = null;
        }
        if (sparkSession != null) {
            sparkSession.stop();
            sparkSession = null;
        }
    }

    /**
     * 从 Spark LogicalPlan 树中提取血缘关系（writer + reader 表名）
     * <p>
     * INSERT 类语句：Spark 的 {@code InsertIntoStatement} 是 UnaryNode，
     * 唯一的 child 是 query 子计划，目标表 {@code table} 是独立字段（<b>不在 children 中</b>），
     * 需通过反射调用 {@code table()} 获取 writer，{@code query()} 获取 reader 子树。
     * 非 INSERT 语句（如纯 SELECT）：所有 UnresolvedRelation 都视为 reader
     */
    private void extractSparkLineage(LogicalPlan plan, Set<String> writers, Set<String> readers) {
        if (plan == null) {
            return;
        }

        String className = plan.getClass().getSimpleName();

        // INSERT INTO/OVERWRITE TABLE ... SELECT ...
        // 包括: InsertIntoStatement, InsertIntoHadoopFsRelationCommand 等
        if (className.contains("InsertInto")) {
            // writer：目标表通过 table() 方法反射获取（不在 children 中）
            LogicalPlan targetTable = invokeLogicalPlanAccessor(plan, "table");
            if (targetTable != null) {
                collectTableNames(targetTable, writers);
            }
            // reader：query 子计划，优先 query()，取不到则退化为遍历全部 children
            LogicalPlan query = invokeLogicalPlanAccessor(plan, "query");
            if (query != null) {
                collectAllTableNames(query, readers);
            } else {
                scala.collection.Iterator<LogicalPlan> it = plan.children().iterator();
                while (it.hasNext()) {
                    collectAllTableNames(it.next(), readers);
                }
            }
            return;
        }

        // 非 INSERT 计划（如纯 SELECT）：所有表引用都是 reader
        collectAllTableNames(plan, readers);
    }

    /**
     * 反射调用 LogicalPlan 的无参访问器方法（如 {@code InsertIntoStatement.table()} / {@code query()}），
     * 返回值为 LogicalPlan 时返回，否则（方法不存在 / 调用失败 / 类型不符）返回 null。
     */
    private LogicalPlan invokeLogicalPlanAccessor(LogicalPlan plan, String methodName) {
        try {
            java.lang.reflect.Method method = plan.getClass().getMethod(methodName);
            Object value = method.invoke(plan);
            if (value instanceof LogicalPlan) {
                return (LogicalPlan) value;
            }
        } catch (Exception ignored) {
            // 方法不存在或调用失败：交由调用方降级处理
        }
        return null;
    }

    /**
     * 递归收集 LogicalPlan 子树中所有 {@link UnresolvedRelation} 表名
     */
    private void collectAllTableNames(LogicalPlan plan, Set<String> tables) {
        if (plan == null) {
            return;
        }

        if (plan instanceof UnresolvedRelation) {
            UnresolvedRelation rel = (UnresolvedRelation) plan;
            tables.add(seqToDotString(rel.multipartIdentifier()));
            return;
        }

        // 递归处理所有子节点
        scala.collection.Seq<LogicalPlan> children = plan.children();
        scala.collection.Iterator<LogicalPlan> it = children.iterator();
        while (it.hasNext()) {
            collectAllTableNames(it.next(), tables);
        }
    }

    /**
     * 收集单个节点中的 writer 表名（通常为 INSERT 语句 target table 的 UnresolvedRelation）
     */
    private void collectTableNames(LogicalPlan plan, Set<String> tables) {
        if (plan instanceof UnresolvedRelation) {
            UnresolvedRelation rel = (UnresolvedRelation) plan;
            tables.add(seqToDotString(rel.multipartIdentifier()));
        } else {
            // writer 节点可能被包装（如 SubqueryAlias），递归收集一次
            collectAllTableNames(plan, tables);
        }
    }

    /**
     * Scala {@code Seq<String>} → Java 字符串，用 {@code .} 连接各部分（如 db.table）
     */
    private String seqToDotString(scala.collection.Seq<String> seq) {
        return seq.mkString(".");
    }

    /**
     * 使用 Calcite SqlParser 解析 SQL，提取 reader/writer 表名
     */
    private LineageResult parseWithCalcite(String sql, Map<String, TableMeta> tableMetaMap) {
        String cleaned = cleanSQL(sql);
        if (cleaned.isEmpty()) {
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }

        ParsedLineageGraph graph = new ParsedLineageGraph(tableMetaMap);

        // 按 ; 拆分多条 INSERT 语句，逐一解析
        String[] statements = cleaned.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                SqlParser parser = SqlParser.create(trimmed, CALCITE_PARSER_CONFIG);
                SqlNode root = parser.parseStmt();

                if (root instanceof SqlInsert) {
                    SqlInsert insert = (SqlInsert) root;
                    Set<String> statementWriters = new LinkedHashSet<>();
                    Set<String> statementReaders = new LinkedHashSet<>();
                    extractTargetTable(insert, statementWriters);
                    SqlNode source = insert.getSource();
                    if (source != null) {
                        extractSourceTables(source, statementReaders);
                    }
                    graph.addStatement(statementReaders, statementWriters);
                }
            } catch (Exception e) {
                // 单条解析失败则整体降级到正则
                return parseWithRegexFallback(sql, tableMetaMap);
            }
        }

        return graph.toLineageResult();
    }

    /**
     * 提取 INSERT 的目标表（writer）
     */
    private void extractTargetTable(SqlInsert insert, Set<String> writerTables) {
        SqlNode target = insert.getTargetTable();
        if (target instanceof SqlIdentifier) {
            writerTables.add(target.toString());
        }
    }

    /**
     * 递归遍历 SqlNode 树，提取所有源表（reader）
     */
    private void extractSourceTables(SqlNode node, Set<String> tables) {
        if (node == null) {
            return;
        }

        if (node instanceof SqlIdentifier) {
            // 叶子节点：表名标识符
            tables.add(node.toString());
        } else if (node instanceof SqlSelect) {
            // SELECT 语句：只读 FROM 子句
            SqlSelect select = (SqlSelect) node;
            SqlNode from = select.getFrom();
            if (from != null) {
                extractSourceTables(from, tables);
            }
            // 也处理 SELECT 列表中的子查询（标量子查询）
            SqlNodeList selectList = select.getSelectList();
            if (selectList != null) {
                for (SqlNode item : selectList) {
                    extractSourceTables(item, tables);
                }
            }
            // 处理 WHERE 子句中的子查询
            SqlNode where = select.getWhere();
            if (where != null) {
                extractSourceTables(where, tables);
            }
        } else if (node instanceof SqlJoin) {
            // JOIN 节点
            SqlJoin join = (SqlJoin) node;
            extractSourceTables(join.getLeft(), tables);
            extractSourceTables(join.getRight(), tables);
            // JOIN 的 ON 条件中可能有子查询
            SqlNode condition = join.getCondition();
            if (condition != null) {
                extractSourceTables(condition, tables);
            }
        } else if (node instanceof SqlBasicCall) {
            // 处理 AS 别名、子查询等
            SqlBasicCall call = (SqlBasicCall) node;
            String operatorName = call.getOperator().getName().toUpperCase();
            if ("AS".equals(operatorName)) {
                // 别名：取第一个操作数（实际表名或子查询）
                extractSourceTables(call.operand(0), tables);
            } else {
                // 其他表达式：遍历所有操作数
                for (int i = 0; i < call.operandCount(); i++) {
                    extractSourceTables(call.operand(i), tables);
                }
            }
        } else if (node instanceof SqlNodeList) {
            // 节点列表
            SqlNodeList list = (SqlNodeList) node;
            for (SqlNode item : list) {
                extractSourceTables(item, tables);
            }
        }
    }

    // ======================== 降级方案：正则解析 ========================

    /**
     * 匹配 INSERT INTO 后的目标表名
     */
    private static final Pattern INSERT_TARGET_PATTERN = Pattern.compile("INSERT\\s+(?:INTO|OVERWRITE)(?:\\s+TABLE)?\\s+([a-zA-Z_][a-zA-Z0-9_.]*)", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配 FROM/JOIN 子句中的表引用
     */
    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile("(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_.]*)", Pattern.CASE_INSENSITIVE);

    /**
     * Calcite 解析失败时的降级方案：基于规则的正则解析
     */
    private LineageResult parseWithRegexFallback(String sql, Map<String, TableMeta> tableMetaMap) {
        String cleaned = cleanSQL(sql);
        if (cleaned.isEmpty()) {
            return new LineageResult(Collections.emptyList(), Collections.emptyList());
        }

        ParsedLineageGraph graph = new ParsedLineageGraph(tableMetaMap);

        String[] statements = cleaned.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Set<String> statementWriters = new LinkedHashSet<>();
            Matcher insertMatcher = INSERT_TARGET_PATTERN.matcher(trimmed);
            while (insertMatcher.find()) {
                String name = insertMatcher.group(1);
                if (!isKeywordOrFunction(name)) {
                    statementWriters.add(name);
                }
            }

            Set<String> statementReaders = new LinkedHashSet<>();
            String noSubqueries = removeParenthesized(trimmed);
            Matcher fromMatcher = FROM_TABLE_PATTERN.matcher(noSubqueries);
            while (fromMatcher.find()) {
                String name = fromMatcher.group(1);
                if (!isKeywordOrFunction(name)) {
                    statementReaders.add(name);
                }
            }
            graph.addStatement(statementReaders, statementWriters);
        }

        return graph.toLineageResult();
    }

    // ======================== 工具方法 ========================

    /**
     * 清理 SQL：去除注释、合并空白
     */
    private String cleanSQL(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        return sql.replaceAll("--[^\n]*", " ").replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * 移除括号内所有内容（用于正则降级方案）
     */
    private String removeParenthesized(String sql) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                depth++;
                result.append(' ');
            } else if (c == ')') {
                depth--;
                result.append(' ');
            } else if (depth == 0) {
                result.append(c);
            } else {
                result.append(' ');
            }
        }
        return result.toString();
    }

    /**
     * 判断 token 是否为 SQL 关键字或内置函数（用于正则降级方案的去噪）
     */
    private boolean isKeywordOrFunction(String token) {
        if (token == null || token.isEmpty()) {
            return true;
        }
        String upper = token.toUpperCase();
        switch (upper) {
            case "SELECT":
            case "INSERT":
            case "INTO":
            case "OVERWRITE":
            case "TABLE":
            case "FROM":
            case "JOIN":
            case "INNER":
            case "LEFT":
            case "RIGHT":
            case "FULL":
            case "CROSS":
            case "OUTER":
            case "ON":
            case "WHERE":
            case "AND":
            case "OR":
            case "AS":
            case "SET":
            case "VALUES":
            case "DISTINCT":
            case "ALL":
            case "CASE":
            case "WHEN":
            case "THEN":
            case "ELSE":
            case "END":
            case "GROUP":
            case "ORDER":
            case "BY":
            case "HAVING":
            case "LIMIT":
            case "OFFSET":
            case "UNION":
            case "EXCEPT":
            case "INTERSECT":
            case "EXISTS":
            case "LATERAL":
                return true;
        }
        return false;
    }

    /**
     * 从解析出的表名集合构建 LineageResult，附加 CREATE TABLE DDL 中提取的元数据
     */
    private LineageResult buildLineageResult(Set<String> writerTables, Set<String> readerTables, Map<String, TableMeta> tableMetaMap) {
        // 先构建所有不重名表的 TableNode（同一表名只建一个实例）
        Set<String> allTables = new LinkedHashSet<>(writerTables);
        allTables.addAll(readerTables);
        Map<String, TableNode> nodeMap = new LinkedHashMap<>();
        for (String tableName : allTables) {
            nodeMap.put(tableName, buildTableNode(tableName, tableMetaMap));
        }

        List<TableNode> writers = new ArrayList<>();
        for (String tableName : writerTables) {
            writers.add(nodeMap.get(tableName));
        }

        List<TableNode> readers = new ArrayList<>();
        for (String tableName : readerTables) {
            readers.add(nodeMap.get(tableName));
        }

        return new LineageResult(writers, readers);
    }

    /**
     * 根据全限定表名创建 TableNode
     * <p>
     * 解析规则：
     * <ul>
     *   <li>{@code catalog.db.table} → name=table, dbName=db, sid=catalog</li>
     *   <li>{@code db.table}       → name=table, dbName=db</li>
     *   <li>{@code table}          → name=table, tableName=table</li>
     * </ul>
     * <p>
     * 同时从 {@code tableMetaMap} 中查找对应的 CREATE TABLE 元数据，
     * 填充 datasourceHostPort、datasourceType、datasourceName。
     * 查找时依次尝试全限定名、去除 catalog 后的名称、短表名。
     */
    private TableNode buildTableNode(String qualifiedName, Map<String, TableMeta> tableMetaMap) {
        // 从 CREATE TABLE DDL 元数据中匹配（多种键值尝试）
        TableMeta meta = resolveTableMeta(qualifiedName, tableMetaMap);
        if (meta == null) {
            return null;
        }

        // 如果 DDL 中指定了 'table-name'，使用真实数据库表名替代 Flink 表名
        String realTableName = qualifiedName;
        if (meta != null && meta.realTableName != null) {
            realTableName = meta.realTableName;
        }

        String[] parts = realTableName.split("\\.");
        String name;
        String dbName = null;
        String sid = null;

        if (parts.length == 1) {
            name = parts[0];
        } else if (parts.length == 2) {
            dbName = parts[0];
            name = parts[1];
        } else {
            sid = parts[parts.length - 3];
            dbName = parts[parts.length - 2];
            name = parts[parts.length - 1];
        }

        // 如果真实表名是简单名，用 DDL 中的库名/模式名补全
        if (dbName == null && meta != null && meta.dbNameFromDDL != null) {
            dbName = meta.dbNameFromDDL;
        }
        if (sid == null && meta != null && meta.sidFromDDL != null) {
            sid = meta.sidFromDDL;
        }
        // Spark SQL：dbtable 为简单名时，用 JDBC URL 中的 dbName 补全
        // 例: dbtable 'orders' + url jdbc:mysql://host:port/source_db → dbName=source_db
        if (dbName == null && meta != null && meta.dbName != null) {
            dbName = meta.dbName;
        }

        DbQueryProperty dbQueryProperty = null;
        if (StringUtils.isNotBlank(meta.datasourceHostPort)) {
            //构建fullTableName
            String[] datasourceHostPort = meta.datasourceHostPort.split(":");
            dbQueryProperty = new DbQueryProperty(meta.datasourceType, datasourceHostPort[0], "", "", Integer.parseInt(datasourceHostPort[1]), dbName, sid);
        } else {
            dbQueryProperty = new DbQueryProperty(meta.datasourceType, "", "", "", 0, dbName, sid);
        }

        DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(dbQueryProperty.getDbType()));
        String fullTableName = name;
        if (!StringUtils.equals(DbType.KAFKA.getDb(), dbQueryProperty.getDbType())) {
            fullTableName = dialect.getTableName(dbQueryProperty, name);
        }
        TableNode.TableNodeBuilder builder = TableNode.builder()
                .name(name)
                .tableName(fullTableName)
                .dbName(dbName)
                .sid(sid);

        if (meta != null) {
            builder.datasourceHostPort(meta.datasourceHostPort)
                    .datasourceType(meta.datasourceType)
                    .datasourceName(meta.datasourceName);
        }

        return builder.build();
    }

    /**
     * 按多种键值尝试匹配 TableMeta
     * <p>匹配优先级：全限定名 → db.table → table</p>
     */
    private TableMeta resolveTableMeta(String qualifiedName, Map<String, TableMeta> tableMetaMap) {
        // 1. 精确匹配
        if (tableMetaMap.containsKey(qualifiedName)) {
            return tableMetaMap.get(qualifiedName);
        }
        // 2. 遍历查找：截取全限定名后面部分的匹配（如 catalog.db.table vs db.table）
        for (Map.Entry<String, TableMeta> entry : tableMetaMap.entrySet()) {
            String key = entry.getKey();
            if (qualifiedName.endsWith("." + key) || key.endsWith("." + qualifiedName)) {
                return entry.getValue();
            }
        }
        // 3. 按短表名（最后一段）匹配
        String shortName = qualifiedName.contains(".") ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1) : qualifiedName;
        if (tableMetaMap.containsKey(shortName)) {
            return tableMetaMap.get(shortName);
        }
        // 4. 遍历按短名匹配
        for (Map.Entry<String, TableMeta> entry : tableMetaMap.entrySet()) {
            String key = entry.getKey();
            String[] keyParts = key.split("\\.");
            if (keyParts[keyParts.length - 1].equals(shortName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Transactional
    public void save() {
        // 1. 源表
        TableNode orders = tableRepository.save(TableNode.builder().name("user").tableName("sales_db.dwh.user")
                //                        .datasourceId(1L)
                .datasourceType("MySql").dbName("sales_db").sid("dwh").build());

        // 2. 任务1
        TaskNode task1 = taskRepository.save(TaskNode.builder()
                .name("任务名称")
                .taskId(3L)
                .taskCode("4000000000")
                .build());

        // 3. 目标表1
        TableNode toOrders = tableRepository.save(TableNode.builder().name("to_user").tableName("sales_db.dwh.to_user")
                //                        .datasourceId(1L)
                .datasourceType("MySql").dbName("sales_db").sid("dwh").build());
        /* 6. 建立关系（核心） */

        // orders -> task1
        orders.setTableToTaskRels(Collections.singletonList(TableToTaskRel.builder().taskId(3L).taskCode("4000000000")
                //                .datasourceId(1L)
                .tableName("sales_db.dwh.user").task(task1).build()));
        tableRepository.save(orders);

        // task1 -> toOrders
        task1.setTaskToTableRels(Collections.singletonList(TaskToTableRel.builder().taskId(3L).taskCode("4000000000")
                //                .datasourceId(1L)
                .tableName("sales_db.dwh.to_user").table(toOrders).build()));
        taskRepository.save(task1);
    }

    @Transactional("neo4jTransactionManager")
    public void deletdAll() {
        taskRepository.deleteTask();
        taskRepository.deleteTable();
    }

}
