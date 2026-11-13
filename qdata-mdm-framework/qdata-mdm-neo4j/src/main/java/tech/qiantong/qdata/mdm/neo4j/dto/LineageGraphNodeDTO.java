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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class LineageGraphNodeDTO {

    /**
     * 前端使用的稳定节点 ID，如 table:host:tableName、task:taskId。
     */
    private String id;

    /**
     * Neo4j 内部节点 ID，仅用于调试和后端排查。
     */
    private Long neo4jId;

    /**
     * 节点类型：TABLE、TASK。
     */
    private String nodeType;

    /**
     * 节点所属业务场景：SOURCE_SYSTEM、INTEGRATION_TASK、DEVELOPMENT_TASK、API_SERVICE。
     */
    private String sceneType;

    /**
     * 节点主标题。
     */
    private String label;

    /**
     * 节点副标题。
     */
    private String subLabel;

    private String name;

    private String tableName;

    /**
     * 元数据表 ID，对应 MC_TABLE.ID；无法关联元数据时为空。
     */
    private Long tableId;

    /**
     * 表中文名/备注。优先来自元数据树，Neo4j 有该属性时也会直接带出。
     */
    private String tableComment;

    private String datasourceHostPort;

    private String datasourceName;

    private String datasourceType;

    private String dbName;

    /**
     * 数据库中文名/备注。当前元数据有值时返回，方便前端同时展示中英文库名。
     */
    private String dbComment;

    private String sid;

    private Long assetId;

    /**
     * 任务节点信息。
     */
    private Long taskId;

    private String taskCode;

    private String type;

    private String taskType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date taskTime;

    private String taskStatus;

    private String personChargeId;

    private String personChargeName;

    private String lastExecuteStatus;

    /**
     * 数据集成任务的草稿配置信息。
     */
    private String draftJson;

    /**
     * 数据服务api路径。
     */
    private String apiUrl;

    /** 整库同步任务的输入数据源名称 */
    private String originDb;

    /** 整库同步任务的输出数据源名称 */
    private String targetDb;

    /** 整库同步任务的输入数据源类型 */
    private String originType;

    /** 整库同步任务的输出数据源类型 */
    private String targetType;

    /**
     * 上下游数量用于前端展示统计和判断是否可展开。
     */
    private Long upstreamCount = 0L;

    private Long downstreamCount = 0L;

    private Boolean hasUpstream = false;

    private Boolean hasDownstream = false;

    private Boolean current = false;

    private Boolean expandable = false;

    /**
     * Neo4j 原始属性，方便前端做补充展示。
     */
    private Map<String, Object> properties = new HashMap<>();
}
