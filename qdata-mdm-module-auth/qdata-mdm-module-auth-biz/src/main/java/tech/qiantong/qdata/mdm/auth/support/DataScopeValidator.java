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

import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.auth.api.authorization.dto.scope.DataScopeConfigDTO;
import tech.qiantong.qdata.mdm.auth.api.authorization.dto.scope.ScopeConditionDTO;
import tech.qiantong.qdata.mdm.auth.api.authorization.dto.scope.ScopeGroupDTO;
import tech.qiantong.qdata.mdm.auth.api.enums.DataScopeType;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.HashSet;
import java.util.Set;

/** 数据范围配置的结构校验。字段归属由模型或服务模块接口继续校验。 */
@Component
public class DataScopeValidator {

    public void validate(DataScopeType scopeType, DataScopeConfigDTO config) {
        if (scopeType == null) {
            throw new ServiceException("请选择数据范围类型");
        }
        if (scopeType == DataScopeType.ALL) {
            return;
        }
        if (config == null || config.getGroups() == null || config.getGroups().isEmpty()) {
            throw new ServiceException("按条件授权至少需要一个条件组");
        }
        if (config.getVersion() == null || config.getVersion() != 1) {
            throw new ServiceException("不支持的数据范围配置版本");
        }

        Set<String> conditions = new HashSet<>();
        for (ScopeGroupDTO group : config.getGroups()) {
            if (group == null || !"AND".equalsIgnoreCase(group.getLogic())) {
                throw new ServiceException("条件组内只支持 AND 关系");
            }
            if (group.getConditions() == null || group.getConditions().isEmpty()) {
                throw new ServiceException("条件组至少需要一条条件");
            }
            for (ScopeConditionDTO condition : group.getConditions()) {
                validateCondition(condition);
                String key = condition.getFieldId() + ":" + condition.getOperator() + ":" + condition.getValue().trim();
                if (!conditions.add(key)) {
                    throw new ServiceException("数据范围中存在重复条件");
                }
            }
        }
    }

    private void validateCondition(ScopeConditionDTO condition) {
        if (condition == null || condition.getFieldId() == null) {
            throw new ServiceException("数据范围条件缺少字段标识");
        }
        if (isBlank(condition.getFieldCode())) {
            throw new ServiceException("数据范围条件缺少字段编码");
        }
        if (condition.getOperator() == null) {
            throw new ServiceException("数据范围条件缺少运算符");
        }
        if (isBlank(condition.getValue())) {
            throw new ServiceException("数据范围条件值不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
