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

package tech.qiantong.qdata.mdm.master.util;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;

import java.util.Locale;
import java.util.Set;

/** 动态物理表记录归属校验工具；只检查物理字段和记录，不加载版本或审批扩展信息。 */
public final class MasterRecordUtils {
    private MasterRecordUtils() {
    }

    /** 校验物理表配置、字段和记录归属；记录不存在或稳定主数据 ID 重复时拒绝查询。 */
    public static void requireRecord(MasterDataQueryRepository repository,
                                     MasterModelRespDTO model, Long recordId) {
        if (recordId == null || recordId <= 0) throw new ServiceException("主数据ID格式无效");
        String table = model.getPhysicalTableName();
        if (table == null) throw new ServiceException("模型物理表名缺失");
        String[] parts = table.split("\\.", -1);
        if (parts.length == 2) {
            table = identifier(parts[0]) + "." + identifier(parts[1]);
        } else if (parts.length == 1) {
            String schema = model.getSchemaName();
            table = (schema == null || schema.trim().isEmpty() ? "" : identifier(schema.trim()) + ".")
                    + identifier(table);
        } else {
            throw new ServiceException("模型物理表名无效");
        }
        try {
            Set<String> physical = repository.findPhysicalColumns(model.getDatasourceId(), table);
            requireColumn(physical, model.getColumnMappings().get("ID"));
            requireColumn(physical, model.getColumnMappings().get("STATUS"));
            if (model.getFields() == null || model.getFields().isEmpty()) {
                throw new ServiceException("模型字段配置为空");
            }
            int nameFields = 0;
            for (MasterFieldRespDTO field : model.getFields()) {
                String code = identifier(field.getCode());
                if (Boolean.TRUE.equals(field.getNameField())) nameFields++;
                if (!"SYSTEM".equalsIgnoreCase(field.getCategory()) || Boolean.TRUE.equals(field.getNameField())) {
                    requireColumn(physical, model.getColumnMappings().get(code));
                }
            }
            if (nameFields != 1) throw new ServiceException("模型必须配置且仅配置一个名称字段");
            repository.requireRecord(model.getDatasourceId(), table, recordId, model.getColumnMappings());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("模型物理表不存在，请检查物化结果");
        }
    }

    private static String identifier(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_$]*")) throw new ServiceException("模型物理字段名无效");
        return value;
    }

    private static void requireColumn(Set<String> physical, String code) {
        if (code == null || !physical.contains(code.toUpperCase(Locale.ROOT))) throw new ServiceException("模型字段配置与物理表不一致：" + code);
    }
}
