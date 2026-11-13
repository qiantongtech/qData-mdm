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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.Valid;

/**
 * 主数据变更申请保存请求，供提交前校验和正式提交共用。
 */
@Data
@Schema(description = "主数据变更申请请求")
public class MasterChangeRequestSaveReqVO {
    /** CREATE / UPDATE，或单条生命周期操作；复制预填后按 CREATE 提交。 */
    @NotBlank(message = "维护类型不能为空")
    @Pattern(regexp = "CREATE|UPDATE|FREEZE|UNFREEZE|INVALIDATE|RECOVER|ARCHIVE",
            message = "维护类型无效")
    @Schema(description = "维护操作类型")
    private String mode;

    /** 主数据记录 ID 的必填规则随操作类型校验；使用字符串避免前端大整数精度损失。 */
    @Size(max = 19, message = "主数据ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "主数据ID格式无效")
    @Schema(description = "主数据记录 ID；新增时为空，其他操作的必填规则由业务层校验")
    private String recordId;

    /** 分类模型的目标父节点，映射到CATEGORY_ID，空表示根节点。 */
    @Size(max = 19, message = "父级节点ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "父级节点ID格式无效")
    @Schema(description = "分类模型目标父节点 ID，对应CATEGORY_ID，空值表示根节点")
    private String parentId;

    @Size(max = 500, message = "申请原因不能超过500字")
    @Schema(description = "申请原因")
    private String applyReason;

    /** 仅允许当前模型配置中的业务字段。 */
    @Size(max = 200, message = "申请字段数量不能超过200个")
    @Schema(description = "当前模型的业务字段编码及取值")
    private Map<@NotBlank(message = "字段编码不能为空")
            @Size(max = 128, message = "字段编码不能超过128字") String,
            @Size(max = 65535, message = "字段内容不能超过65535字") String> values;

    /** 与主记录一起暂存的子模型变更；当前不直接写入物化表。 */
    @Valid
    @Size(max = 500, message = "子模型变更数量不能超过500条")
    @Schema(description = "随主记录提交的子模型变更")
    private List<MasterChangeSubmodelSaveReqVO> submodelChanges;


}
