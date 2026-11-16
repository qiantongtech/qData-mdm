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

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 分发字段映射 创建/修改 Request VO DIST_TASK_FIELD_MAPPING
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发字段映射 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DistTaskFieldMappingSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分发任务ID", example = "")
    private Long taskId;

    @Schema(description = "主数据模型字段ID", example = "")
    private Long modelColumnId;

    @Schema(description = "目标API请求体中的JSON", example = "")
    @Size(max = 512, message = "目标API请求体中的JSON长度不能超过512个字符")
    private String targetFieldName;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
