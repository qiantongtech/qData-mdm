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

export const listServiceAuthorizations = (params) => request({ url: '/auth/service-accesses/list', method: 'get', params })
export const listServiceAuthorizationsByServiceId = (serviceId) => request({ url: '/auth/service-accesses/list-by-service-id', method: 'get', params: { serviceId } })
export const getServiceAuthorization = (id) => request({ url: '/auth/service-accesses/info', method: 'get', params: { id } })
export const createServiceAuthorization = (data) => request({ url: '/auth/service-accesses', method: 'post', data })
export const batchGrantServiceAuthorizations = (data) => request({ url: '/auth/service-accesses/batch-grant', method: 'post', data })
export const updateServiceAuthorization = (data) => request({ url: '/auth/service-accesses', method: 'put', data })
export const revokeServiceAuthorization = (id) => request({ url: `/auth/service-accesses/${id}/revoke`, method: 'post' })
export const batchRevokeServiceAuthorizations = (ids) => request({ url: '/auth/service-accesses/batch-revoke', method: 'post', data: ids })
export const batchDeleteServiceAuthorizations = (ids) => request({ url: '/auth/service-accesses/batch', method: 'delete', data: ids })
