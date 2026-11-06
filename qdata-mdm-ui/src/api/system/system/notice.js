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

import request from '@/utils/request.js';

// Query announcement list
export function listNotice(query) {
  return request({
    url: '/system/notice/list',
    method: 'get',
    params: query
  });
}

// Check the announcement details
export function getNotice(noticeId) {
  return request({
    url: '/system/notice/' + noticeId,
    method: 'get'
  });
}

// New announcement
export function addNotice(data) {
  return request({
    url: '/system/notice',
    method: 'post',
    data: data
  });
}

// Modification notice
export function updateNotice(data) {
  return request({
    url: '/system/notice',
    method: 'put',
    data: data
  });
}

// Delete announcement
export function delNotice(noticeId) {
  return request({
    url: '/system/notice/' + noticeId,
    method: 'delete'
  });
}

// Home Announcement List
export function sortListNotice(query) {
  return request({
    url: '/system/notice/sortList',
    method: 'get',
    params: query
  });
}

// Home page special tips
export function alertNotice() {
  return request({
    url: '/system/notice/alertNotice',
    method: 'get'
  });
}

// Check the announcement details-new
export function getNoticeOne(noticeId) {
  return request({
    url: `/system/notice/one?noticeId=${noticeId}`,
    method: 'get'
  });
}
