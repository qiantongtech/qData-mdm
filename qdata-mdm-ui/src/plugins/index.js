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

import tab from './tab';
import auth from './auth';
import cache from './cache';
import modal from './modal';
import download from './download';

export default function installPlugins(app) {
  // Tab operation
  app.config.globalProperties.$tab = tab;
  // Authentication object
  app.config.globalProperties.$auth = auth;
  // Cache object
  app.config.globalProperties.$cache = cache;
  // Modal object
  app.config.globalProperties.$modal = modal;
  // Download file
  app.config.globalProperties.$download = download;
}
