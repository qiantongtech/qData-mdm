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

package tech.qiantong.qdata.mdm.merge.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

/**
 * 多源融合 JSON 读写。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public final class MergeJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MergeJson() {
    }

    /** 序列化融合快照；失败时保留原始异常，便于定位具体字段。 */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("结果序列化失败");
            failure.initCause(ex);
            throw failure;
        }
    }

    /**
     * 严格解析检测用来源数据；调用方必须显式决定拒绝或跳过非法数据。
     *
     * @param text JSON 文本
     * @return JSON 节点
     * @throws ServiceException 文本为空或解析失败
     */
    public static JsonNode readStrict(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException("JSON 数据不能为空");
        }
        try {
            JsonNode value = MAPPER.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(text);
            if (value == null) {
                throw new ServiceException("JSON 数据不能为空");
            }
            return value;
        } catch (JsonProcessingException ex) {
            ServiceException failure = new ServiceException("JSON 数据格式无效");
            failure.initCause(ex);
            throw failure;
        }
    }

    /** 容错读取历史展示数据；空值或非法 JSON 返回空对象，不用于来源有效性判断。 */
    public static JsonNode read(String text) {
        if (text == null || text.trim().isEmpty()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(text);
        } catch (Exception ex) {
            return MAPPER.createObjectNode();
        }
    }

    /** 容错读取历史配置；解析失败返回 null，由调用方使用兼容默认值。 */
    public static <T> T read(String text, TypeReference<T> type) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(text, type);
        } catch (Exception ex) {
            return null;
        }
    }
}
