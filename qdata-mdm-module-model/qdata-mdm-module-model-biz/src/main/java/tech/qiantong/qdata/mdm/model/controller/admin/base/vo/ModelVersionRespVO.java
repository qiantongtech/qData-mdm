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
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 主数据模型版本 Response VO 对象 MODEL_VERSION
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "主数据模型版本 Response VO")
@Data
public class ModelVersionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "所属主数据模型ID，关联MODEL_BASE.ID；同一模型可以对应多个正式版本")
    @Schema(description = "所属主数据模型ID，关联MODEL_BASE.ID；同一模型可以对应多个正式版本", example = "")
    private Long modelId;

    @Excel(name = "上一正式版本ID，关联MODEL_VERSION.ID；首次发布时为空")
    @Schema(description = "上一正式版本ID，关联MODEL_VERSION.ID；首次发布时为空", example = "")
    private Long baseVersionId;

    @Excel(name = "版本号")
    @Schema(description = "版本号", example = "")
    private String versionNo;

    @Excel(name = "发布时的模型业务说明快照")
    @Schema(description = "发布时的模型业务说明快照", example = "")
    private String description;

    @Schema(description = "相对于上一版本的字段变更明细（JSON）")
    private String changeData;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志；0：未删除，1：已删除")
    @Schema(description = "删除标志；0：未删除，1：已删除", example = "")
    private Boolean delFlag;

    @Excel(name = "版本发布人")
    @Schema(description = "版本发布人", example = "")
    private String createBy;

    @Excel(name = "版本发布人ID")
    @Schema(description = "版本发布人ID", example = "")
    private Long creatorId;

    @Excel(name = "版本正式发布成功时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "版本正式发布成功时间", example = "")
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

    @Excel(name = "版本发布说明，由发布人填写")
    @Schema(description = "版本发布说明，由发布人填写", example = "")
    private String remark;

    @Excel(name = "模型状态；0：草稿，1：已发布，2：修订中，3：已封存，4：已删除")
    @Schema(description = "模型状态；0：草稿，1：已发布，2：修订中，3：已封存，4：已删除", example = "")
    private Long status;

}
