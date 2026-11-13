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
        path: '/quality',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'qualityIssueTask/detail',
                component: () => import('@/views/quality/qualityIssueTask/detail/index.vue'),
                name: 'QualityIssueTaskDetail',
                meta: { title: '整改任务详情', activeMenu: '/quality/qualityIssueTask' }
            }
        ]
    },
    {
        path: '/quality',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'qualityTask/detail',
                component: () => import('@/views/quality/qualityTask/detail/index.vue'),
                name: 'qualityTaskDetail',
                meta: { title: '质量任务详情', activeMenu: '/quality/qualityTask' }
            }
        ]
    },
    {
        path: '/quality',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'qualityIssue/qualityIssueDetail',
                component: () => import('@/views/quality/qualityIssue/detail/index.vue'),
                name: 'qualityIssueDetail',
                meta: { title: '问题数据详情', activeMenu: '/quality/qualityIssue' }
            }
        ]
    },
];
