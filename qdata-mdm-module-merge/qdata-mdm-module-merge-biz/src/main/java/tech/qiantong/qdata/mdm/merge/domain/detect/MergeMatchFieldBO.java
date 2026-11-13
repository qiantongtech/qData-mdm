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

import lombok.Data;

/**
 * 单个匹配字段的编译配置。
 *
 * @author chentangwen
 */
@Data
public class MergeMatchFieldBO {

    /** 模型字段编码，兼容历史字段 ID。 */
    private String key;

    /** EXACT 精确匹配或 FUZZY 模糊匹配。 */
    private String method;

    /** 字段权重，必须为正数。 */
    private int weight;

    /** 模糊字段相似度阈值，范围为 0 至 100。 */
    private int threshold;
}
