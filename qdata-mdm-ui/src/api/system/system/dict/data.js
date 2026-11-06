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

// Query dictionary data list
export function listData(query) {
  return request({
    url: '/system/dict/data/list',
    method: 'get',
    params: query
  });
}

// Query dictionary data details
export function getData(dictCode) {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'get'
  });
}

// Query dictionary data information based on dictionary type
export function getDicts(dictType) {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get'
  });
}

// Add dictionary data
export function addData(data) {
  return request({
    url: '/system/dict/data',
    method: 'post',
    data: data
  });
}

// Modify dictionary data
export function updateData(data) {
  return request({
    url: '/system/dict/data',
    method: 'put',
    data: data
  });
}

// Delete dictionary data
export function delData(dictCode) {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'delete'
  });
}
