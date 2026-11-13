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
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 主数据模型版本 Request VO 对象 MODEL_VERSION
 *
 * @author qdata
 * @date 2026-09-18
 */
@Schema(description = "主数据模型版本 Request VO")
@Data
public class ModelVersionPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "所属主数据模型ID，关联MODEL_BASE.ID；同一模型可以对应多个正式版本", example = "")
    private Long modelId;

    @Schema(description = "上一正式版本ID，关联MODEL_VERSION.ID；首次发布时为空", example = "")
    private Long baseVersionId;

    @Schema(description = "版本号", example = "")
    private String versionNo;

    @Schema(description = "发布时的模型业务说明快照", example = "")
    private String description;

    @Schema(description = "相对于上一版本的字段变更明细（JSON）")
    private String changeData;

    @Schema(description = "模型状态；0：草稿，1：已发布，2：修订中，3：已封存，4：已删除", example = "")
    private Long status;


}
