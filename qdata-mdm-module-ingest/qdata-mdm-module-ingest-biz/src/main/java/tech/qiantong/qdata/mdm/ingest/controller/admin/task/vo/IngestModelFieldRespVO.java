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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import lombok.Data;

/**
 * 接入任务字段映射使用的目标模型字段响应对象。
 *
 * @author chentangwen
 */
@Data
public class IngestModelFieldRespVO {

    /**
     * 目标模型字段 ID。
     */
    private Long id;

    /**
     * 模型字段显示名称。
     */
    private String columnName;

    /**
     * 模型字段编码。
     */
    private String columnCode;

    /**
     * 模型字段分类编码，与模型字段定义保持一致。
     */
    private String columnCategory;

    /**
     * 字段数据类型。
     */
    private String dataType;

    /**
     * 字段数据长度。
     */
    private Integer dataLength;

    /**
     * 数值字段的小数位数。
     */
    private Integer dataScale;
}
