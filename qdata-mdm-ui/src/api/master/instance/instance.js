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
 * @file master/instance/instance.js
 * @description 封装权威主数据入口卡片的分页查询接口。
 * @author chentangwen
 * @created 2026-10-12
 * @lastModifiedBy chentangwen
 * @lastModified 2026-10-29
 */

import request from '@/utils/request'

/**
 * 按筛选条件分页查询权威主数据入口卡片。
 * @param {Object} query 分页与筛选参数。
 * @param {number} query.pageNum 当前页码，从 1 开始。
 * @param {number} query.pageSize 每页条数。
 * @param {string} [query.name] 模型名称。
 * @param {string|number} [query.projectId] 主数据域 ID。
 * @param {string|number} [query.layerId] 模型目录 ID。
 * @param {string|number} [query.modelId] 主数据模型 ID，精确匹配。
 * @param {string} [query.type] 模型类型编码。
 * @returns {Promise<{data: {rows: Object[], total: number}}>} 卡片列表及总条数。
 * @throws {Error} 请求失败时返回的 Promise 被拒绝，由调用方处理。
 */
export function listMasterInstances(query) {
  return request({
    url: '/master/instance/list',
    method: 'get',
    params: query
  })
}
