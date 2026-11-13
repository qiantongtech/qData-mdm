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
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 编码方案规则段 Response VO 对象 MODEL_ENCODING_ITEM
 *
 * @author qdata
 * @date 2026-09-17
 */
@Schema(description = "编码方案规则段 Response VO")
@Data
public class ModelEncodingItemRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "编码版本ID")
    @Schema(description = "编码版本ID", example = "")
    private Long encodingVersionId;

    @Excel(name = "规则段类型")
    @Schema(description = "规则段类型", example = "")
    private String type;

    @Excel(name = "排序号")
    @Schema(description = "排序号", example = "")
    private Long sortOrder;

    @Excel(name = "固定值/字段参数名称/日期时间输出格式")
    @Schema(description = "固定值/字段参数名称/日期时间输出格式", example = "")
    private String fixedValue;

    @Excel(name = "位数")
    @Schema(description = "位数", example = "")
    private Long sequenceLength;

    @Excel(name = "流水号开始")
    @Schema(description = "流水号开始", example = "")
    private Long sequenceStart;

    @Excel(name = "补位字符")
    @Schema(description = "补位字符", example = "")
    private String padChar;

    @Excel(name = "扩展配置JSON")
    @Schema(description = "扩展配置JSON", example = "")
    private String config;

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

    @Excel(name = "重置方式")
    @Schema(description = "重置方式", example = "")
    private String resetMethod;

    @Excel(name = "步长")
    @Schema(description = "步长", example = "")
    private Long stepSize;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

}
