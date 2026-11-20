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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 来源数据分页查询请求对象，封装模型、来源记录及业务字段等筛选条件。
 *
 * @author chentangwen
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SourceDataPageReqVO extends PageParam {

    /**
     * 来源系统 ID，指定时仅查询该来源系统的数据。
     */
    private Long sourceSystemId;

    /**
     * 待查询的模型 ID，用于定位已发布的来源表。
     */
    private Long modelId;

    /**
     * 来源记录唯一标识查询条件，支持模糊匹配；非空时优先于 keyword。
     */
    private String sourceRecordKey;

    /**
     * 业务字段查询关键词，模糊匹配已启用的非系统业务列。
     */
    private String businessFieldValue;

    /**
     * 来源记录唯一标识查询关键词。
     */
    private String keyword;

    /**
     * 来源记录治理状态，按来源表 status 字段精确匹配。
     */
    private String governanceStatus;

    /**
     * 是否包含历史采集版本；为 true 时返回所有未删除版本，否则仅返回各来源记录的最新版本。
     */
    private Boolean includeHistory;

    /**
     * 排序字段：id、sourceRecordKey 或 lastReceiveTime；未指定时按采集时间排序。
     */
    private String orderByColumn;

    /**
     * 排序方向：ascending 或 asc 表示升序，其他值按降序处理。
     */
    private String isAsc;
}
