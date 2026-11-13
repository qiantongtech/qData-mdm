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

package tech.qiantong.qdata.mdm.ingest.utils.task;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

/** 接入运行策略：仅重试未成功的操作，取消或失去执行占用时立即退出。 */
public final class IngestRunPolicy {
    private IngestRunPolicy() {}

    public static void retry(Runnable operation, int retries, int intervalSeconds,
                             BooleanSupplier retryAllowed, IntConsumer beforeRetry) {
        for (int attempt = 0; ; attempt++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException("任务执行已中断");
            try {
                operation.run();
                return;
            } catch (CancellationException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                if (attempt >= retries || !retryAllowed.getAsBoolean()) throw ex;
                beforeRetry.accept(attempt + 1);
                try {
                    Thread.sleep(intervalSeconds * 1000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new CancellationException("任务重试等待已中断");
                }
            }
        }
    }

    /** 只检查已映射且处理完成的值，不把未映射字段误判为空值。 */
    public static boolean checkNulls(Map<String, Object> mappedValues, String strategy) {
        boolean hasNull = mappedValues.values().stream().anyMatch(value -> value == null);
        if (hasNull && "FAIL".equals(strategy)) throw new ServiceException("映射字段存在空值");
        return hasNull;
    }
}
