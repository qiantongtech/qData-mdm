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

// 查询模型目录层级列表
export function listLayer(query) {
  return request({
    url: '/model/layer/list',
    method: 'get',
    params: query
  })
}

// 查询模型目录层级列表
export function treeLayer(query) {
  return request({
    url: '/model/layer/tree',
    method: 'get',
    params: query
  })
}

// 查询模型目录层级详细
export function getLayer(id) {
  return request({
    url: '/model/layer/' + id,
    method: 'get'
  })
}

// 新增模型目录层级
export function addLayer(data) {
  return request({
    url: '/model/layer/',
    method: 'post',
    data: data
  })
}

// 修改模型目录层级
export function updateLayer(data) {
  return request({
    url: '/model/layer/',
    method: 'put',
    data: data
  })
}

// 修改模型目录层级状态
export function updateLayerStatus(id, status) {
  return request({
    url: `/model/layer/updateStatus/${id}/${status}`,
    method: 'get'
  })
}

// 删除模型目录层级
export function delLayer(id) {
  return request({
    url: '/model/layer/' + id,
    method: 'delete'
  })
}
