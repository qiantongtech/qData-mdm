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

// Query application and user association list
export function listUser(query) {
  return request({
    url: '/auth/user/list',
    method: 'get',
    params: query
  });
}

// Query application and user association details
export function getUser(clientId) {
  return request({
    url: '/auth/user/' + clientId,
    method: 'get'
  });
}

// Add new application and user association
export function addUser(data) {
  return request({
    url: '/auth/user',
    method: 'post',
    data: data
  });
}

// Modify application and user associations
export function updateUser(data) {
  return request({
    url: '/auth/user',
    method: 'put',
    data: data
  });
}

// Delete app and user associations
export function delUser(clientId) {
  return request({
    url: '/auth/user/' + clientId,
    method: 'delete'
  });
}
