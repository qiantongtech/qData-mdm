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

package tech.qiantong.qdata.mdm.model.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.query.AbstractDbQueryFactory;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishTarget;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 统一管理发布目标库资源：内部作用域复用普通连接，会话独立持有事务连接。
 * 两类资源分别关闭，不放进发布JSON或日志。
 */
@Getter
public class ModelPublishDatabaseSession implements AutoCloseable {

    /**
     * 当前会话使用的查询器；普通流程借用共享资源，事务流程绑定本次专用连接。
     */
    private DbQuery query;

    /** common原查询器负责创建真实连接；共享发布时由入口关闭，事务内查询器不拥有它。 */
    private DbQuery originalQuery;
    /** 整批DDL独占的真实连接，执行及核验都复用它。 */
    private Connection transactionConnection;
    /** 借用会话把提交动作交回真实事务所有者。 */
    private ModelPublishDatabaseSession transactionOwner;
    /** 提交是否已经开始，返回异常时不能改口声称整批已回滚。 */
    private boolean commitStarted;
    /** 原查询器是否由发布入口共享持有；共享会话只关闭自己的事务连接。 */
    private boolean sharedQuery;
    /** 异常时废弃普通自动提交连接；事务连接仍交给原回滚/恢复流程处理。 */
    private Runnable invalidateReadConnection;
    /** 防止重复关闭同一会话。 */
    private boolean closed;

    /**
     * 含连接凭证的参数，只能在服务端局部使用。
     */
    private DbQueryProperty property;

    /**
     * 当前数据库方言，所有DDL都由它生成。
     */
    private DbDialect dialect;

    /**
     * 可以保存进计划的非敏感目标描述。
     */
    private ModelPublishTarget target;

    /**
     * 组装本次自有资源，调用方必须用try-with-resources关闭。
     */
    public ModelPublishDatabaseSession(DbQuery query, DbQueryProperty property, DbDialect dialect, ModelPublishTarget target) {
        this.query = query;
        this.originalQuery = query;
        this.property = property;
        this.dialect = dialect;
        this.target = target;
    }

    /** 组装发布内共享会话；originalQuery用于创建独立事务连接，query只处理普通自动提交查询。 */
    ModelPublishDatabaseSession(DbQuery query, DbQuery originalQuery, DbQueryProperty property, DbDialect dialect,
                                ModelPublishTarget target, Runnable invalidateReadConnection) {
        this(query, property, dialect, target);
        this.originalQuery = originalQuery;
        this.sharedQuery = true;
        this.invalidateReadConnection = invalidateReadConnection;
    }

    /** 普通SQL或读取发生异常时关闭异常连接；不能提前关闭整批DDL事务连接。 */
    public void invalidateReadConnection() {
        if (transactionConnection == null && invalidateReadConnection != null) {
            invalidateReadConnection.run();
        }
    }

    /**
     * 读取当前目标的四项JDBC能力。只有全部满足才关闭自动提交，不按数据库名称猜测。
     */
    public boolean beginDdlTransaction() throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("BEGIN_DDL_TRANSACTION", null, null);
        try {
            Connection connection = originalQuery.getConnection();
            try {
                DatabaseMetaData metadata = connection.getMetaData();
                // 四项能力要一起满足：支持事务、DDL可参与事务、DDL不隐式提交、事务不会忽略DDL。
                boolean supported = metadata.supportsTransactions()
                        && metadata.supportsDataDefinitionAndDataManipulationTransactions()
                        && !metadata.dataDefinitionCausesTransactionCommit()
                        && !metadata.dataDefinitionIgnoredInTransactions();
                if (!supported) {
                    connection.close();
                    return false;
                }
                if (!connection.getAutoCommit()) {
                    throw new SQLException("本次DDL连接已存在其他事务");
                }
                // 确认能力后才关闭自动提交；此连接由整个批次持有，所有DDL和事务内核验都走它。
                connection.setAutoCommit(false);
                transactionConnection = connection;
                query = transactionQuery(connection);
                return true;
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.close();
                } catch (SQLException closeFailure) {
                    ex.addSuppressed(closeFailure);
                }
                throw ex;
            }
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /**
     * 复用common原有查询实现，只把数据源和JdbcTemplate绑定到同一真实连接。
     * suppressClose让既有try-with-resources只关闭借用句柄，整批结束再关闭真实连接。
     */
    private DbQuery transactionQuery(Connection connection) {
        // suppressClose=true：借用连接的close只是释放句柄，真正物理连接留给整批会话统一关闭。
        SingleConnectionDataSource source = new SingleConnectionDataSource(connection, true);
        AbstractDbQueryFactory transactionQuery = new AbstractDbQueryFactory() {
            /** 从本批次单连接数据源借用句柄，不新建物理连接。 */
            @Override
            public Connection getConnection() {
                try {
                    return source.getConnection();
                } catch (SQLException ex) {
                    throw new ServiceException("业务库事务连接不可用");
                }
            }

            /** 借用查询器不拥有连接池，由原会话统一释放资源。 */
            @Override
            public void close() {
                // 原会话统一释放连接和连接池，借用查询器不重复关闭。
            }
        };
        transactionQuery.setDataSource(source);
        transactionQuery.setJdbcTemplate(new JdbcTemplate(source));
        transactionQuery.setDbDialect(dialect);
        transactionQuery.setDbQueryProperty(property);
        return transactionQuery;
    }

