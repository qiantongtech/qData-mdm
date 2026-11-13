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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestApiReadConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskApiConfigVO;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 复用现有参数 JSON 字段保存请求参数与读取配置，通过版本标识兼容历史平铺参数。
 * 包装结构仅用于持久化，实际 HTTP 请求只发送 requestParams 中的内容。
 *
 * @author chentangwen
 */
public final class IngestApiConfigCodec {
    private static final String VERSION_KEY = "_ingestReadConfigVersion";
    private IngestApiConfigCodec() {
    }

    /**
     * 未配置读取规则时保持历史格式，否则写入版本化包装结构。
     *
     * @param config API 配置
     * @return 用于参数配置字段持久化的 JSON
     */
    public static String encode(IngestTaskApiConfigVO config) {
        if (config.getReadConfig() == null) {
            return JSON.toJSONString(config.getParamConfig());
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put(VERSION_KEY, 1);
        // 空参数使用空对象，避免序列化忽略 null 后丢失包装结构的识别字段。
        envelope.put("requestParams", config.getParamConfig() == null ? Collections.emptyMap() : config.getParamConfig());
        envelope.put("readConfig", config.getReadConfig());
        return JSON.toJSONString(envelope);
    }

    /**
     * 仅在版本和包装字段均匹配时拆分读取配置，其余 JSON 作为历史请求参数保留。
     *
     * @param json 数据库中的参数配置 JSON
     * @param config 接收解析结果的 API 配置
     */
    public static void decode(String json, IngestTaskApiConfigVO config) {
        JSONObject params = JSON.parseObject(json);
        if (params == null) {
            return;
        }
        if (Integer.valueOf(1).equals(params.get(VERSION_KEY)) && params.size() == 3
                && params.get("readConfig") instanceof Map && params.containsKey("requestParams")) {
            config.setParamConfig(params.getJSONObject("requestParams"));
            config.setReadConfig(params.getObject("readConfig", IngestApiReadConfigVO.class));
        } else {
            config.setParamConfig(params);
        }
    }
}
