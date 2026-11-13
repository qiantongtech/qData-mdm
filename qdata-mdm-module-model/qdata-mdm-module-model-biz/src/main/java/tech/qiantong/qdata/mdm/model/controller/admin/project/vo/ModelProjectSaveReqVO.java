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

package tech.qiantong.qdata.mdm.model.controller.admin.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 主数据域 创建/修改 Request VO MODEL_PROJECT
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "主数据域 Response VO")
@Data
public class ModelProjectSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "主数据域编码", example = "")
    @NotBlank(message = "主数据域编码不能为空")
    @Size(max = 512, message = "主数据域编码长度不能超过512个字符")
    private String code;

    @Schema(description = "主数据域名称", example = "")
    @Size(max = 512, message = "主数据域名称长度不能超过512个字符")
    private String name;

    /**
     * 主数据域图标路径，未配置时前端显示默认图标。
     */
    @Schema(description = "主数据域图标路径")
    @Size(max = 512, message = "图标路径长度不能超过512个字符")
    private String icon;

    /**
     * 负责人用户ID，关联系统用户。
     */
    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "描述", example = "")
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

}
