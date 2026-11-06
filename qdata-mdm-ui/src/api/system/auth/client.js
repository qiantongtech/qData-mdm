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

// Query application management list
export function listClient(query) {
  return request({
    url: '/auth/client/list',
    method: 'get',
    params: query
  });
}

// Query application management details
export function getClient(id) {
  return request({
    url: '/auth/client/' + id,
    method: 'get'
  });
}

// Added application management
export function addClient(data) {
  return request({
    url: '/auth/client',
    method: 'post',
    data: data
  });
}

// Modify application management
export function updateClient(data) {
  return request({
    url: '/auth/client',
    method: 'put',
    data: data
  });
}

// Delete application management
export function delClient(id) {
  return request({
    url: '/auth/client/' + id,
    method: 'delete'
  });
}