    /** 每条SQL借用同一个事务资源，close不提前结束整批事务。 */
    public ModelPublishDatabaseSession borrow() {
        ModelPublishDatabaseSession result = new ModelPublishDatabaseSession(query, property, dialect, target);
        result.transactionConnection = transactionConnection;
        result.transactionOwner = this;
        return result;
    }

    /** 仅本次会话创建的事务连接允许非自动提交，外部传入的其他事务不能混入。 */
    public boolean transactional() {
        return transactionConnection != null;
    }

    /** 提交前已独立保存意图；提交异常时保留commitStarted，不尝试用回滚掩盖结果。 */
    public void commitDdlTransaction() throws SQLException {
        if (transactionOwner != null) {
            transactionOwner.commitDdlTransaction();
            return;
        }
        // 先标记已经开始提交，再调用JDBC；即使commit抛异常，也不能假定事务没提交而继续回滚或删表。
        commitStarted = true;
        ModelPublishTimingUtils.ModelPublishTiming commitTiming = ModelPublishTimingUtils.start("COMMIT_DDL_TRANSACTION", null, null);
        try {
            transactionConnection.commit();
        } catch (SQLException | RuntimeException | Error ex) {
            commitTiming.failed(ex);
            throw ex;
        } finally {
            commitTiming.close();
        }
    }

    /** 回滚正常返回后仍由执行服务重新核对原结构，才能登记已回滚。 */
    public void rollbackDdlTransaction() throws SQLException {
        // 只记录调用耗时，finally确保提前返回或异常时也结束计时；原事务注解和异常继续保留。
        ModelPublishTimingUtils.ModelPublishTiming timing = ModelPublishTimingUtils.start("ROLLBACK_DDL_TRANSACTION", null, null);
        try {
            transactionConnection.rollback();
        } catch (SQLException | RuntimeException | Error ex) {
            timing.failed(ex);
            throw ex;
        } finally {
            timing.close();
        }
    }

    /** 关闭本次自有资源；不重设autoCommit，以免意外提交未知事务。 */
    @Override
    public void close() {
        // 借用会话不拥有真实连接，关闭时直接返回；只有原会话负责释放整批连接和查询器。
        if (transactionOwner != null || closed) {
            return;
        }
        closed = true;
        try {
            if (transactionConnection != null) {
                transactionConnection.close();
            }
        } catch (SQLException ex) {
            throw new ServiceException("关闭业务库事务连接失败，需核对原实例");
        } finally {
            if (!sharedQuery) {
                originalQuery.close();
            }
        }
    }

    /**
     * 只在一次同步发布内复用目标查询器及普通自动提交连接，结束后统一释放。
     * 不缓存模型结构，不把连接放入业务上下文或JSON，也不跨请求共享资源。
     */
    @Slf4j
    public static class ModelPublishTargetResourceScope implements AutoCloseable {

