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

/**
 * @file operation-log.js
 * @description 业务操作审计接口
 * @author zyy
 */
import request from '@/utils/request'

// 查询业务操作审计列表
export function listLOG(query) {
  return request({
    url: '/tech/qiantong/qdata/mdm/audit/LOG/list',
    method: 'get',
    params: query
  })
}

// 查询业务操作审计详细
export function getLOG(id) {
  return request({
    url: '/tech/qiantong/qdata/mdm/audit/LOG/' + id,
    method: 'get'
  })
}

// 新增业务操作审计
export function addLOG(data) {
  return request({
    url: '/tech/qiantong/qdata/mdm/audit/LOG',
    method: 'post',
    data: data
  })
}

// 修改业务操作审计
export function updateLOG(data) {
  return request({
    url: '/tech/qiantong/qdata/mdm/audit/LOG',
    method: 'put',
    data: data
  })
}

// 删除业务操作审计
export function delLOG(id) {
  return request({
    url: '/tech/qiantong/qdata/mdm/audit/LOG/' + id,
    method: 'delete'
  })
}

