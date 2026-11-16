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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request'

// 查询分发目标系统列表
export function listTargetSystem(query) {
  return request({
    url: '/dist/target/list',
    method: 'get',
    params: query
  })
}

// 查询分发目标系统详细
export function getTargetSystem(id) {
  return request({
    url: '/dist/target/' + id,
    method: 'get'
  })
}

// 新增分发目标系统
export function addTargetSystem(data) {
  return request({
    url: '/dist/target',
    method: 'post',
    data: data
  })
}

// 修改分发目标系统
export function updateTargetSystem(data) {
  return request({
    url: '/dist/target',
    method: 'put',
    data: data
  })
}

// 删除分发目标系统
export function delTargetSystem(id) {
  return request({
    url: '/dist/target/' + id,
    method: 'delete'
  })
}
