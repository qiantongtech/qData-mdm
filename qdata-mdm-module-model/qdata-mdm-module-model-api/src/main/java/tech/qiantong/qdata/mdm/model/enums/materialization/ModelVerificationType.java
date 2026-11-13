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
 * 声明式核验条件，由后续目标核验器解释执行；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelVerificationType {
    /**
     * 目标连接身份与计划一致
     */
    TARGET_IDENTITY_MATCH,
    /**
     * 目标对象不存在，读取失败不能视为不存在
     */
    OBJECT_ABSENT,
    /**
     * 注释执行前确认对应表或列存在，不重复比较完整结构
     */
    OBJECT_PRESENT,
    /**
     * 实际结构与预期结构一致
     */
    STRUCTURE_MATCH,
    /**
     * 必要字段不存在空值
     */
    NO_NULL_VALUES,
    /**
     * 唯一字段组合不存在重复
     */
    NO_DUPLICATE_VALUES,
    /**
     * 对象不存在业务数据
     */
    NO_DATA,
    /**
     * 旧计划中的依赖条件；新计划不再生成，遇到时保留对象，不自动视为通过
     */
    NO_DEPENDENCIES,
    /**
     * 核对新增对象确由本实例创建，且本实例尚未正式生效。
     */
    CREATED_BY_INSTANCE,
    /**
     * 仅用于读取旧计划；注释现按SQL执行结果处理，不再读取核验。
     */
    COMMENT_MATCH
}

