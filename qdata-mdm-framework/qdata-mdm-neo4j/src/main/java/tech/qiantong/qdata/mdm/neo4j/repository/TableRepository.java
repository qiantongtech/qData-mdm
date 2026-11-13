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

package tech.qiantong.qdata.mdm.neo4j.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.qiantong.qdata.mdm.neo4j.dto.LineageDTO;
import tech.qiantong.qdata.mdm.neo4j.node.TableNode;

import java.util.Optional;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 13:44
 **/
@Repository
public interface TableRepository extends Neo4jRepository<TableNode, Long> {
//    Optional<TableNode> findByTableNameAndDatasourceId(String tableName,Long datasourceId);

    /**
     * 幂等 MERGE 输出表（writer）并建立 (task)-[:TASK_TO_TABLE]->(table) 关系。
     * <p>
     * 表节点按 (tableName, datasourceHostPort) 唯一定位：存在则用新值补齐（新值为 null 保留旧值），
     * 不存在则创建。关系通过 MERGE 幂等建立，只影响当前任务这一条边，
     * 不会像 SDN 对象图级联那样误删其它任务已有的关系。
     */
    @Query("MERGE (tb:Table {tableName: $tableName, datasourceHostPort: $datasourceHostPort}) " +
            "SET tb.name = coalesce($name, tb.name), " +
            "tb.datasourceName = coalesce($datasourceName, tb.datasourceName), " +
            "tb.datasourceType = coalesce($datasourceType, tb.datasourceType), " +
            "tb.dbName = coalesce($dbName, tb.dbName), " +
            "tb.sid = coalesce($sid, tb.sid) " +
            "WITH tb " +
            "MATCH (tk:Task {taskId: $taskId}) " +
            "MERGE (tk)-[r:TASK_TO_TABLE]->(tb) " +
            "SET r.taskId = $taskId, r.taskCode = $taskCode, " +
            "r.tableName = $tableName, r.datasourceHostPort = $datasourceHostPort")
    void mergeTaskToTable(@Param("tableName") String tableName,
                          @Param("datasourceHostPort") String datasourceHostPort,
                          @Param("name") String name,
                          @Param("datasourceName") String datasourceName,
                          @Param("datasourceType") String datasourceType,
                          @Param("dbName") String dbName,
                          @Param("sid") String sid,
                          @Param("taskId") Long taskId,
                          @Param("taskCode") String taskCode);

    /**
     * 幂等 MERGE 输入表（reader）并建立 (table)-[:TABLE_TO_TASK]->(task) 关系。
     * 语义同 {@link #mergeTaskToTable}，方向相反。
     */
    @Query("MERGE (tb:Table {tableName: $tableName, datasourceHostPort: $datasourceHostPort}) " +
            "SET tb.name = coalesce($name, tb.name), " +
            "tb.datasourceName = coalesce($datasourceName, tb.datasourceName), " +
            "tb.datasourceType = coalesce($datasourceType, tb.datasourceType), " +
            "tb.dbName = coalesce($dbName, tb.dbName), " +
            "tb.sid = coalesce($sid, tb.sid) " +
            "WITH tb " +
            "MATCH (tk:Task {taskId: $taskId}) " +
            "MERGE (tb)-[r:TABLE_TO_TASK]->(tk) " +
            "SET r.taskId = $taskId, r.taskCode = $taskCode, " +
            "r.tableName = $tableName, r.datasourceHostPort = $datasourceHostPort")
    void mergeTableToTask(@Param("tableName") String tableName,
                          @Param("datasourceHostPort") String datasourceHostPort,
                          @Param("name") String name,
                          @Param("datasourceName") String datasourceName,
                          @Param("datasourceType") String datasourceType,
                          @Param("dbName") String dbName,
                          @Param("sid") String sid,
                          @Param("taskId") Long taskId,
                          @Param("taskCode") String taskCode);

    @Query("MATCH (t:Table {tableName: $tableName, datasourceHostPort: $datasourceHostPort}) " +
            "OPTIONAL MATCH (t)-[r1:TABLE_TO_TASK]->(task1) " +
            "OPTIONAL MATCH (t)<-[r2:TASK_TO_TABLE]-(task2) " +
            "RETURN t, collect(r1), collect(task1), collect(r2), collect(task2)")
    Optional<TableNode> findByTableNameAndDatasourceHostPort(
            @Param("tableName") String tableName,
            @Param("datasourceHostPort") String datasourceHostPort
    );

    @Query(value = "MATCH (currentTable:Table {datasourceHostPort: $datasourceHostPort,tableName: $tableName})  " +
            "  " +
            "OPTIONAL MATCH (currentTable)<-[r1:TASK_TO_TABLE]-(sourceTask:Task)  " +
            "OPTIONAL MATCH (sourceTask)<-[r2:TABLE_TO_TASK]-(sourceTable:Table)  " +
            "  " +
            "OPTIONAL MATCH (currentTable)-[r3:TABLE_TO_TASK]->(targetTask:Task)  " +
            "OPTIONAL MATCH (targetTask)-[r4:TASK_TO_TABLE]->(targetTable:Table)  " +
            "RETURN  " +
            "  currentTable,  " +
            "  collect(DISTINCT sourceTask) + collect(DISTINCT targetTask) AS tasks,  " +
            "  [currentTable] + collect(DISTINCT sourceTable) + collect(DISTINCT targetTable) AS tables")
    Optional<LineageDTO> findLineage(@Param("datasourceHostPort") String datasourceHostPort, @Param("tableName") String tableName);

    /**
     * <code>
     *     MATCH (currentTask:Task {taskId: $taskId, type: $type})
     *             OPTIONAL MATCH (currentTask)<-[:TABLE_TO_TASK]-(sourceTable:Table)
     *             OPTIONAL MATCH (sourceTable)<-[:TASK_TO_TABLE]-(sourceTask:Task)
     *             OPTIONAL MATCH (currentTask)-[:TASK_TO_TABLE]->(targetTable:Table)
     *             OPTIONAL MATCH (targetTable)-[:TABLE_TO_TASK]->(targetTask:Task)
     *             RETURN collect(DISTINCT currentTask) AS tasks,
     *             collect(DISTINCT sourceTable) + collect(DISTINCT targetTable) AS tables
     * </code>
     * @param taskId
     * @param type
     * @return
     */
    @Query(value = "MATCH (currentTask:Task {taskId: $taskId, type: $type}) " +
            "OPTIONAL MATCH (currentTask)<-[:TABLE_TO_TASK]-(sourceTable:Table) " +
            "OPTIONAL MATCH (sourceTable)<-[:TASK_TO_TABLE]-(sourceTask:Task) " +
            "OPTIONAL MATCH (currentTask)-[:TASK_TO_TABLE]->(targetTable:Table) " +
            "OPTIONAL MATCH (targetTable)-[:TABLE_TO_TASK]->(targetTask:Task) " +
            "RETURN collect(DISTINCT currentTask) AS tasks," +
            "collect(DISTINCT sourceTable) + collect(DISTINCT targetTable) AS tables")
    Optional<LineageDTO> findLineage(@Param("taskId") Long taskId, @Param("type") String type);
}
