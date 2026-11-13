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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest;

import lombok.Data;

/**
 * 来源数据只读查询所需的跨模块字段。
 * 不是 MODEL_* 或 INGEST_SOURCE_SYSTEM 的持久化实体。
 */
@Data
public class SourceDataReferenceRow {
    private Long id;
    private String code;
    private String name;
    private String icon;
    private String description;
    private Long projectId;
    private Long layerId;
    private Long parentId;
}
