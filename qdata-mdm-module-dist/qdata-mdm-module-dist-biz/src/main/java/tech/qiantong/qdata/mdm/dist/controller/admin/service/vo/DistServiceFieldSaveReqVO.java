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

package tech.qiantong.qdata.mdm.dist.controller.admin.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import javax.validation.constraints.*;

/**
 * 服务字段配置随主表整体保存，不接受客户端指定子表主键和服务ID。
 * @author chentangwen
 */
@Data
@Schema(description = "数据服务字段保存参数")
public class DistServiceFieldSaveReqVO {
    @NotNull(message = "模型字段ID不能为空")
    @Positive(message = "模型字段ID必须为正数")
    @Schema(description = "所属模型的已发布字段ID")
    private Long modelColumnId;

    @Pattern(regexp = DistServiceConstants.FLAG_REGEX, message = "查询标志只能为0或1")
    @Schema(description = "是否作为查询条件：0否、1是，默认0")
    private String queryableFlag;

    @Pattern(regexp = DistServiceConstants.FLAG_REGEX, message = "输出标志只能为0或1")
    @Schema(description = "是否作为输出字段：0否、1是，默认1")
    private String outputFlag;

    @Size(max = 128, message = "字段别名长度不能超过128个字符")
    @Schema(description = "输出别名，未填时使用模型字段编码")
    private String fieldAlias;

    @Size(max = 512, message = "示例值长度不能超过512个字符")
    @Schema(description = "示例值")
    private String exampleValue;

    @Size(max = 512, message = "查询条件示例值长度不能超过512个字符")
    @Schema(description = "查询条件示例值")
    private String queryExampleValue;

    @Min(value = 0, message = "排序不能小于0")
    @Schema(description = "字段顺序，未填时按提交顺序保存")
    private Integer sortOrder;

    @Size(max = 512, message = "备注长度不能超过512个字符")
    @Schema(description = "备注")
    private String remark;
}
