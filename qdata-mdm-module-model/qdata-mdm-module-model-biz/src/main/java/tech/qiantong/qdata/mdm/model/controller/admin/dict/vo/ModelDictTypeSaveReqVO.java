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

package tech.qiantong.qdata.mdm.model.controller.admin.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 参考数据 创建/修改 Request VO MODEL_DICT_DATA
 *
 * @author qdata
 * @date 2026-09-16
 */
@Schema(description = "参考数据 Response VO")
@Data
public class ModelDictTypeSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "参考数据编码", example = "")
    @NotBlank(message = "参考数据编码不能为空")
    @Size(max = 64, message = "参考数据编码长度不能超过64个字符")
    @Pattern(regexp = "[A-Za-z0-9_]*", message = "参考数据编码支持英文字母、数字和下划线")
    private String code;

    @Schema(description = "参考数据名称", example = "")
    @NotBlank(message = "参考数据名称不能为空")
    @Size(max = 64, message = "参考数据名称长度不能超过64个字符")
    @Pattern(regexp = "[\u4e00-\u9fa5A-Za-z0-9_-]*", message = "参考数据名称支持中文、英文字符、数字、下划线和短横线")
    private String name;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}
