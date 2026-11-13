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

package tech.qiantong.qdata.mdm.model.base.dto;

import lombok.Data;

/**
 * 三类规则共有属性及匹配规则专用属性，均从成功发布配置读取。
 */
@Data
public class ModelPublishedRuleRespDTO {
    /**
     * 规则ID。
     */
    private Long id;
    /**
     * 规则所属模型ID。
     */
    private Long modelId;
    /**
     * 规则编码。
     */
    private String ruleCode;
    /**
     * 规则名称。
     */
    private String ruleName;
    /**
     * 规则记录中的模型版本号。
     */
    private String modelVersion;
    /**
     * 关联的模型字段标识，沿用规则模块的字符串格式。
     */
    private String modelFieldId;
    /**
     * 具体匹配、质量类型或生存策略，不是MATCH等类别。
     */
    private String ruleType;
    /**
     * 规则模块保存的完整配置文本，不在查询时重新解释或改写。
     */
    private String ruleConfig;
    /**
     * 匹配规则优先级，其他类别没有时为空。
     */
    private Long priority;
    /**
     * 匹配分值，其他类别没有时为空。
     */
    private Long ruleScore;
    /**
     * 配置异常标志，保留发布时的值，不自行增加异常规则过滤逻辑。
     */
    private String abnormalFlag;
    /**
     * 规则说明。
     */
    private String description;
    /**
     * 发布时的启用标记。
     */
    private Boolean validFlag;
    /**
     * 发布时的逻辑删除标记。
     */
    private Boolean delFlag;
}
