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

package tech.qiantong.qdata.mdm.system.controller.admin.monitor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.config.web.domain.Server;
import tech.qiantong.qdata.mdm.system.service.monitor.IResourceServerService;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 服务器监控
 *
 * @author qdata
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController
{
    @Resource
    private IResourceServerService  resourceServerService;
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception
    {
        Server server = new Server();
        server.copyTo();
        return AjaxResult.success(server);
    }

//    @PreAuthorize("@ss.hasPermi('monitor:server:list')")

    /**
     * 资源监控后端,目前暂定DS\SPARK\FLINK\MQ
     * @return
     */
    @GetMapping("/resource")
    public AjaxResult getResource()
    {
        Map<String, Object> map = resourceServerService.resourceResult();
        return AjaxResult.success(map);
    }
}
