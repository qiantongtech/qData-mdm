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

package tech.qiantong.qdata.mdm.neo4j.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.qiantong.qdata.mdm.neo4j.node.TaskNode;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 14:11
 **/
@Repository
public interface TaskRepository extends Neo4jRepository<TaskNode, Long> {

    @Query("MATCH (t:Task {taskId: $taskId}) RETURN t")
    TaskNode findByTaskId(@Param("taskId") Long taskId);

    @Query("MATCH (t:Task {taskId: $taskId, type: $type}) RETURN t")
    TaskNode findByTaskIdAndType(@Param("taskId") Long taskId, @Param("type") String type);

    /**
     * 幂等 MERGE 任务节点：存在则按新值补齐（新值为 null 时保留旧值），不存在则创建。
     * 仅更新节点属性，不触碰任何关系，避免 SDN 级联误删其它任务的边。
     */
    @Query("MERGE (t:Task {taskId: $taskId}) " +
            "SET t.name = coalesce($name, t.name), " +
            "t.taskCode = coalesce($taskCode, t.taskCode), " +
            "t.type = coalesce($type, t.type), " +
            "t.taskType = coalesce($taskType, t.taskType)")
    void upsertTask(@Param("taskId") Long taskId,
                    @Param("name") String name,
                    @Param("taskCode") String taskCode,
                    @Param("type") String type,
                    @Param("taskType") String taskType);

    @Query("MATCH ()-[r {taskId:$taskId}]->() DELETE r")
    void deleteRelByTaskId(@Param("taskId") Long taskId);

    @Query("MATCH (ta:Task) DETACH DELETE ta")
    void deleteTask();

    @Query("MATCH (t:Table) DETACH DELETE t")
    void deleteTable();
}
