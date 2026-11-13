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

// 查询当前主数据已生效的业务变更；状态变更由独立页签处理
export function getMasterChangeList(instanceId, masterDataId, query) {
  return request({
    url: `/master/change/list/${instanceId}/${masterDataId}`,
    method: 'get',
    params: query
  })
}

// 查询变更前后快照及按原始 JSON 计算的字段差异
export function getMasterChangeSnapshot(instanceId, masterDataId, changeId) {
  return request({
    url: `/master/change/snapshot/${instanceId}/${masterDataId}/${changeId}`,
    method: 'get'
  })
}
