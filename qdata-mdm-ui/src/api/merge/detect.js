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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

/**
 * 多源融合接口封装，统一通过项目请求工具调用后端。
 * @author chentangwen
 */
import request from '@/utils/request'

/**
 * 分页查询重复检测任务。
 * @param {*} query 接口查询条件。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function listMergeTask(query) {
  return request({
    url: '/merge/detect/task/list',
    method: 'get',
    params: query,
  })
}

/**
 * 分页查询检测执行实例。
 * @param {*} query 接口查询条件。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function listMergeInstance(query) {
  return request({
    url: '/merge/detect/instance/list',
    method: 'get',
    params: query,
  })
}

/**
 * 增量读取检测过程日志及当前进度。
 * @param {*} taskId 检测任务编号。
 * @param {*} instanceId 执行实例编号。
 * @param {number} afterSeq 排他日志游标。
 * @returns {Promise<Object>} 过程日志与后续游标。
 */
export function getMergeDetectRunLogs(taskId, instanceId, afterSeq = 0) {
  return request({
    url: `/merge/detect/task/${taskId}/instance/${instanceId}/logs`,
    method: 'get',
    params: { afterSeq },
  })
}

/**
 * 分页查询检测产生的候选组。
 * @param {*} query 接口查询条件。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function listMergeGroup(query) {
  return request({
    url: '/merge/detect/group/list',
    method: 'get',
    params: query,
  })
}

/** 读取当前执行记录下候选组的来源数据及列定义。 */
export function listMergeGroupSources(groupId, taskInstanceId) {
  return request({
    url: `/merge/detect/group/${groupId}/sources`,
    method: 'get',
    params: { taskInstanceId },
  })
}

/**
 * 预览检测范围内的系统、数量及来源记录。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function previewMergeScope(data) {
  return request({
    url: '/merge/detect/scope',
    method: 'post',
    data,
  })
}

/**
 * 保存检测配置并立即发起检测。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function startMergeDetect(data) {
  return request({
    url: '/merge/detect/start',
    method: 'post',
    data,
  })
}

/**
 * 保存重复检测任务配置。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function saveMergeDetectTask(data) {
  return request({
    url: '/merge/detect/task/save',
    method: 'post',
    data,
  })
}

/**
 * 更新指定检测任务的范围配置。
 * @param {*} taskId 检测任务编号。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function updateMergeDetectTask(taskId, data) {
  return request({
    url: `/merge/detect/task/${taskId}`,
    method: 'put',
    data,
  })
}

/** 更新重复检测任务启停状态。 */
export function changeMergeDetectTaskStatus(data) {
  return request({ url: '/merge/detect/task/status', method: 'put', data })
}

/** 独立启停重复检测任务的定时调度。 */
export function changeMergeDetectScheduleStatus(data) {
  return request({ url: '/merge/detect/task/schedule-status', method: 'put', data })
}

/**
 * 删除指定重复检测任务。
 * @param {*} taskId 检测任务编号。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function deleteMergeDetectTask(taskId) {
  return request({
    url: `/merge/detect/task/${taskId}`,
    method: 'delete',
  })
}

/**
 * 发起指定任务的一次检测执行。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function executeMergeDetectTask(data) {
  return request({
    url: '/merge/detect/task/execute',
    method: 'post',
    data,
  })
}

/**
 * 将检测候选组转入匹配审核。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function transferMergeGroups(data) {
  return request({
    url: '/merge/detect/group/transfer',
    method: 'post',
    data,
  })
}

/**
 * 按给定来源范围重新发起检测。
 * @param {*} data 接口提交数据。
 * @returns {Promise<Object>} 后端接口响应。
 */
export function redetectMerge(data) {
  return request({
    url: '/merge/detect/redetect',
    method: 'post',
    data,
  })
}
