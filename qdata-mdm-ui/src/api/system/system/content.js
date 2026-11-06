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

import request from '@/utils/request';

// Query system configuration list
export function listContent(query) {
  return request({
    url: '/system/content/list',
    method: 'get',
    params: query
  });
}

// Query system configuration details
export function getContent(id) {
  return request({
    url: '/sys/content/' + id,
    method: 'get'
  });
}

// Add new system configuration
export function addContent(data) {
  return request({
    url: '/system/content',
    method: 'post',
    data: data
  });
}

// Modify system configuration
export function updateContent(data) {
  return request({
    url: '/system/content/edit',
    method: 'post',
    data: data
  });
}

// Delete system configuration
export function delContent(id) {
  return request({
    url: '/system/content/' + id,
    method: 'delete'
  });
}
