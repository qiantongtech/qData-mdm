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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request'

// 查询分发任务列表
export function listTASK(query) {
  return request({
    url: '/dist/task/list',
    method: 'get',
    params: query
  })
}

// 查询分发任务详细
// 向导回显可按需加载筛选条件和分发字段映射；默认详情保持生成器原有响应规模。
export function getTASK(id, options = {}) {
  return request({
    url: '/dist/task/' + id,
    method: 'get',
    params: options
  })
}

// 人工执行分发任务一次
export function runTASK(id) {
  return request({
    url: '/dist/task/' + id + '/run',
    method: 'post'
  })
}

// 与 Pro2 离线任务一致，由服务端完整克隆任务及关联配置。
export function cloneTASK(id) {
  return request({
    url: '/dist/task/' + id + '/clone',
    method: 'post'
  })
}

// 新增分发任务
export function addTASK(data) {
  return request({
    url: '/dist/task',
    method: 'post',
    data: data
  })
}

// 修改分发任务
export function updateTASK(data) {
  return request({
    url: '/dist/task',
    method: 'put',
    data: data
  })
}

// 删除分发任务
export function delTASK(id) {
  return request({
    url: '/dist/task/' + id,
    method: 'delete'
  })
}
