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

package tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 容器管理-服务器配置 Request VO 对象 CONTAINER_SERVER
 *
 * @author CHAOS
 * @date 2025-09-02
 */
@Schema(description = "容器管理-服务器配置 Request VO")
@Data
public class ContainerServerPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "id", example = "")
    private Long id;

    @Schema(description = "服务器名称", example = "")
    private String serverName;

    @Schema(description = "服务器地址", example = "")
    private String sshHost;

    @Schema(description = "SSH端口号", example = "")
    private Integer sshPort;

    @Schema(description = "用户名", example = "")
    private String userName;

    @Schema(description = "状态", example = "")
    private String serverStatus;

}
