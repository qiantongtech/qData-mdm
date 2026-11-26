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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestApiReadConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskApiConfigVO;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接入任务 API 配置的持久化编解码器。
 *
 * <p>复用参数配置字段保存请求参数与响应读取规则：历史格式直接保存请求参数，
 * 版本化格式使用 {@code _ingestReadConfigVersion}、{@code requestParams} 和
 * {@code readConfig} 包装。版本标识与任一包装字段同时出现时视为版本化结构；
 * 业务参数包含同样的字段组合时也使用包装格式，避免解码歧义。
 * 包装结构仅用于持久化，解码后的请求参数才参与 HTTP 请求。</p>
 *
 * <p>本类只校验存储结构；响应路径、分页方式等业务规则由读取器校验。</p>
 *
 * @author chentangwen
 */
public final class IngestApiConfigCodec {

    /** 版本化存储结构的识别字段。 */
    private static final String VERSION_KEY = "_ingestReadConfigVersion";

    private static final int CURRENT_VERSION = 1;
    private static final String REQUEST_PARAMS_KEY = "requestParams";
    private static final String READ_CONFIG_KEY = "readConfig";

    private IngestApiConfigCodec() {
    }

    /**
     * 将 API 配置编码为参数配置字段的存储 JSON。
     *
     * <p>未配置读取规则且请求参数不包含包装字段组合时，保持历史格式。
     * 包装格式中的空请求参数保存为空对象，空读取规则显式保存为 {@code null}。</p>
     *
     * @param config API 配置，不可为 {@code null}
     * @return 用于参数配置字段持久化的 JSON
     * @throws ServiceException API 配置为空时抛出
     */
    public static String encode(IngestTaskApiConfigVO config) {
        requireConfig(config);
        Map<String, Object> requestParams = config.getParamConfig();
        if (ObjectUtil.isNull(config.getReadConfig()) && !isEnvelope(requestParams)) {
            return JSON.toJSONString(requestParams);
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put(VERSION_KEY, CURRENT_VERSION);
        envelope.put(REQUEST_PARAMS_KEY, ObjectUtil.isNull(requestParams) ? Collections.emptyMap() : requestParams);
        envelope.put(READ_CONFIG_KEY, config.getReadConfig());
        if (ObjectUtil.isNull(config.getReadConfig())) {
            // 包装字段组合冲突时仍需完整包装，避免业务参数被误识别为存储元数据。
            return JSON.toJSONString(envelope, JSONWriter.Feature.WriteMapNullValue);
        }
        return JSON.toJSONString(envelope);
    }

    /**
     * 将存储 JSON 解码为请求参数和响应读取规则。
     *
     * <p>普通参数按历史格式读取；识别到包装字段组合时校验版本和必要字段，允许额外的扩展字段。
     * 仅包含同名版本字段的历史业务参数仍按普通参数处理。
     * 不支持的版本或损坏的包装结构直接报错，避免将存储元数据作为请求参数发送。</p>
     *
     * <p>解码成功后覆盖目标对象的请求参数与读取规则；空 JSON 清空这两个字段。
     * 解析失败时保留目标对象原值，其他 API 配置字段始终不变。</p>
     *
     * @param json 数据库中的参数配置 JSON，允许为 {@code null}、空白或 JSON {@code null}
     * @param config 接收解析结果的 API 配置，不可为 {@code null}
     * @throws ServiceException 目标配置为空、JSON 格式错误、存储版本不支持或包装字段类型错误时抛出
     */
    public static void decode(String json, IngestTaskApiConfigVO config) {
        requireConfig(config);
        Map<String, Object> requestParams;
        IngestApiReadConfigVO readConfig = null;
        try {
            JSONObject stored = ObjectUtil.isNull(json) || json.trim().isEmpty() ? null : JSON.parseObject(json);
            requestParams = stored;
            if (isEnvelope(stored)) {
                if (!Integer.valueOf(CURRENT_VERSION).equals(stored.get(VERSION_KEY))) {
                    throw new ServiceException("API配置存储版本不支持");
                }
                if (!(stored.get(REQUEST_PARAMS_KEY) instanceof Map)
                        || !stored.containsKey(READ_CONFIG_KEY)
                        || (ObjectUtil.isNotNull(stored.get(READ_CONFIG_KEY))
                            && !(stored.get(READ_CONFIG_KEY) instanceof Map))) {
                    throw new ServiceException("API配置存储结构错误，请求参数和读取规则必须为JSON对象，读取规则允许为空");
                }
                requestParams = stored.getJSONObject(REQUEST_PARAMS_KEY);
                readConfig = stored.getObject(READ_CONFIG_KEY, IngestApiReadConfigVO.class);
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // JSON 字段转换可能抛出多种运行时异常；统一提示并保留原因，不暴露请求参数。
            ServiceException failure = new ServiceException("API配置JSON格式或字段类型错误");
            failure.initCause(ex);
            throw failure;
        }
        config.setParamConfig(requestParams);
        config.setReadConfig(readConfig);
    }

    /**
     * 根据版本标识与包装字段组合识别存储结构，兼容单独使用同名版本字段的历史参数。
     *
     * @param params 待识别的 JSON 对象，允许为 {@code null}
     * @return 同时存在版本标识与任一包装字段时返回 {@code true}
     */
    private static boolean isEnvelope(Map<String, Object> params) {
        return CollectionUtils.isNotEmpty(params) && params.containsKey(VERSION_KEY)
                && (params.containsKey(REQUEST_PARAMS_KEY) || params.containsKey(READ_CONFIG_KEY));
    }

    /**
     * 校验编解码入口，避免空配置导致无上下文的空指针异常。
     *
     * @param config 待校验的 API 配置
     * @throws ServiceException API 配置为空时抛出
     */
    private static void requireConfig(IngestTaskApiConfigVO config) {
        if (ObjectUtil.isNull(config)) {
            throw new ServiceException("API配置不能为空");
        }
    }
}
