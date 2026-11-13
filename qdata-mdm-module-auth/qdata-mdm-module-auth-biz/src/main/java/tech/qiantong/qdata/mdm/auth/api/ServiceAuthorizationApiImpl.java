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
import tech.qiantong.qdata.mdm.auth.api.serviceauthorization.dto.ServiceAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.api.serviceauthorization.service.ServiceAuthorizationApi;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationStatus;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantFieldDO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthServiceAccessDO;
import tech.qiantong.qdata.mdm.auth.service.ServiceAuthorizationService;

import javax.annotation.Resource;
import java.util.Date;

/** 本期服务调用授权判断。 */
@Service
public class ServiceAuthorizationApiImpl implements ServiceAuthorizationApi {
    @Resource
    private ServiceAuthorizationService serviceAuthorizationService;

    @Override
    public boolean hasServicePermission(Long subjectId, Long serviceId) {
        Date now = new Date();
        return serviceAuthorizationService.list().stream()
                .filter(item -> item.getSubjectId().equals(subjectId) && item.getServiceId().equals(serviceId))
                .anyMatch(item -> isActive(item, now));
    }

    @Override
    public boolean hasFieldPermission(Long subjectId, Long serviceId, Long modelFieldId, boolean editable) {
        // 保留接口参数兼容调用方；服务授权不再授予字段修改权限。
        if (editable) {
            return false;
        }
        Date now = new Date();
        return serviceAuthorizationService.list().stream()
                .filter(item -> item.getSubjectId().equals(subjectId) && item.getServiceId().equals(serviceId))
                .filter(item -> isActive(item, now))
                .flatMap(item -> serviceAuthorizationService.getFields(item.getId()).stream())
                .anyMatch(field -> hasFieldPermission(field, modelFieldId));
    }

    @Override
    public Long save(ServiceAuthorizationSaveReqDTO request) {
        return serviceAuthorizationService.save(request);
    }

    @Override
    public void revoke(Long authorizationId) {
        serviceAuthorizationService.revoke(authorizationId, "SYSTEM");
    }

    private boolean isActive(AuthServiceAccessDO item, Date now) {
        return AuthorizationStatus.ENABLED.matches(item.getStatus()) && "1".equals(item.getValidFlag())
                && (item.getEffectiveTime() == null || !now.before(item.getEffectiveTime()))
                && (item.getExpireTime() == null || !now.after(item.getExpireTime()));
    }

    private boolean hasFieldPermission(AuthGrantFieldDO field, Long modelFieldId) {
        return modelFieldId.equals(field.getModelFieldId()) && "1".equals(field.getViewFlag());
    }
}
