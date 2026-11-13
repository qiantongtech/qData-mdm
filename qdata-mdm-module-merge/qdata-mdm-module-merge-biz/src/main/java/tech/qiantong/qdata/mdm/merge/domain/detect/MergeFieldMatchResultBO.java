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
 * 单个字段匹配策略的结果。
 *
 * @author chentangwen
 */
@Data
public class MergeFieldMatchResultBO {

    /** 是否达到该字段的命中阈值。 */
    private boolean matched;

    /** 字段匹配分数，范围为 0 至 100。 */
    private BigDecimal score = BigDecimal.ZERO;

    /** 比较使用的左侧值，便于生成可追溯的匹配说明。 */
    private String leftValue;

    /** 比较使用的右侧值，便于生成可追溯的匹配说明。 */
    private String rightValue;

    /** 策略说明。 */
    private String reason;
}
