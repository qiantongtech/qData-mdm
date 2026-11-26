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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

/* Layout */
import Layout from '@/layout/index.vue';

export default [
  {
    path: '/quality',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'issueTask/detail',
        component: () => import('@/views/quality/issueTask/detail/index.vue'),
        name: 'QualityIssueTaskDetail',
        meta: { title: '整改任务详情', activeMenu: '/quality/issueTask' }
      }
    ]
  },
  {
    path: '/quality',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'task/detail',
        component: () => import('@/views/quality/task/detail/index.vue'),
        name: 'qualityTaskDetail',
        meta: { title: '质量任务详情', activeMenu: '/quality/task' }
      }
    ]
  },
  {
    path: '/quality',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'issue/detail',
        component: () => import('@/views/quality/issue/detail/index.vue'),
        name: 'qualityIssueDetail',
        meta: { title: '问题数据详情', activeMenu: '/quality/issue' }
      }
    ]
  },
];
