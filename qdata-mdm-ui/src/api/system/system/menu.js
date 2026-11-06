/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request.js';

// Query menu list
export function listMenu(query) {
  return request({
    url: '/system/menu/list',
    method: 'get',
    params: query
  });
}

// Query menu details
export function getMenu(menuId) {
  return request({
    url: '/system/menu/' + menuId,
    method: 'get'
  });
}

// Query menu drop-down tree structure
export function treeselect() {
  return request({
    url: '/system/menu/treeselect',
    method: 'get'
  });
}

// Query menu drop-down tree structure based on role ID
export function roleMenuTreeselect(roleId) {
  return request({
    url: '/system/menu/roleMenuTreeselect/' + roleId,
    method: 'get'
  });
}

// New menu
export function addMenu(data) {
  return request({
    url: '/system/menu',
    method: 'post',
    data: data
  });
}

// Modify menu
export function updateMenu(data) {
  return request({
    url: '/system/menu',
    method: 'put',
    data: data
  });
}

// delete menu
export function delMenu(menuId) {
  return request({
    url: '/system/menu/' + menuId,
    method: 'delete'
  });
}
