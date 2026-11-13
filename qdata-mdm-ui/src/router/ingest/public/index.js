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

/*
 * Layout
 */
import Layout from '@/layout/index.vue';

/*
 * 数据接入模块
 */
export default [
    ...['add', 'edit', 'detail'].map((page) => ({
        path: `/ingest/task/${page}`,
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: page === 'detail'
                    ? () => import('@/views/ingest/task/detail/index.vue')
                    : () => import('@/views/ingest/task/config/index.vue'),
                name: page === 'detail' ? 'IngestTaskDetail' : page === 'add' ? 'IngestTaskAdd' : 'IngestTaskEdit',
                meta: {
                    title: page === 'detail' ? '接入任务详情' : page === 'add' ? '新增接入任务' : '修改接入任务',
                    activeMenu: '/ingest/task',
                    noCache: true,
                },
            },
        ],
    })),
    {
        path: '/ingest/source/task/add',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ingest/task/config/index.vue'),
                name: 'IngestSourceTaskAdd',
                meta: {
                    title: '新增接入任务',
                    activeMenu: '/ingest/source',
                    noCache: true,
                },
            },
        ],
    },
    {
        path: '/ingest/source/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ingest/source/detail/index.vue'),
                name: 'IngestSourceDetail',
                meta: {
                    title: '来源系统详情',
                    activeMenu: '/ingest/source',
                },
            },
        ],
    },
    {
        path: '/ingest/sourceData/detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/ingest/sourceData/detail/index.vue'),
                name: 'IngestSourceDataDetail',
                props: route => ({
                    recordId: route.query.id,
                    modelId: route.query.modelId,
                    sourceSystemId: route.query.sourceSystemId,
                    sourceRecordKey: route.query.sourceRecordKey,
                }),
                meta: {
                    title: '来源数据详情',
                    activeMenu: '/ingest/sourceData',
                    noCache: true,
                },
            },
        ],
    },
];
