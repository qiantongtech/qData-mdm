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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.util.List;

/**
 * 来源系统新增及修改请求对象，封装基础配置和数据库连接关联信息。
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Schema(description = "来源系统新增/修改 Request VO")
@Data
public class IngestSourceSaveReqVO extends BaseEntity {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 来源系统 ID，修改时必填；新增时由服务端生成。
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 来源系统编码，支持英文字母、数字和下划线，最长 64 个字符；修改时保留原编码。
     */
    @Schema(description = "来源系统编码")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "来源系统编码支持英文字母、数字和下划线")
    @Size(max = 64, message = "来源系统编码长度不能超过64个字符")
    private String code;

    /**
     * 来源系统名称，不能为空，最长 64 个字符，支持中文、英文字母、数字、下划线和短横线。
     */
    @Schema(description = "来源系统名称")
    @NotBlank(message = "来源系统名称不能为空")
    @Size(max = 64, message = "来源系统名称长度不能超过64个字符")
    @Pattern(regexp = "[\\u4e00-\\u9fa5A-Za-z0-9_-]+", message = "来源系统名称支持中文、英文字符、数字、下划线和短横线")
    private String name;

    /**
     * 来源系统图标路径，最长 512 个字符。
     */
    @Schema(description = "来源系统图标")
    @Size(max = 512, message = "图标路径不能超过512个字符")
    private String icon;

    /**
     * 来源系统状态：0 表示停用，1 表示启用；新增时未指定则默认启用。
     */
    @Schema(description = "状态：0停用，1启用")
    @javax.validation.constraints.Pattern(regexp = "[01]", message = "状态只能为0或1")
    private String validFlag;

    /**
     * 来源系统责任人用户 ID。
     */
    @Schema(description = "责任人用户ID")
    private Long responsibleUserId;

    /**
     * 来源系统业务说明，最长 512 个字符。
     */
    @Schema(description = "描述")
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    /**
     * 来源可信度，新增和修改时必填，取值范围为 0～100。
     */
    @Schema(description = "来源可信度，0～100")
    @NotNull(message = "来源可信度不能为空")
    @Min(value = 0, message = "来源可信度最小为0")
    @Max(value = 100, message = "来源可信度最大为100")
    private Integer trustScore;

    /**
     * 关联数据库连接 ID 列表；修改时不能移除已被接入任务使用的连接。
     */
    @Schema(description = "关联数据库连接ID")
    private List<Long> datasourceIds;
}
