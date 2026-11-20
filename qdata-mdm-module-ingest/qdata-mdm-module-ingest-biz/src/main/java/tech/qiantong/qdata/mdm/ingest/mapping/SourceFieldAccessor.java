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

package tech.qiantong.qdata.mdm.ingest.mapping;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONPath;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据接入的来源字段取值器工厂。
 *
 * <p>数据库和文件按原始字段名取值，API 来源额外支持属性访问和非负数组下标组成的简单路径。
 * 读取时优先匹配完整字段名，路径解析仅作为 API 来源的后备读取方式，不执行字段值转换。</p>
 *
 * @author chentangwen
 */
public final class SourceFieldAccessor {

    /**
     * 来源字段配置允许的最大长度，与现有映射配置限制保持一致。
     */
    private static final int MAX_FIELD_LENGTH = 128;

    /**
     * 简单路径语法：属性名以字母或下划线开头，后续允许字母、数字、下划线和连字符。
     */
    private static final Pattern SIMPLE_PATH_PATTERN =
            Pattern.compile("\\$(\\.[\\p{L}_][\\p{L}\\p{N}_-]*|\\[[0-9]+\\])+");

    /**
     * 已校验路径中的属性片段，用于兼容 JSONPath 对连字符属性名的解析。
     */
    private static final Pattern PROPERTY_SEGMENT_PATTERN = Pattern.compile("\\.([\\p{L}_][\\p{L}\\p{N}_-]*)");

    private SourceFieldAccessor() {
    }

    /**
     * 根据字段配置创建可复用的原值读取函数。
     *
     * <p>API 路径支持 {@code customer.name}、{@code $.customer.name} 和
     * {@code items[0].code} 等形式。不符合简单路径语法的配置仍作为完整字段名使用，
     * 不启用通配符、过滤表达式等 JSONPath 功能。字段名不自动去除空格或转换大小写。</p>
     *
     * @param field 来源字段名或 API 简单路径，不能为空白，长度不超过 128 个字符
     * @param api 是否允许在完整字段名未匹配时按简单路径读取
     * @return 接收非空来源记录并返回字段原值的函数；完整字段存在时允许返回 null
     * @throws ServiceException 字段配置为空、超长或路径编译失败时抛出；
     *                          返回的函数在来源记录为空或字段不存在时抛出
     */
    public static Function<Map<String, Object>, Object> compile(String field, boolean api) {
        if (field == null || field.trim().isEmpty() || field.length() > MAX_FIELD_LENGTH) {
            throw new ServiceException("来源字段不能为空且不能超过" + MAX_FIELD_LENGTH + "个字符");
        }
        // 路径只在创建读取器时编译，逐行处理复用结果；非 API 来源不解析路径。
        JSONPath compiled = api ? compilePath(field) : null;
        return row -> {
            if (row == null) {
                throw new ServiceException("来源记录不能为空");
            }
            // 必须先判断键是否存在，保证带点的真实字段名及其 null 值优先于嵌套路径。
            if (row.containsKey(field)) {
                return row.get(field);
            }
            // 完整字段未命中时才尝试简单路径，字段缺失由调用方与字段空值分别处理。
            if (compiled != null && compiled.contains(row)) {
                return compiled.eval(row);
            }
            throw new ServiceException("配置的来源字段不存在");
        };
    }

    /**
     * 补齐根路径前缀并编译简单路径；不符合语法时返回 null，保留完整字段名读取能力。
     */
    private static JSONPath compilePath(String field) {
        String path;
        if (field.startsWith("$")) {
            path = field;
        } else if (field.startsWith("[")) {
            path = "$" + field;
        } else {
            path = "$." + field;
        }
        // 限定现有支持范围，避免将含特殊字符的真实字段名解释为复杂 JSONPath 表达式。
        if (!SIMPLE_PATH_PATTERN.matcher(path).matches()) {
            return null;
        }
        // 将连字符属性改为带引号的属性访问，避免被 JSONPath 当成运算符解析。
        // 仅转换已通过简单语法校验的片段，不开放用户输入的复杂路径表达式。
        Matcher matcher = PROPERTY_SEGMENT_PATTERN.matcher(path);
        StringBuffer compatiblePath = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String segment = name.indexOf('-') >= 0 ? "['" + name + "']" : matcher.group();
            matcher.appendReplacement(compatiblePath, Matcher.quoteReplacement(segment));
        }
        matcher.appendTail(compatiblePath);
        try {
            return JSONPath.of(compatiblePath.toString());
        } catch (JSONException ex) {
            ServiceException failure = new ServiceException("来源字段路径配置不正确");
            failure.initCause(ex);
            throw failure;
        }
    }
}
