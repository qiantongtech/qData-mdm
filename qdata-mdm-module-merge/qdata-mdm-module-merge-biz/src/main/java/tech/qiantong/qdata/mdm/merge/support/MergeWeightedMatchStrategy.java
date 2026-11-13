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

/** 多字段加权策略，字段算法由同一注册表按 matchMethod 分派。 */
@Service("WEIGHTED")
public class MergeWeightedMatchStrategy implements MergeMatchStrategy {
    @Override
    public MergeMatchEdgeBO match(Map<String, String> left, Map<String, String> right,
            MergeCompiledRuleBO rule, MergeMatchStrategyRegistry registry) {
        return MergeMatchScoring.score(left, right, rule, (lv, rv, field, current) -> {
            String method = field.getMethod();
            if ("EXACT".equalsIgnoreCase(method)) {
                return MergeMatchScoring.exact(lv, rv);
            }
            if ("STD_EXACT".equalsIgnoreCase(method)) {
                return MergeMatchScoring.standardizedExact(lv, rv, current);
            }
            if ("FUZZY".equalsIgnoreCase(method)) {
                return MergeMatchScoring.fuzzy(lv, rv, field);
            }
            // 由注册表统一校验并抛出明确错误，避免未知算法静默降级。
            registry.get(method);
            return MergeMatchScoring.exact(lv, rv);
        });
    }
}
