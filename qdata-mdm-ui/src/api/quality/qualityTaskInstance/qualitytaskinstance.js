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

// 查询主数据质量任务执行实例列表
export function executeQualitytask(taskId) {
  return request({ url: '/quality/qualitytaskinstance/execute/' + taskId, method: 'post' })
}

export function listQualitytaskinstance(query) {
  return request({
    url: '/quality/qualitytaskinstance/list',
    method: 'get',
    params: query
  })
}

// 查询主数据质量任务执行实例详细
export function getQualitytaskinstance(id) {
  return request({
    url: '/quality/qualitytaskinstance/' + id,
    method: 'get'
  })
}

// 新增主数据质量任务执行实例
export function addQualitytaskinstance(data) {
  return request({
    url: '/quality/qualitytaskinstance',
    method: 'post',
    data: data
  })
}

// 修改主数据质量任务执行实例
export function updateQualitytaskinstance(data) {
  return request({
    url: '/quality/qualitytaskinstance',
    method: 'put',
    data: data
  })
}

// 删除主数据质量任务执行实例
export function delQualitytaskinstance(id) {
  return request({
    url: '/quality/qualitytaskinstance/' + id,
    method: 'delete'
  })
}
