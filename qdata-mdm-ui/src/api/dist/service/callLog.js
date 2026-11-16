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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request';

// 分页查询指定数据服务的调用记录。
export function listServiceCallLogs(params) {
  return request({ url: '/dist/service-call-logs/list', method: 'get', params });
}

// 根据日志主键获取单次调用详情。
export function getServiceCallLog(id) {
  return request({ url: `/dist/service-call-logs/${id}`, method: 'get' });
}

// 获取近七天调用统计及历史最近一次调用摘要。
export function getServiceCallLogSummary(serviceId) {
  return request({ url: '/dist/service-call-logs/summary', method: 'get', params: { serviceId } });
}
