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

package tech.qiantong.qdata.mdm.master.dal.dto.data;

import lombok.Data;

import java.util.Date;

/** 同一次聚合查询得到的权威主表统计；查询失败由调用方标记不可用。 */
@Data
public class MasterDataStatisticsDTO {
    private Long dataCount;
    private Long activeCount;
    private Long frozenCount;
    private Long invalidCount;
    private Long archivedCount;
    private Date latestDataUpdateTime;
}
