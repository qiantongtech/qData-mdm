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

// 按页查询一条主数据的正式版本及排序、变更展示信息，不加载 SNAPSHOT_DATA。
export function getMasterDataVersions(instanceId, masterDataId, query) {
  return request({
    url: `/master/data/version/list/${instanceId}/${masterDataId}`,
    method: 'get',
    params: query
  })
}

// 按字符串版本 ID 单独读取所属主数据的历史快照及截断标志，避免 BIGINT 精度丢失。
export function getMasterDataVersionSnapshot(instanceId, masterDataId, versionId) {
  return request({
    url: `/master/data/version/snapshot/${instanceId}/${masterDataId}/${versionId}`,
    method: 'get'
  })
}
