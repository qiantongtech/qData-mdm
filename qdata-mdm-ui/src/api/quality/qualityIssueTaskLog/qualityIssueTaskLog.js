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

// 查询问题整改记录日志列表
export function listQualityIssueTaskLog(query) {
  return request({
    url: '/quality/qualityIssueTaskLog/list',
    method: 'get',
    params: query
  })
}

// 查询问题整改记录日志详细
export function getQualityIssueTaskLog(id) {
  return request({
    url: '/quality/qualityIssueTaskLog/' + id,
    method: 'get'
  })
}

// 新增问题整改记录日志
export function addQualityIssueTaskLog(data) {
  return request({
    url: '/quality/qualityIssueTaskLog',
    method: 'post',
    data: data
  })
}

// 修改问题整改记录日志
export function updateQualityIssueTaskLog(data) {
  return request({
    url: '/quality/qualityIssueTaskLog',
    method: 'put',
    data: data
  })
}

// 删除问题整改记录日志
export function delQualityIssueTaskLog(id) {
  return request({
    url: '/quality/qualityIssueTaskLog/' + id,
    method: 'delete'
  })
}
