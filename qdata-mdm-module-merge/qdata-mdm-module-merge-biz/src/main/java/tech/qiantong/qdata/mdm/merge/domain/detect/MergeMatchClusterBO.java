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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 按匹配关系的连通分量生成的候选组。
 *
 * @author chentangwen
 */
@Data
public class MergeMatchClusterBO {

    /** 成员在检测快照列表中的索引。 */
    private List<Integer> indexes = new ArrayList<>();

    /** 成员间命中的最佳匹配边。 */
    private List<MergeMatchEdgeBO> edges = new ArrayList<>();

    /** 组内最高匹配评分；单条组为 0。 */
    private BigDecimal maxScore = BigDecimal.ZERO;
}
