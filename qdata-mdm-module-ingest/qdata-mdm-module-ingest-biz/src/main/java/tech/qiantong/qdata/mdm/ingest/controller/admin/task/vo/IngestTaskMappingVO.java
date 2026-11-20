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

import java.util.*;
import lombok.Data;

/**
 * 接入任务来源字段与目标模型字段的映射及转换配置对象。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskMappingVO {

    /**
     * 来源字段名称。
     */
    private String sourceFieldName;

    /**
     * 来源字段数据类型。
     */
    private String sourceFieldType;

    /**
     * 为空表示不写入模型，来源字段仍可用于记录唯一标识。
     */
    private Long targetFieldId;

    /**
     * 字段转换类型：0 不处理，1 类型转换，2 去空格，3 空值处理，4 固定值，5 值映射，6 日期处理。
     */
    private String transformType;

    /**
     * 与转换类型对应的参数配置。
     */
    private Map<String, Object> transformConfig;

    /**
     * 字段映射说明。
     */
    private String remark;
}
