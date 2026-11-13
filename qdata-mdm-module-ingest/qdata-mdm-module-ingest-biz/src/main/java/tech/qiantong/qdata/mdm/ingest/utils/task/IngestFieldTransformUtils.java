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

package tech.qiantong.qdata.mdm.ingest.utils.task;

import com.alibaba.fastjson2.JSON;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestModelFieldRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskFieldMappingDO;
import tech.qiantong.qdata.mdm.ingest.mapping.FieldProcessors;
import tech.qiantong.qdata.mdm.ingest.mapping.TypeConverters;
import java.util.Collections;
import java.util.Map;

/**
 * 字段处理兼容入口，轻量处理后统一按模型类型转换。
 */
public final class IngestFieldTransformUtils {
    private IngestFieldTransformUtils() { }

    public static Object apply(IngestTaskFieldMappingDO mapping, Object value, String modelType) {
        if (mapping == null) return value;
        return apply(mapping.getTransformType(), mapping.getTransformConfig() == null
                ? null : JSON.parseObject(mapping.getTransformConfig()), value, modelType);
    }

    public static Object apply(String type, Map<String, Object> config, Object value, String modelType) {
        IngestModelFieldRespVO field = new IngestModelFieldRespVO();
        field.setDataType(modelType);
        Map<String, Object> params = config == null ? Collections.emptyMap() : config;
        return TypeConverters.compile(field, params).convert(FieldProcessors.compile(type, params, field).process(value));
    }
}
