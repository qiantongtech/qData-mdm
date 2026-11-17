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

import lombok.Data;

import java.util.Date;

/**
 * 数据服务调用统计 Response VO 对象 DIST_SERVICE_CALL_LOG
 *
 * @author chentangwen
 * @date 2026-10-22
 */
@Data
public class DistServiceCallLogSummaryRespVO {
    /**
     * 近七天调用次数
     */
    private Long callCount;
    /**
     * 近七天成功次数
     */
    private Long successCount;
    /**
     * 近七天失败次数
     */
    private Long failureCount;
    /**
     * 历史最近调用时间
     */
    private Date lastCallTime;
    /**
     * 最近调用的系统名称
     */
    private String lastCallSystem;
    /**
     * 最近调用结果
     */
    private String lastCallResult;
    /**
     * 最近调用的脱敏请求摘要
     */
    private String requestSummary;
    /**
     * 最近调用的脱敏响应摘要
     */
    private String responseSummary;
    /**
     * 最近调用异常信息
     */
    private String errorMessage;
}
