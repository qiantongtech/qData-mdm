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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 编码方案规则段 创建/修改 Request VO MODEL_ENCODING_ITEM
 *
 * @author qdata
 * @date 2026-09-17
 */
@Schema(description = "编码方案规则段 Response VO")
@Data
public class ModelEncodingItemSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "编码版本ID", example = "")
    private Long encodingVersionId;

    @Schema(description = "规则段类型", example = "")
    @Size(max = 512, message = "规则段类型长度不能超过512个字符")
    private String type;

    @Schema(description = "排序号", example = "")
    private Long sortOrder;

    @Schema(description = "固定值/字段参数名称/日期时间输出格式", example = "")
    @Size(max = 512, message = "固定值/字段参数名称/日期时间输出格式长度不能超过512个字符")
    private String fixedValue;

    @Schema(description = "位数", example = "")
    private Long sequenceLength;

    @Schema(description = "流水号开始", example = "")
    private Long sequenceStart;

    @Schema(description = "补位字符", example = "")
    @Size(max = 512, message = "补位字符长度不能超过512个字符")
    private String padChar;

    @Schema(description = "扩展配置JSON", example = "")
    @Size(max = 512, message = "扩展配置JSON长度不能超过512个字符")
    private String config;

    @Schema(description = "重置方式", example = "")
    @Size(max = 512, message = "重置方式长度不能超过512个字符")
    private String resetMethod;

    @Schema(description = "步长", example = "")
    private Long stepSize;


}
