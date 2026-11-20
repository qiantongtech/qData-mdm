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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 来源数据详情响应对象，包含来源记录、模型及采集任务信息。
 *
 * @author chentangwen
 */
@Data
public class SourceDataDetailRespVO {

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
     * 模型发布版本号。
     */
    private String modelVersionNo;

    /**
     * 模型当前版本状态。
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
     * 当前采集版本 ID。
     */
    private Long currentVersionId;

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
     * 来源记录采集版本号。
     */
    private Integer versionNo;

    /**
     * 采集该记录的接入任务 ID。
     */
    private Long taskId;

    /**
     * 接入任务名称。
     */
    private String taskName;

    /**
     * 关联运行实例 ID。
     */
    private Long taskRunId;

    /**
     * 接入批次号，对应来源表的 batch_id 字段。
     */
    private String batchNo;

    /**
     * 来源记录逻辑删除标识，对应来源表的 del_flag 字段。
     */
    private String delFlag;

    /**
     * 运行实例触发方式。
     */
    private String triggerType;

    /**
     * 运行实例状态。
     */
    private String taskRunStatus;

    /**
     * 运行实例开始时间。
     */
    private Date taskStartTime;

    /**
     * 运行实例结束时间。
     */
    private Date taskEndTime;

    /**
     * 运行实例累计处理总数。
     */
    private Long totalCount;

    /**
     * 运行实例成功记录数。
     */
    private Long successCount;

    /**
     * 运行实例失败记录数。
     */
    private Long failureCount;

    /**
     * 运行实例跳过记录数。
     */
    private Long skipCount;

    /**
     * 来源记录在原系统中的更新时间。
     */
    private Date sourceUpdateTime;

    /**
     * 记录采集时间的字符串表示，由来源表 collection_time 字段转换。
     */
    private String receiveTime;

    /**
     * 原始来源记录 JSON。
     */
    private String rawData;

    /**
     * 按已发布来源表业务列组织的 JSON 数据，键为物理列名。
     */
    private String modelData;
}
