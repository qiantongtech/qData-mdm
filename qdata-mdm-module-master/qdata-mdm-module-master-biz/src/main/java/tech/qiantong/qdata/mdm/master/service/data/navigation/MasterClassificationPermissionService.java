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

package tech.qiantong.qdata.mdm.master.service.data.navigation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
//import tech.qiantong.qdata.mdm.auth.api.dataauthorization.service.DataAuthorizationApi;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.security.web.service.SysPermissionService;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;

/** 导航复用已有模型 VIEW 及字段可见授权，不引入行级权限。 */
@Component
@RequiredArgsConstructor
public class MasterClassificationPermissionService {
//    private final DataAuthorizationApi authorizationApi;
    private final SysPermissionService permissionService;
    private final ISysUserService userService;

    public void requireView(MasterModelRespDTO model, MasterFieldRespDTO... fields) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userService.selectUserById(userId);
        if (user == null) throw new ServiceException("当前用户不存在");
        if (permissionService.getMenuPermission(user).contains("*:*:*")) return;
//        if (!authorizationApi.hasOperationPermission(userId, model.getModelId(), "VIEW"))
//            throw new ServiceException("无分类导航模型查看权限：" + model.getModelName());
//        for (MasterFieldRespDTO field : fields) {
//            if ("SYSTEM".equalsIgnoreCase(field.getCategory())) continue;
//            if (field.getId() == null || !authorizationApi.hasFieldPermission(userId, model.getModelId(), field.getId(), false))
//                throw new ServiceException("无分类导航字段查看权限：" + field.getName());
//        }
    }
}
