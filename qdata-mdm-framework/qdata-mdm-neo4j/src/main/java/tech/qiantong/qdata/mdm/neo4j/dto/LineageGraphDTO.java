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
public class LineageGraphDTO {

    /**
     * 当前查询根节点 ID，前端可用于高亮当前节点。
     */
    private String rootNodeId;

    /**
     * 当前查询根节点类型：TABLE 或 TASK。
     */
    private String rootType;

    /**
     * 查询范围上下文，保留左侧树联动参数和默认数据源信息。
     */
    private Map<String, Object> scope = new HashMap<>();

    /**
     * 图谱节点集合。本周期只返回 TABLE 和 TASK 两类节点。
     */
    private List<LineageGraphNodeDTO> nodes = new ArrayList<>();

    /**
     * 图谱边集合。
     */
    private List<LineageGraphEdgeDTO> edges = new ArrayList<>();

    /**
     * 是否为空图。为空时前端展示兜底空态。
     */
    private Boolean empty = true;

    /**
     * 是否还有下一批数据。类目/库级范围查询时通常来自叶子种子的分页结果。
     */
    private Boolean hasMore = false;

    /**
     * 下一次动态加载使用的游标。前端原样回传即可。
     */
    private String nextCursor;

    /**
     * 空态或暂不支持场景的提示文案。
     */
    private String message;
}
