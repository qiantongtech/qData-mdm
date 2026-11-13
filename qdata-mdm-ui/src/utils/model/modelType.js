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

/** 模型类型，与后端类型编码保持一致。 */
export const MODEL_TYPE = Object.freeze({
    NORMAL: 'NORMAL', // 普通模型。
    CATEGORY: 'CATEGORY', // 分类模型。
});

/** 模型类型的下拉选项和名称展示统一使用这些配置。 */
export const MODEL_TYPE_OPTIONS = Object.freeze([
    {label: '普通模型', value: MODEL_TYPE.NORMAL},
    {label: '分类模型', value: MODEL_TYPE.CATEGORY},
]);
