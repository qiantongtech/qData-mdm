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

import { DatasourceConfig, DatasourceTypes, config } from "./config";
import {
  createDatasourceTypeFilter,
  getTypeConfig,
  isDatasourceEngineCategory,
  isDatasourceEngineType,
  isDatasourceTypeAllowed,
  isDatasourceTypeExplicitlyAllowed,
} from "./typeFilter";

export const DEFAULT_DATASOURCE_TYPE_SELECTOR_FLAG = "datasource";

function getRules(flag) {
  return config?.[flag || DEFAULT_DATASOURCE_TYPE_SELECTOR_FLAG] || {};
}

function sortDisabledLast(a, b) {
  if (a.disabled == b.disabled) return 0;
  return a.disabled ? 1 : -1;
}

export function getDatasourceTypeSelectorTypes(options = {}) {
  const {
    flag = DEFAULT_DATASOURCE_TYPE_SELECTOR_FLAG,
    datasourceType,
    includeEngine = false,
  } = options || {};
  const rules = getRules(flag);
  const filter = createDatasourceTypeFilter(datasourceType, getTypeConfig);
  const seenKeys = new Set();

  return Object.values(DatasourceTypes)
    .filter((type) => {
      if (!type?.key || seenKeys.has(type.key)) return false;
      seenKeys.add(type.key);
      if (
        !includeEngine &&
        isDatasourceEngineType(type) &&
        !isDatasourceTypeExplicitlyAllowed(type, filter, getTypeConfig)
      )
        return false;
      if (!isDatasourceTypeAllowed(type, filter, getTypeConfig)) return false;
      return rules[type.key] !== 3;
    })
    .map((type) => ({
      ...type,
      disabled: rules[type.key] === 2,
    }))
    .sort(sortDisabledLast);
}

export function getDatasourceTypeSelectorCategories(
  types = getDatasourceTypeSelectorTypes()
) {
  const categoryKeys = new Set(types.map((type) => type.category));

  return DatasourceConfig.filter((category) => {
    return (
      categoryKeys.has(category.key) && !isDatasourceEngineCategory(category)
    );
  }).map(({ key, label, icon }) => ({ key, label, icon }));
}
