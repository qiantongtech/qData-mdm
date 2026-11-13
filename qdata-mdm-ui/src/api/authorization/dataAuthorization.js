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

import request from '@/utils/request.js'

export const listDataAuthorizations = (params) => request({ url: '/auth/data-grants/list', method: 'get', params })
export const getDataAuthorization = (id) => request({ url: '/auth/data-grants/info', method: 'get', params: { id } })
export const createDataAuthorization = (data) => request({ url: '/auth/data-grants', method: 'post', data })
export const batchGrantDataAuthorizations = (data) => request({ url: '/auth/data-grants/batch-grant', method: 'post', data })
export const updateDataAuthorization = (data) => request({ url: '/auth/data-grants', method: 'put', data })
export const revokeDataAuthorization = (id) => request({ url: `/auth/data-grants/${id}/revoke`, method: 'post' })
export const batchRevokeDataAuthorizations = (ids) => request({ url: '/auth/data-grants/batch-revoke', method: 'post', data: ids })
export const batchDeleteDataAuthorizations = (ids) => request({ url: '/auth/data-grants/batch', method: 'delete', data: ids })
