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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LineageMaintenanceResultDTO {

    private List<GraphResult> graphs = new ArrayList<>();

    @Data
    public static class GraphResult {
        private Integer graphIndex;
        private Integer addedCount = 0;
        private Integer updatedCount = 0;
        private Integer deletedCount = 0;
        private Integer unchangedCount = 0;
        private List<Long> addedRelIds = new ArrayList<>();
        private List<Long> deletedRelIds = new ArrayList<>();
        private List<UpdatedRel> updatedRels = new ArrayList<>();
        private RefreshNode refreshNode;
    }

    @Data
    public static class RefreshNode {
        private String graphNodeId;
        private String nodeType;
        private String sceneType;
    }

    @Data
    public static class UpdatedRel {
        private Long oldRelId;
        private Long newRelId;
    }
}
