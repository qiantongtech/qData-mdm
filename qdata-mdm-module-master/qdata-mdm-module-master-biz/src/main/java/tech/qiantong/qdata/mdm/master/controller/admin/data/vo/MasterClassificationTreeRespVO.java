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

package tech.qiantong.qdata.mdm.master.controller.admin.data.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 分类导航和维护选项共享完整层级；节点ID与实际关联键分别返回。 */
@Data
public class MasterClassificationTreeRespVO {
    private String targetModelId;
    private String targetInstanceId;
    private String targetVersionId;
    private String revision;
    private List<Node> nodes = new ArrayList<>();

    @Data
    public static class Node {
        private String id;
        private String label;
        private String path;
        private String value;
        private String status;
        private boolean assignable;
        private List<Node> children = new ArrayList<>();
    }
}
