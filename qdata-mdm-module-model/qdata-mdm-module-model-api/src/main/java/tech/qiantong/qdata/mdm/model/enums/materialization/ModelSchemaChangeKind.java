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
 * 单个对象的变更类型，不直接授权执行DDL；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelSchemaChangeKind {
    /**
     * 新增对象
     */
    ADDED,
    /**
     * 已有对象属性变化，是否允许由CHANGE校验
     */
    MODIFIED,
    /**
     * 仅业务元数据变化
     */
    METADATA_ONLY,
    /**
     * 模型封存，物理对象保留
     */
    DISABLED,
    /**
     * 配置逻辑删除，物理对象保留
     */
    LOGICALLY_DELETED,
    /**
     * 上次发布成功的对象没有明确停用或删除，却在本次配置中缺失，需要阻止发布
     */
    UNEXPECTED_MISSING
}

