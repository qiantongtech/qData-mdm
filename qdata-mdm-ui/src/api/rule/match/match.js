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

import request from '@/utils/request';

export function listRulematch(query) {
  return request({ url: '/rule/rulematch/list', method: 'get', params: query });
}

export function getRulematch(id) {
  return request({ url: `/rule/rulematch/${id}`, method: 'get' });
}

export function addRulematch(data) {
  return request({ url: '/rule/rulematch', method: 'post', data });
}

export function updateRulematch(data) {
  return request({ url: '/rule/rulematch', method: 'put', data });
}

export function delRulematch(id) {
  return request({ url: `/rule/rulematch/${id}`, method: 'delete' });
}

