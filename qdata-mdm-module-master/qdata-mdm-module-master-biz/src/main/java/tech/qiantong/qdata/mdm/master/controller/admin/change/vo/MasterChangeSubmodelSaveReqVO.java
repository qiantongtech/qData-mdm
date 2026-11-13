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
 * 主数据变更申请中的子模型维护内容。
 */
@Data
@Schema(description = "主数据子模型维护请求")
public class MasterChangeSubmodelSaveReqVO {
    @NotBlank(message = "子模型ID不能为空")
    @Size(max = 19, message = "子模型ID长度不能超过19位")
    @Pattern(regexp = "^[1-9]\\d*$", message = "子模型ID格式无效")
    @Schema(description = "子模型 ID")
    private String submodelId;

    @Size(max = 256, message = "子模型名称不能超过256字")
    @Schema(description = "子模型名称")
    private String submodelName;

    /** CREATE / UPDATE / DELETE */
    @NotBlank(message = "子模型变更类型不能为空")
    @Pattern(regexp = "CREATE|UPDATE|DELETE", message = "子模型变更类型无效")
    @Schema(description = "子模型操作类型：CREATE、UPDATE、DELETE")
    private String operation;

    /** UPDATE / DELETE 时为原子记录 ID，CREATE 时为空。 */
    @Size(max = 19, message = "子数据ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "子数据ID格式无效")
    @Schema(description = "主数据记录 ID；新增时为空，其他操作的必填规则由业务层校验")
    private String recordId;

    @Size(max = 200, message = "子模型字段数量不能超过200个")
    @Schema(description = "当前模型的业务字段编码及取值")
    private Map<@NotBlank(message = "子模型字段编码不能为空")
            @Size(max = 128, message = "子模型字段编码不能超过128字") String,
            @Size(max = 65535, message = "子模型字段内容不能超过65535字") String> values;
}
