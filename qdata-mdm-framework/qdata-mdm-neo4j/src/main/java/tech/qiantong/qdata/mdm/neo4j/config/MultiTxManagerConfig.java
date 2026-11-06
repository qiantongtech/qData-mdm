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

package tech.qiantong.qdata.mdm.neo4j.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-09-09 16:03
 **/
@Configuration
@EnableTransactionManagement
public class MultiTxManagerConfig {

    @Bean("transactionManager")
    @Primary
    public DataSourceTransactionManager jdbcTransactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    /**
     * Neo4j 事务管理器
     *
     * @param dbProvider
     * @return
     */
    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver dbProvider) {
        return new Neo4jTransactionManager(dbProvider);
    }
}
