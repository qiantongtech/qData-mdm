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

package tech.qiantong.qdata.mdm.rule.api.rule.dto;

import lombok.Data;
import tech.qiantong.qdata.mdm.rule.api.ruleMatch.dto.RuleMatchRespDTO;
import tech.qiantong.qdata.mdm.rule.api.ruleSurvivor.dto.RuleSurvivorRespDTO;
import tech.qiantong.qdata.mdm.rule.api.ruleQuality.dto.RuleQualityRespDTO;
import java.util.ArrayList;
import java.util.List;

/** 模型发布需要保存的三类规则，复用规则模块现有DTO和完整配置文本。 */
@Data
public class RuleModelRulesRespDTO {
    /** 匹配规则，包含发布时的启停、逻辑删除标记。 */
    private List<RuleMatchRespDTO> match = new ArrayList<>();
    /** 生存规则，包含发布时的启停、逻辑删除标记。 */
    private List<RuleSurvivorRespDTO> survivor = new ArrayList<>();
    /** 质量规则，包含发布时的启停、逻辑删除标记。 */
    private List<RuleQualityRespDTO> quality = new ArrayList<>();
}
