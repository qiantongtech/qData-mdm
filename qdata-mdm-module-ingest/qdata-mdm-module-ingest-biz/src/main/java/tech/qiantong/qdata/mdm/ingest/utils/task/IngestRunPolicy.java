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

package tech.qiantong.qdata.mdm.ingest.utils.task;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * 接入任务的失败重试策略。
 */
public final class IngestRunPolicy {
    /**
     * 禁止实例化，仅通过静态方法执行重试。
     */
    private IngestRunPolicy() {
    }

    /**
     * 在调用方允许时重试失败操作；取消或线程中断时立即退出。
     *
     * @param operation 待执行的操作，成功后不再重试
     * @param retries 首次执行失败后允许的重试次数
     * @param intervalSeconds 重试前等待的秒数
     * @param retryAllowed 失败后检查操作是否还能安全重试
     * @param beforeRetry 等待前执行的回调，接收从 1 开始的重试次数
     * @throws CancellationException 操作取消或线程中断
     */
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
}
