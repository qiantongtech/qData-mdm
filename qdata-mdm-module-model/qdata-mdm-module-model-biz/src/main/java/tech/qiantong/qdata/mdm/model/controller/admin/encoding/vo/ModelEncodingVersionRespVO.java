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

package tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 编码方案版本 Response VO 对象 MODEL_ENCODING_VERSION
 *
 * @author qdata
 * @date 2026-09-17
 */
@Schema(description = "编码方案版本 Response VO")
@Data
public class ModelEncodingVersionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "方案ID")
    @Schema(description = "方案ID", example = "")
    private Long encodingId;

    @Excel(name = "版本号")
    @Schema(description = "版本号", example = "")
    private String versionNo;

    @Excel(name = "状态", readConverterExp = "1=草稿,2=停用,3=发布")
    @Schema(description = "状态", example = "")
    private Integer status;

    @Excel(name = "生效时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效时间", example = "")
    private Date effectiveTime;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "逻辑删除标志：0未删除，1已删除")
    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private Boolean delFlag;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "更新人ID")
    @Schema(description = "更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    @Schema(description = "${comment}", example = "")
    private Boolean validFlag;

}
