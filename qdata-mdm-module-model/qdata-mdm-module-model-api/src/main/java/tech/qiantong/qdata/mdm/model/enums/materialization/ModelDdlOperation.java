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
 * 允许规划的正向DDL种类，不包含业务停用删除DDL；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelDdlOperation {
    /**
     * 创建新物理表，主键优先包含在CREATE中
     */
    CREATE_TABLE,
    /**
     * 已有表新增列
     */
    ADD_COLUMN,
    /**
     * 按批准属性扩大已有字符串长度
     */
    EXPAND_COLUMN,
    /**
     * 已有业务列从必填改为可空。
     */
    RELAX_COLUMN,
    /**
     * 仅设置或清除已有业务列的静态默认值，不更新历史行。
     */
    ALTER_DEFAULT,
    /**
     * 按表角色及批准变更创建必要索引
     */
    CREATE_INDEX,
    /**
     * 首次建表或新增列附带的物理注释，不用于已有中文名等元数据修改。
     */
    COMMENT
}

