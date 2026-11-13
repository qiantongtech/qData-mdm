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

// 查询主数据质量任务列表
export function listQualitytask(query) {
  return request({
    url: '/quality/qualitytask/list',
    method: 'get',
    params: {
      ...query,
      modelIds: Array.isArray(query?.modelIds) ? query.modelIds.join(',') : query?.modelIds
    }
  })
}

// 查询主数据质量任务详细
export function getQualitytask(id) {
  return request({
    url: '/quality/qualitytask/' + id,
    method: 'get'
  })
}

// 新增主数据质量任务
export function addQualitytask(data) {
  return request({
    url: '/quality/qualitytask',
    method: 'post',
    data: data
  })
}

// 修改主数据质量任务
export function updateQualitytask(data) {
  return request({
    url: '/quality/qualitytask',
    method: 'put',
    data: data
  })
}

// 删除主数据质量任务
export function delQualitytask(id) {
  return request({
    url: '/quality/qualitytask/' + id,
    method: 'delete'
  })
}
