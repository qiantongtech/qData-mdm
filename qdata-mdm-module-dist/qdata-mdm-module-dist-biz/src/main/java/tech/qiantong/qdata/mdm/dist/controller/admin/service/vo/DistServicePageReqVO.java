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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import javax.validation.constraints.Pattern;

/**
 * 数据服务分页筛选参数。
 *
 * @author chentangwen
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据服务分页查询参数")
public class DistServicePageReqVO extends PageParam {
    @Schema(description = "服务名称或API路径关键字")
    private String keyword;
    @Schema(description = "服务名称，模糊查询")
    private String name;
    @Schema(description = "服务编码，模糊查询")
    private String code;
    @Schema(description = "API路径，模糊查询")
    private String servicePath;
    @Schema(description = "主数据模型ID")
    private Long modelId;
    @Schema(description = "API类型，精确筛选：0单条查询、1列表分页查询")
    private String apiType;
    @Schema(description = "服务状态：0停用、1启用；不传或空值查询全部")
    @Pattern(regexp = DistServiceConstants.STATUS_REGEX + "?", message = "服务状态只能为0（停用）或1（启用）")
    private String status;
    @Schema(description = "发布状态：0未发布、1已发布")
    private String publishStatus;
}
