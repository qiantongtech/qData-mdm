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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerRespVO;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 主数据模型基本信息 Response VO 对象 MODEL_BASE
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "主数据模型基本信息 Response VO")
@Data
public class ModelBaseRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "模型编码，例如MDM_SUPPLIER；业务上要求系统内唯一")
    @Schema(description = "模型编码，例如MDM_SUPPLIER；业务上要求系统内唯一", example = "")
    private String code;

    @Excel(name = "模型名称")
    @Schema(description = "模型名称", example = "")
    private String name;

    /**
     * 模型图标路径，空字符串表示使用对应类型的默认图标。
     */
    private String icon;

    /**
     * 负责人用户ID，名称与联系方式由前端公共用户列表匹配。
     */
    private Long responsibleUserId;

    @Excel(name = "所属主数据域ID，关联MODEL_PROJECT.ID")
    @Schema(description = "所属主数据域ID，关联MODEL_PROJECT.ID", example = "")
    private Long projectId;

    @Schema(description = "所属主数据域名称")
    private String projectName;

    @Schema(description = "所属主数据域图标")
    private String projectIcon;

    @Excel(name = "所属模型目录ID，关联MODEL_LAYER.ID；目录必须属于PROJECT_ID对应的主数据域")
    @Schema(description = "所属模型目录ID，关联MODEL_LAYER.ID；目录必须属于PROJECT_ID对应的主数据域", example = "")
    private Long layerId;

    @Excel(name = "模型类型；0：普通模型，1：分类模型")
    @Schema(description = "模型类型；0：普通模型，1：分类模型", example = "")
    private String type;

    @Excel(name = "版本id")
    @Schema(description = "版本id", example = "")
    private Long versionId;

    @Excel(name = "模型业务说明")
    @Schema(description = "模型业务说明", example = "")
    private String description;

    @Schema(description = "上级模型ID；0表示主模型", example = "")
    private Long parentId;

    @Excel(name = "逻辑删除标志；0：未删除，1：已删除")
    @Schema(description = "逻辑删除标志；0：未删除，1：已删除", example = "")
    private Boolean delFlag;

    @Schema(description = "当前版本")
    private ModelVersionRespVO version;

    @Schema(description = "基准版本")
    private ModelVersionRespVO releaseVersion;

    @Schema(description = "模型目录层级路径")
    private List<ModelLayerRespVO> layers;

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

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人ID")
    @Schema(description = "更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    /**
     * 模型字段定义；详情查询时由模型字段表补充，非 MODEL_BASE 表字段。
     */
    private List<ModelColumnRespVO> fields;

    /**
     * 子模型列表；详情接口根据主模型 ID 查询 PARENT_ID 后返回。
     */
    private List<ModelBaseRespVO> subModels;

    /**
     * 最近一次成功发布记录ID，仅由发布成功流程维护
     */
    private Long publishedInstanceId;
}
