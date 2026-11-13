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
 * 来源系统 Response VO
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Schema(description = "来源系统 Response VO")
@Data
public class IngestSourceRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "来源系统编码")
    private String code;

    @Schema(description = "来源系统名称")
    private String name;

    private String icon;
    private String validFlag;
    private Long duplicateTaskCount;
    private Long qualityTaskCount;
    private List<IngestSourceModelRespVO> modelList;

    @Schema(description = "责任人用户ID")
    private Long responsibleUserId;

    @Schema(description = "责任人名称")
    private String responsiblePersonName;

    @Schema(description = "责任人电话号码")
    private String responsiblePersonPhone;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "来源可信度，0～100")
    private Integer trustScore;

    @Schema(description = "接入任务引用数量")
    private Long taskCount;

    @Schema(description = "涉及主数据模型名称")
    private List<String> modelNames;

    @Schema(description = "关联数据库连接ID")
    private List<Long> datasourceIds;

    @Schema(description = "关联数据库连接")
    private List<IngestDatasourceSimpleVO> datasourceList;

    @Schema(description = "接入任务正在使用的数据库连接ID")
    private List<Long> usedDatasourceIds;

    @Schema(description = "接入任务引用列表")
    private List<IngestSourceTaskRespVO> taskList;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建人用户ID")
    private Long creatorId;

    @Schema(description = "创建人电话号码")
    private String creatorPhone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
