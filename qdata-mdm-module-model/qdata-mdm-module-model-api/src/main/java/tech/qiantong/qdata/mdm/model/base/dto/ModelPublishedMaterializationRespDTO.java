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

package tech.qiantong.qdata.mdm.model.base.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近成功模型的目标数据库和完整物化记录；不包含连接凭证或执行计划。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelPublishedMaterializationRespDTO extends ModelPublishedRespDTO {
    /**
     * 本次成功发布使用的数据源ID。
     */
    private Long datasourceId;
    /**
     * 当前数据连接配置中的数据库类型。
     */
    private String databaseType;
    /**
     * 当前数据库版本；未实际读取时为空，不使用发布时的历史版本。
     */
    private String databaseVersion;
    /**
     * 当前数据连接配置中的数据库名。
     */
    private String databaseName;
    /**
     * 当前数据连接配置解析出的Schema，可为空。
     */
    private String schemaName;
    /**
     * 同一成功实例的来源、权威主子表，停用对象保留并带enabledFlag。
     */
    private List<ModelPublishedMaterializedTableRespDTO> tables = new ArrayList<>();
}
