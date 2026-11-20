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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest;

import lombok.Data;

/**
 * 来源数据查询使用的关联对象信息，承载来源系统、主数据域、目录或模型的基础属性。
 *
 * @author chentangwen
 */
@Data
public class SourceDataReferenceRow {

    /**
     * 关联对象 ID。
     */
    private Long id;

    /**
     * 关联对象编码。
     */
    private String code;

    /**
     * 关联对象名称。
     */
    private String name;

    /**
     * 关联对象图标路径。
     */
    private String icon;

    /**
     * 关联对象业务说明。
     */
    private String description;

    /**
     * 所属主数据域 ID。
     */
    private Long projectId;

    /**
     * 所属模型目录 ID。
     */
    private Long layerId;

    /**
     * 上级目录或模型 ID。
     */
    private Long parentId;
}
