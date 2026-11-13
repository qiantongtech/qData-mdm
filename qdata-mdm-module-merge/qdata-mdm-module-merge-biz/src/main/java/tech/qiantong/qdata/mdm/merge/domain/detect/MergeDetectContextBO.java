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

package tech.qiantong.qdata.mdm.merge.domain.detect;

import java.util.List;
import lombok.Data;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;

/**
 * 本次检测固定的模型、规则、范围和来源快照。
 *
 * @author chentangwen
 */
@Data
public class MergeDetectContextBO {

    /** 已发布的模型及版本。 */
    private ModelBaseDO model;

    /** 模型字段。 */
    private List<ModelColumnDO> columns;

    /** 可执行匹配规则，顺序决定同分时优先选择的规则。 */
    private List<RuleMatchDO> rules;

    /** 已校验的检测范围。 */
    private MergeDetectScopeBO scope;

    /** 目标和历史比较数据的固定快照。 */
    private List<SourceSnapshot> snapshots;

    /** 无法构成有效快照的记录数量。 */
    private int skippedCount;

    /** 可参与本次检测的待治理目标数量。 */
    private int targetCount;
}