        /** 每个发布线程各自持有资源，关闭时恢复上层作用域，避免线程复用时串到下一次请求。 */
        private static final ThreadLocal<ModelPublishTargetResourceScope> CURRENT = new ThreadLocal<>();
        /** 嵌套调用时保留上层资源，不让子调用提前关闭上层发布。 */
        private ModelPublishTargetResourceScope previous;
        /** 每个数据源当前使用的配置；参数变化时切换到新资源，旧资源仍由本作用域最终关闭。 */
        private Map<Long, ModelPublishTargetResource> active = new LinkedHashMap<>();
        /** 本次创建过的全部资源，包含中途更换账号等参数之前的旧资源。 */
        private List<ModelPublishTargetResource> resources = new ArrayList<>();
        /** 避免CLOSE阶段与入口finally重复释放。 */
        private boolean closed;

        /** 只允许统一入口通过begin创建作用域。 */
        private ModelPublishTargetResourceScope() {
            previous = CURRENT.get();
            CURRENT.set(this);
        }

        /** 开始本次发布的资源作用域；不提前连接业务库。 */
        public static ModelPublishTargetResourceScope begin() {
            return new ModelPublishTargetResourceScope();
        }

        /** 读取当前入口资源；单独调用内部接口时为空，仍沿用原来的局部自有资源方式。 */
        public static ModelPublishTargetResourceScope current() {
            return CURRENT.get();
        }

        /** 正式保存成功后先释放目标资源，再追加CLOSE日志；入口finally再次关闭不会重复操作。 */
        public static void closeCurrent() {
            ModelPublishTargetResourceScope scope = CURRENT.get();
            if (scope != null) {
                scope.close();
            }
        }

        /**
         * 数据源有效性仍由openTarget每次查询；这里只复用连接参数完全相同的资源。
         * 参数含账号密码，仅在服务端内存比较，不输出或保存到日志、配置快照。
         */
        public ModelPublishTargetResource acquire(Long datasourceId, DbQueryProperty property, DbDialect dialect,
                                                   Supplier<DbQuery> factory) {
            if (closed) {
                throw new ServiceException("本次发布的目标资源作用域已关闭");
            }
            ModelPublishTargetResource resource = active.get(datasourceId);
            JSONObject configuration = JSON.parseObject(JSON.toJSONString(property));
            if (resource == null || !Objects.equals(resource.configuration, configuration)) {
                resource = new ModelPublishTargetResource(datasourceId, configuration, factory.get(), property, dialect);
                resources.add(resource);
                active.put(datasourceId, resource);
            }
            return resource;
        }

        /** 所有资源都尝试关闭；即使其中一个关闭失败也清理线程引用，错误不带连接凭证。 */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            boolean failed = false;
            try {
                for (int index = resources.size() - 1; index >= 0; index--) {
                    try {
                        resources.get(index).close();
                    } catch (RuntimeException ex) {
                        failed = true;
                    }
                }
            } finally {
                active.clear();
                resources.clear();
                if (previous == null) {
                    CURRENT.remove();
                } else {
                    CURRENT.set(previous);
                }
                previous = null;
            }
            if (failed) {
                throw new ServiceException("发布目标资源关闭未完成，请核对服务端日志");
            }
        }

        /** 同一份连接配置的查询资源，事务专用连接仍由ModelPublishDatabaseSession单独创建和关闭。 */
        public static class ModelPublishTargetResource implements AutoCloseable {

            /** 仅用于关闭失败日志定位，不输出连接配置。 */
            private Long datasourceId;
            /** 独立复制的配置，用来识别中途修改账号等参数的情况。 */
            private JSONObject configuration;
            /** common创建的原查询器，负责按既有驱动配置创建真实连接。 */
            private DbQuery originalQuery;
            /** 复用原有查询实现，普通SQL和JdbcTemplate都借用同一个自动提交连接。 */
            private DbQuery query;
            /** 普通自动提交连接，与整批DDL事务连接始终分开。 */
            private Connection readConnection;
            /** suppressClose借用句柄，让下层try-with-resources不提前关闭普通连接。 */
            private SingleConnectionDataSource readSource;
            /** 记录本次打开过的普通连接，异常后废弃的连接也在最终关闭时再次确认释放。 */
            private List<Connection> connections = new ArrayList<>();
            /** 关闭后不再创建新连接。 */
            private boolean closed;

