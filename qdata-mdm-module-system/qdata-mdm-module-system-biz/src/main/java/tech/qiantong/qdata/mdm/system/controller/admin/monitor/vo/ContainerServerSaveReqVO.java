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

package tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 容器管理-服务器配置 创建/修改 Request VO CONTAINER_SERVER
 *
 * @author CHAOS
 * @date 2025-09-02
 */
@Schema(description = "容器管理-服务器配置 Response VO")
@Data
public class ContainerServerSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "服务器名称", example = "")
    @Size(max = 256, message = "服务器名称长度不能超过256个字符")
    @NotBlank(message = "serverName blank")
    private String serverName;

    @Schema(description = "服务器地址", example = "")
    @Size(max = 256, message = "服务器地址长度不能超过256个字符")
    @NotBlank(message = "sshHost blank")
    private String sshHost;

    @Schema(description = "SSH端口号", example = "")
    @NotNull(message = "sshPort null")
    private Integer sshPort;

    @Schema(description = "用户名", example = "")
    @Size(max = 256, message = "用户名长度不能超过256个字符")
    @NotBlank(message = "userName blank")
    private String userName;

    @Schema(description = "密码", example = "")
    @Size(max = 2048, message = "密码长度不能超过256个字符")
    @NotBlank(message = "sshPassword blank")
    private String sshPassword;

    @Schema(description = "状态 0：不启用，1：启用", example = "")
    @Size(max = 1, message = "状态长度不能超过1个字符")
    private String serverStatus;

}
