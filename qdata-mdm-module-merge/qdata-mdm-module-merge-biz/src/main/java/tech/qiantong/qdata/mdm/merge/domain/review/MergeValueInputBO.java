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
 * 一个来源版本提供的字段值及生存规则判定依据。
 *
 * @author chentangwen
 */
@Data
public class MergeValueInputBO {

    /** 来源字段值。 */
    private String value;

    /** 来源固定版本编号。 */
    private Long versionId;

    /** 来源系统编码。 */
    private String systemCode;

    /** 来源系统名称。 */
    private String systemName;

    /** 来源可信度。 */
    private int trust;

    /** 接收时间毫秒值，未配置时为 0。 */
    private long receiveTime;
}
