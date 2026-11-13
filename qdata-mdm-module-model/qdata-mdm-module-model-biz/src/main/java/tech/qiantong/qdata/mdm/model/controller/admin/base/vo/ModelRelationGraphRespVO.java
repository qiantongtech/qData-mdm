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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "模型关系图谱 Response VO")
public class ModelRelationGraphRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模型节点")
    private List<Node> nodes = new ArrayList<>();

    @Schema(description = "模型关系边")
    private List<Edge> edges = new ArrayList<>();

    @Data
    public static class Node implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private String code;
        @Schema(description = "归属主数据域 ID；子模型继承所属主模型的数据域")
        private Long projectId;
        @Schema(description = "归属主数据域名称；子模型继承所属主模型的数据域")
        private String projectName;
        private String type;
        private String description;
        private Long parentId;
        private String versionNo;
        private Long versionStatus;
        private Boolean root;
        private Boolean subModel;
        private List<String> tags = new ArrayList<>();
    }

    @Data
    public static class Edge implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private Long source;
        private Long target;
        private String type;
        private String label;
        private Long fieldId;
    }
}
