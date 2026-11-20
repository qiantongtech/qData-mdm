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

import Layout from '@/layout/index.vue';

export default [
  {
    path: '/dist/service/list',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/dist/service/index.vue'),
        name: 'DistServiceList',
        meta: { title: '数据服务', activeMenu: '/dist/dataService', noCache: true },
      },
    ],
  },
  {
    path: '/dist/service/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/dist/service/detail/index.vue'),
        name: 'DistServiceDetail',
        meta: { title: '数据服务详情', activeMenu: '/dist/dataService', noCache: true },
      },
    ],
  },
  // 分发任务新增和修改共用四步整页向导，与接入任务的独立路由结构一致。
  ...['add', 'edit'].map((page) => ({
    path: `/dist/task/${page}`,
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/dist/task/TaskCreatePage.vue'),
        name: page === 'add' ? 'DistTaskAdd' : 'DistTaskEdit',
        meta: {
          title: page === 'add' ? '新增分发任务' : '修改分发任务',
          activeMenu: '/dist/taskDistribute',
          noCache: true,
        },
      },
    ],
  })),
  {
    path: '/dist/task/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/dist/task/detail/index.vue'),
        name: 'DistTaskDetail',
        meta: {
          title: '分发任务详情',
          // 分发任务菜单的实际路由为 taskDistribute，详情页沿用该菜单的选中态。
          activeMenu: '/dist/taskDistribute',
          noCache: true,
        },
      },
    ],
  },
  {
    path: '/dist/target/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/dist/target/detail/index.vue'),
        name: 'DistTargetSystemDetail',
        meta: {
          title: '目标系统详情',
          activeMenu: '/dist/targetSystem',
          noCache: true,
        },
      },
    ],
  },
];
