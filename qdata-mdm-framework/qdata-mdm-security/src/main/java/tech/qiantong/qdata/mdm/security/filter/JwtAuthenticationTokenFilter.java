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

package tech.qiantong.qdata.mdm.security.filter;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.domain.model.LoginUser;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.security.web.service.SsoSessionService;
import tech.qiantong.qdata.mdm.security.web.service.TokenService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * token过滤器 验证token有效性
 *
 * @author qdata
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    /**
     * 登录、退出和主动校验接口自行处理会话，避免重复请求中心或阻断重新登录。
     */
    private static final Set<String> SESSION_LIFECYCLE_PATHS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("/logout", "/login", "/oauth2/codeLogin",
                    "/oauth2/sso/logout", "/oauth2/sso/session/check")));

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SsoSessionService ssoSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication())) {
            if (!SESSION_LIFECYCLE_PATHS.contains(request.getServletPath())) {
                SsoSessionService.State state = ssoSessionService.check(loginUser);
                if (state != SsoSessionService.State.VALID) {
                    writeSsoError(response, loginUser);
                    return;
                }
            }
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }

    /**
     * 校验未明确成功时统一删除本地会话，并交由前端显示登录失效弹窗。
     */
    private void writeSsoError(HttpServletResponse response, LoginUser loginUser) throws IOException {
        tokenService.delLoginUser(loginUser.getToken());
        int status = HttpStatus.UNAUTHORIZED.value();
        AjaxResult error = AjaxResult.error(status, SsoSessionService.SESSION_EXPIRED_MESSAGE);
        error.put("ssoExpired", true);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(error));
    }

}
