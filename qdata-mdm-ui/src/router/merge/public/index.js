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

import Layout from '@/layout/index.vue'

export default [
  {
    path: '/merge/review/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/merge/review/detail/index.vue'),
        name: 'mergeReviewDetail',
        meta: { title: '匹配审核详情', activeMenu: '/merge/review', noCache: true },
      },
    ],
  },
  {
    path: '/merge/detect/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/merge/detect/detail/index.vue'),
        name: 'mergeDetectDetail',
        meta: { title: '重复检测详情', activeMenu: '/merge/detect', noCache: true },
      },
    ],
  },
  {
    path: '/merge/manage/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/merge/manage/detail/index.vue'),
        name: 'mergeManageDetail',
        meta: { title: '合并管理详情', activeMenu: '/merge/manage', noCache: true },
      },
    ],
  },
]
