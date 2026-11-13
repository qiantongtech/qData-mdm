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
import lombok.Data;

/**
 * 两条记录之间选出的最佳匹配。
 *
 * @author chentangwen
 */
@Data
public class MergeMatchEdgeBO {

    /** 左记录在检测快照列表中的索引。 */
    private int left;

    /** 右记录在检测快照列表中的索引。 */
    private int right;

    /** 最佳匹配规则编号。 */
    private Long ruleId;

    /** 0 至 100 的综合评分，以两位小数保存。 */
    private BigDecimal score = BigDecimal.ZERO;

    /** 命中字段列表 JSON。 */
    private String matchFields;

    /** 命中规则与评分说明。 */
    private String reason;

    /** 最佳规则配置快照。 */
    private String snapshot;
}
