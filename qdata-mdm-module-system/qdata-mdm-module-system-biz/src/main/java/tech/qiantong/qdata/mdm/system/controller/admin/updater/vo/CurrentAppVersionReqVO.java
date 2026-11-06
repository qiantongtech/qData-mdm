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

package tech.qiantong.qdata.mdm.system.controller.admin.updater.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Current app version request.
 *
 * @author qdata
 */
@Data
@Schema(description = "Current App Version Request")
public class CurrentAppVersionReqVO {

    @Schema(description = "Application name")
    private String name;

    @Schema(description = "Application version")
    private String version;

    @Schema(description = "Application description")
    private String description;

    @Schema(description = "Application author")
    private String author;

}
