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

package tech.qiantong.qdata.mdm.common.database.datasource;

import cn.hutool.db.ds.simple.SimpleDataSource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.database.exception.DataQueryException;
import tech.qiantong.qdata.mdm.common.database.query.DefaultDbQueryFactoryBean;

import javax.sql.DataSource;
import javax.validation.constraints.Min;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 创建独占单个物理连接的短期查询会话，无连接池、全局缓存或 HTTP 请求依赖。
 * 同一会话只供单线程串行使用，调用方必须在 finally 中调用 close() 释放资源。
 * 维护写入事务仍使用原默认工厂及事务入口。
 */
@Component
@EnableConfigurationProperties(SessionDataSourceFactoryBean.ConnectionScopeProperties.class)
public class SessionDataSourceFactoryBean extends AbstractDataSourceFactory {
    private final ConnectionScopeProperties properties;

    public SessionDataSourceFactoryBean(ConnectionScopeProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        properties.validate();
    }

    @Override
    public DataSource createDataSource(DbQueryProperty property) {
        SimpleDataSource source = (SimpleDataSource) super.createDataSource(property);
        applyConnectionTimeouts(source, DbType.getDbType(property.getDbType()));
        return source;
    }

    /** JDBC 驱动的连接属性和时间单位适配；配置统一使用毫秒。 */
    private void applyConnectionTimeouts(SimpleDataSource source, DbType type) {
        int connectMillis = properties.getConnectTimeoutMs();
        int socketMillis = properties.getSocketTimeoutMs();
        String seconds = String.valueOf(Math.max(1L, (connectMillis + 999L) / 1000));
        switch (type) {
            case POSTGRE_SQL:
            case KINGBASE8:
                source.addConnProps("connectTimeout", seconds);
                source.addConnProps("socketTimeout", String.valueOf(Math.max(1L, (socketMillis + 999L) / 1000)));
                break;
            case ORACLE:
            case ORACLE_12C:
                source.addConnProps("oracle.net.CONNECT_TIMEOUT", String.valueOf(connectMillis));
                source.addConnProps("oracle.jdbc.ReadTimeout", String.valueOf(socketMillis));
                break;
            case SQL_SERVER:
            case SQL_SERVER2008:
                source.addConnProps("loginTimeout", seconds);
                source.addConnProps("socketTimeout", "net.sourceforge.jtds.jdbc.Driver".equals(source.getDriver())
                        ? String.valueOf(Math.max(1L, (socketMillis + 999L) / 1000)) : String.valueOf(socketMillis));
                break;
            case MYSQL:
            case MARIADB:

            case DM8:
                source.addConnProps("connectTimeout", String.valueOf(connectMillis));
                source.addConnProps("socketTimeout", String.valueOf(socketMillis));
                break;
            default:
                // 其他驱动保留原有连接属性；新增驱动时应补充建连和读取超时适配。
                break;
        }
    }

    @Override
    public DbQuery createDbQuery(DbQueryProperty property, DataSource source, DbType type) {
        if (!(source instanceof SimpleDataSource)) {
            throw new DataQueryException("连接会话只支持独立的 JDBC 数据源");
        }
        ConnectionScopedDbQuery query = new ConnectionScopedDbQuery((SimpleDataSource) source);
        try {
            // 先绑定资源归属，初始化或建立连接失败时仍由同一个对象清理。
            query.setDbQueryProperty(property);
            DbDialect dialect = DialectFactory.getDialect(type);
            if (dialect == null) {
                throw new DataQueryException("该数据库类型正在开发中");
            }
            query.setDbQueryProperty(property.copy());
            query.setDbDialect(dialect);
            query.openConnection(properties.getQueryTimeoutSeconds());
            return query;
        } catch (SQLException | RuntimeException ex) {
            try {
                query.close();
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            DataQueryException failure = new DataQueryException("建立数据库连接会话失败");
            failure.initCause(ex);
            throw failure;
        }
    }

    /** 超时配置与校验集中在工厂内部，也支持非 Spring 调用方显式构造。 */
    @Getter
    @Setter
    @Validated
    @ConfigurationProperties(prefix = "qdata.database.connection-scope")
    public static class ConnectionScopeProperties {
        @Min(1)
        private int connectTimeoutMs = 3000;
        @Min(1)
        private int socketTimeoutMs = 30000;
        @Min(1)
        private int queryTimeoutSeconds = 10;

        public void validate() {
            if (connectTimeoutMs < 1 || socketTimeoutMs < 1 || queryTimeoutSeconds < 1) {
                throw new IllegalArgumentException("数据库连接、读取和查询超时必须大于 0");
            }
        }
    }

    private static final class ConnectionScopedDbQuery extends DefaultDbQueryFactoryBean {
        private final SimpleDataSource ownedSource;
        private SingleConnectionDataSource session;
        private boolean closed;

        private ConnectionScopedDbQuery(SimpleDataSource ownedSource) {
            this.ownedSource = ownedSource;
        }

        private void openConnection(int queryTimeoutSeconds) throws SQLException {
            session = new SingleConnectionDataSource(ownedSource.getConnection(), true);
            setDataSource(session);
            setJdbcTemplate(new JdbcTemplate(session));
            jdbcTemplate.setQueryTimeout(queryTimeoutSeconds);
        }

        @Override
        public Connection getConnection() {
            if (closed) {
                throw new DataQueryException("数据库连接会话已关闭");
            }
            try {
                // 包括 DM 在内，所有元数据读取均使用会话连接。
                return session.getConnection();
            } catch (SQLException ex) {
                DataQueryException failure = new DataQueryException("获取会话数据库连接失败");
                failure.initCause(ex);
                throw failure;
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                if (session != null) {
                    session.destroy();
                }
            } finally {
                setDataSource(ownedSource);
                super.close();
            }
        }
    }
}
