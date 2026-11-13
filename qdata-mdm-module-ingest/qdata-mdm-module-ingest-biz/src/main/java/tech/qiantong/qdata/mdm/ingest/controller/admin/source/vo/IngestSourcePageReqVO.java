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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.List;

/**
 * 来源系统分页查询 Request VO
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Schema(description = "来源系统分页查询 Request VO")
@Data
public class IngestSourcePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词，匹配名称/编码/描述/数据库连接名称")
    private String keyword;

    @Schema(description = "来源系统名称")
    private String name;

    @Schema(description = "来源系统编码")
    private String code;

    @Schema(description = "责任人")
    private String responsiblePerson;

    @Schema(hidden = true)
    private List<Long> responsibleUserIds;

    @Schema(description = "数据库连接名称")
    private String datasourceName;

    @Schema(description = "业务引用：used/free")
    private String refFilter;
}
