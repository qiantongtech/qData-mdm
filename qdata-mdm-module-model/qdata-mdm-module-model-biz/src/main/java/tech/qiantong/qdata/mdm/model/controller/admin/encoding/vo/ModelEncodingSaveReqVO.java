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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 编码方案 创建/修改 Request VO MODEL_ENCODING
 *
 * @author qdata
 * @date 2026-09-16
 */
@Schema(description = "编码方案 Response VO")
@Data
public class ModelEncodingSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "方案编码", example = "")
    @NotBlank(message = "方案编码不能为空")
    @Size(max = 512, message = "方案编码长度不能超过512个字符")
    private String code;

    @Schema(description = "方案名称", example = "")
    @Size(max = 512, message = "方案名称长度不能超过512个字符")
    private String name;

    @Schema(description = "当前版本ID", example = "")
    private Long currentVersionId;

    @Schema(description = "描述", example = "")
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;

    @Schema(description = "新增编码方案时一同保存的初始版本规则段")
    @Valid
    private List<@NotNull ModelEncodingItemSaveReqVO> items;

}
