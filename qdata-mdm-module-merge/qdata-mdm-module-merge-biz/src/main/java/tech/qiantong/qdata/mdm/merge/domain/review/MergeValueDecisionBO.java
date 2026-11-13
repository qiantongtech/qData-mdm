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

package tech.qiantong.qdata.mdm.merge.domain.review;

import lombok.Data;

/**
 * 字段生存规则判定结果及其来源依据。
 *
 * @author chentangwen
 */
@Data
public class MergeValueDecisionBO {

    /** 是否存在无法唯一确定的字段冲突。 */
    private boolean conflict;

    /** 推荐值，无来源非空值时为空文本。 */
    private String value = "";

    /** 选定值的来源版本。 */
    private Long versionId;

    /** 决定该值的生存规则，没有命中时为空。 */
    private Long ruleId;
}
