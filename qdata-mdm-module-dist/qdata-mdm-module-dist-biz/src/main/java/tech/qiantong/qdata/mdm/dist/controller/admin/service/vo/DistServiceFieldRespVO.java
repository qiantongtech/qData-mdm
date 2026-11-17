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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.io.Serializable;

/**
 * 数据服务字段配置 Response VO 对象 DIST_SERVICE_FIELD
 *
 * @author chentangwen
 * @date 2026-10-20
 */
@Schema(description = "数据服务字段配置 Response VO")
@Data
public class DistServiceFieldRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "数据服务ID", example = "")
    private Long serviceId;

    @Schema(description = "模型字段ID", example = "")
    private Long modelColumnId;

    @Schema(description = "是否可作为查询条件；0：否，1：是", example = "")
    private String queryableFlag;

    @Schema(description = "是否作为响应字段输出；0：否，1：是", example = "")
    private String outputFlag;

    @Schema(description = "服务输出字段别名", example = "")
    private String fieldAlias;

    @Schema(description = "示例值")
    private String exampleValue;

    @Schema(description = "查询条件示例值")
    private String queryExampleValue;

    @Schema(description = "排序", example = "")
    private Integer sortOrder;

    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Schema(description = "创建人", example = "")
    private String createBy;

    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Schema(description = "备注", example = "")
    private String remark;

}
