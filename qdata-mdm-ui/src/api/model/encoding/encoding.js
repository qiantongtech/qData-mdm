/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This file is part of qData-mdm Master Data Platform (Open Source Edition).
 *
 * qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
 * You may use qData-mdm for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData-mdm logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData-mdm as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request'

// 查询编码方案主列表
export function listEncoding(query) {
  return request({
    url: '/model/encoding/list',
    method: 'get',
    params: query
  })
}

// 查询当前版本已发布且存在编码规则段的可选编码方案
export function listAvailableEncoding(query) {
  return request({
    url: '/model/encoding/available-list',
    method: 'get',
    params: query
  })
}

// 查询编码方案主详细
export function getEncoding(id) {
  return request({
    url: '/model/encoding/' + id,
    method: 'get'
  })
}

// 查询编码方案版本使用情况
export function listEncodingUsage(encodingVersionId) {
  return request({
    url: '/model/encoding/usage',
    method: 'get',
    params: {encodingVersionId}
  })
}

// 新增编码方案主
export function addEncoding(data) {
  return request({
    url: '/model/encoding',
    method: 'post',
    data: data
  })
}

// 修改编码方案主
export function updateEncoding(data) {
  return request({
    url: '/model/encoding',
    method: 'put',
    data: data
  })
}

// 删除编码方案主
export function delEncoding(id) {
  return request({
    url: '/model/encoding/' + id,
    method: 'delete'
  })
}
