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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSimpleVO;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 来源系统列表及详情响应对象，包含基础信息、关联连接和业务引用信息。
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Schema(description = "来源系统 Response VO")
@Data
public class IngestSourceRespVO implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 来源系统 ID。
     */
    @Schema(description = "主键ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 来源系统编码。
     */
    @Schema(description = "来源系统编码")
    private String code;

    /**
     * 来源系统名称。
     */
    @Schema(description = "来源系统名称")
    private String name;

    /**
     * 来源系统图标路径。
     */
    private String icon;

    /**
     * 来源系统状态：0 表示停用，1 表示启用。
     */
    private String validFlag;

    /**
     * 关联模型的重复检测任务数量汇总。
     */
    private Long duplicateTaskCount;

    /**
     * 关联模型的质量任务数量汇总。
     */
    private Long qualityTaskCount;

    /**
     * 通过接入任务关联的模型展示信息列表。
     */
    private List<IngestSourceModelRespVO> modelList;

    /**
     * 来源系统责任人用户 ID。
     */
    @Schema(description = "责任人用户ID")
    private Long responsibleUserId;

    /**
     * 来源系统责任人名称。
     */
    @Schema(description = "责任人名称")
    private String responsiblePersonName;

    /**
     * 来源系统责任人联系电话。
     */
    @Schema(description = "责任人电话号码")
    private String responsiblePersonPhone;

    /**
     * 来源系统业务说明。
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 来源可信度，取值范围为 0～100。
     */
    @Schema(description = "来源可信度，0～100")
    private Integer trustScore;

    /**
     * 引用该来源系统的接入任务数量。
     */
    @Schema(description = "接入任务引用数量")
    private Long taskCount;

    /**
     * 通过接入任务关联的模型名称列表。
     */
    @Schema(description = "涉及主数据模型名称")
    private List<String> modelNames;

    /**
     * 来源系统关联的数据库连接 ID 列表。
     */
    @Schema(description = "关联数据库连接ID")
    private List<Long> datasourceIds;

    /**
     * 来源系统关联的数据库连接简要信息列表。
     */
    @Schema(description = "关联数据库连接")
    private List<IngestDatasourceSimpleVO> datasourceList;

    /**
     * 详情中返回的、已被接入任务使用的关联数据库连接 ID 列表。
     */
    @Schema(description = "接入任务正在使用的数据库连接ID")
    private List<Long> usedDatasourceIds;

    /**
     * 详情中返回的接入任务引用列表。
     */
    @Schema(description = "接入任务引用列表")
    private List<IngestSourceTaskRespVO> taskList;

    /**
     * 来源系统创建人名称。
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 来源系统创建人用户 ID。
     */
    @Schema(description = "创建人用户ID")
    private Long creatorId;

    /**
     * 来源系统创建人联系电话。
     */
    @Schema(description = "创建人电话号码")
    private String creatorPhone;

    /**
     * 来源系统创建时间，格式为 yyyy-MM-dd HH:mm:ss。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 来源系统最近更新时间，格式为 yyyy-MM-dd HH:mm:ss。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
