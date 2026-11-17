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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request';
import { listPublishedModels } from '@/api/model/base/base';

// 数据服务与字段配置由后端作为一个整体保存。
export function listService(params) {
  return request({ url: '/dist/service/list', method: 'get', params });
}

export function getService(id) {
  return request({ url: `/dist/service/${id}`, method: 'get' });
}

export function addService(data) {
  return request({ url: '/dist/service', method: 'post', data });
}

export function updateService(data) {
  return request({ url: '/dist/service', method: 'put', data });
}

// 启停只提交服务ID及状态，避免覆盖基本信息和字段配置。
export function changeServiceStatus(data) {
  return request({ url: '/dist/service/status', method: 'put', data });
}

export function deleteService(ids) {
  return request({ url: `/dist/service/${ids.join(',')}`, method: 'delete' });
}

// 下拉框需要完整的已发布模型列表，使用不分页参数一次获取。
export async function listServiceModels() {
  const response = await listPublishedModels({ pageNum: 1, pageSize: -1 });
  return response.data.rows || [];
}
