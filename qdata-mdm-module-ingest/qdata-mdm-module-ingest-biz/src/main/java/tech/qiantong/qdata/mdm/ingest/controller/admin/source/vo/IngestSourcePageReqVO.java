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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import java.util.List;

/**
 * 来源系统分页查询请求对象，封装关键词、责任人及业务引用等筛选条件。
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Schema(description = "来源系统分页查询 Request VO")
@Data
public class IngestSourcePageReqVO extends PageParam {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 综合查询关键词，模糊匹配来源系统名称、编码、描述或关联数据库连接名称。
     */
    @Schema(description = "关键词，匹配名称/编码/描述/数据库连接名称")
    private String keyword;

    /**
     * 来源系统名称，支持模糊匹配。
     */
    @Schema(description = "来源系统名称")
    private String name;

    /**
     * 来源系统编码，支持模糊匹配。
     */
    @Schema(description = "来源系统编码")
    private String code;

    /**
     * 责任人查询关键词，匹配用户昵称、账号或手机号。
     */
    @Schema(description = "责任人")
    private String responsiblePerson;

    /**
     * 服务端根据责任人关键词解析的用户 ID 列表，用于构造查询条件。
     */
    @Schema(hidden = true)
    private List<Long> responsibleUserIds;

    /**
     * 关联数据库连接名称，支持模糊匹配。
     */
    @Schema(description = "数据库连接名称")
    private String datasourceName;

    /**
     * 接入任务引用筛选：used 表示已引用，free 表示未引用；未指定时不按引用情况筛选。
     */
    @Schema(description = "业务引用：used/free")
    private String refFilter;
}
