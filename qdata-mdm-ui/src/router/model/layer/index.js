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

import Layout from '@/layout/index.vue';

// 详情通过列表按钮进入，不单独出现在菜单里；打开详情时仍高亮“模型目录”。
export default [{
  path: '/model/layer/detail',
  component: Layout,
  hidden: true,
  children: [{
    path: '',
    component: () => import('@/views/model/layer/detail/index.vue'),
    name: 'modelLayerDetail',
    meta: { title: '模型目录详情', activeMenu: '/model/layer' },
  }],
}];
