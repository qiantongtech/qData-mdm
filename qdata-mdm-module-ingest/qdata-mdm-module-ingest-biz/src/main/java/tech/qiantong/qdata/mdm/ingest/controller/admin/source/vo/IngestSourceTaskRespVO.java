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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 来源系统详情中的接入任务引用响应对象。
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Schema(description = "来源系统接入任务引用 Response VO")
@Data
public class IngestSourceTaskRespVO implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 接入任务 ID。
     */
    @Schema(description = "任务ID")
    private Long id;

    /**
     * 接入任务编码。
     */
    @Schema(description = "任务编码")
    private String code;

    /**
     * 接入任务名称。
     */
    @Schema(description = "任务名称")
    private String name;

    /**
     * 接入类型：DATABASE 表示数据库，FILE 表示文件，API 表示接口。
     */
    @Schema(description = "接入类型")
    private String type;

    /**
     * 目标主数据模型 ID。
     */
    @Schema(description = "目标主数据模型ID")
    private Long modelId;

    /**
     * 目标主数据模型名称。
     */
    @Schema(description = "目标主数据模型")
    private String modelName;

    /**
     * 接入任务启停状态：0 表示停用，1 表示启用。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 最近一次未删除运行实例的开始时间，格式为 yyyy-MM-dd HH:mm:ss；无运行记录时为空。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近执行时间")
    private Date lastRunTime;
}
