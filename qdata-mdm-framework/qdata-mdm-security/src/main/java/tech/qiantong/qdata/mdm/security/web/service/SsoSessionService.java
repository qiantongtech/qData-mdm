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

package tech.qiantong.qdata.mdm.security.web.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tech.qiantong.qdata.mdm.common.core.domain.model.LoginUser;
import tech.qiantong.qdata.mdm.common.core.redis.RedisCache;

import java.util.concurrent.TimeUnit;

/**
 * 单点登录会话校验与统一退出服务。
 *
 * <p>后端校验决定本地会话是否有效，前端通知仅用于及时更新页面。
 * 普通登录不依赖认证中心；SSO 校验未明确成功时，一律按会话无效处理。</p>
 *
 * @author qdata
 */
@Service
public class SsoSessionService {
    private static final Logger log = LoggerFactory.getLogger(SsoSessionService.class);

    private static final int REQUEST_TIMEOUT_MILLIS = 3000;
    public static final String SESSION_EXPIRED_MESSAGE = "您的账号已经退出登录，请重新登录。";
    private static final String USER_INFO_PATH = "/oauth2/userinfo";
    private static final String LOGOUT_PATH = "/oauth2/logout";
    private static final String CREDENTIAL_KEY_SUFFIX = ":sso-session:";

    /**
     * 校验结果。退出操作返回 VALID 表示中心已明确确认注销成功。
     */
    public enum State {
        /**
         * 会话有效，或统一退出已成功。
         */
        VALID,
        /**
         * 凭据失效或中心校验失败，不允许继续使用本地 SSO 会话。
         */
        INVALID
    }

    @Autowired
    private RedisCache redisCache;

    @Value("${oauth2.serverUrl}")
    private String serverUrl;

    @Value("${oauth2.redis-prefix}")
    private String redisPrefix;

    private final RestTemplate restTemplate;

    public SsoSessionService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT_MILLIS);
        restTemplate = new RestTemplate(requestFactory);
    }

    public boolean isSso(LoginUser loginUser) {
        return loginUser instanceof SsoLoginUser;
    }

    public String getIdHubId(LoginUser loginUser) {
        return isSso(loginUser) ? ((SsoLoginUser) loginUser).getIdHubId() : null;
    }

    /**
     * 将中心凭据关联到本地会话，不使用用户 ID 作为会话缓存键。
     *
     * <p>凭据过期后仍保留 SsoLoginUser 身份标记，避免失效会话被误当成普通登录。</p>
     *
     * @param loginUser   已创建的本地 SSO 会话
     * @param accessToken 中心访问凭据，仅保存在后端
     * @param expiresIn   中心凭据剩余有效秒数
     */
    public void register(LoginUser loginUser, String accessToken, long expiresIn) {
        if (!isSso(loginUser) || getIdHubId(loginUser) == null || getIdHubId(loginUser).isEmpty()
                || accessToken == null || accessToken.isEmpty() || expiresIn <= 0) {
            throw new IllegalArgumentException("认证中心未返回有效的登录凭据");
        }
        redisCache.setCacheObject(buildCredentialKey(loginUser.getToken()), accessToken,
                (int) Math.min(expiresIn, Integer.MAX_VALUE), TimeUnit.SECONDS);
    }

    /**
     * 仅清理指定本地会话的凭据，避免影响同账号的其他会话。
     *
     * @param sessionId 本地会话 UUID
     */
    public void remove(String sessionId) {
        redisCache.deleteObject(buildCredentialKey(sessionId));
    }

    /**
     * 校验当前会话的中心凭据及统一账号 ID，不缓存校验成功结果。
     *
     * @param loginUser 当前登录会话
     * @return 普通登录直接返回 VALID；SSO 会话返回中心校验结果
     */
    public State check(LoginUser loginUser) {
        if (!isSso(loginUser)) {
            return State.VALID;
        }
        String accessToken = redisCache.getCacheObject(buildCredentialKey(loginUser.getToken()));
        if (accessToken == null || accessToken.isEmpty()) {
            return State.INVALID;
        }
        try {
            JSONObject result = requestCenter(USER_INFO_PATH, accessToken);
            if (result.getIntValue("code") == HttpStatus.OK.value()) {
                JSONObject userInfo = result.getJSONObject("data");
                if (userInfo == null || userInfo.getString("id") == null) {
                    return State.INVALID;
                }
                return userInfo.getString("id").equals(getIdHubId(loginUser)) ? State.VALID : State.INVALID;
            }
            return State.INVALID;
        } catch (RuntimeException exception) {
            // 按统一失效策略处理网络、协议和解析异常；日志仅记录类型，避免泄露凭据。
            log.warn("认证中心会话校验失败，异常类型：{}", exception.getClass().getSimpleName());
            return State.INVALID;
        }
    }

    /**
     * 使用当前会话凭据请求中心统一退出。
     *
     * <p>凭据失效时不尝试刷新，避免恢复已退出的会话；只有中心明确成功才返回 VALID；其余情况返回 INVALID 并清理本地登录。</p>
     *
     * @param loginUser 当前登录会话
     * @return 中心统一退出结果
     */
    public State logout(LoginUser loginUser) {
        if (!isSso(loginUser)) {
            return State.INVALID;
        }
        String accessToken = redisCache.getCacheObject(buildCredentialKey(loginUser.getToken()));
        if (accessToken == null || accessToken.isEmpty()) {
            return State.INVALID;
        }
        try {
            JSONObject result = requestCenter(LOGOUT_PATH, accessToken);
            if (result.getIntValue("code") == HttpStatus.OK.value()) {
                return State.VALID;
            }
            return State.INVALID;
        } catch (RuntimeException exception) {
            log.warn("认证中心统一退出失败，异常类型：{}", exception.getClass().getSimpleName());
            return State.INVALID;
        }
    }

    private String buildCredentialKey(String sessionId) {
        return redisPrefix + CREDENTIAL_KEY_SUFFIX + sessionId;
    }

    /**
     * 请求地址仅由服务端配置和固定路径组成，中心凭据不下发到浏览器。
     */
    private JSONObject requestCenter(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("access_token", accessToken);
        String responseBody = restTemplate.postForObject(serverUrl.replaceAll("/+$", "") + path,
                new HttpEntity<>(form, headers), String.class);
        JSONObject result = JSON.parseObject(responseBody);
        if (result == null || !result.containsKey("code")) {
            throw new IllegalStateException("认证中心响应异常");
        }
        return result;
    }
}
