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
import javax.validation.constraints.Min;
import javax.validation.Valid;

/**
 * 主数据批量变更申请保存请求，供整批校验和正式提交共用。
 */
@Data
@Schema(description = "主数据批量变更申请请求")
public class MasterChangeRequestBatchSaveReqVO {
    /** BATCH_UPDATE / BATCH_STATUS / BATCH_IMPORT。 */
    @NotBlank(message = "批量申请类型不能为空")
    @Pattern(regexp = "BATCH_UPDATE|BATCH_STATUS|BATCH_IMPORT", message = "批量申请类型无效")
    @Schema(description = "维护操作类型")
    private String mode;

    /** 批量修改、批量状态的正式主数据 ID。 */
    @Size(max = 500, message = "单批主数据不能超过500条")
    @Schema(description = "批量操作的主数据记录 ID 集合")
    private List<@NotBlank(message = "主数据ID不能为空")
            @Size(max = 19, message = "主数据ID长度不能超过19位")
            @Pattern(regexp = "^[1-9]\\d*$", message = "主数据ID格式无效") String> recordIds;

    /** 批量修改时统一应用的业务字段变更。 */
    @Size(max = 200, message = "申请字段数量不能超过200个")
    @Schema(description = "当前模型的业务字段编码及取值")
    private Map<@NotBlank(message = "字段编码不能为空")
            @Size(max = 128, message = "字段编码不能超过128字") String,
            @Size(max = 65535, message = "字段内容不能超过65535字") String> values;

    /** 批量状态的 FREEZE / UNFREEZE / INVALIDATE / RECOVER / ARCHIVE。 */
    @Pattern(regexp = "^$|FREEZE|UNFREEZE|INVALIDATE|RECOVER|ARCHIVE", message = "批量状态操作无效")
    @Schema(description = "批量状态操作")
    private String action;

    /** 导入解析后的逐行业务字段和分类父级。 */
    @Valid
    @Size(max = 500, message = "单批导入不能超过500条")
    @Schema(description = "解析后的导入数据行")
    private List<ImportRow> rows;

    /** CREATE_ONLY / UPSERT / UPDATE_ONLY。 */
    @Pattern(regexp = "^$|CREATE_ONLY|UPSERT|UPDATE_ONLY", message = "导入模式无效")
    @Schema(description = "导入模式：CREATE_ONLY、UPSERT、UPDATE_ONLY")
    private String importMode;

    @Size(max = 128, message = "匹配字段编码不能超过128字")
    @Pattern(regexp = "^$|[A-Za-z_][A-Za-z0-9_$]*", message = "匹配字段编码格式无效")
    @Schema(description = "用户从当前模型全部唯一业务字段中选择的导入匹配字段编码")
    private String matchField;

    @Size(max = 256, message = "上传文件名不能超过256字")
    @Schema(description = "批量导入的原始文件名称，仅用于提交申请时记录上传信息")
    private String fileName;

    @Size(max = 500, message = "申请原因不能超过500字")
    @Schema(description = "申请原因")
    private String applyReason;

    @Data
    public static class ImportRow {
        @Min(value = 2, message = "导入文件行号不能小于2")
        @Schema(description = "原始文件行号，包含表头和空行")
        private Integer rowNo;
        @Size(max = 19, message = "主数据ID长度不能超过19位")
        @Pattern(regexp = "^$|^[1-9]\\d*$", message = "主数据ID格式无效")
        @Schema(description = "正式数据导出行的主数据ID，用于重新导入时匹配当前模型记录")
        private String recordId;
        @Size(max = 19, message = "父级节点ID长度不能超过19位")
        @Pattern(regexp = "^$|^[1-9]\\d*$", message = "父级节点ID格式无效")
        @Schema(description = "分类模型目标父节点 ID，对应CATEGORY_ID，空值表示根节点")
        private String parentId;
        @Size(max = 200, message = "导入行字段数量不能超过200个")
        @Schema(description = "当前模型的业务字段编码及取值")
        private Map<@NotBlank(message = "导入字段编码不能为空")
                @Size(max = 128, message = "导入字段编码不能超过128字") String,
                @Size(max = 65535, message = "导入字段内容不能超过65535字") String> values;
    }
}
