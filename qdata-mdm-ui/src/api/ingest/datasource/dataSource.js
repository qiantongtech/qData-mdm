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

import request from '@/utils/request'

// 查询数据源列表
export function listDaDatasource(query) {
    return request({
        url: '/ingest/dataSource/list',
        method: 'get',
        params: query
    });
}

export function daList(query) {
    return request({
        url: '/da/list',
        method: 'get',
        params: query
    });
}
// 查询数据源列表
export function sqlParse(data) {
    return request({
        url: '/ingest/dataSource/sqlParse',
        method: 'post',
        data: data
    });
}

// 数据集成中排除Kafka并且是当前项目的数据源列表
export function listDaDatasourceNoKafkaByProjectCode(query) {
    return request({
        url: '/ingest/dataSource/dppNoKafka/list',
        method: 'get',
        params: query
    });
}

// 查询研发模块的数据源列表
export function listDaDatasourceByProjectCode(query) {
    return request({
        url: '/ingest/dataSource/dpp/list',
        method: 'get',
        params: query
    });
}

// 查询研发模块的数据源列表
export function noDppAdd(query) {
    return request({
        url: '/ingest/dataSource/noDppAdd/list',
        method: 'get',
        params: query
    });
}

// 查询数据源列表
export function getDaDatasourceList(query) {
    return request({
        url: '/ingest/dataSource/getDataSourceByAsset',
        method: 'get',
        params: query
    });
}

// 查询数据源详细
export function getDaDatasource(id) {
    return request({
        url: '/ingest/dataSource/' + id,
        method: 'get'
    });
}

// 查询数据源详细
export function clientsTest(id) {
    return request({
        url: '/ingest/dataSource/clientsTest/' + id,
        method: 'get'
    });
}
// 新增数据源
export function addDaDatasource(data) {
    return request({
        url: '/ingest/dataSource',
        method: 'post',
        data: data
    });
}

// 修改数据源
export function updateDaDatasource(data) {
    return request({
        url: '/ingest/dataSource',
        method: 'put',
        data: data
    });
}

// 删除数据源
export function delDaDatasource(id) {
    return request({
        url: '/ingest/dataSource/' + id,
        method: 'delete'
    });
}

// 删除数据源
export function removeDppOrDa(id) {
    return request({
        url: '/ingest/dataSource/' + id,
        method: 'delete'
    });
}

// 修改状态
export function editDatasourceStatus(id, status) {
    return request({
        url: `/ingest/dataSource/editDatasourceStatus/${id}/${status}`,
        method: 'get'
    });
}

export function tableList(ID) {
    const idVal = typeof ID === 'object' ? ID.datasourceId : ID
    const params = { limit: -1 }
    if (typeof ID === 'object') {
        if (ID.tableName) params.tableName = ID.tableName
        if (ID.sourceId) params.sourceId = ID.sourceId
        if (ID.limit !== undefined && ID.limit !== null) params.limit = ID.limit
    }
    return request({
        url: '/ingest/dataSource/tableList/' + idVal,
        method: 'get',
        params
    });
}
// 表字段
export function columnsList(data) {
    return request({
        url: `/ingest/dataSource/columnsList`,
        method: 'post',
        data: data
    });
}

// 根据id获取表信息
export function getTablesByDataSourceId(query) {
    return request({
        url: '/da/asset/getTablesByDataSourceId',
        method: 'get',
        params: query
    });
}

// 根据id获取表信息
export function getColumnByAssetId(query) {
    return request({
        url: '/da/assetColumn/getColumnByAssetId',
        method: 'get',
        params: query
    });
}

// 查询研发模块的数据源列表
export function executeSqlQuery(query) {
    return request({
        url: '/ingest/dataSource/executeSqlQuery',
        method: 'get',
        params: query
    });
}
// 数据资产资产申请列表
export function getDaAssetRespList(query) {
    return request({
        url: '/da/asset/getDaAssetRespList',
        method: 'get',
        params: query
    });
}
// 查询数据源详细
export function clientsTestStatus(data) {
    return request({
        url: '/ingest/dataSource/clientsTestStatus',
        method: 'post',
        data: data
    });
}

export function diagnoseDatasource(data) {
    return request({
        url: '/ingest/dataSource/diagnose',
        method: 'post',
        data: data
    });
}
