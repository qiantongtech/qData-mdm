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

import useUserStore from '@/store/system/user';

/**
 * Character permission verification
 * @param {Array} value check value
 * @returns {Boolean}
 */
export function checkPermi(value) {
  if (value && value instanceof Array && value.length > 0) {
    const permissions = useUserStore().permissions;
    const permissionDatas = value;
    const all_permission = '*:*:*';

    const hasPermission = permissions.some((permission) => {
      return all_permission === permission || permissionDatas.includes(permission);
    });

    if (!hasPermission) {
      return false;
    }
    return true;
  } else {
    console.error(`need roles! Like checkPermi="['system:user:add','system:user:edit']"`);
    return false;
  }
}

/**
 * Role permission verification
 * @param {Array} value check value
 * @returns {Boolean}
 */
export function checkRole(value) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = useUserStore().roles;
    const permissionRoles = value;
    const super_admin = 'admin';

    const hasRole = roles.some((role) => {
      return super_admin === role || permissionRoles.includes(role);
    });

    if (!hasRole) {
      return false;
    }
    return true;
  } else {
    console.error(`need roles! Like checkRole="['admin','editor']"`);
    return false;
  }
}
