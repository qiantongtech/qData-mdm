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

import { createWebHistory, createRouter } from 'vue-router';
import { clearCancelTokens } from '@/utils/request'; // Make sure the import path is correct

/* System module public routing */
import systemPublicRouter from './system/public/index.js';
/* System module dynamic routing */
import systemDynamicRouter from './system/dynamic/index.js';
/* 模型管理 */
import modelRouter from "@/router/model/index.js";
/* 主数据管理 */
import masterRouter from './master/index.js';
/*任务管理*/
import ruleRouter from './rule/index.js';
import ingestPublicRouter from './ingest/public/index.js';
import mergePublicRouter from './merge/public/index.js';
import qualityRouter from './quality/qualityTask/index.js';
import auditRouter from './audit/public/index.js'
/* 数据分发*/
import distPublicRouter from './dist/public/index.js';
/* 授权管理*/
import authorizationRouter from './authorization/index.js';

/**
 * Note: Routing configuration items
 *
 * hidden: true // When true is set, the route will not appear in the sidebar, such as 401, login and other pages, or some editing pages /edit/1
 * alwaysShow: true // When you declare more than 1 route as children under a route, it will automatically become a nested mode - such as a component page
 * // When there is only one, that sub-route will be displayed in the sidebar as the root route - such as the guide page
 * // If you want to display your root route regardless of the number of children declared under the route
 * // You can set alwaysShow: true so that it will ignore the previously defined rules and always show the root route
 * redirect: noRedirect // When noRedirect is set, the route cannot be clicked in the breadcrumb navigation.
 * name:'router-name' // Set the name of the route, be sure to fill it in otherwise various problems will occur when using <keep-alive>
 * query: '{"id": 1, "name": "ry"}' // Default passing parameters for access routes
 * roles: ['admin', 'common'] // Role permissions to access routing
 * permissions: ['a:a:a', 'b:b:b'] // Access route menu permissions
 * meta : {
 noCache: true // If set to true, it will not be cached by <keep-alive> (default false)
 title: 'title' // Set the name of the route displayed in the sidebar and breadcrumbs
 icon: 'svg-name' // Set the icon of this route, corresponding to the path src/assets/common/icons/svg
 breadcrumb: false // If set to false, it will not be displayed in breadcrumbs
 activeMenu: '/system/user' // When the route sets this attribute, the corresponding sidebar will be highlighted.
 }
 */

// public route
export const constantRoutes = [
    ...systemPublicRouter,
    ...modelRouter,
    ...masterRouter, ...ruleRouter,
    ...distPublicRouter,
    ...authorizationRouter,
    ...ingestPublicRouter, ...mergePublicRouter
,...qualityRouter,...auditRouter];

// Dynamic routing, dynamic loading based on user permissions
export const dynamicRoutes = [...systemDynamicRouter];

const router = createRouter({
    history: createWebHistory(),
    routes: constantRoutes,
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition;
        } else {
            return {top: 0};
        }
    }
});

// Add cancellation request logic in route guard
router.beforeEach((to, from, next) => {
    if (to.path !== from.path) {
        clearCancelTokens(); // Cancel all outstanding requests before routing switch
    }
    next();
});

/**
 * Reset routing
 */
export function resetRouter() {
    window.location.href = `${window.location.protocol}//${window.location.host}/login/`;
}

export default router;
