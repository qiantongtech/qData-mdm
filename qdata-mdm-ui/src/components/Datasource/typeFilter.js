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

import { DatasourceConfig } from "./config.js";

export function normalizeDatasourceType(value) {
  if (value === undefined || value === null || value === "") return [];
  const values = Array.isArray(value) ? value : [value];
  return values
    .flatMap((item) => String(item).split(","))
    .map((item) => item.trim())
    .filter(Boolean);
}

function addValue(target, value) {
  if (value === undefined || value === null || value === "") return;
  target.add(String(value).toUpperCase());
}

function addTypeValues(target, type, resolveType) {
  if (!type) return;
  if (typeof type === "object") {
    addValue(target, type.key);
    addValue(target, type.value);
    return;
  }
  addValue(target, type);
  const config = resolveType?.(type);
  if (config) {
    addValue(target, config.key);
    addValue(target, config.value);
  }
}

export function createDatasourceTypeFilter(datasourceType, resolveType) {
  const types = normalizeDatasourceType(datasourceType);
  if (!types.length) return null;
  const filter = new Set();
  types.forEach((type) => addTypeValues(filter, type, resolveType));
  return filter;
}

export function isDatasourceTypeAllowed(type, filter, resolveType) {
  if (!filter?.size) return true;
  const candidates = new Set();
  addTypeValues(candidates, type, resolveType);
  return [...candidates].some((candidate) => filter.has(candidate));
}

export function isDatasourceTypeExplicitlyAllowed(type, filter, resolveType) {
  if (!filter?.size) return false;
  const candidates = new Set();
  addTypeValues(candidates, type, resolveType);
  return [...candidates].some((candidate) => filter.has(candidate));
}

const BIG_DATA_CATEGORIES = ["engine"];

export function isCategoryBigData(categoryKey) {
  return BIG_DATA_CATEGORIES.includes(String(categoryKey || "").toLowerCase());
}

export function isDatasourceTypeHiddenByFlag(
  type,
  flag,
  config,
  filter,
  resolveType
) {
  const rules = flag ? config?.[flag] : null;

  const typeConfig = typeof type === "object" ? type : resolveType?.(type);
  const typeKey = typeConfig?.key ?? type;
  const typeCategory = typeConfig?.category;

  const isHiddenByFlag = rules && rules[typeKey] === 3;
  const isBigData = isCategoryBigData(typeCategory);
  const isExplicitlyAllowed = isDatasourceTypeExplicitlyAllowed(
    typeConfig || type,
    filter,
    resolveType
  );

  return (isHiddenByFlag || isBigData) && !isExplicitlyAllowed;
}

const ENGINE_DATASOURCE_CATEGORY = "engine";

export function isDatasourceEngineCategory(category) {
  const categoryKey = typeof category === "object" ? category?.key : category;
  return String(categoryKey || "").toLowerCase() === ENGINE_DATASOURCE_CATEGORY;
}

export function isDatasourceEngineType(type, resolveType) {
  const typeConfig = typeof type === "object" ? type : resolveType?.(type);
  return isDatasourceEngineCategory(typeConfig?.category);
}

export function getTypeConfig(typeKey) {
  const targetTypeKey = String(typeKey || "").toUpperCase();
  return DatasourceConfig.flatMap((c) => Object.values(c.types)).find(
    (t) =>
      t.key?.toUpperCase() === targetTypeKey ||
      t.value?.toUpperCase() === targetTypeKey
  );
}
