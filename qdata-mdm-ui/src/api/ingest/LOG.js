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

// 来源数据页面接口
export function listSourceData(query) {
  return request({
    url: '/ingest/source-data/list',
    method: 'get',
    params: query
  })
}

/** 获取来源数据左侧的主数据域、模型目录和模型树。 */
export function getSourceDataModelTree() {
  return request({
    url: '/ingest/source-data/model-tree',
    method: 'get'
  })
}

export function getSourceDataModelColumns(modelId) {
  return request({
    url: '/ingest/source-data/model/' + modelId + '/columns',
    method: 'get'
  })
}

export function getSourceDataDetail(id, modelId) {
  return request({
    url: '/ingest/source-data/' + id,
    method: 'get',
    params: { modelId }
  })
}

export function deleteSourceData(id, modelId) {
  return request({
    url: '/ingest/source-data/' + id,
    method: 'delete',
    params: { modelId }
  })
}
