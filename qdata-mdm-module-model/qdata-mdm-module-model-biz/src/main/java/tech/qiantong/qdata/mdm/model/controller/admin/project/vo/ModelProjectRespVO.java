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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.util.Date;
import java.io.Serializable;

/**
 * 主数据域 Response VO 对象 MODEL_PROJECT
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "主数据域 Response VO")
@Data
public class ModelProjectRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "主数据域编码")
    @Schema(description = "主数据域编码", example = "")
    private String code;

    @Excel(name = "主数据域名称")
    @Schema(description = "主数据域名称", example = "")
    private String name;

    /**
     * 主数据域图标路径，未配置时前端显示默认图标。
     */
    @Schema(description = "主数据域图标路径")
    private String icon;

    /**
     * 负责人用户ID，关联系统用户。
     */
    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Schema(description = "创建人用户ID")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "逻辑删除标志：0未删除，1已删除")
    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private Boolean delFlag;

    @Excel(name = "是否有效;0：无效，1：有效；")
    @Schema(description = "是否有效;0：无效，1：有效；", example = "")
    private Boolean validFlag;

    @Excel(name = "关联主数据模型总数")
    @Schema(description = "关联的主数据模型总数", example = "0")
    private String modelCount;

    @Excel(name = "关联模型目录总数")
    @Schema(description = "关联的模型目录总数", example = "0")
    private String layerCount;

    @Excel(name = "关联规则数")
    @Schema(description = "域下全部模型关联的未删除规则总数，包含匹配、生存和质量规则", example = "0")
    private Long ruleCount;

    /**
     * 域下未删除的匹配规则数。
     */
    @Schema(description = "域下未删除的匹配规则数", example = "0")
    private Long matchRuleCount;

    /**
     * 域下未删除的生存规则数。
     */
    @Schema(description = "域下未删除的生存规则数", example = "0")
    private Long survivorRuleCount;

    /**
     * 域下未删除的质量规则数。
     */
    @Schema(description = "域下未删除的质量规则数", example = "0")
    private Long qualityRuleCount;
}
