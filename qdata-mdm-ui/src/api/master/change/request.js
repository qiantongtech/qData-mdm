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

// 查询当前发布实例的主、子模型维护表单配置
export function getMaintenanceFormConfig(instanceId, query) {
  return request({
    url: `/master/data/metadata/${instanceId}/maintenance`,
    method: 'get',
    params: query
  })
}

// 查询当前登录人已经提交的正式主数据申请
export function listMasterChangeRequests(query) {
  return request({
    url: '/master/change/request/list',
    method: 'get',
    params: query
  })
}

// 查询当前登录人的申请所涉及的模型选项
export function listMasterChangeRequestModels() {
  return request({
    url: '/master/change/request/modelOptions',
    method: 'get'
  })
}

// 查询当前登录人的一条正式申请及其明细
export function getMasterChangeRequestDetail(requestId) {
  return request({
    url: `/master/change/request/${requestId}`,
    method: 'get'
  })
}

// 撤回本人仍在审批中且尚未执行的申请
export function withdrawMasterChangeRequest(requestId) {
  return request({
    url: `/master/change/request/${requestId}/withdraw`,
    method: 'post'
  })
}

// 基于已驳回、已撤回或已终止的申请生成新申请
export function reapplyMasterChangeRequest(requestId) {
  return request({
    url: `/master/change/request/${requestId}/reapply`,
    method: 'post'
  })
}

// 服务端只读预校验；不创建申请或修改正式数据
export function validateMasterChangeMaintenance(instanceId, data) {
  return request({
    url: `/master/change/request/maintenance/validate/${instanceId}`,
    method: 'post',
    data
  })
}

// 预校验通过后建立正式申请记录
export function submitMasterChangeMaintenance(instanceId, data) {
  return request({
    url: `/master/change/request/maintenance/submit/${instanceId}`,
    method: 'post',
    data
  })
}

// 批量修改、状态和导入共用整批预校验
export function validateMasterChangeBatch(instanceId, data) {
  return request({
    url: `/master/change/request/maintenance/batch/validate/${instanceId}`,
    method: 'post',
    data
  })
}

// 批量预校验通过后建立正式申请记录
export function submitMasterChangeBatch(instanceId, data) {
  return request({
    url: `/master/change/request/maintenance/batch/submit/${instanceId}`,
    method: 'post',
    data
  })
}

// 下载当前模型动态字段的导入模板
export function downloadMasterChangeImportTemplate(instanceId) {
  return request({
    url: `/master/change/request/maintenance/batch/import/template/${instanceId}`,
    method: 'get',
    responseType: 'blob'
  })
}

// 解析导入文件，不保存或校验业务数据
export function parseMasterChangeImport(instanceId, file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: `/master/change/request/maintenance/batch/import/parse/${instanceId}`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  })
}

// 查询当前主数据已审批并执行成功的状态操作
export function getMasterChangeStatusRecords(instanceId, masterDataId, query) {
  return request({
    url: `/master/change/request/status/list/${instanceId}/${masterDataId}`,
    method: 'get',
    params: query
  })
}

// 查询当前主数据关联的直接维护申请
export function getMasterChangeRecordApprovals(instanceId, masterDataId, query) {
  return request({
    url: `/master/change/request/recordApproval/list/${instanceId}/${masterDataId}`,
    method: 'get',
    params: query
  })
}

// 查询申请中属于当前主数据的明细
export function getMasterChangeRecordApprovalDetail(instanceId, masterDataId, requestId) {
  return request({
    url: `/master/change/request/recordApproval/detail/${instanceId}/${masterDataId}/${requestId}`,
    method: 'get'
  })
}

// 查询指定模型的直接维护申请
export function getMasterChangeApprovals(instanceId, query) {
  return request({
    url: `/master/change/request/approval/list/${instanceId}`,
    method: 'get',
    params: query
  })
}

// 查询指定模型的直接维护申请详情
export function getMasterChangeApprovalDetail(instanceId, requestId) {
  return request({
    url: `/master/change/request/approval/detail/${instanceId}/${requestId}`,
    method: 'get'
  })
}
