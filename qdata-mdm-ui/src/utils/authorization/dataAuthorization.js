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

export const DATA_AUTHORIZATION_SUBJECT_TYPES = [
  { value: 'user', label: '用户', elTagType: 'primary' },
  { value: 'role', label: '角色', elTagType: 'success' },
  { value: 'organization', label: '组织', elTagType: 'warning' }
]

export const DATA_AUTHORIZATION_OPERATIONS = {
  VIEW: '查看', CREATE: '新增', UPDATE: '修改', COPY: '复制', IMPORT: '导入', EXPORT: '导出',
  BATCH_UPDATE: '批量修改', FREEZE: '冻结', UNFREEZE: '解冻', INVALIDATE: '失效', RESTORE: '恢复', ARCHIVE: '归档'
}
