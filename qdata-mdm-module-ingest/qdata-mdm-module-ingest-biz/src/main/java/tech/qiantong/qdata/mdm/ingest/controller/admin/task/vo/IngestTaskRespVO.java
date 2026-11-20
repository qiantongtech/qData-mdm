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

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 接入任务列表及详情响应对象，在任务配置基础上补充关联信息和最近运行情况。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskRespVO extends IngestTaskSaveReqVO {

    /**
     * 数据库来源配置记录 ID，仅供服务端使用，不参与 JSON 响应。
     */
    @JsonIgnore
    private Long dbConfigId;

    /**
     * 服务端维护的当前增量位置
     */
    @JsonIgnore
    private String currentIncrementValue;

    /**
     * 来源系统名称。
     */
    private String sourceName;

    /**
     * 来源系统编码。
     */
    private String sourceCode;

    /**
     * 来源系统图标路径。
     */
    private String sourceIcon;

    /**
     * 目标模型图标路径。
     */
    private String modelIcon;

    /**
     * 目标模型类型：0 表示普通模型，1 表示分类模型。
     */
    private String modelType;

    /**
     * 目标模型业务说明。
     */
    private String modelDescription;

    /**
     * 目标模型最近一次成功发布的版本号。
     */
    private String modelVersionNo;

    /**
     * 目标模型当前版本状态。
     */
    private Long modelVersionStatus;

    /**
     * 最近一次运行的结束时间。
     */
    private Date lastRunEndTime;

    /**
     * 最近一次运行的展示耗时，单位：毫秒。
     */
    private Long lastRunDurationMillis;

    /**
     * 来源系统可信度，取值范围为 0～100。
     */
    private Integer sourceTrustScore;

    /**
     * 目标模型名称。
     */
    private String modelName;

    /**
     * 持久化调度开关：0 表示关闭，1 表示开启；调度同步异常通过服务日志报告。
     */
    private String schedulerStatus;

    /**
     * 任务责任人名称。
     */
    private String responsiblePersonName;

    /**
     * 任务责任人联系电话。
     */
    private String responsiblePersonPhone;

    /**
     * 任务创建人联系电话。
     */
    private String createPhoneNumber;

    /**
     * 最近一次运行状态：1 表示运行中，2 表示失败，4 表示成功。
     */
    private String lastRunStatus;

    /**
     * 最近一次运行的开始时间。
     */
    private Date lastRunTime;

    /**
     * 目标模型编码。
     */
    private String modelCode;

    /**
     * 数据获取方式摘要，按接入类型展示数据库来源、文件名称或 API 请求信息。
     */
    private String dataFetch;

    /**
     * 数据库连接名称，仅供服务端组装数据获取摘要，不参与 JSON 响应。
     */
    @JsonIgnore
    private String datasourceName;

    /**
     * 数据库来源对象名称
     */
    @JsonIgnore
    private String fetchSourceObject;

    /**
     * 来源文件名称
     */
    @JsonIgnore
    private String fetchFileName;

    /**
     * 来源 API 地址
     */
    @JsonIgnore
    private String fetchApiUrl;

    /**
     * 来源 API 请求方法
     */
    @JsonIgnore
    private String fetchHttpMethod;
}
