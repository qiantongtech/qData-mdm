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

import normalModelIcon from '@/assets/common/icons/model/normal-model.svg';
import classModelIcon from '@/assets/common/icons/model/class-model.svg';
import subModelIcon from '@/assets/common/icons/model/sub-model.svg';
import defaultProjectIcon from '@/assets/common/icons/icon-project-default.svg';

export {defaultProjectIcon};

/** 未上传时使用默认 SVG；上传路径补齐接口前缀，完整地址直接使用。 */
export function getProjectIcon(icon) {
    if (!icon) return defaultProjectIcon;
    const baseUrl = import.meta.env.VITE_APP_BASE_API || '';
    // 上传组件保存的路径可能已经带接口前缀，这种情况不能再拼一次。
    if (/^https?:\/\//i.test(icon) || (baseUrl && icon.startsWith(baseUrl + '/'))) return icon;
    return baseUrl + icon;
}

/** 补齐图片地址；为空时使用指定的默认图。 */
export function resolveImageUrl(path, fallback = '') {
    if (!path) return fallback;

    const baseUrl = import.meta.env.VITE_APP_BASE_API || '';

    if (
        /^https?:\/\//i.test(path) ||
        (baseUrl && path.startsWith(baseUrl + '/'))
    ) {
        return path;
    }

    return baseUrl + path;
}

// 子模型先按主子关系识别，不能因为它沿用了主模型类型，就显示成普通或分类图标。
export function getDefaultModelIcon(type, isSubModel = false) {
    if (isSubModel) return subModelIcon;
    return type === 'CATEGORY' ? classModelIcon : normalModelIcon;
}

/** 自定义图标优先；未上传时根据主模型类型或主子关系选择默认图标。 */
export function getModelIcon(model, isSubModel = model?.parentId != null && String(model.parentId) !== '0') {
    const icon = model?.icon;
    if (!icon) return getDefaultModelIcon(model?.type, isSubModel);
    const baseUrl = import.meta.env.VITE_APP_BASE_API || '';
    // 完整地址或已经带接口前缀的路径直接使用，只有相对路径才补前缀。
    if (/^https?:\/\//i.test(icon) || (baseUrl && icon.startsWith(baseUrl + '/'))) return icon;
    return baseUrl + icon;
}
