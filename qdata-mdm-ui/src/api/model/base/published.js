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

/** 查询已成功发布的模型，供分发任务选择正式模型。 */
export function listPublishedModels(query) {
  return request({ url: '/model/published/list', method: 'get', params: query })
}

/** 按模型 ID 读取成功发布的模型详情。 */
export function getPublishedModel(modelId) {
  return request({ url: `/model/published/${modelId}`, method: 'get' })
}

/** 读取已发布模型的字段结构，供分发映射和筛选使用。 */
export function getPublishedModelStructure(modelId) {
  return request({ url: `/model/published/${modelId}/structure`, method: 'get' })
}
