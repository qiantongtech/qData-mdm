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

package tech.qiantong.qdata.mdm.dist.controller.admin.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 分发任务过滤条件 Response VO 对象 DIST_TASK_FILTER
 *
 * @author qdata
 * @date 2026-10-13
 */
@Schema(description = "分发任务过滤条件 Response VO")
@Data
public class DistTaskFilterRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "分发任务ID")
    @Schema(description = "分发任务ID", example = "")
    private Long taskId;

    @Excel(name = "主数据模型字段ID")
    @Schema(description = "主数据模型字段ID", example = "")
    private Long modelColumnId;

    @Excel(name = "条件运算符：EQ、NE、CONTAINS、NOT_CONTAINS、GT、GE、LT、LE、IS_NULL、IS_NOT_NULL")
    @Schema(description = "条件运算符：EQ、NE、CONTAINS、NOT_CONTAINS、GT、GE、LT、LE、IS_NULL、IS_NOT_NULL", example = "")
    private String operator;

    @Excel(name = "筛选比较值，按模型字段类型解析；IS_NULL、IS_NOT_NULL时为空")
    @Schema(description = "筛选比较值，按模型字段类型解析；IS_NULL、IS_NOT_NULL时为空", example = "")
    private String filterValue;

    @Excel(name = "条件展示及执行顺序；多个条件固定使用AND连接")
    @Schema(description = "条件展示及执行顺序；多个条件固定使用AND连接", example = "")
    private Long sortOrder;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Excel(name = "逻辑删除标志：0未删除，1已删除")
    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人ID")
    @Schema(description = "更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
