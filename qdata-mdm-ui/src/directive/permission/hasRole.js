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

/**
 * v-hasRole role permission processing
 * Copyright (c) 2019 qdata
 */

import { i18n } from '@/plugins/vueI18n';
import useUserStore from '@/store/system/user';

export default {
  mounted(el, binding, vnode) {
    const { value } = binding;
    const super_admin = 'admin';
    const roles = useUserStore().roles;

    if (value && value instanceof Array && value.length > 0) {
      const roleFlag = value;

      const hasRole = roles.some((role) => {
        return super_admin === role || roleFlag.includes(role);
      });

      if (!hasRole) {
        el.parentNode && el.parentNode.removeChild(el);
      }
    } else {
      throw new Error(i18n.global.t('common.error.setRoleValue'));
    }
  }
};
