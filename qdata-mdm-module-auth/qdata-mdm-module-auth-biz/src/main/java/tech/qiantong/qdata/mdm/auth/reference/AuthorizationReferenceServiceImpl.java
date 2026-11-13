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

package tech.qiantong.qdata.mdm.auth.reference;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationSubjectType;
import tech.qiantong.qdata.mdm.auth.api.reference.AuthorizationModelResourceApi;
import tech.qiantong.qdata.mdm.auth.api.reference.AuthorizationServiceResourceApi;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationModelFieldDTO;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceDTO;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceFieldDTO;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysDept;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysRole;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.system.service.ISysDeptService;
import tech.qiantong.qdata.mdm.system.service.ISysRoleService;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/** 使用各模块公开接口补充授权展示信息。 */
@Service
public class AuthorizationReferenceServiceImpl implements AuthorizationReferenceService {
    @Resource
    private ISysUserService userService;
    @Resource
    private ISysRoleService roleService;
    @Resource
    private ISysDeptService deptService;
    @Resource
    private ObjectProvider<AuthorizationModelResourceApi> modelResourceApiProvider;
    @Resource
    private ObjectProvider<AuthorizationServiceResourceApi> serviceResourceApiProvider;

    @Override
    public AuthorizationReferenceDTO getSubject(AuthorizationSubjectType subjectType, Long subjectId) {
        if (subjectType == null || subjectId == null) {
            return null;
        }
        switch (subjectType) {
            case USER:
                SysUser user = userService.selectUserById(subjectId);
                return user == null ? null : reference(user.getUserId(), user.getUserName(), user.getNickName(), user.getRemark());
            case ROLE:
                SysRole role = roleService.selectRoleById(subjectId);
                return role == null ? null : reference(role.getRoleId(), role.getRoleKey(), role.getRoleName(), role.getRemark());
            case ORG:
                SysDept dept = deptService.selectDeptById(subjectId);
                return dept == null ? null : reference(dept.getDeptId(), String.valueOf(dept.getDeptId()), dept.getDeptName(), dept.getRemark());
            case CLIENT:
                AuthorizationServiceResourceApi api = serviceResourceApiProvider.getIfAvailable();
                return api == null ? null : api.getClients(Collections.singleton(subjectId)).get(subjectId);
            default:
                return null;
        }
    }

    @Override
    public Map<Long, AuthorizationReferenceDTO> getModels(Collection<Long> modelIds) {
        AuthorizationModelResourceApi api = modelResourceApiProvider.getIfAvailable();
        return api == null ? Collections.emptyMap() : api.getModels(modelIds);
    }

    @Override
    public Map<Long, AuthorizationModelFieldDTO> getModelFields(Collection<Long> fieldIds) {
        AuthorizationModelResourceApi api = modelResourceApiProvider.getIfAvailable();
        return api == null ? Collections.emptyMap() : api.getFields(fieldIds);
    }

    @Override
    public boolean modelResourceApiAvailable() {
        return modelResourceApiProvider.getIfAvailable() != null;
    }

    @Override
    public Map<Long, AuthorizationReferenceDTO> getServices(Collection<Long> serviceIds) {
        AuthorizationServiceResourceApi api = serviceResourceApiProvider.getIfAvailable();
        return api == null ? Collections.emptyMap() : api.getServices(serviceIds);
    }

    @Override
    public Map<Long, AuthorizationReferenceFieldDTO> getServiceFields(Collection<Long> fieldIds) {
        AuthorizationServiceResourceApi api = serviceResourceApiProvider.getIfAvailable();
        return api == null ? Collections.emptyMap() : api.getServiceFields(fieldIds);
    }

    @Override
    public boolean serviceResourceApiAvailable() {
        return serviceResourceApiProvider.getIfAvailable() != null;
    }

    private AuthorizationReferenceDTO reference(Long id, String code, String name, String description) {
        AuthorizationReferenceDTO result = new AuthorizationReferenceDTO();
        result.setId(id);
        result.setCode(code);
        result.setName(name);
        result.setDescription(description);
        return result;
    }
}
