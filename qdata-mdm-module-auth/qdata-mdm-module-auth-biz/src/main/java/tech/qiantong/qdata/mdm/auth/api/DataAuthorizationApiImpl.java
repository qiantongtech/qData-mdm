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

package tech.qiantong.qdata.mdm.auth.api;

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.auth.api.dataauthorization.dto.DataAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.api.dataauthorization.service.DataAuthorizationApi;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationStatus;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantDO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantFieldDO;
import tech.qiantong.qdata.mdm.auth.service.DataAuthorizationService;
import tech.qiantong.qdata.mdm.auth.support.AuthorizationJsonCodec;

import javax.annotation.Resource;
import java.util.Date;

/** 本期只按直接授权主体判断；角色、组织关系后续由对应模块接口补充。 */
@Service
public class DataAuthorizationApiImpl implements DataAuthorizationApi {
    @Resource
    private DataAuthorizationService dataAuthorizationService;
    @Resource
    private AuthorizationJsonCodec jsonCodec;

    @Override
    public boolean hasOperationPermission(Long userId, Long modelId, String operationCode) {
        Date now = new Date();
        return dataAuthorizationService.list().stream()
                .filter(item -> item.getSubjectId().equals(userId) && item.getModelId().equals(modelId))
                .filter(item -> isActive(item, now))
                .anyMatch(item -> containsOperation(item.getOperationScope(), operationCode));
    }

    @Override
    public boolean hasFieldPermission(Long userId, Long modelId, Long modelFieldId, boolean editable) {
        Date now = new Date();
        return dataAuthorizationService.list().stream()
                .filter(item -> item.getSubjectId().equals(userId) && item.getModelId().equals(modelId))
                .filter(item -> isActive(item, now))
                .flatMap(item -> dataAuthorizationService.getFields(item.getId()).stream())
                .anyMatch(field -> hasFieldPermission(field, modelFieldId, editable));
    }

    @Override
    public Long save(DataAuthorizationSaveReqDTO request) {
        return dataAuthorizationService.save(request);
    }

    @Override
    public void revoke(Long authorizationId) {
        dataAuthorizationService.revoke(authorizationId, "SYSTEM");
    }

    private boolean isActive(AuthGrantDO item, Date now) {
        return AuthorizationStatus.ENABLED.matches(item.getStatus()) && "1".equals(item.getValidFlag())
                && (item.getEffectiveTime() == null || !now.before(item.getEffectiveTime()))
                && (item.getExpireTime() == null || !now.after(item.getExpireTime()));
    }

    private boolean containsOperation(String operationCodes, String operationCode) {
        if (operationCodes == null || operationCode == null) {
            return false;
        }
        return jsonCodec.readOperations(operationCodes).stream().anyMatch(code -> operationCode.equals(code.name()));
    }

    private boolean hasFieldPermission(AuthGrantFieldDO field, Long modelFieldId, boolean editable) {
        return modelFieldId.equals(field.getModelFieldId()) && "1".equals(field.getViewFlag())
                && (!editable || "1".equals(field.getEditFlag()));
    }
}
