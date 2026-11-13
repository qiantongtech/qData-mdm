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

/** 普通模型成功发布的分类导航分组，包含目标模型名称和完整树节点。 */
@Data
public class MasterClassificationFieldRespVO {
    private String id;
    private String code;
    private String label;
    private String targetModelId;
    private String targetModelName;
    private String targetInstanceId;
    private String targetVersionId;
    private boolean available;
    private String message;
    private String revision;
    private List<MasterClassificationTreeRespVO.Node> nodes = new ArrayList<>();
}
