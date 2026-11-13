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
 * 分页查询合并管理记录。
 * @param {*} query 接口查询条件。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function listMergePending(query) {
  return request({
    url: '/merge/manage/list',
    method: 'get',
    params: query,
  })
}

/**
 * 读取合并记录详情及字段候选值。
 * @param {*} id 业务记录编号。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function getMergePending(id) {
  return request({
    url: '/merge/manage/' + id,
    method: 'get',
  })
}

/**
 * 查询合并结果的审批与流转记录分页。
 * @param {*} id 当前业务记录编号。
 * @param {*} query 接口查询条件。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function getMergePendingApprovalRecords(id, query) {
  return request({
    url: `/merge/manage/${id}/approval-records`,
    method: 'get',
    params: query,
  })
}

/**
 * 保存合并字段取值和调整原因。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function adjustMergeField(data) {
  return request({
    url: '/merge/manage/field',
    method: 'put',
    data,
  })
}

/**
 * 确认当前合并结果。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function confirmMergePending(data) {
  return request({
    url: '/merge/manage/confirm',
    method: 'post',
    data,
  })
}

/**
 * 执行合并结果质量检测。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function qualityMergePending(data) {
  return request({
    url: '/merge/manage/quality',
    method: 'post',
    data,
  })
}

/**
 * 提交合并结果审批。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function submitMergePending(data) {
  return request({
    url: '/merge/manage/submit',
    method: 'post',
    data,
  })
}

/**
 * 撤回合并结果审批申请。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function withdrawMergePending(data) {
  return request({
    url: '/merge/manage/withdraw',
    method: 'post',
    data
  })
}

/**
 * 取消当前合并结果的处理。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function cancelMergePending(data) {
  return request({
    url: '/merge/manage/cancel',
    method: 'post',
    data
  })
}

/**
 * 重新提交合并结果审批。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function resubmitMergePending(data) {
  return request({
    url: '/merge/manage/resubmit',
    method: 'post',
    data
  })
}
