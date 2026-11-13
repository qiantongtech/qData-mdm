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

package tech.qiantong.qdata.mdm.merge.support;

import java.util.Map;

import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeCompiledRuleBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchEdgeBO;

/** 精确匹配策略。 */
@Service("EXACT")
public class MergeExactMatchStrategy implements MergeMatchStrategy {
    @Override
    public MergeMatchEdgeBO match(Map<String, String> left, Map<String, String> right,
            MergeCompiledRuleBO rule, MergeMatchStrategyRegistry registry) {
        return MergeMatchScoring.score(left, right, rule,
                (lv, rv, field, current) -> MergeMatchScoring.exact(lv, rv));
    }
}
