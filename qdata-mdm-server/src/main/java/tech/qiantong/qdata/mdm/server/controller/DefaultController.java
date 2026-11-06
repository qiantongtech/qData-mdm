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

package tech.qiantong.qdata.mdm.server.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.utils.MessageUtils;

/**
 * The default Controller solves the 404 prompt when some modules are not enabled.
 * For example, /bpm/** path, workflow
 *
 * @author qdata
 */
@RestController
public class DefaultController {

    @RequestMapping({"/dev-api/example/**", "/prod-api/example/**", "/example/**"})
    public AjaxResult example404() {
        return AjaxResult.error(MessageUtils.messageWithFallback(
                "sys.error.example.module.disabled", "Example module qdata-mdm-module-example is disabled"));
    }

}
