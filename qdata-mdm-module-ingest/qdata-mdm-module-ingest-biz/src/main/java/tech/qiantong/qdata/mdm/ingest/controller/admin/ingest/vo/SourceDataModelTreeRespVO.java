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

package tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo;

import lombok.Data;

import java.util.List;

/**
 * 来源数据左侧主数据模型树节点。
 */
@Data
public class SourceDataModelTreeRespVO {

    /**
     * 前端树节点唯一标识。
     */
    private String id;

    /**
     * 节点显示名称。
     */
    private String label;

    /**
     * 节点类型：project（主数据域）、layer（模型目录）、model（主数据模型）。
     */
    private String type;

    /**
     * 节点编码。
     */
    private String code;

    /**
     * 叶子节点对应的 MODEL_BASE.ID；非模型节点为空。
     */
    private Long modelId;

    /**
     * 当前模型下的来源数据数量；非模型节点为空。
     */
    private Long count;

    /**
     * 子节点。
     */
    private List<SourceDataModelTreeRespVO> children;
}
