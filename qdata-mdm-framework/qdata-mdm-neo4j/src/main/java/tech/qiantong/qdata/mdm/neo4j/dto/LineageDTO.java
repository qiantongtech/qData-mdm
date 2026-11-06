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

package tech.qiantong.qdata.mdm.neo4j.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import tech.qiantong.qdata.mdm.neo4j.node.TableNode;
import tech.qiantong.qdata.mdm.neo4j.node.TaskNode;

import java.util.List;

@Data
public class LineageDTO {

    /**
     * 当前表节点
     */
    private TableNode currentTable;

    /**
     * 所有Task节点
     */
    private List<TaskNode> tasks;
    /**
     * 所有Table节点
     */
    private List<TableNode> tables;

    /**
     * 节点关系
     */
    private List<JSONObject> rels;
}
