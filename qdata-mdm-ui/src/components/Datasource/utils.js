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

import { DatasourceTypes, config } from './config';
import {
    listDaDatasource,
    listDaDatasourceByProjectCode,
} from '@/api/ingest/datasource/dataSource';

export function getDatasourceTypes(flag) {
    if (!flag || !config[flag]) return Object.values(DatasourceTypes);
    const rules = config[flag];

    return Object.keys(DatasourceTypes)
        .reduce((acc, key) => {
            if (rules[key] && rules[key] === 3) return acc;
            acc.push({ ...DatasourceTypes[key], disabled: rules[key] === 2 });
            return acc;
        }, [])
        .sort((a, b) => {
            if (a.disabled == b.disabled) return 0;
            return a.disabled ? 1 : -1;
        });
}

// 获取全部数据数据源
export function getDatasourceData(params, listApi) {
    const request = listApi === 'dpp'
        ? listDaDatasourceByProjectCode
        : listDaDatasource;
    return request(params).then((res) => {
        const rows = res.data.rows || [];
        rows.forEach((item) => {
            if (item.datasourceConfigs && typeof item.datasourceConfigs === 'string') {
                try {
                    item.datasourceConfigs = JSON.parse(item.datasourceConfigs);
                } catch {
                    item.datasourceConfigs = {};
                }
            } else if (!item.datasourceConfigs) {
                item.datasourceConfigs = {};
            }
        });
        return rows;
    });
}

// 根据flag获取可用数据源
export function getAvailableDatasource(data, flag) {
    if (!data) return [];
    if (!flag || !config[flag]) return data;
    const rules = config[flag];
    const types = Object.values(DatasourceTypes);
    return data
        .reduce((acc, item) => {
            if (!item.datasourceType) return acc;
            const type = types.find((type) => type.value === item.datasourceType);
            if (!type) return acc;
            if (rules[type.key] && rules[type.key] === 3) return acc;
            acc.push({ ...item, disabled: rules[type.key] === 2 });
            return acc;
        }, [])
        .sort((a, b) => {
            if (a.disabled == b.disabled) return 0;
            return a.disabled ? 1 : -1;
        });
}

export function getDatasourceActive(value) {
    return Object.values(DatasourceTypes).find((item) =>
        item.value?.toUpperCase() === value?.toUpperCase()
    );
}
