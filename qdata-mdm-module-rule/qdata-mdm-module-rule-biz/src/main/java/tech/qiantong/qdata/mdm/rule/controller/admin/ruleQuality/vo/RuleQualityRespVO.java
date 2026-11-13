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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 质量规则 Response VO 对象 RULE_QUALITY
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "质量规则 Response VO")
@Data
public class RuleQualityRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "规则编码")
    @Schema(description = "规则编码", example = "")
    private String ruleCode;

    @Excel(name = "规则名称")
    @Schema(description = "规则名称", example = "")
    private String ruleName;

    @Excel(name = "主数据模型ID")
    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "主数据模型名称")
    private String modelName;
    @Schema(description = "主数据模型编码")
    private String modelCode;
    @Schema(description = "主数据模型类型")
    private String modelType;
    @Schema(description = "主数据模型图标")
    private String modelIcon;
    @Schema(description = "主数据模型描述")
    private String modelDescription;

    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "负责人姓名")
    private String responsibleUserName;

    @Schema(description = "负责人电话")
    private String responsibleUserPhone;

    @Schema(description = "主数据域名称")
    private String projectName;

    @Schema(description = "模型目录名称")
    private String layerName;

    @Schema(description = "所属模型的质量任务数")
    private Long qualityTaskCount;

    @Schema(description = "规则快照包含当前规则的有效合并记录数")
    private Long mergeSnapshotCount;

    @Excel(name = "主数据模型版本号")
    @Schema(description = "主数据模型版本号", example = "")
    private String modelVersion;

    @Excel(name = "模型字段")
    @Schema(description = "模型字段", example = "")
    private String modelFieldId;

    @Excel(name = "配置异常标志;0正常，1配置异常")
    @Schema(description = "配置异常标志;0正常，1配置异常", example = "")
    private String abnormalFlag;

    @Excel(name = "质量规则类型")
    @Schema(description = "质量规则类型", example = "")
    private String ruleType;

    @Excel(name = "校验规则JSON配置")
    @Schema(description = "校验规则JSON配置", example = "")
    private String ruleConfig;

    @Excel(name = "规则启用状态;0：无效停用，1：有效启用")
    @Schema(description = "规则启用状态;0：无效停用，1：有效启用", example = "")
    private Boolean validFlag;

    @Excel(name = "规则说明")
    @Schema(description = "规则说明", example = "")
    private String description;

    @Excel(name = "删除标志;1：已删除，0：未删除")
    @Schema(description = "删除标志;1：已删除，0：未删除", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人姓名")
    @Schema(description = "创建人姓名", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人姓名")
    @Schema(description = "更新人姓名", example = "")
    private String updateBy;

    @Excel(name = "更新人id")
    @Schema(description = "更新人id", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
