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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.model.controller.admin.layer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.util.Date;

import tech.qiantong.qdata.mdm.common.annotation.Excel;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 模型目录层级 创建/修改 Request VO MODEL_LAYER
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "模型目录层级 Response VO")
@Data
public class ModelLayerSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "主数据域ID，MODEL_PROJECT.ID", example = "")
    @NotNull(message = "请选择所属主数据域")
    private Long projectId;

    @Schema(description = "上级模型目录ID；0表示根目录", example = "")
    @NotNull(message = "请选择上级模型目录")
    private Long parentId;

    @Schema(description = "模型目录编码", example = "")
    @NotBlank(message = "模型目录编码不能为空")
    @Size(max = 64, message = "模型目录编码长度不能超过64个字符")
    @Pattern(regexp = "[A-Za-z0-9_]*", message = "模型目录编码支持英文字母、数字和下划线")
    private String code;

    @Schema(description = "模型目录名称", example = "")
    @NotBlank(message = "模型目录名称不能为空")
    @Size(max = 64, message = "模型目录名称长度不能超过64个字符")
    @Pattern(regexp = "[\u4e00-\u9fa5A-Za-z0-9_-]*", message = "模型目录名称支持中文、英文字符、数字、下划线和短横线")
    private String name;

    /**
     * 负责人用户ID，关联系统用户。
     */
    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "同级目录排序号，按数值从小到大排列，默认0", example = "")
    @PositiveOrZero(message = "排序不能小于0")
    private Long sortOrder;

    @Schema(description = "模型目录的业务分类范围说明", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}
