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

package tech.qiantong.qdata.mdm.system.controller.admin.auth;

import com.ejlchina.okhttps.OkHttps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.core.domain.model.LoginUser;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.common.utils.SoMap;
import tech.qiantong.qdata.mdm.security.web.service.SsoLoginUser;
import tech.qiantong.qdata.mdm.security.web.service.SsoSessionService;
import tech.qiantong.qdata.mdm.security.web.service.SysPermissionService;
import tech.qiantong.qdata.mdm.security.web.service.TokenService;
import tech.qiantong.qdata.mdm.system.dal.dataobject.auth.RelUserAuthProductDO;
import tech.qiantong.qdata.mdm.system.enums.auth.AuthProductEnums;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import tech.qiantong.qdata.mdm.system.service.auth.IRelUserAuthProductService;

import java.util.Optional;

/**
 * oauth2 Controller
 *
 * @author surge
 * @date 2022-09-16
 */
@RestController
@RequestMapping("/oauth2")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private ISysUserService userService;
    @Autowired
    private SysPermissionService permissionService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private SsoSessionService ssoSessionService;
    @Autowired
    private IRelUserAuthProductService userAuthProductService;

    // 相关参数配置
    @Value("${oauth2.clientId}")
    private String clientId;            // 应用id
    @Value("${oauth2.clientSecret}")
    private String clientSecret;        // 应用秘钥
    @Value("${oauth2.serverUrl}")
    private String serverUrl;            // 服务端接口

    // 根据Code码进行登录，获取 Access-Token 和 openid
    @RequestMapping("/codeLogin")
    @Transactional
    public AjaxResult codeLogin(String code) {
        // 调用Server端接口，获取 Access-Token 以及其他信息
        String str = OkHttps.sync(serverUrl + "/oauth2/token")
                .addBodyPara("grant_type", "authorization_code")
                .addBodyPara("code", code)
                .addBodyPara("client_id", clientId)
                .addBodyPara("client_secret", clientSecret)
                .post()
                .getBody()
                .toString();
        SoMap so = SoMap.getSoMap().setJsonString(str);

        // code不等于200  代表请求失败
        if (so.getInt("code") != 200) {
            return AjaxResult.error(so.getString("msg"));
        }

        // 根据openid获取其对应的userId
        SoMap data = so.getMap("data");

        // idHubId
        Long idHubId = data.getLong("idHubId");
        // Access-Token值
        String accessToken = data.getString("access_token");
        // Access-Token剩余有效期，单位秒
        long expiresIn = data.getLong("expires_in");

        SysUser user = this.getUserByIdHubId(idHubId);

        if (user == null) {
            // 通过openid获取userInfo
            SoMap userinfo = this.getUserinfo(accessToken);
            if (userinfo != null) {
                // 统一身份认证账户手机号
                String phone = userinfo.getString("phone");
                // 通过手机号查找用户
                SysUser userByPhone = userService.findUserByNameOrPhone(phone);

                if (userByPhone != null) {
                    RelUserAuthProductDO productDO = RelUserAuthProductDO.builder()
                            .userId(userByPhone.getUserId())
                            .authId(idHubId.toString())
                            .authProductType(AuthProductEnums.ANIVIA.code)
                            .build();

                    userAuthProductService.save(productDO);
                    user = userByPhone;
                } else {
                    return AjaxResult.error("系统中不存在此用户，请联系管理员！");
                }
            } else {
                return AjaxResult.error("获取统一身份认证平台用户身份信息失败！");
            }
        }

        SsoLoginUser loginUser = new SsoLoginUser(user,
                permissionService.getMenuPermission(user), String.valueOf(idHubId));
        String token = tokenService.createToken(loginUser);
        try {
            ssoSessionService.register(loginUser, accessToken, expiresIn);
        } catch (RuntimeException exception) {
            tokenService.delLoginUser(loginUser.getToken());
            throw exception;
        }
        log.info("用户：{}，通过统一身份认证平台登录成功！", user.getUserName());
        AjaxResult result = AjaxResult.success(token);
        result.put("authType", "sso");
        result.put("idHubId", loginUser.getIdHubId());
        return result;
    }

    public LoginUser createLoginUser(SysUser user) {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
    }

    /**
     * 根据 ddHubId获取 user
     *
     * @param idHubId 统一身份认证 id
     * @return
     */
    private SysUser getUserByIdHubId(Long idHubId) {
        // 认证平台关联关系
        // 用 authId + 平台类型定位一条绑定关系
        Optional<RelUserAuthProductDO> opt = userAuthProductService.lambdaQuery()
                .eq(RelUserAuthProductDO::getAuthId, String.valueOf(idHubId)) // 统一身份认证ID是字符串列
                .eq(RelUserAuthProductDO::getAuthProductType, AuthProductEnums.ANIVIA.getCode())
                .oneOpt(); // 返回 Optional，避免多条抛异常

        if (opt.isPresent()) {
            return userService.selectUserById(opt.get().getUserId());
        }
        return null;
    }

    /**
     * 根据 Access-Token 置换相关的资源: 获取账号昵称、头像、性别等信息
     *
     * @param accessToken
     * @return
     */
    @RequestMapping("/getUserinfo")
    public SoMap getUserinfo(String accessToken) {
        // 调用Server端接口，查询开放的资源
        String str = OkHttps.sync(serverUrl + "/oauth2/userinfo")
                .addBodyPara("access_token", accessToken)
                .post()
                .getBody()
                .toString();
        SoMap so = SoMap.getSoMap().setJsonString(str);

        // code不等于200  代表请求失败
        if (so.getInt("code") != 200) {
            throw new ServiceException("获取用户账号信息失败！");
        }

        // 返回相关参数 (data=获取到的资源 )
        return so.getMap("data");
    }

    /**
     * 注销当前 SSO 会话；注销对象由后端登录身份决定，不接受前端指定用户。
     *
     * @return 中心统一退出结果，失败时不返回成功
     */
    @PostMapping("/sso/logout")
    public AjaxResult loginOut() {
        LoginUser user = SecurityUtils.getLoginUser();
        SsoSessionService.State state = ssoSessionService.logout(user);
        if (state == SsoSessionService.State.VALID) {
            tokenService.delLoginUser(user.getToken());
            return AjaxResult.success();
        }
        return sessionResult(user, state);
    }

    /**
     * 主动检查当前 SSO 会话，供通知、恢复前台和轮询共用。
     *
     * @return 当前会话校验结果
     */
    @PostMapping("/sso/session/check")
    public AjaxResult checkSession() {
        LoginUser user = SecurityUtils.getLoginUser();
        return sessionResult(user, ssoSessionService.check(user));
    }

    /**
     * 校验或退出未明确成功时统一清理本地会话，不再返回独立的中心不可用状态。
     */
    private AjaxResult sessionResult(LoginUser user, SsoSessionService.State state) {
        if (state == SsoSessionService.State.INVALID) {
            tokenService.delLoginUser(user.getToken());
            AjaxResult error = AjaxResult.error(401, SsoSessionService.SESSION_EXPIRED_MESSAGE);
            error.put("ssoExpired", true);
            return error;
        }
        return AjaxResult.success();
    }

}
