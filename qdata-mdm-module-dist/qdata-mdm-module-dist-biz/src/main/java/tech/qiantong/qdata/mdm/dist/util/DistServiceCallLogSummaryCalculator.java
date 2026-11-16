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

package tech.qiantong.qdata.mdm.dist.util;

import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogSummaryRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceCallLogDO;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 根据调用日志计算服务调用统计
 */
public final class DistServiceCallLogSummaryCalculator {
    private DistServiceCallLogSummaryCalculator() {
    }

    /**
     * 次数按最近七天统计，最近调用摘要从全部记录中选取。
     */
    public static DistServiceCallLogSummaryRespVO calculate(List<DistServiceCallLogDO> calls, Date now) {
        long start = now.getTime() - TimeUnit.DAYS.toMillis(7);
        long callCount = 0;
        long successCount = 0;
        long failureCount = 0;
        for (DistServiceCallLogDO call : calls) {
            Date callTime = call.getCallTime();
            if (callTime == null || callTime.getTime() < start || callTime.after(now)) {
                continue;
            }
            callCount++;
            // 其他结果值只计入总次数，不归入成功或失败次数。
            if ("SUCCESS".equalsIgnoreCase(call.getCallResult())) {
                successCount++;
            } else if ("FAILURE".equalsIgnoreCase(call.getCallResult())) {
                failureCount++;
            }
        }

        DistServiceCallLogDO latest = calls.stream()
                .filter(call -> call.getCallTime() != null)
                .max(Comparator.comparing(DistServiceCallLogDO::getCallTime))
                .orElse(null);
        DistServiceCallLogSummaryRespVO summary = new DistServiceCallLogSummaryRespVO();
        summary.setCallCount(callCount);
        summary.setSuccessCount(successCount);
        summary.setFailureCount(failureCount);
        if (latest != null) {
            summary.setLastCallTime(latest.getCallTime());
            summary.setLastCallSystem(latest.getCallerSystem());
            summary.setLastCallResult(latest.getCallResult());
            summary.setRequestSummary(latest.getRequestSummary());
            summary.setResponseSummary(latest.getResponseSummary());
            summary.setErrorMessage(latest.getErrorMessage());
        }
        return summary;
    }
}
