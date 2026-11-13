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
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 同一成功模型版本、指定类别的规则信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelPublishedRulesRespDTO extends ModelPublishedRespDTO {
    /**
     * MATCH匹配、SURVIVOR生存、QUALITY质量，与具体策略ruleType区分。
     */
    private String ruleCategory;
    /**
     * 该主模型正常启用的规则，不混入子模型或其他模型的规则。
     */
    private List<ModelPublishedRuleRespDTO> rules = new ArrayList<>();
}
