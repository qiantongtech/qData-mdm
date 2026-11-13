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

package tech.qiantong.qdata.mdm.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JSONL 文件解析工具。
 *
 * <p>与 CSV 输入组件保持一致，解析阶段只返回字段名称；字段类型由页面配置，
 * 任务运行时由 Spark JSON Reader 推断。</p>
 */
public final class JsonlUtil {

    private static final char UTF8_BOM = '\uFEFF';
    private static final Pattern COLUMN_PATTERN = Pattern.compile("^(?!\\d+$)[\\u4e00-\\u9fffA-Za-z0-9_]+$");

    private JsonlUtil() {
    }

    /**
     * 读取 JSONL 第一条有效记录并返回字段列表。
     *
     * @param sourceFile 上传文件的本地路径
     * @return 第一条有效 JSON 记录中的字段列表
     */
    public static List<String> parseJsonl(String sourceFile) {
        if (StringUtils.isBlank(sourceFile)) {
            throw new ServiceException("JSONL文件路径不能为空");
        }

        Path sourcePath = Paths.get(sourceFile);
        if (!Files.isRegularFile(sourcePath)) {
            throw new ServiceException("JSONL文件不存在");
        }

        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == UTF8_BOM) {
                    line = line.substring(1);
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                JSONObject record = parseRecord(line, lineNumber);
                validateSimpleValues(record, lineNumber);

                List<String> columnList = new ArrayList<>(record.keySet());
                if (columnList.isEmpty()) {
                    throw new ServiceException("JSONL第" + lineNumber + "行不包含字段");
                }
                verifyColumns(columnList, lineNumber);
                return columnList;
            }
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new ServiceException("JSONL文件读取失败");
        }

        throw new ServiceException("JSONL为空无法解析");
    }

    private static JSONObject parseRecord(String line, int lineNumber) {
        final Object value;
        try {
            value = JSON.parse(line);
        } catch (JSONException e) {
            throw new ServiceException("JSONL第" + lineNumber + "行解析失败");
        }
        if (!(value instanceof JSONObject)) {
            throw new ServiceException("JSONL第" + lineNumber + "行必须为JSON对象");
        }
        return (JSONObject) value;
    }

    private static void validateSimpleValues(JSONObject record, int lineNumber) {
        for (String field : record.keySet()) {
            Object value = record.get(field);
            if (value instanceof JSONObject || value instanceof JSONArray) {
                throw new ServiceException("JSONL第" + lineNumber + "行字段" + field + "暂不支持对象或数组类型");
            }
        }
    }

    private static void verifyColumns(List<String> columns, int lineNumber) {
        for (String column : columns) {
            if (!COLUMN_PATTERN.matcher(column).matches()) {
                throw new ServiceException("JSONL第" + lineNumber + "行字段名" + column + "格式有误");
            }
        }
    }
}
