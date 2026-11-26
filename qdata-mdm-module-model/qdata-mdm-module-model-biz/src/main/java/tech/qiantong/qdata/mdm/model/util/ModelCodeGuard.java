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

package tech.qiantong.qdata.mdm.model.util;

import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** 在同一实例内串行执行同类编码的查询和保存，数据库唯一索引覆盖多实例写入。 */
public enum ModelCodeGuard {
    PROJECT("主数据域", "UK_MODEL_PROJECT_CODE"),
    LAYER("模型目录", "UK_MODEL_LAYER_CODE"),
    DICT_TYPE("参考数据集合", "UK_MODEL_DICT_TYPE_CODE"),
    BASE("模型", "UK_MODEL_BASE_CODE");

    private final ReentrantLock lock = new ReentrantLock();
    private final String label;
    private final String indexName;

    ModelCodeGuard(String label, String indexName) {
        this.label = label;
        this.indexName = indexName;
    }

    /** 在第一次读库前调用，锁随当前事务提交或回滚释放。 */
    public void lockTransaction() {
        if (!TransactionSynchronizationManager.isSynchronizationActive() || lock.isHeldByCurrentThread()) return;
        lock.lock();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
    }

    public ServiceException duplicateCode(String code) {
        return new ServiceException(label + "“" + code + "”编码已存在", HttpStatus.WARN);
    }

    public <T> T write(String code, Supplier<T> action) {
        return write(action, () -> duplicateCode(code));
    }

    public <T> T write(Supplier<T> action, Supplier<ServiceException> codeConflict) {
        try {
            return action.get();
        } catch (DataAccessException ex) {
            String message = ex.getMostSpecificCause().getMessage();
            if (message != null && message.toUpperCase(Locale.ROOT).contains(indexName)) throw codeConflict.get();
            throw ex;
        }
    }
}
