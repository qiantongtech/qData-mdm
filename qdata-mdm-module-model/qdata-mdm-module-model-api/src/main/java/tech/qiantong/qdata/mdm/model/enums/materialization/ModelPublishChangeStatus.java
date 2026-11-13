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
 * 发布记录变化汇总；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelPublishChangeStatus {
    /**
     * 首次发布，还没有上次发布成功的结构
     */
    FIRST_PUBLISH,
    /**
     * 存在物理结构变化
     */
    STRUCTURE_CHANGE,
    /**
     * 只有名称、描述、展示等元数据变化
     */
    METADATA_ONLY,
    /**
     * 只有业务停用或逻辑删除变化
     */
    DISABLE,
    /**
     * 包含多类变化
     */
    MIXED,
    /**
     * 完整比较后没有变化
     */
    NO_CHANGE
}

