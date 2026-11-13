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

package tech.qiantong.qdata.mdm.rule.api.ruleMatch.dto;

import lombok.*;

/**
 * 匹配规则 DTO 对象 RULE_MATCH
 *
 * @author qdata
 * @date 2026-09-15
 */
@Data
public class RuleMatchReqDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 主数据模型ID */
    private Long modelId;

    /** 主数据模型版本号 */
    private String modelVersion;

    /** 优先级 */
    private Long priority;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 配置异常标志 */
    private String abnormalFlag;

    /** 模型字段ID */
    private String modelFieldId;

    /** 规则类型 */
    private String ruleType;

    /** 匹配参数 */
    private String ruleConfig;

    /** 匹配分值 */
    private Long ruleScore;

    /** 描述 */
    private String description;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
