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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 主数据模型版本 创建/修改 Request VO MODEL_VERSION
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "主数据模型版本 Response VO")
@Data
public class ModelVersionSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Schema(description = "上一正式版本ID", example = "")
    private Long baseVersionId;

    @Schema(description = "版本号", example = "")
    private String versionNo;

    @Schema(description = "说明", example = "")
    @Size(max = 512, message = "说明长度不能超过512个字符")
    private String description;

    @Schema(description = "变更明细（JSON）")
    private String changeData;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;

    @Schema(description = "模型状态；0：草稿，1：已发布，2：修订中，3：已封存，4：已删除", example = "")
    private Long status;


}
