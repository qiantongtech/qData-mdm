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

// 查询参考数据项列表
export function listData(query) {
  return request({
    url: '/model/dict/data/list',
    method: 'get',
    params: query
  })
}

// 查询指定参考数据类型下的有效参考数据项，返回结构与 listData 一致。
export function listValidData(query) {
  return request({ url: '/model/dict/data/valid-list', method: 'get', params: query })
}

// 查询参考数据引用情况：模型基础信息及 fields 引用字段列表
export function listReferenceUsage(query) {
  // 每个模型返回一次，fields 仅包含引用当前 typeId 的字段。
  return request({ url: '/model/dict/data/reference-usage', method: 'get', params: query })
}

// 查询参考数据项详细
export function getData(id) {
  return request({
    url: '/model/dict/data/' + id,
    method: 'get'
  })
}

// 新增参考数据项
export function addData(data) {
  return request({
    url: '/model/dict/data',
    method: 'post',
    data: data
  })
}

// 修改参考数据项
export function updateData(data) {
  return request({
    url: '/model/dict/data',
    method: 'put',
    data: data
  })
}

// 修改参考数据项状态
export function updateDataStatus(id, status) {
  return request({
    url: `/model/dict/data/updateStatus/${id}/${status}`,
    method: 'post'
  })
}

// 删除参考数据项
export function delData(id) {
  return request({
    url: '/model/dict/data/' + id,
    method: 'delete'
  })
}
