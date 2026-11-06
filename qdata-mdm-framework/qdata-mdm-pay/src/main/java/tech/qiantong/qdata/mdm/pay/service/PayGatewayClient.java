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

package tech.qiantong.qdata.mdm.pay.service;

import tech.qiantong.qdata.mdm.pay.domain.*;
import tech.qiantong.qdata.mdm.pay.domain.*;

import java.util.Map;

/**
 * 接口定义了与支付网关集成的基本操作。
 * 该接口包括发起支付、退款、查询支付状态以及处理回调的方法。
 * @author qdata
 */
public interface PayGatewayClient {

    /**
     * 发起支付请求。
     * @param request 包含支付请求的详细信息
     * @return 返回支付响应信息
     */
    PaymentResponse initiatePayment(PaymentRequest request);

    /**
     * 发起退款请求。
     * @param request 包含退款请求的详细信息
     * @return 返回退款响应信息
     */
    RefundResponse refund(RefundRequest request);

    /**
     * 查询支付状态。
     * @param paymentId 支付订单的唯一标识符
     * @return 返回支付状态响应信息
     */
    PaymentStatusResponse queryStatus(String paymentId);

    /**
     * 处理支付回调通知。
     * @param parameters 包含回调通知的所有参数
     */
    Notification handleNotification(Map<String, String> parameters);
}
