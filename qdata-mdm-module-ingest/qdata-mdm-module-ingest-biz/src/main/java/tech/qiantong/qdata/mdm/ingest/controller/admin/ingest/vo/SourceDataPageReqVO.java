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

package tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 来源数据分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SourceDataPageReqVO extends PageParam {

    private Long sourceSystemId;
    private Long modelId;
    private String sourceRecordKey;
    private String businessFieldValue;
    /**
     * 兼容旧版来源记录 ID 查询参数。
     */
    private String keyword;
    private String governanceStatus;
    /**
     * 是否包含同一来源记录的历史采集版本。
     */
    private Boolean includeHistory;
    /**
     * 来源数据列表允许排序的字段：id、sourceRecordKey、lastReceiveTime。
     */
    private String orderByColumn;
    /**
     * 表格排序方向：ascending 或 descending。
     */
    private String isAsc;
}
