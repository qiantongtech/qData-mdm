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

package tech.qiantong.qdata.mdm.common.httpClient;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Json data conversion tool class
 * @author chen
 */
public class JsonUtil {

    public static final String TYPE_long = "long";
    public static final String TYPE_double = "double";
    public static final String TYPE_Boolean = "Boolean";
    public static final String TYPE_String = "String";
    public static final String TYPE_Data = "Data";
    public static final String TYPE_Object = "Object";
    public static final String TYPE_Array = "Array";
    public static final String TYPE_ArrayList = "ArrayList";

    /**
     * Convert Json string to Map
     *
     * @param jsonString
     * @return map
     */
    public static Map<String,Object> parseJsonToMap(String jsonString) {
        Map<String,Object> map = JSON.parseObject(jsonString, Map.class);

        for (Object obj : map.keySet()) {

        }

        return map;
    }

    /**
     * Convert Json string to List<Map<String, Object>>
     *
     * @param jsonString JSON string
     * @return converted List<Map<String, Object>>
     */
    public static List<Object>  parseJsonToListMap(String jsonString) {
        List<Object> list = JSON.parseObject(jsonString, new TypeReference<List<Object>>(){});

        return list;
    }

    /**
     * Convert Map to Json
     *
     * @param map
     * @return
     */
    public static String parseMapToJson(Map<String, Object> map) {
        String json = JSON.toJSONString(map);

        return json;
    }

    /**
     * Convert Object to Json
     *
     * @param map
     * @return
     */
    public static String parseObjectToJson(Object map) {
        String json = JSON.toJSONString(map);

        return json;
    }

}
