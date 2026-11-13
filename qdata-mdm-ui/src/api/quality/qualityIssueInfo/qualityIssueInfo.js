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

// 查询问题数据明细列表
export function listQualityIssueInfo(query) {
  return request({
    url: '/quality/qualityIssueInfo/list',
    method: 'get',
    params: query
  })
}

// 查询问题数据明细详细
export function getQualityIssueInfo(id) {
  return request({
    url: '/quality/qualityIssueInfo/' + id,
    method: 'get'
  })
}

// 新增问题数据明细
export function addQualityIssueInfo(data) {
  return request({
    url: '/quality/qualityIssueInfo',
    method: 'post',
    data: data
  })
}

// 修改问题数据明细
export function updateQualityIssueInfo(data) {
  return request({
    url: '/quality/qualityIssueInfo',
    method: 'put',
    data: data
  })
}

// 删除问题数据明细
export function delQualityIssueInfo(id) {
  return request({
    url: '/quality/qualityIssueInfo/' + id,
    method: 'delete'
  })
}
