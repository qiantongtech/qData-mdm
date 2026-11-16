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

package tech.qiantong.qdata.mdm.dist.enums.service;

/**
 * 数据服务配置常量。
 *
 * @author chentangwen
 */
public final class DistServiceConstants {

    /**
     * 服务编码前缀，最终编码为 API-SUP-加数据库主键ID。
     */
    public static final String SERVICE_CODE_PREFIX = "API-SUP-";

    /**
     * 默认请求方法。
     */
    public static final String DEFAULT_HTTP_METHOD = "GET";

    /**
     * 数据服务运行时接口前缀，沿用 qData-pro2 的 services 路由且不带版本段。
     */
    public static final String API_ROUTE_PREFIX = "/services";

    /**
     * API类型：0单条查询、1列表分页查询。
     */
    public static final String API_TYPE_SINGLE = "0";
    public static final String API_TYPE_PAGE = "1";
    public static final String API_TYPE_REGEX = "[01]";

    /**
     * 列表查询默认分页参数。
     */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 服务状态：0停用、1启用；新增服务默认停用。
     */
    public static final String STATUS_DISABLED = "0";
    public static final String STATUS_ENABLED = "1";
    public static final String STATUS_REGEX = "[01]";
    public static final String DEFAULT_STATUS = STATUS_DISABLED;

    /**
     * 兼容历史停用状态；升级脚本将其统一转换为0。
     */
    public static final String LEGACY_STATUS_DISABLED = "2";

    /**
     * 发布状态：0未发布、1已发布，与服务启停状态独立。
     */
    public static final String PUBLISH_STATUS_UNPUBLISHED = "0";
    public static final String PUBLISH_STATUS_PUBLISHED = "1";
    public static final String DEFAULT_PUBLISH_STATUS = PUBLISH_STATUS_UNPUBLISHED;

    /**
     * 查询和输出标志：0否、1是。
     */
    public static final String FLAG_NO = "0";
    public static final String FLAG_YES = "1";
    public static final String FLAG_REGEX = "[01]";

    /**
     * 记录有效。
     */
    public static final String VALID_FLAG_YES = "1";

    /**
     * 逻辑删除标志。
     */
    public static final String NOT_DELETED = "0";
    public static final String DELETED = "1";

    /**
     * 路径以/开头，不能包含空白、查询参数或锚点。
     */
    public static final String SERVICE_PATH_REGEX = "/[^\\s?#]*";

    private DistServiceConstants() {
    }
}
