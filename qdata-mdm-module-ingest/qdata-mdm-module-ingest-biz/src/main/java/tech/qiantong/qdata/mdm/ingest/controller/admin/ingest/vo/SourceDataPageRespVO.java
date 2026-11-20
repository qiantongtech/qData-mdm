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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 来源数据分页列表响应对象，业务字段由 modelData 动态承载。
 * <p>
 * 列表业务字段由 {@code modelData} 承载，前端依据 {@code modelCode}
 * 按当前主数据模型动态组织展示，不在接口层固化供应商、客户或物料字段。
 *
 * @author chentangwen
 */
@Data
public class SourceDataPageRespVO {

    /**
     * 当前模型来源表中的记录主键 ID。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 来源系统 ID。
     */
    private Long sourceSystemId;

    /**
     * 来源系统名称。
     */
    private String sourceSystemName;

    /**
     * 来源系统编码。
     */
    private String sourceSystemCode;

    /**
     * 来源系统图标路径。
     */
    private String sourceSystemIcon;

    /**
     * 来源数据所属模型 ID。
     */
    private Long modelId;

    /**
     * 模型编码。
     */
    private String modelCode;

    /**
     * 模型名称。
     */
    private String modelName;

    /**
     * 模型图标路径。
     */
    private String modelIcon;

    /**
     * 模型类型：0 表示普通模型，1 表示分类模型。
     */
    private String modelType;

    /**
     * 模型业务说明。
     */
    private String modelDescription;

    /**
     * 模型最近一次成功发布的版本号。
     */
    private String modelVersionNo;

    /**
     * 模型当前版本状态，与模型定义列表一致。
     */
    private Long modelVersionStatus;

    /**
     * 来源记录唯一标识，对应来源表的 source_record_id 字段。
     */
    private String sourceRecordKey;

    /**
     * 来源记录稳定标识。
     */
    private String stableRecordId;

    /**
     * 接入批次号，对应来源表的 batch_id 字段。
     */
    private String batchNo;

    /**
     * 当前采集版本 ID。
     */
    private Long currentVersionId;

    /**
     * 采集该记录的接入任务 ID。
     */
    private Long taskId;

    /**
     * 接入任务名称。
     */
    private String taskName;

    /**
     * 接入任务编码。
     */
    private String taskCode;

    /**
     * 接入类型：DATABASE 表示数据库，FILE 表示文件，API 表示接口。
     */
    private String taskType;

    /**
     * 接入任务说明。
     */
    private String taskDescription;

    /**
     * 来源记录治理状态，对应来源表的 status 字段。
     */
    private String governanceStatus;

    /**
     * 首次接收时间。
     */
    private Date firstReceiveTime;

    /**
     * 记录采集时间，对应来源表的 collection_time 字段。
     */
    private Date lastReceiveTime;

    /**
     * 按已发布来源表业务列组织的 JSON 数据，键为物理列名。
     */
    private String modelData;
}
