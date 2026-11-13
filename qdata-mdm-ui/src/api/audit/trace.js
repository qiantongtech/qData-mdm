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
 * @file trace.js
 * @description 审计追溯列表及详情接口
 * @author zyy
 */
import request from '@/utils/request'

const base = '/audit/trace'
const pageParams = (params = {}) => {
  const { timeRange, ...rest } = params
  return { ...rest, beginTime: timeRange?.[0], endTime: timeRange?.[1] }
}

export const listAuditOperations = (params) => request({ url: `${base}/operations`, method: 'get', params: pageParams(params) })
export const getAuditOperation = (id, source = 'log') => request({ url: `${base}/operations/${id}`, method: 'get', params: { source } })
export const listAuditChanges = (params) => request({ url: `${base}/changes`, method: 'get', params: pageParams(params) })
export const getAuditChange = (id) => request({ url: `${base}/changes/${id}`, method: 'get' })
export const listAuditVersions = (params) => request({ url: `${base}/versions`, method: 'get', params: pageParams(params) })
export const getAuditVersion = (id) => request({ url: `${base}/versions/${id}`, method: 'get' })
export const listAuditObjectVersions = (modelId, masterRecordId) => request({ url: `${base}/versions/object`, method: 'get', params: { modelId, masterRecordId } })
export const listAuditSources = (params) => request({ url: `${base}/sources`, method: 'get', params: pageParams(params) })
export const getAuditSource = (modelId, masterRecordId) => request({ url: `${base}/sources/${modelId}/${masterRecordId}`, method: 'get' })
export const listAuditModels = () => request({ url: `${base}/models`, method: 'get' })
