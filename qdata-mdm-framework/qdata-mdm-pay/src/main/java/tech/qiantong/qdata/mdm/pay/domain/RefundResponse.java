/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.pay.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 类表示退款请求的响应数据。
 * 该类包含支付网关返回的退款结果信息。
 * @author qdata
 */
@Data
@AllArgsConstructor
public class RefundResponse {

    /**
     * 退款状态，表示退款操作是否成功或失败。
     */
    private String status;

    /**
     * 商户系统中的订单唯一标识符。
     */
    private String orderId;

}
