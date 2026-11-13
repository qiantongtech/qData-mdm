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

package tech.qiantong.qdata.mdm.model.enums.materialization;

/**
 * 主键及索引约束的用途分类；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelIndexType {
    /**
     * 主键定义，不重复生成主键索引
     */
    PRIMARY_KEY,
    /**
     * 仅权威表按允许场景生成业务唯一
     */
    UNIQUE,
    /**
     * parent_id、category_id等必要普通索引
     */
    NORMAL
}

