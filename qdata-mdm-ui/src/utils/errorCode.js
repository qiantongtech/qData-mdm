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

import { i18n } from '@/plugins/vueI18n';

const errorCode = {};

const errorCodeMap = {
  401: 'common.error.code401',
  403: 'common.error.code403',
  404: 'common.error.code404',
  default: 'common.error.default'
};

Object.entries(errorCodeMap).forEach(([code, key]) => {
  Object.defineProperty(errorCode, code, {
    enumerable: true,
    get: () => i18n.global.t(key)
  });
});

export default errorCode;
