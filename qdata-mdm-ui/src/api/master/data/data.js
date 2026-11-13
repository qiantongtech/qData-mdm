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

// 维护控件只获取关联模型的发布实例定位信息，不依赖模型定义管理权限。
export function getMasterReferenceModel(modelId) {
  return request({ url: `/master/data/metadata/referenceModel/${modelId}`, method: 'get' })
}

// 页面进入时读取列定义；分页请求只获取记录和统计。
export function getMasterDataMetadata(instanceId, query) {
  return request({ url: `/master/data/metadata/${instanceId}`, method: 'get', params: query })
}

// 来源系统筛选项独立于字段定义、列表数据加载。
export function getMasterSourceSystems(instanceId, query) {
  return request({ url: `/master/data/sourceSystems/${instanceId}`, method: 'get', params: query })
}

// 按当前模型和筛选条件查询正式主数据列表
export function getMasterData(instanceId, query) {
  return request({
    url: `/master/data/${instanceId}`,
    method: 'get',
    params: query
  })
}

// 分类树形表格一次获取完整筛选结果，避免行分页拆开父子节点。
export function getMasterCategoryData(instanceId, query) {
  return request({
    url: `/master/data/category/${instanceId}`,
    method: 'get',
    params: query
  })
}

// 按稳定主数据 ID 查询正式记录，不依赖当前列表页缓存
export function getMasterRecordDetail(instanceId, masterDataId, modelId) {
  return request({
    url: `/master/data/${instanceId}/${masterDataId}`,
    method: 'get',
    params: { modelId }
  })
}

// 查询单条正式主数据关联的子模型定义及选中子模型的分页数据
export function getMasterSubmodels(instanceId, masterDataId, query) {
  return request({
    url: `/master/data/submodels/${instanceId}/${masterDataId}`,
    method: 'get',
    params: query
  })
}

// 新增主记录时查询子模型及可维护字段，不依赖已存在的父记录
export function getMasterSubmodelDefinitions(instanceId, query) {
  return request({
    url: `/master/data/submodelDefinitions/${instanceId}`,
    method: 'get',
    params: query
  })
}

// 查询单条子记录，服务端同时校验主记录和子模型归属
export function getMasterSubmodelRecord(instanceId, masterDataId, submodelId, subRecordId) {
  return request({
    url: `/master/data/submodels/${instanceId}/${masterDataId}/${submodelId}/${subRecordId}`,
    method: 'get'
  })
}

// 查询当前分类模型的分类树
export function getMasterTree(instanceId) {
  return request({
    url: `/master/data/tree/${instanceId}`,
    method: 'get'
  })
}

// 一次读取普通模型的分类字段、目标模型名称和完整导航树。
export function getMasterClassifications(instanceId, query) {
  return request({ url: `/master/data/classifications/${instanceId}`, method: 'get', params: query })
}

// 分类控件完整选项，避免从普通列表第一页拼接分类树。
export function getMasterClassificationOptions(instanceId, query) {
  return request({ url: `/master/data/classificationOptions/${instanceId}`, method: 'get', params: query })
}
