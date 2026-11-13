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

export const SERVICE_AUTHORIZATION_SUBJECT_TYPES = [
  { value: 'user', label: '平台用户', elTagType: 'primary' },
  { value: 'client', label: '服务客户端', elTagType: 'info' }
]

export const SERVICE_AUTHORIZATION_CLIENTS = [
  { id: 4001, name: 'SRM-PROD', code: 'C4001', type: 'client' },
  { id: 4002, name: 'BI-REPORT', code: 'C4002', type: 'client' },
  { id: 4003, name: 'CRM-SYNC', code: 'C4003', type: 'client' },
  { id: 4004, name: 'PORTAL-APP', code: 'C4004', type: 'client' }
]

export const SERVICE_AUTHORIZATION_SERVICES = [
  { id: 1, name: '供应商主数据查询服务', code: 'API_SUPPLIER_QUERY', version: 'v2.1' },
  { id: 2, name: '客户主数据查询服务', code: 'API_CUSTOMER_QUERY', version: 'v1.6' },
  { id: 3, name: '组织机构查询服务', code: 'API_ORG_QUERY', version: 'v3.0' },
  { id: 4, name: '供应商变更订阅服务', code: 'API_SUPPLIER_CHANGE', version: 'v1.3' }
]

export const SERVICE_AUTHORIZATION_FIELDS = [
  ['供应商名称', 'supplier_name'],
  ['供应商类型', 'supplier_type'],
  ['供应商等级', 'supplier_level'],
  ['所属区域', 'region'],
  ['统一社会信用代码', 'credit_code'],
  ['联系人', 'contact'],
  ['联系电话', 'phone'],
  ['注册地址', 'address'],
  ['开户银行', 'bank_name'],
  ['银行账号', 'bank_account'],
  ['备注', 'remark'],
  ['状态', 'status']
].map(([name, code], index) => ({ id: index + 1, name, code }))
