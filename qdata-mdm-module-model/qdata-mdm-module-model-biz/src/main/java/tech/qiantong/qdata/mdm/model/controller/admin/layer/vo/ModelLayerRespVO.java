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

package tech.qiantong.qdata.mdm.model.controller.admin.layer.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.util.Date;
import java.io.Serializable;

/**
 * 模型目录层级 Response VO 对象 MODEL_LAYER
 *
 * @author qdata
 * @date 2026-09-15
 */
@Schema(description = "模型目录层级 Response VO")
@Data
public class ModelLayerRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "主数据域ID，MODEL_PROJECT.ID")
    @Schema(description = "主数据域ID，MODEL_PROJECT.ID", example = "")
    private Long projectId;

    @Excel(name = "上级模型目录ID；0表示根目录")
    @Schema(description = "上级模型目录ID；0表示根目录", example = "")
    private Long parentId;

    @Excel(name = "模型目录编码")
    @Schema(description = "模型目录编码", example = "")
    private String code;

    @Excel(name = "模型目录名称")
    @Schema(description = "模型目录名称", example = "")
    private String name;

    /**
     * 负责人用户ID，关联系统用户。
     */
    @Schema(description = "负责人用户ID")
    private Long responsibleUserId;

    @Excel(name = "同级目录排序号，按数值从小到大排列，默认0")
    @Schema(description = "同级目录排序号，按数值从小到大排列，默认0", example = "")
    private Long sortOrder;

    @Excel(name = "模型目录的业务分类范围说明")
    @Schema(description = "模型目录的业务分类范围说明", example = "")
    private String description;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志；1：已删除，0：未删除")
    @Schema(description = "删除标志；1：已删除，0：未删除", example = "")
    private Boolean delFlag;

    @Excel(name = "关联主数据模型数量")
    @Schema(description = "关联的主数据模型数量", example = "0")
    private String modelCount;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "最后更新人")
    @Schema(description = "最后更新人", example = "")
    private String updateBy;

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "最后更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后更新时间", example = "")
    private Date updateTime;

}
