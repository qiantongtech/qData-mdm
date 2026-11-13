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

package tech.qiantong.qdata.mdm.system.controller.admin.system;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.constant.Constants;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysMenu;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.core.domain.model.LoginBody;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.security.web.service.SsoLoginUser;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import tech.qiantong.qdata.mdm.security.web.service.SysLoginService;
import tech.qiantong.qdata.mdm.security.web.service.SysPermissionService;
import tech.qiantong.qdata.mdm.system.service.ISysMenuService;

/**
 * 登录验证
 *
 * @author qdata
 */
@RestController
public class SysLoginController {
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SpringUtil springUtil;

    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) throws Exception {
        AjaxResult ajax = AjaxResult.success();
        //需求: 如果是这个密码, 可以登录任何用户的账号
//        if ("gfh78h23789#$gfdy845".equals(loginBody.getPassword())) {
//            SysUser sysUser = userService.selectUserByUserName(loginBody.getUsername());
//            loginBody.setPassword(sysUser.getPassword());
//        }
        // 生成令牌
        Map map = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, MapUtil.getStr(map, "token"));

        // 用户id
        String userId = MapUtil.getStr(map, "userId");

        // 通知 flow 登录成功
        String flowEnable = SpringUtil.getProperty("flow.enable");
        if ("true".equals(flowEnable)) {
            String flowBizUrl = SpringUtil.getProperty("flow.url");
            String url = StrUtil.format("{}login/auto" , flowBizUrl);

            String post = HttpUtil.post(url, JSON.toJSONString(map));

            JSONObject jsonObject = JSON.parseObject(post);
            Boolean ok = jsonObject.getBoolean("ok");
            if (ok) {
                return ajax;
            } else {
                return AjaxResult.error("登录失败");
            }
        }
        return ajax;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajax = AjaxResult.success();
        boolean sso = SecurityUtils.getLoginUser() instanceof SsoLoginUser;
        ajax.put("authType", sso ? "sso" : "client");
        ajax.put("idHubId", sso ? ((SsoLoginUser) SecurityUtils.getLoginUser()).getIdHubId() : null);
        ajax.put("user" , user);
        ajax.put("roles" , roles);
        ajax.put("permissions" , permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRoutersDpp/{id}")
    public AjaxResult getRoutersDpp(@PathVariable("id") Long id) {

        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserIdAndProjectId(userId,id);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
