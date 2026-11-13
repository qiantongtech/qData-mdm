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

package tech.qiantong.qdata.mdm.master.api.instance.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 可用物化实例及同一次成功发布的模型字段配置。
 */
@Data
public class MasterModelRespDTO {
    /** 模型物化实例 ID。 */
    private Long instanceId;
    /** 主数据模型 ID。 */
    private Long modelId;
    /** 成功发布实例使用的模型版本 ID。 */
    private Long modelVersionId;
    /** 模型编码。 */
    private String modelCode;
    /** 模型名称。 */
    private String modelName;
    /** 模型类型。 */
    private String modelType;
    /** 所属主数据域名称。 */
    private String domainName;
    /** 版本号。 */
    private String versionNo;
    /** 同一次成功发布的目标数据源。 */
    private Long datasourceId;
    /** 物理表所属数据库模式。 */
    private String schemaName;
    /** 模型物化后的物理表名称。 */
    private String physicalTableName;
    /** 列表字段键到成功物化真实列名的绑定，不由前端提供。 */
    private Map<String, String> columnMappings;
    /** 同一次成功发布的模型字段配置。 */
    private List<MasterFieldRespDTO> fields;
}
