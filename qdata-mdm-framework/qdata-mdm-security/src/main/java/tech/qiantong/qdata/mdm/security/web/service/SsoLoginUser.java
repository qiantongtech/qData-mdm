/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.security.web.service;

import java.util.Set;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.core.domain.model.LoginUser;

/**
 * 单点登录会话身份。
 *
 * <p>通过现有 Redis 多态序列化保留登录来源，无需修改 common 模块。
 * 普通登录仍使用 LoginUser，不参与认证中心的会话校验。</p>
 *
 * @author qdata
 */
public class SsoLoginUser extends LoginUser
{
    private static final long serialVersionUID = 1L;

    /** 认证中心账号 ID，与本平台用户 ID 分开保存。 */
    private String idHubId;

    /** Redis 反序列化使用的无参构造方法。 */
    public SsoLoginUser()
    {
    }

    public SsoLoginUser(SysUser user, Set<String> permissions, String idHubId)
    {
        super(user.getUserId(), user.getDeptId(), user, permissions);
        this.idHubId = idHubId;
    }

    public String getAuthType()
    {
        return "sso";
    }

    public String getIdHubId()
    {
        return idHubId;
    }

    public void setIdHubId(String idHubId)
    {
        this.idHubId = idHubId;
    }
}
