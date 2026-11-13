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

/**
 * 来源数据页面使用的模型业务字段定义。
 */
@Data
public class SourceDataModelColumnRespVO {

    private Long id;
    private Long modelId;
    /**
     * 页面显示名称，例如“物料编码”。
     */
    private String columnName;
    /**
     * MODEL_DATA 中对应的 JSON 键，例如 materialCode。
     */
    private String columnCode;
    private String dataType;
    private Integer dataLength;
    private Integer dataScale;
    private Boolean primaryKeyFlag;
    private Boolean requiredFlag;
    private Boolean uniqueFlag;
    private Boolean nameFlag;
}
