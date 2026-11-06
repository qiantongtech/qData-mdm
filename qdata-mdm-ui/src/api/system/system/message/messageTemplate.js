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

// Query message template list
export function listMessageTemplate(query) {
  return request({
    url: '/system/messageTemplate/list',
    method: 'get',
    params: query
  });
}

// Query message template details
export function getMessageTemplate(id) {
  return request({
    url: '/system/messageTemplate/' + id,
    method: 'get'
  });
}

// Add message template
export function addMessageTemplate(data) {
  return request({
    url: '/system/messageTemplate',
    method: 'post',
    data: data
  });
}

// Modify message template
export function updateMessageTemplate(data) {
  return request({
    url: '/system/messageTemplate',
    method: 'put',
    data: data
  });
}

// Delete message template
export function delMessageTemplate(id) {
  return request({
    url: '/system/messageTemplate/' + id,
    method: 'delete'
  });
}
