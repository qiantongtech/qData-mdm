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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 主数据模型基本信息 创建/修改 Request VO MODEL_BASE
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "主数据模型基本信息 Response VO")
@Data
public class ModelBaseSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "模型编码", example = "")
    @NotBlank(message = "模型编码不能为空")
    @Size(max = 64, message = "模型编码长度不能超过64个字符")
    @Pattern(regexp = "[A-Za-z0-9_]*", message = "模型编码支持英文字母、数字和下划线")
    private String code;

    @Schema(description = "模型名称", example = "")
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 64, message = "模型名称长度不能超过64个字符")
    @Pattern(regexp = "[\u4e00-\u9fa5A-Za-z0-9_-]*", message = "模型名称支持中文、英文字符、数字、下划线和短横线")
    private String name;

    /**
     * 模型图标路径，空字符串表示使用对应类型的默认图标。
     */
    private String icon;

    /**
     * 负责人用户ID，名称与联系方式由前端公共用户列表匹配。
     */
    private Long responsibleUserId;

    @Schema(description = "所属主数据域", example = "")
    private Long projectId;

    @Schema(description = "所属模型目录", example = "")
    private Long layerId;

    @Schema(description = "模型类型", example = "")
    @Size(max = 512, message = "模型类型长度不能超过512个字符")
    private String type;

    @Schema(description = "版本id", example = "")
    private Long versionId;

    @Schema(description = "模型业务说明", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "上级模型", example = "")
    private Long parentId;

    @Schema(description = "逻辑删除标志", example = "")
    private Boolean delFlag;

    @Schema(description = "模型字段")
    @Valid
    @ConvertGroup(to = ModelColumnSaveReqVO.Required.class)
    private List<ModelColumnSaveReqVO> fields;

    @Schema(description = "子模型")
    @Valid
    private List<ModelBaseSaveReqVO> subModels;

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}
