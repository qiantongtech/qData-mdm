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
 * 多源融合接口封装，统一通过项目请求工具调用后端。
 * @author chentangwen
 */
import request from '@/utils/request'

/**
 * 分页查询候选组审核记录。
 * @param {*} query 接口查询条件。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function listMergeReview(query) {
  return request({
    url: '/merge/review/list',
    method: 'get',
    params: query,
  })
}

/**
 * 读取候选组及其来源成员。
 * @param {*} id 业务记录编号。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function getMergeReview(id) {
  return request({
    url: '/merge/review/' + id,
    method: 'get',
  })
}

/**
 * 确认候选组成员属于同一业务对象。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function confirmMergeReview(data) {
  return request({
    url: '/merge/review/confirm',
    method: 'post',
    data,
  })
}

/**
 * 暂缓候选组审核。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function suspendMergeReview(data) {
  return request({
    url: '/merge/review/suspend',
    method: 'post',
    data,
  })
}

/**
 * 恢复已暂缓候选组的审核。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function resumeMergeReview(data) {
  return request({
    url: '/merge/review/resume',
    method: 'post',
    data,
  })
}

/**
 * 删除候选组并释放来源数据。
 * @param {*} id 业务记录编号。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function deleteMergeReview(id) {
  return request({
    url: '/merge/review/' + id,
    method: 'delete',
  })
}
