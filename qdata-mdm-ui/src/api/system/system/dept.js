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

// Query department list
export function listDept(query) {
  return request({
    url: '/system/dept/list',
    method: 'get',
    params: query
  });
}

// Query department list (exclude nodes)
export function listDeptExcludeChild(deptId) {
  return request({
    url: '/system/dept/list/exclude/' + deptId,
    method: 'get'
  });
}

// Query department details
export function getDept(deptId) {
  return request({
    url: '/system/dept/' + deptId,
    method: 'get'
  });
}

// Add new department
export function addDept(data) {
  return request({
    url: '/system/dept',
    method: 'post',
    data: data
  });
}

// Modify department
export function updateDept(data) {
  return request({
    url: '/system/dept',
    method: 'put',
    data: data
  });
}

// Delete department
export function delDept(deptId) {
  return request({
    url: '/system/dept/' + deptId,
    method: 'delete'
  });
}
