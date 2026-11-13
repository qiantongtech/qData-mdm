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

package tech.qiantong.qdata.mdm.auth.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.auth.api.authorization.dto.scope.DataScopeConfigDTO;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationOperation;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** 授权配置的统一 JSON 编解码器。 */
@Component
public class AuthorizationJsonCodec {
    @Resource
    private ObjectMapper objectMapper;

    public String writeDataScope(DataScopeConfigDTO config) {
        return config == null ? null : write(config, "数据范围配置序列化失败");
    }

    public DataScopeConfigDTO readDataScope(String json) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DataScopeConfigDTO.class);
        } catch (JsonProcessingException exception) {
            throw new ServiceException("数据范围配置格式错误");
        }
    }

    public String writeOperations(List<AuthorizationOperation> operations) {
        return write(operations == null ? Collections.emptyList() : operations, "操作权限序列化失败");
    }

    public List<AuthorizationOperation> readOperations(String json) {
        if (isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            // 兼容早期版本使用逗号分隔保存的操作权限。
            if (!json.trim().startsWith("[")) {
                return Arrays.stream(json.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(value -> AuthorizationOperation.valueOf(value.toUpperCase(Locale.ROOT)))
                        .distinct()
                        .collect(Collectors.toList());
            }
            return objectMapper.readValue(json, new TypeReference<List<AuthorizationOperation>>() { });
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ServiceException("操作权限配置格式错误");
        }
    }

    private String write(Object value, String message) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ServiceException(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
