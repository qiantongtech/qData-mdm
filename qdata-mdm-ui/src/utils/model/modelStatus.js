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

/** 模型版本状态，与后端状态值保持一致。 */
export const MODEL_STATUS = Object.freeze({
    DRAFT: 0, // 草稿，尚未发布。
    PUBLISHED: 1, // 已发布。
    REVISING: 2, // 修订中。
    SEALED: 3, // 已封存。
    HISTORY: 4, // 历史版本，再次发布后被新版本覆盖的版本。
});

/** 状态展示和筛选统一使用这些名称。 */
// 标签颜色：草稿和历史版本灰色，已发布绿色，修订中橙色，已封存红色。
export const MODEL_STATUS_OPTIONS = Object.freeze([
    {label: '草稿', value: MODEL_STATUS.DRAFT, elTagType: 'info'},
    {label: '已发布', value: MODEL_STATUS.PUBLISHED, elTagType: 'success'},
    {label: '修订中', value: MODEL_STATUS.REVISING, elTagType: 'warning'},
    {label: '已封存', value: MODEL_STATUS.SEALED, elTagType: 'danger'},
    {label: '历史版本', value: MODEL_STATUS.HISTORY, elTagType: 'info'},
]);
