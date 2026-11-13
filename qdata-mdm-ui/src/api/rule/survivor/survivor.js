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

export function listRulesurvivor(query) {
  return request({ url: '/rule/rulesurvivor/list', method: 'get', params: query });
}

export function getRulesurvivor(id) {
  return request({ url: `/rule/rulesurvivor/${id}`, method: 'get' });
}

export function addRulesurvivor(data) {
  return request({ url: '/rule/rulesurvivor', method: 'post', data });
}

export function updateRulesurvivor(data) {
  return request({ url: '/rule/rulesurvivor', method: 'put', data });
}

export function delRulesurvivor(id) {
  return request({ url: `/rule/rulesurvivor/${id}`, method: 'delete' });
}

export function listSurvivorMergeRecords(id, query) {
  return request({ url: `/rule/rulesurvivor/${id}/merge-records`, method: 'get', params: query });
}
