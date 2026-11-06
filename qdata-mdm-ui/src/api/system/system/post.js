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

// Query job list
export function listPost(query) {
  return request({
    url: '/system/post/list',
    method: 'get',
    params: query
  });
}

// Check job details
export function getPost(postId) {
  return request({
    url: '/system/post/' + postId,
    method: 'get'
  });
}

// New positions
export function addPost(data) {
  return request({
    url: '/system/post',
    method: 'post',
    data: data
  });
}

// Modify position
export function updatePost(data) {
  return request({
    url: '/system/post',
    method: 'put',
    data: data
  });
}

// Delete position
export function delPost(postId) {
  return request({
    url: '/system/post/' + postId,
    method: 'delete'
  });
}
