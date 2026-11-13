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

package tech.qiantong.qdata.mdm.merge.domain.manage;

import lombok.Data;

import java.util.List;

/**
 * 合并管理解析后的查询条件，null 关联集合或状态集合表示不限制。
 *
 * @author chentangwen
 */
@Data
public class MergePendingQueryBO {

    /**
     * 处理类型对应的合并记录编号。
     */
    private List<Long> recordIds;

    /**
     * 组编号对应的候选组编号。
     */
    private List<Long> groupIds;

    /**
     * 兼容旧关键字时匹配的候选组编号。
     */
    private List<Long> keywordGroupIds;

    /**
     * 已展开的结果状态，null 表示查询全部状态。
     */
    private List<String> statuses;

    /**
     * 已规范化的业务名称。
     */
    private String businessName;

    /**
     * 兼容旧接口的关键字；显式指定组编号或业务名称时为空。
     */
    private String legacyKeyword;
}
