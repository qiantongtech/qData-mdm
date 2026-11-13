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
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 检测范围；结束日期按当天 23:59:59.999 包含边界处理。
 *
 * @author chentangwen
 */
@Data
public class MergeDetectScopeBO {

    /** 是否限制待治理目标的来源系统与接收时间。 */
    private boolean custom;

    /** 指定来源系统编号，按输入顺序去重。 */
    private List<Long> systemIds = new ArrayList<>();

    /** 接收时间下限，包含边界。 */
    private Date start;

    /** 用户选择的结束日期，用于保存和展示。 */
    private Date end;

    /** 结束日期当天的最后一毫秒，用于实际筛选。 */
    private Date endInclusive;
}
