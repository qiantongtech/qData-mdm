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

import java.util.Map;
import lombok.Data;

/**
 * API 请求及读取配置，请求参数与响应解析规则分别维护。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskApiConfigVO {
    /**
     * HTTP 或 HTTPS 地址，可携带接口固定查询参数。
     */
    private String url;
    /**
     * 请求方法，支持 GET 和 POST。
     */
    private String httpMethod;
    /**
     * 自定义请求头，例如接口鉴权所需的头信息。
     */
    private Map<String, String> headerConfig;
    /**
     * GET 时追加为查询参数，POST 时作为 JSON 请求体。
     */
    private Map<String, Object> paramConfig;
    /**
     * 响应路径和分页规则，为空时兼容历史单次根数组读取。
     */
    private IngestApiReadConfigVO readConfig;
}
