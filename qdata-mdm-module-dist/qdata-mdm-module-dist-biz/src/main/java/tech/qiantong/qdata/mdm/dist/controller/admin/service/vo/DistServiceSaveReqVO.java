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

package tech.qiantong.qdata.mdm.dist.controller.admin.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/**
 * 数据服务及字段配置保存参数，编码、发布信息和审计字段由后端维护。
 * @author chentangwen
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据服务保存参数")
public class DistServiceSaveReqVO extends BaseEntity {
    @Schema(description = "修改时必填")
    private Long id;

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 128, message = "服务名称长度不能超过128个字符")
    @Schema(description = "服务名称")
    private String name;

    @NotNull(message = "主数据模型ID不能为空")
    @Positive(message = "主数据模型ID必须为正数")
    @Schema(description = "已发布且可用的主数据模型ID")
    private Long modelId;

    @NotBlank(message = "API类型不能为空")
    @Pattern(regexp = DistServiceConstants.API_TYPE_REGEX, message = "API类型只能为0（单条查询）或1（列表分页查询）")
    @Schema(description = "API类型：0单条查询、1列表分页查询")
    private String apiType;

    @Positive(message = "负责人用户ID必须为正数")
    @Schema(description = "负责人用户ID，可不填；姓名和电话取系统用户信息")
    private Long responsibleUserId;

    @NotBlank(message = "服务路径不能为空")
    @Size(max = 512, message = "服务路径长度不能超过512个字符")
    @Pattern(regexp = DistServiceConstants.SERVICE_PATH_REGEX, message = "服务路径须以/开头，且不能包含空白、查询参数或锚点")
    @Schema(description = "服务路径", example = "/master-data/customer/list")
    private String servicePath;

    @Size(max = 512, message = "描述长度不能超过512个字符")
    @Schema(description = "服务描述")
    private String description;

    @Override
    @Size(max = 512, message = "备注长度不能超过512个字符")
    public String getRemark() {
        return super.getRemark();
    }

    @Valid
    @NotEmpty(message = "服务字段配置不能为空")
    @Schema(description = "完整字段配置，新增和修改均须提交，至少一个输出字段")
    private List<@NotNull(message = "字段配置不能为空") DistServiceFieldSaveReqVO> fields;
}
