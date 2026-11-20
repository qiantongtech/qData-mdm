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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 接入任务分页查询请求对象。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskPageReqVO extends PageParam {

    /**
     * 任务查询关键词，模糊匹配任务名称或编码。
     */
    private String keyword;

    /**
     * 任务名称，支持模糊匹配。
     */
    private String name;

    /**
     * 任务编码，支持模糊匹配。
     */
    private String code;

    /**
     * 来源系统 ID。
     */
    private Long sourceId;

    /**
     * 目标模型 ID。
     */
    private Long modelId;

    /**
     * 接入类型：DATABASE 表示数据库，FILE 表示文件，API 表示接口。
     */
    private String type;

    /**
     * 任务启停状态：0 表示停用，1 表示启用。
     */
    private String status;
}
