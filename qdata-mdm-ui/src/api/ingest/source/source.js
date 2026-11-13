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

import request from '@/utils/request.js'

// 查询数据库连接下拉列表
export function listIngestDatasourceOptions(query) {
  return request({
    url: '/ingest/dataSource/options',
    method: 'get',
    params: query
  })
}

// 查询来源系统列表
export function listIngestSource(query) {
  return request({
    url: '/ingest/source/list',
    method: 'get',
    params: query
  })
}

// 查询来源系统详细
export function getIngestSource(id) {
  return request({
    url: '/ingest/source/' + id,
    method: 'get'
  })
}

// 新增来源系统
export function addIngestSource(data) {
  return request({
    url: '/ingest/source',
    method: 'post',
    data: data
  })
}

// 修改来源系统
export function updateIngestSource(data) {
  return request({
    url: '/ingest/source',
    method: 'put',
    data: data
  })
}

// 删除来源系统
export function delIngestSource(id) {
  return request({
    url: '/ingest/source/' + id,
    method: 'delete'
  })
}
