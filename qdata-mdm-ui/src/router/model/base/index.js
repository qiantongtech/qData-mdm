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

/* Layout */
import Layout from '@/layout/index.vue';

export default [
  {
    path: '/model/base/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/model/base/detail/index.vue'),
        name: 'modelBaseDetail',
        meta: {title: '主数据模型详情', activeMenu: '/model/base'},
      },
    ],
  },
  {
    // 模型新增与修订共用该隐藏路由，通过 query.id 区分模式。
    path: '/model/base/add',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/model/base/form/index.vue'),
        name: 'modelBaseAdd',
        meta: {title: '主数据模型新增', activeMenu: '/model/base'},
      },
    ],
  },
  {
    // 模型新增与修订共用该隐藏路由，通过 query.id 区分模式。
    path: '/model/base/edit',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/model/base/form/index.vue'),
        name: 'modelBaseEdit',
        meta: {title: '主数据模型修改', activeMenu: '/model/base'},
      },
    ],
  },
];
