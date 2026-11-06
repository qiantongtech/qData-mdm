/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import request from '@/utils/request';

// Query container management-server configuration list
export function listcontainerServer(query) {
  return request({
    url: '/system/containerServer/list',
    method: 'get',
    params: query
  });
}

// Query container management-server configuration details
export function getcontainerServer(ID) {
  return request({
    url: '/system/containerServer/info',
    method: 'get',
    params: { id: ID }
  }).then((response) => {
    response.data.containerList?.forEach((i) => {
      if (i.CreatedAt) {
        i.CreatedAt = new Date(i.CreatedAt.replace(' CST', ''));
      }
    });
    return response;
  });
}

// Added container management-server configuration
export function addcontainerServer(data) {
  return request({
    url: '/system/containerServer',
    method: 'post',
    data: data
  });
}

// Modify container management-server configuration
export function updatecontainerServer(data) {
  return request({
    url: '/system/containerServer',
    method: 'put',
    data: data
  });
}

// Remove container management-server configuration
export function delcontainerServer(ID) {
  return request({
    url: '/system/containerServer/' + ID,
    method: 'delete'
  });
}
