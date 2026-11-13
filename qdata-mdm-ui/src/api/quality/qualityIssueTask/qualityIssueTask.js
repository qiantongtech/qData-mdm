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

// 查询问题整改任务列表
export function listQualityIssueTask(query) {
  return request({
    url: '/quality/qualityIssueTask/list',
    method: 'get',
    params: query
  })
}

/**
 * 统计各状态的整改任务数量。
 * @param {Object} query 筛选条件
 * @returns {Promise<Object>} 任务状态统计
 */
export function getQualityIssueTaskStatusCount(query) {
  return request({
    url: '/quality/qualityIssueTask/statusCount',
    method: 'get',
    params: query
  })
}

// 查询问题整改任务详细
export function getQualityIssueTask(id) {
  return request({
    url: '/quality/qualityIssueTask/' + id,
    method: 'get'
  })
}

// 新增问题整改任务
export function addQualityIssueTask(data) {
  return request({
    url: '/quality/qualityIssueTask',
    method: 'post',
    data: data
  })
}

// 修改问题整改任务
export function updateQualityIssueTask(data) {
  return request({
    url: '/quality/qualityIssueTask',
    method: 'put',
    data: data
  })
}

/**
 * 提交整改任务审批：待审批 → 审批中。
 * @param {number|string} id 整改任务 ID
 * @returns {Promise<Object>} 接口响应
 */
export function submitQualityIssueTaskApproval(id) {
  return request({
    url: '/quality/qualityIssueTask/' + id + '/submitApproval',
    method: 'put'
  })
}

/**
 * 撤回整改任务审批：审批中 → 待审批。
 * @param {number|string} id 整改任务 ID
 * @returns {Promise<Object>} 接口响应
 */
export function withdrawQualityIssueTaskApproval(id) {
  return request({
    url: '/quality/qualityIssueTask/' + id + '/withdrawApproval',
    method: 'put'
  })
}

// 删除问题整改任务
export function delQualityIssueTask(ids) {
  return request({
    url: '/quality/qualityIssueTask/' + ids,
    method: 'delete'
  })
}
