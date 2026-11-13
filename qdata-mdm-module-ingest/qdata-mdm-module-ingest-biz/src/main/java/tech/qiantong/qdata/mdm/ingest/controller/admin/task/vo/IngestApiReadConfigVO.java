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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import lombok.Data;

/**
 * API 响应解析与分页配置，默认读取单次请求返回的根数组。
 * 路径仅支持根节点、对象字段和数组下标，例如 $.data.items、$.data[0]。
 *
 * @author chentangwen
 */
@Data
public class IngestApiReadConfigVO {
    /**
     * 记录数组路径，路径对应的每条记录必须为 JSON 对象。
     */
    private String recordsPath = "$";
    /**
     * 分页方式：NONE 单次请求、PAGE 页码、OFFSET 偏移量、CURSOR 游标。
     */
    private String pagingMode = "NONE";
    /**
     * 页码、偏移量或游标的请求参数名，按接口实际协议配置。
     */
    private String positionParam = "page";
    /**
     * 每页条数的请求参数名，不得与位置参数名相同。
     */
    private String pageSizeParam = "pageSize";
    /**
     * 请求页大小，允许 1～10000；OFFSET 每次按该值推进。
     */
    private int pageSize = 500;
    /**
     * 起始位置：PAGE 从 1 起，OFFSET 允许从 0 起；CURSOR 不使用该值。
     */
    private long startPosition = 1;
    /**
     * 首次请求的游标，空值表示不发送游标参数。
     */
    private String initialCursor;
    /**
     * 是否还有下一页的响应路径，配置后必须解析为布尔值。
     */
    private String hasMorePath;
    /**
     * 下一游标的响应路径，CURSOR 必填；游标必须为字符串或数字。
     */
    private String nextCursorPath;
    /**
     * 总记录数的响应路径，配置后必须解析为非负整数。
     */
    private String totalPath;
    /**
     * 无明确 hasMore 或总数时，是否允许 PAGE/OFFSET 以不足一页作为结束条件。
     */
    private boolean shortPageEnds;
    /**
     * 最多请求页数，达到上限仍未结束时报错，防止分页无法终止。
     */
    private int maxPages = 10000;
}
