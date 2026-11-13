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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 匹配规则 Response VO 对象 RULE_MATCH
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "匹配规则 Response VO")
@Data
public class RuleMatchRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "负责人姓名")
    private String responsibleUserName;

    /** 负责人手机号 */
    @Schema(description = "负责人手机号")
    private String responsibleUserPhone;

    /** 主数据域名称 */
    @Schema(description = "主数据域名称")
    private String projectName;

    /** 模型目录名称 */
    @Schema(description = "模型目录名称")
    private String layerName;

    /** 所属模型的重复检测任务数 */
    @Schema(description = "所属模型的重复检测任务数")
    private Long duplicateTaskCount;


    @Excel(name = "规则编码")
    @Schema(description = "规则编码", example = "")
    private String ruleCode;

    @Excel(name = "规则名称")
    @Schema(description = "规则名称", example = "")
    private String ruleName;

    @Excel(name = "主数据模型ID")
    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Excel(name = "主数据模型版本号")
    @Schema(description = "主数据模型版本号", example = "")
    private String modelVersion;

    @Excel(name = "优先级")
    @Schema(description = "优先级", example = "")
    private Long priority;

    @Excel(name = "是否有效;0：无效，1：有效")
    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "配置异常标志")
    @Schema(description = "配置异常标志", example = "")
    private String abnormalFlag;

    @Excel(name = "模型字段ID")
    @Schema(description = "模型字段ID", example = "")
    private String modelFieldId;

    @Excel(name = "规则类型")
    @Schema(description = "规则类型", example = "")
    private String ruleType;

    @Excel(name = "匹配参数")
    @Schema(description = "匹配参数", example = "")
    private String ruleConfig;

    @Excel(name = "匹配分值")
    @Schema(description = "匹配分值", example = "")
    private Long ruleScore;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;

    @Excel(name = "删除标志;1：已删除，0：未删除")
    @Schema(description = "删除标志;1：已删除，0：未删除", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
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
