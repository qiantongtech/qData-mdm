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

package tech.qiantong.qdata.mdm.system.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取yml后端监控配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    private DsConfig ds = new DsConfig();
    private RabbitMqConfig rabbitmq = new RabbitMqConfig();
    private SparkConfig spark = new SparkConfig();

    @Data
    public static class DsConfig {
        private String host;
        private String port;
        private String username;
        private String password;
        private Integer timeout;
        private String token;
    }

    @Data
    public static class RabbitMqConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private Integer  timeout;
    }

    @Data
    public static class SparkConfig {
        private String masterUrl;
        private String masterWebUrl;
        private String mainJar;
        private String mainClass;
        private String allDbMainClass;
        private Integer  timeout;
    }
}