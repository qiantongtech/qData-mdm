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

package tech.qiantong.qdata.mdm.ingest.mapping;

import com.alibaba.fastjson2.JSONPath;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import java.util.Map;
import java.util.function.Function;

/**
 * 数据库和文件使用原始字段名，API 额外支持记录内的字段路径。
 */
public final class SourceFieldAccessor {
    private SourceFieldAccessor() { }

    public static Function<Map<String, Object>, Object> compile(String field, boolean api) {
        if (field == null || field.trim().isEmpty() || field.length() > 128)
            throw new ServiceException("来源字段不能为空且不能超过128个字符");
        String path = field.startsWith("$") ? field : field.startsWith("[") ? "$" + field : "$." + field;
        JSONPath compiled = api && path.matches("\\$(\\.[\\p{L}_][\\p{L}\\p{N}_-]*|\\[[0-9]+\\])+")
                ? JSONPath.of(path) : null;
        return row -> {
            // 优先精确列名，避免带点的真实字段名被误当成嵌套路径。
            if (row.containsKey(field)) return row.get(field);
            if (compiled != null && compiled.contains(row)) return compiled.eval(row);
            throw new ServiceException("配置的来源字段不存在");
        };
    }
}
