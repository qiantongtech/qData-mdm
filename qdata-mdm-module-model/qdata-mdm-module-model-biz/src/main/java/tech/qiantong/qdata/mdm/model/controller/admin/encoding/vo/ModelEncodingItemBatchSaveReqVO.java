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
package tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 编码方案规则段批量保存 Request VO。
 */
@Schema(description = "编码方案规则段批量保存 Request VO")
@Data
public class ModelEncodingItemBatchSaveReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "编码版本ID不能为空")
    @Schema(description = "编码版本ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long encodingVersionId;

    @Valid
    @NotNull(message = "规则段列表不能为空")
    @Schema(description = "按生成顺序排列的规则段", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ModelEncodingItemSaveReqVO> items = new ArrayList<>();
}
