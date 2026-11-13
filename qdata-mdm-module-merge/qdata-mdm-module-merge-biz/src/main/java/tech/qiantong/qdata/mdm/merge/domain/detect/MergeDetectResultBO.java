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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 聚类结果落库后的统计信息。
 *
 * @author chentangwen
 */
@Data
public class MergeDetectResultBO {

    /** 多成员候选组数量。 */
    private long groupCount;

    /** 单成员候选组数量。 */
    private long singleCount;

    /** 参与比较的历史来源记录数量。 */
    private long historyCount;

    /** 已纳入有效候选组的待治理来源编号。 */
    private List<Long> sourceIds = new ArrayList<>();
}
