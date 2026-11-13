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

package tech.qiantong.qdata.mdm.master.controller.admin.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * 权威主数据入口分页 Request VO。
 */
@Data
@Schema(description = "权威主数据入口分页 Request VO")
public class MasterInstancePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主数据域 ID")
    @Positive(message = "主数据域ID必须为正数")
    private Long projectId;

    @Schema(description = "模型目录 ID")
    @Positive(message = "模型目录ID必须为正数")
    private Long layerId;

    @Schema(description = "主数据模型 ID")
    @Positive(message = "主数据模型ID必须为正数")
    private Long modelId;

    @Schema(description = "主数据模型名称")
    @Size(max = 100, message = "模型名称不能超过100字")
    private String name;

    @Schema(description = "主数据模型类型")
    private String type;
}
