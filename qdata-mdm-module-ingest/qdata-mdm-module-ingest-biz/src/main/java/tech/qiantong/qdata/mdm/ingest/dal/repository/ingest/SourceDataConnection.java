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

package tech.qiantong.qdata.mdm.ingest.dal.repository.ingest;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

/** 融合写事务持有目标连接，来源锁与状态变更使用同一连接。 */
final class SourceDataConnection implements TransactionSynchronization {
    final DbQuery query;
    final Connection connection;
    private final String key;

    /** 接管目标数据源连接并关闭自动提交，供来源行锁和状态更新共同使用。 */
    SourceDataConnection(DbQuery query, String key) throws SQLException {
        this.query = query;
        this.connection = query.getConnection();
        try {
            this.connection.setAutoCommit(false);
        } catch (SQLException ex) {
            this.connection.close();
            throw ex;
        }
        this.key = key;
    }

    @Override
    /** 主事务提交后再提交来源库连接；两个数据库连接不构成分布式原子事务。 */
    public void afterCommit() {
        try {
            connection.commit();
        } catch (SQLException ex) {
            throw failure("来源实体表状态提交失败", ex);
        }
    }

    @Override
    /** 无论主事务提交或回滚，均解绑线程资源并释放目标连接。 */
    public void afterCompletion(int status) {
        TransactionSynchronizationManager.unbindResourceIfPossible(key);
        close();
    }

    /** 回滚目标连接尚未提交的操作后释放连接；已提交的数据不会因此撤销。 */
    void close() {
        try {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        } catch (SQLException ex) {
            throw failure("来源实体表连接释放失败", ex);
        } finally {
            query.close();
        }
    }

    private ServiceException failure(String message, Exception cause) {
        ServiceException failure = new ServiceException(message);
        failure.initCause(cause);
        return failure;
    }
}
