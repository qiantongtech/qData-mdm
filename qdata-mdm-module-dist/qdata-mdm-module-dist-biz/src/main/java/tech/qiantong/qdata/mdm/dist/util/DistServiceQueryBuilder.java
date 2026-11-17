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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.util;

import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceFieldDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 根据服务字段配置构造运行时查询条件和输出字段。
 */
public final class DistServiceQueryBuilder {
    private DistServiceQueryBuilder() {
    }

    /**
     * 仅允许配置为可查询的字段；请求值通过占位符绑定，不拼接进 SQL。
     */
    public static String buildWhere(List<DistServiceFieldDO> fields, Map<Long, String> fieldCodes,
            Map<Long, String> physicalColumns, Map<String, String> requestParameters,
            DbDialect dialect, List<Object> values) {
        Map<String, DistServiceFieldDO> queryFields = new HashMap<>();
        for (DistServiceFieldDO field : fields) {
            if (!isActive(field) || !"1".equals(field.getQueryableFlag())) continue;
            String code = fieldCodes.get(field.getModelColumnId());
            String column = physicalColumns.get(field.getModelColumnId());
            if (code == null || column == null) continue;
            if (!isIdentifier(column)) throw new ServiceException("模型字段名称无效");
            queryFields.put(code.toLowerCase(Locale.ROOT), field);
        }

        List<String> predicates = new ArrayList<>();
        for (Map.Entry<String, String> parameter : requestParameters.entrySet()) {
            DistServiceFieldDO field = queryFields.get(parameter.getKey().toLowerCase(Locale.ROOT));
            if (field == null) throw new ServiceException("请求字段未配置为可查询字段");
            String column = physicalColumns.get(field.getModelColumnId());
            predicates.add(dialect.quoteIdentifier(column) + " = ?");
            values.add(parameter.getValue());
        }
        return predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
    }

    /**
     * 按有效状态和输出标志筛选响应字段，未配置输出字段时拒绝查询。
     */
    public static List<DistServiceFieldDO> outputFields(List<DistServiceFieldDO> fields) {
        if (fields == null) return Collections.emptyList();
        List<DistServiceFieldDO> output = new ArrayList<>();
        for (DistServiceFieldDO field : fields) {
            if (isActive(field) && "1".equals(field.getOutputFlag())) output.add(field);
        }
        if (output.isEmpty()) throw new ServiceException("数据服务未配置输出字段");
        return output;
    }

    private static boolean isActive(DistServiceFieldDO field) {
        return field != null && "1".equals(field.getValidFlag()) && !"1".equals(field.getDelFlag());
    }

    private static boolean isIdentifier(String value) {
        return value != null && value.matches("[\\p{L}_][\\p{L}\\p{N}_$]*");
    }
}
