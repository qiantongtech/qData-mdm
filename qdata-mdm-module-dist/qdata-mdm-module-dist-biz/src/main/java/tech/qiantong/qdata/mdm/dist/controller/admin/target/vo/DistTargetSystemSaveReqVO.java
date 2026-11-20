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

package tech.qiantong.qdata.mdm.dist.controller.admin.target.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 分发目标系统 创建/修改 Request VO DIST_TARGET_SYSTEM
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
@Schema(description = "分发目标系统 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DistTargetSystemSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "目标系统编码", example = "")
    @Size(max = 64, message = "目标系统编码长度不能超过64个字符")
    private String code;

    @Schema(description = "目标系统名称", example = "")
    @Size(max = 128, message = "目标系统名称长度不能超过128个字符")
    private String name;

    @Schema(description = "目标系统类型：ERP、SRM、CRM、OTHER", example = "")
    @Size(max = 5, message = "系统类型不能超过5个字符")
    private String type;

    @Schema(description = "目标系统联系人姓名", example = "")
    @Size(max = 512, message = "目标系统联系人姓名长度不能超过512个字符")
    private String contactName;

    @Schema(description = "负责人平台用户ID，存于 RESP_USER_ID")
    private Long responsibleUserId;

    @Schema(description = "目标系统联系人信息", example = "")
    @Size(max = 512, message = "目标系统联系人信息长度不能超过512个字符")
    private String contactInfo;

    @Schema(description = "状态：1、启用、2、停用", example = "")
    @Size(max = 1, message = "状态只能填写1或2")
    private String status;

    @Schema(description = "目标系统用途及业务说明", example = "")
    @Size(max = 256, message = "描述不能超过256个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
