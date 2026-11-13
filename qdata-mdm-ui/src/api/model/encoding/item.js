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

// 按编码版本分页查询规则段，用于进入规则配置页时回显数据库数据。
export function listEncodingItems(query) {
  return request({
    url: '/model/encoding/item/list',
    method: 'get',
    params: query
  })
}

// 查询有效编码规则段，参数和返回结构与 listEncodingItems 一致。
export function listValidEncodingItems(query) {
  return request({
    url: '/model/encoding/item/valid-list',
    method: 'get',
    params: query
  })
}

// 新增单个规则段，保留给其他需要单条提交的页面使用。
export function addEncodingItem(data) {
  return request({
    url: '/model/encoding/item',
    method: 'post',
    data
  })
}

// 修改单个规则段，保留给其他需要单条提交的页面使用。
export function updateEncodingItem(data) {
  return request({
    url: '/model/encoding/item',
    method: 'put',
    data
  })
}

// 将当前版本的整套规则段一次性提交，由后端在同一事务内完成增、改、删和排序。
export function saveEncodingItems(data) {
  return request({
    url: '/model/encoding/item/batch',
    method: 'put',
    data
  })
}

// 按规则段 ID 删除数据，保留给单条删除场景使用。
export function deleteEncodingItems(ids) {
  return request({
    url: `/model/encoding/item/${ids}`,
    method: 'delete'
  })
}
