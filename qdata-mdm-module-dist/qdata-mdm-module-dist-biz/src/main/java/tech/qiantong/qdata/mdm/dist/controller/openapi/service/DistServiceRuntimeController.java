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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.openapi.service;

import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.dist.service.service.DistServiceRuntimeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
/**
 * 为已启用的数据服务提供动态路由处理方法。
 */
@RestController
public class DistServiceRuntimeController {
    @Resource
    private DistServiceRuntimeService runtimeService;

    public CommonResult<Object> query(HttpServletRequest request) {
        return CommonResult.success(runtimeService.query(request));
    }
}