            /** 只组装查询器；第一次身份核验或实际查询时才创建普通连接。 */
            private ModelPublishTargetResource(Long datasourceId, JSONObject configuration, DbQuery originalQuery,
                                              DbQueryProperty property, DbDialect dialect) {
                this.datasourceId = datasourceId;
                this.configuration = configuration;
                this.originalQuery = originalQuery;
                try {
                    AbstractDataSource source = new AbstractDataSource() {
                        /** JdbcTemplate与直接JDBC统一使用本次普通连接，不触发平台事务。 */
                        @Override
                        public Connection getConnection() throws SQLException {
                            return borrowConnection();
                        }

                        /** 查询器只使用数据源配置中的账号，不在调用过程中替换凭证。 */
                        @Override
                        public Connection getConnection(String username, String password) throws SQLException {
                            return getConnection();
                        }
                    };
                    AbstractDbQueryFactory shared = new AbstractDbQueryFactory() {
                        /** 直接JDBC也走同一借用入口，兼容common中DM等数据库的连接分支。 */
                        @Override
                        public Connection getConnection() {
                            try {
                                return source.getConnection();
                            } catch (SQLException ex) {
                                throw new ServiceException("本次发布的目标连接不可用");
                            }
                        }

                        /** 本查询器不拥有原数据源，由外层资源作用域统一关闭。 */
                        @Override
                        public void close() {
                            // 借用查询器不关闭物理连接或原查询器。
                        }
                    };
                    shared.setDataSource(source);
                    shared.setJdbcTemplate(new JdbcTemplate(source));
                    shared.setDbDialect(dialect);
                    shared.setDbQueryProperty(property);
                    query = shared;
                } catch (RuntimeException | Error ex) {
                    originalQuery.close();
                    throw ex;
                }
            }

            /** 返回借用查询器，结构信息仍在每次调用时真实读取，不增加结构缓存。 */
            public DbQuery getQuery() {
                return query;
            }

            /** 新会话只借用普通查询器；开启整批事务时使用originalQuery另外创建真实事务连接。 */
            public ModelPublishDatabaseSession session(DbQueryProperty property, DbDialect dialect, ModelPublishTarget target) {
                return new ModelPublishDatabaseSession(query, originalQuery, property, dialect, target, this::invalidateConnection);
            }

            /** 普通查询不改变提交模式；保留现有驱动连接方式，不要求旧驱动实现额外的校验API。 */
            private Connection borrowConnection() throws SQLException {
                if (closed) {
                    throw new SQLException("本次发布的目标资源已关闭");
                }
                if (readConnection == null || readConnection.isClosed()) {
                    try {
                        readConnection = originalQuery.getConnection();
                        connections.add(readConnection);
                        if (!readConnection.getAutoCommit()) {
                            throw new SQLException("普通发布连接必须使用自动提交，不能混入其他事务");
                        }
                        readSource = new SingleConnectionDataSource(readConnection, true);
                    } catch (SQLException | RuntimeException ex) {
                        // 初始化未完成时不留下半个借用会话，后续恢复仍能重新创建普通连接。
                        invalidateConnection();
                        throw ex;
                    }
                }
                return readSource.getConnection();
            }

            /** SQL或核验读取异常后废弃普通连接，后面的只读核验会重新连接，不在旧异常会话上继续判断。 */
            private void invalidateConnection() {
                Connection connection = readConnection;
                readConnection = null;
                readSource = null;
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException | RuntimeException ex) {
                        log.warn("关闭异常发布目标连接失败，datasourceId={}", datasourceId);
                    }
                }
            }

            /** 关闭所有普通物理连接，再关闭common原查询器；不设置autoCommit，不尝试提交未知事务。 */
            @Override
            public void close() {
                if (closed) {
                    return;
                }
                closed = true;
                boolean failed = false;
                try {
                    for (Connection connection : connections) {
                        try {
                            connection.close();
                        } catch (SQLException | RuntimeException ex) {
                            failed = true;
                            log.warn("关闭发布目标连接失败，datasourceId={}", datasourceId);
                        }
                    }
                } finally {
                    connections.clear();
                    readConnection = null;
                    readSource = null;
                    try {
                        originalQuery.close();
                    } catch (RuntimeException ex) {
                        failed = true;
                        log.warn("关闭发布目标查询器失败，datasourceId={}", datasourceId);
                    }
                }
                if (failed) {
                    throw new ServiceException("发布目标资源释放失败");
                }
            }
        }
    }
}
