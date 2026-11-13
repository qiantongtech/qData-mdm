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

package tech.qiantong.qdata.mdm.audit.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 操作日志编码映射。单字符业务编码由项目配置提供，不推测数字含义。
 * @author zyy
 */
@Data
@Component
@ConfigurationProperties(prefix = "qdata.audit.operation")
public class AuditOperationCodeProperties {
    /** 数据库存储值 -> master/model。 */
    private Map<String, String> moduleScopes = new LinkedHashMap<>();
    /** 数据库存储值 -> 操作名称；可用 master:编码/model:编码区分范围。 */
    private Map<String, String> types = new LinkedHashMap<>();
    /** 数据库存储值 -> 操作结果名称。 */
    private Map<String, String> results = new LinkedHashMap<>();

    public String scope(String code) {
        String value = code == null ? "" : code.trim();
        if (moduleScopes.containsKey(value)) return moduleScopes.get(value);
        // 兼容旧接口中的文字编码；新的 CHAR(1) 编码必须明确配置。
        if ("MODEL".equalsIgnoreCase(value)) return "model";
        if ("MASTER".equalsIgnoreCase(value) || "GOVERN".equalsIgnoreCase(value)) return "master";
        return null;
    }

    public List<String> moduleCodes(String scope) {
        List<String> codes = new ArrayList<>();
        moduleScopes.forEach((code, value) -> {
            if (scope.equals(value)) codes.add(code);
        });
        String[] legacy = "model".equals(scope) ? new String[]{"MODEL"} : new String[]{"MASTER", "GOVERN"};
        for (String code : legacy) {
            for (int mask = 0; mask < (1 << code.length()); mask++) {
                char[] letters = code.toCharArray();
                for (int i = 0; i < letters.length; i++) {
                    if ((mask & (1 << i)) != 0) letters[i] = Character.toLowerCase(letters[i]);
                }
                String variant = new String(letters);
                if (scope.equals(scope(variant)) && !codes.contains(variant)) codes.add(variant);
            }
        }
        return codes;
    }

    public String type(String scope, String code) {
        return types.getOrDefault(scope + ":" + code, types.getOrDefault(code, code));
    }

    public List<String> typeCodes(String scope, String label) {
        List<String> codes = new ArrayList<>();
        codes.add(label);
        types.forEach((key, value) -> {
            if (!label.equals(value)) return;
            int separator = key.indexOf(':');
            if (separator < 0) codes.add(key);
            else if (key.substring(0, separator).equals(scope)) codes.add(key.substring(separator + 1));
        });
        return codes;
    }

    public String result(String code) {
        return results.getOrDefault(code, code);
    }
}
