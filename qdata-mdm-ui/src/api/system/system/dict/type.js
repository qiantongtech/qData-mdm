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

// Query dictionary type list
export function listType(query) {
  return request({
    url: '/system/dict/type/list',
    method: 'get',
    params: query
  });
}

// Query dictionary type details
export function getType(dictId) {
  return request({
    url: '/system/dict/type/' + dictId,
    method: 'get'
  });
}

// Add new dictionary type
export function addType(data) {
  return request({
    url: '/system/dict/type',
    method: 'post',
    data: data
  });
}

// Modify dictionary type
export function updateType(data) {
  return request({
    url: '/system/dict/type',
    method: 'put',
    data: data
  });
}

// Delete dictionary type
export function delType(dictId) {
  return request({
    url: '/system/dict/type/' + dictId,
    method: 'delete'
  });
}

// Flush dictionary cache
export function refreshCache() {
  return request({
    url: '/system/dict/type/refreshCache',
    method: 'delete'
  });
}

// Get dictionary select box list
export function optionselect() {
  return request({
    url: '/system/dict/type/optionselect',
    method: 'get'
  });
}
