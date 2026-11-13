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

import {createSvgIconsPlugin} from 'vite-plugin-svg-icons';
import path from 'path';

export default function createSvgIcon(isBuild) {
    return createSvgIconsPlugin({
        iconDirs: [
            path.resolve(process.cwd(), 'src/assets/common/icons/svg'),
            path.resolve(process.cwd(), 'src/assets/modules/model/icons/base'),
            path.resolve(process.cwd(), 'src/assets/modules/model/icons/encoding'),
            path.resolve(process.cwd(), 'src/assets/modules/master'),
            path.resolve(process.cwd(), 'src/assets/modules/merge/icons'),
            path.resolve(process.cwd(), 'src/assets/modules/quality/icons')
        ],
        symbolId: 'icon-[dir]-[name]',
        svgoOptions: isBuild
    });
}
