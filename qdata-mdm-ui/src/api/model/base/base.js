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

// 查询主数据模型基本信息列表
export function listBase(query) {
  return request({
    url: '/model/base/list',
    method: 'get',
    params: query
  })
}

// 查询主数据模型基本信息详细
export function getBase(id) {
  return request({
    url: '/model/base/' + id,
    method: 'get'
  })
}

// 查询模型关系图谱；id 作为可选查询参数包含在 query 中
export function getModelRelationGraph(query = {}) {
  return request({
    url: '/model/base/graph',
    method: 'get',
    params: query
  })
}

// 查询主数据域下除当前模型外的关联模型
export function listRelatedModels(query) {
  return request({
    url: '/model/base/related',
    method: 'get',
    params: query
  })
}

// 新增主数据模型基本信息
export function addBase(data) {
  return request({
    url: '/model/base',
    method: 'post',
    data: data
  })
}

// 修改主数据模型基本信息
export function updateBase(data) {
  return request({
    url: '/model/base',
    method: 'put',
    data: data
  })
}

// 删除主数据模型基本信息
export function delBase(id) {
  return request({
    url: '/model/base/' + id,
    method: 'delete'
  })
}

// 发布主数据模型，并同步主模型及子模型的物理实例
export function publishBase(id, data) {
  return request({
    url: '/model/base/' + id + '/publish',
    method: 'post',
    data
  })
}

// 停用模型，将当前版本设置为封存状态
export function disableBase(id) {
  return request({url: '/model/base/' + id + '/disable', method: 'post'})
}

// 批量操作一次提交整批模型，逐项执行和结果汇总由后端处理。
export function batchPublishBase(data) {
  return request({url: '/model/base/batch/publish', method: 'post', data})
}

export function batchDisableBase(ids) {
  return request({url: '/model/base/batch/disable', method: 'post', data: {ids}})
}

export function batchDeleteBase(ids) {
  return request({url: '/model/base/batch', method: 'delete', data: {ids}})
}

// 查询已发布主数据模型列表
export function listPublishedModels(query) {
  return request({
    url: '/model/published/list',
    method: 'get',
    params: query
  })
}

// 查询已发布模型字段及子模型
export function getPublishedModelStructure(id) {
  return request({ url: `/model/published/${id}/structure`, method: 'get' })
}

// 主数据域 → 模型目录 → 最近一次成功发布的模型（不含空目录）
export function getPublishedModelTree() {
  return request({ url: '/model/published/tree', method: 'get' })
}

// 查询成功发布版本绑定的物化信息，用于发布弹窗回显原数据源
export function getPublishedModelMaterialization(id) {
  return request({
    url: '/model/published/' + id + '/materialization',
    method: 'get'
  })
}
