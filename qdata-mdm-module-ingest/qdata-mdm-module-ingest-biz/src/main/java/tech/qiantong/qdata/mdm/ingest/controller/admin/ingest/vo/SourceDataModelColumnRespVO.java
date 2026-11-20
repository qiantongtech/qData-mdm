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

package tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo;

import lombok.Data;

/**
 * 来源数据页面的模型业务字段响应对象，字段定义取自成功物化快照。
 *
 * @author chentangwen
 */
@Data
public class SourceDataModelColumnRespVO {

    /**
     * 模型字段 ID。
     */
    private Long id;

    /**
     * 字段所属模型 ID。
     */
    private Long modelId;

    /**
     * 页面显示名称，例如“物料编码”。
     */
    private String columnName;

    /**
     * 业务字段在已发布来源表中的物理列名，同时作为 modelData 中对应的 JSON 键。
     */
    private String columnCode;

    /**
     * 字段逻辑数据类型。
     */
    private String dataType;

    /**
     * 字段字符长度。
     */
    private Integer dataLength;

    /**
     * 数值字段的小数位数。
     */
    private Integer dataScale;

    /**
     * 是否为主键字段。
     */
    private Boolean primaryKeyFlag;

    /**
     * 是否必填，由物化列的可空标记转换。
     */
    private Boolean requiredFlag;

    /**
     * 是否唯一。
     */
    private Boolean uniqueFlag;

    /**
     * 是否为模型的名称字段，用于记录名称展示。
     */
    private Boolean nameFlag;
}
