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

package tech.qiantong.qdata.mdm.dist.controller.admin.service.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 主数据服务定义 Response VO 对象 DIST_SERVICE
 *
 * @author chentangwen
 * @date 2026-10-20
 */
@Schema(description = "主数据服务定义 Response VO")
@Data
public class DistServiceRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "服务字段配置，仅详情返回")
    private java.util.List<DistServiceFieldRespVO> fields;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "服务编码", example = "")
    private String code;

    @Schema(description = "服务名称", example = "")
    private String name;

    @Schema(description = "近7天调用次数")
    private Long callCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近调用时间")
    private Date lastCallTime;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "API类型：0单条查询、1列表分页查询；历史未配置记录为空")
    private String apiType;

    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Schema(description = "负责人姓名")
    private String responsiblePersonName;

    @Schema(description = "负责人联系电话")
    private String responsiblePersonPhone;

    @Schema(description = "服务路径", example = "")
    private String servicePath;

    /**
     * 不包含主机和端口的运行时路由路径。
     */
    @Schema(description = "运行时相对调用路径")
    private String serviceUrl;

    @Schema(description = "HTTP方法", example = "")
    private String httpMethod;

    @Schema(description = "服务状态：0停用、1启用", example = "0")
    private String status;

    @Schema(description = "发布状态：0未发布、1已发布", example = "0")
    private String publishStatus;

    @Schema(description = "描述", example = "")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间", example = "")
    private Date publishTime;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Schema(description = "创建人", example = "")
    private String createBy;

    @Schema(description = "创建人联系电话")
    private String creatorPhone;

    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Schema(description = "备注", example = "")
    private String remark;

}
