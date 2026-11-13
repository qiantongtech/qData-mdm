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

// 查询编码方案版本列表
export function listVersion(query) {
    return request({
        url: '/model/encoding/version/list',
        method: 'get',
        params: query
    })
}

// 查询编码方案版本详细
export function getVersion(id) {
    return request({
        url: '/model/encoding/version/' + id,
        method: 'get'
    })
}

// 新增编码方案版本
export function addVersion(data) {
    return request({
        url: '/model/encoding/version',
        method: 'post',
        data: data
    })
}

// 修改编码方案版本
export function updateVersion(data) {
    return request({
        url: '/model/encoding/version',
        method: 'put',
        data: data
    })
}

// 删除编码方案版本
export function delVersion(id) {
    return request({
        url: '/model/encoding/version/' + id,
        method: 'delete'
    })
}

// 修改版本状态
export function updateStatus(id, status) {
    return request({
        url: `/model/encoding/version/updateStatus/${id}/${status}`,
        method: 'get'
    })
}