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
import { checkPermi } from '@/utils/permission';

export default [
  {
    path: '/master/instance/model/detail',
    component: Layout,
    hidden: true,
    beforeEnter: () => checkPermi(['model:base:query']) || '/401',
    children: [
      {
        path: '',
        component: () => import('@/views/model/base/detail/index.vue'),
        name: 'masterInstanceModelDetail',
        meta: { title: '主数据模型详情', activeMenu: '/master/instance', noCache: true },
      },
    ],
  },
  {
    path: '/master/instance/data',
    component: Layout,
    hidden: true,
    beforeEnter: () => checkPermi(['master:instance:list']) || '/401',
    children: [
      {
        path: '',
        component: () => import('@/views/master/data/index.vue'),
        name: 'masterInstanceData',
        meta: { title: '主数据列表', activeMenu: '/master/instance' },
      },
    ],
  },
  {
    path: '/master/instance/category',
    component: Layout,
    hidden: true,
    beforeEnter: () => checkPermi(['master:instance:list']) || '/401',
    children: [
      {
        path: '',
        component: () => import('@/views/master/data/category/index.vue'),
        name: 'masterInstanceCategory',
        meta: { title: '主数据分类列表', activeMenu: '/master/instance' },
      },
    ],
  },
  {
    path: '/master/instance/detail',
    component: Layout,
    hidden: true,
    beforeEnter: () => checkPermi(['master:instance:query']) || '/401',
    children: [
      {
        path: ':masterDataId',
        component: () => import('@/views/master/data/detail/index.vue'),
        name: 'masterInstanceRecordDetail',
        meta: { title: '主数据详情', activeMenu: '/master/instance', noCache: true },
      },
    ],
  },
];
