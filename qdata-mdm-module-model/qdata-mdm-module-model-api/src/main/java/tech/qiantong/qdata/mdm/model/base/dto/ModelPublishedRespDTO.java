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

import java.util.Date;
import java.util.Map;

/**
 * 已发布模型的基础信息；不包含字段、子模型、物化SQL或连接配置。
 */
@Data
public class ModelPublishedRespDTO {
    /**
     * 主模型ID。
     */
    private Long id;
    /**
     * 成功发布时的模型编码。
     */
    private String code;
    /**
     * 成功发布时的模型名称。
     */
    private String name;
    /**
     * 成功发布时所属主数据域ID。
     */
    private Long projectId;
    /**
     * 成功发布时关联的主数据域的当前展示名称，不随模型草稿归属变化。
     */
    private String domainName;
    /**
     * 成功发布时所属模型目录ID。
     */
    private Long layerId;
    /**
     * 成功发布时的模型类型。
     */
    private String type;
    /**
     * 成功发布时的模型说明。
     */
    private String description;
    /**
     * 本次返回内容对应的成功发布实例ID。
     */
    private Long instanceId;
    /**
     * 本次返回内容对应的成功版本ID。
     */
    private Long versionId;
    /**
     * 成功发布时的版本号。
     */
    private String versionNo;
    /**
     * 本次成功发布时间。
     */
    private Date publishedTime;

    /** 卡片分页统计使用的数据源ID。 */
    private Long authorityDatasourceId;

    /** 按数据源ID从当前数据连接配置解析的权威主表Schema，可为空。 */
    private String authoritySchemaName;

    /** 分页查询关联的权威主表名。 */
    private String authorityTableName;

    /** 卡片统计系统字段到发布时实际列名的映射，保留数据库大小写。 */
    private Map<String, String> authorityStatisticsColumns;
}
