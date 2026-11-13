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

// 查询主数据域列表
export function listProject(query) {
  return request({
    url: '/model/project/list',
    method: 'get',
    params: query
  })
}

// 查询主数据域列表 不分页
export function treeProject() {
  return request({
    url: '/model/project/tree',
    method: 'get'
  })
}

// 查询主数据域详细
export function getProject(id) {
  return request({
    url: '/model/project/' + id,
    method: 'get'
  })
}

// 新增主数据域
export function addProject(data) {
  return request({
    url: '/model/project',
    method: 'post',
    data: data
  })
}

// 修改主数据域
export function updateProject(data) {
  return request({
    url: '/model/project',
    method: 'put',
    data: data
  })
}

// 修改主数据域状态
export function updateProjectStatus(id, status) {
  return request({
    url: `/model/project/updateStatus/${id}/${status}`,
    method: 'get'
  })
}

// 删除主数据域
export function delProject(id) {
  return request({
    url: '/model/project/' + id,
    method: 'delete'
  })
}
