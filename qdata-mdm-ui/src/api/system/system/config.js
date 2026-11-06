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

// Query parameter list
export function listConfig(query) {
  return request({
    url: '/system/config/list',
    method: 'get',
    params: query
  });
}

// Query parameter details
export function getConfig(configId) {
  return request({
    url: '/system/config/' + configId,
    method: 'get'
  });
}

// Query parameter value based on parameter key name
export function getConfigKey(configKey) {
  return request({
    url: '/system/config/configKey/' + configKey,
    method: 'get'
  });
}

// New parameter configuration
export function addConfig(data) {
  return request({
    url: '/system/config',
    method: 'post',
    data: data
  });
}

// Modify parameter configuration
export function updateConfig(data) {
  return request({
    url: '/system/config',
    method: 'put',
    data: data
  });
}

// Delete parameter configuration
export function delConfig(configId) {
  return request({
    url: '/system/config/' + configId,
    method: 'delete'
  });
}

// Refresh parameter cache
export function refreshCache() {
  return request({
    url: '/system/config/refreshCache',
    method: 'delete'
  });
}
