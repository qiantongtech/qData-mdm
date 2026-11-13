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

import request from '@/utils/request.js';

export const listIngestTask = (params) =>
  request({ url: '/ingest/task/list', method: 'get', params });
export const getIngestTask = (id) =>
  request({ url: `/ingest/task/${id}`, method: 'get' });
export const listIngestTaskRuns = (id, params) =>
  request({ url: `/ingest/task/${id}/runs`, method: 'get', params });
export const getIngestRunLogs = (id, runId, afterSeq = 0) =>
  request({ url: `/ingest/task/${id}/runs/${runId}/logs`, method: 'get', params: { afterSeq } });
export const runIngestTask = (id) =>
  request({ url: `/ingest/task/${id}/run`, method: 'post' });
export const addIngestTask = (data) =>
  request({ url: '/ingest/task', method: 'post', data });
export const updateIngestTask = (data) =>
  request({ url: '/ingest/task', method: 'put', data });
export const copyIngestTask = (id) =>
  request({ url: `/ingest/task/${id}/copy`, method: 'post' });
export const changeIngestTaskStatus = (data) =>
  request({ url: '/ingest/task/status', method: 'put', data });
export const changeIngestTaskScheduleStatus = (data) =>
  request({ url: '/ingest/task/schedule/status', method: 'put', data });
export const delIngestTask = (ids) =>
  request({ url: `/ingest/task/${ids}`, method: 'delete' });
export const previewIngestTask = (data) =>
  request({ url: '/ingest/task/preview', method: 'post', data });
export function uploadIngestFile(file) {
  const data = new FormData();
  data.append('file', file);
  return request({
    url: '/ingest/task/file',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
  });
}
