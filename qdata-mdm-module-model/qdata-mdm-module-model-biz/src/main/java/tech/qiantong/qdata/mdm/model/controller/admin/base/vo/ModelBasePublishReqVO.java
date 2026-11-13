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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 主数据模型发布请求。
 *
 * 数据连接信息用于发布时创建或更新模型物理实例。
 */
@Data
@Schema(description = "主数据模型发布请求")
public class ModelBasePublishReqVO {

    @NotNull(message = "数据连接不能为空")
    @Schema(description = "目标数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long datasourceId;

    @Schema(description = "目标数据源类型")
    private String datasourceType;

    @Schema(description = "目标数据源名称")
    private String datasourceName;
}
