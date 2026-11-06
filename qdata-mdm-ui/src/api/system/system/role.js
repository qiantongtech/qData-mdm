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

// Query role list
export function listRole(query) {
  return request({
    url: '/system/role/list',
    method: 'get',
    params: query
  });
}

// Query role details
export function getRole(roleId) {
  return request({
    url: '/system/role/' + roleId,
    method: 'get'
  });
}

// Add new role
export function addRole(data) {
  return request({
    url: '/system/role',
    method: 'post',
    data: data
  });
}

// Modify role
export function updateRole(data) {
  return request({
    url: '/system/role',
    method: 'put',
    data: data
  });
}

// Role data permissions
export function dataScope(data) {
  return request({
    url: '/system/role/dataScope',
    method: 'put',
    data: data
  });
}

// Character status modification
export function changeRoleStatus(roleId, status) {
  const data = {
    roleId,
    status
  };
  return request({
    url: '/system/role/changeStatus',
    method: 'put',
    data: data
  });
}

// Delete role
export function delRole(roleId) {
  return request({
    url: '/system/role/' + roleId,
    method: 'delete'
  });
}

// Query the list of users authorized by the role
export function allocatedUserList(query) {
  return request({
    url: '/system/role/authUser/allocatedList',
    method: 'get',
    params: query
  });
}

// Query the list of users whose role is not authorized
export function unallocatedUserList(query) {
  return request({
    url: '/system/role/authUser/unallocatedList',
    method: 'get',
    params: query
  });
}

// Cancel user authorization role
export function authUserCancel(data) {
  return request({
    url: '/system/role/authUser/cancel',
    method: 'put',
    data: data
  });
}

// Cancel user authorization roles in batches
export function authUserCancelAll(data) {
  return request({
    url: '/system/role/authUser/cancelAll',
    method: 'put',
    params: data
  });
}

// Authorized user selection
export function authUserSelectAll(data) {
  return request({
    url: '/system/role/authUser/selectAll',
    method: 'put',
    params: data
  });
}

// Query department tree structure based on role ID
export function deptTreeSelect(roleId) {
  return request({
    url: '/system/role/deptTree/' + roleId,
    method: 'get'
  });
}
