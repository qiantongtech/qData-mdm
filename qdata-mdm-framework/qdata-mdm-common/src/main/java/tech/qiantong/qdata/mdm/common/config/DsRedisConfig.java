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

package tech.qiantong.qdata.mdm.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Purpose: Read Redis related configuration
 * </p>
 *
 * <p>Corresponding to the configuration of ds.redis.* in application.yml</p>
 *
 * @author MING
 * @since 2025-04-28
 */
@Data
@Component
@ConfigurationProperties(prefix = "ds.redis")
public class DsRedisConfig {

    /**
     * Redis service address
     */
    private String host;

    /**
     * Redis service port
     */
    private Integer port;

    /**
     * Database index used by Redis
     */
    private Integer database;

    /**
     * Redis password
     */
    private String password;
}
