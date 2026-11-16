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
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/**
 * 数据服务启停参数，审计信息由后端填充。
 *
 * @author chentangwen
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据服务状态修改参数")
public class DistServiceStatusReqVO extends BaseEntity {
    @NotNull(message = "数据服务ID不能为空")
    @Positive(message = "数据服务ID必须为正数")
    @Schema(description = "数据服务ID")
    private Long id;

    @NotBlank(message = "服务状态不能为空")
    @Pattern(regexp = DistServiceConstants.STATUS_REGEX, message = "服务状态只能为0（停用）或1（启用）")
    @Schema(description = "服务状态：0停用、1启用")
    private String status;
}
