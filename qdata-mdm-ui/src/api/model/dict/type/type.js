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

// 查询参考数据列表
export function listType(query) {
  return request({
    url: '/model/dict/type/list',
    method: 'get',
    params: query
  })
}

// 查询有效参考数据类型，返回结构与 listType 一致。
export function listValidType(query) {
  return request({ url: '/model/dict/type/valid-list', method: 'get', params: query })
}

// 查询参考数据详细
export function getType(id) {
  return request({
    url: '/model/dict/type/' + id,
    method: 'get'
  })
}

// 新增参考数据
export function addType(data) {
  return request({
    url: '/model/dict/type/',
    method: 'post',
    data: data
  })
}

// 修改参考数据
export function updateType(data) {
  return request({
    url: '/model/dict/type/',
    method: 'put',
    data: data
  })
}

// 修改参考数据状态
export function updateTypeStatus(id, status) {
  return request({
    url: `/model/dict/type/updateStatus/${id}/${status}`,
    method: 'get'
  })
}

// 批量启用或停用参考数据
export function updateTypeStatusBatch(ids, status) {
  return request({
    url: '/model/dict/type/batchStatus/' + status,
    method: 'put',
    data: ids
  })
}

// 删除参考数据
export function delType(id) {
  return request({
    url: '/model/dict/type/' + id,
    method: 'delete'
  })
}
