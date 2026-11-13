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
package tech.qiantong.qdata.mdm.ingest.service.datasource.impl;

import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisRespVO;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/** 基础网络探测，供不同协议诊断器复用。 */
class DiagnosisNetworkChecker {

    private static final int NETWORK_TIMEOUT_MS = 3000;
    private final DiagnosisResultBuilder resultBuilder;

    DiagnosisNetworkChecker(DiagnosisResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    boolean checkDns(IngestDatasourceDiagnosisRespVO result, String host) {
        long startedAt = System.currentTimeMillis();
        try {
            InetAddress address = InetAddress.getByName(host);
            resultBuilder.addPass(result, "DNS_RESOLVE", "网络地址解析",
                    "域名/IP解析成功：" + address.getHostAddress(), startedAt);
            return true;
        } catch (Exception exception) {
            resultBuilder.addFailure(result, "DNS_RESOLVE", "网络地址解析", exception,
                    "NETWORK_DNS_FAILED", "无法定位数据源服务器");
            return false;
        }
    }

    boolean checkTcp(IngestDatasourceDiagnosisRespVO result, String host, Integer port) {
        long startedAt = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), NETWORK_TIMEOUT_MS);
            long duration = System.currentTimeMillis() - startedAt;
            if (duration > 1500L) {
                resultBuilder.addWarn(result, "TCP_CONNECT", "端口连通性",
                        "目标端口可以连接，但响应耗时较长（" + duration + "ms）",
                        "连接较慢可能导致任务启动或运行超时",
                        Collections.singletonList("检查应用服务器与数据源之间的网络延迟"), duration);
            } else {
                resultBuilder.addPass(result, "TCP_CONNECT", "端口连通性",
                        host + ":" + port + " 可以连接", startedAt);
            }
            return true;
        } catch (Exception exception) {
            resultBuilder.addFailure(result, "TCP_CONNECT", "端口连通性", exception,
                    exception instanceof SocketTimeoutException
                            ? "NETWORK_TIMEOUT" : "NETWORK_PORT_UNREACHABLE",
                    "无法建立数据库或存储服务连接");
            return false;
        }
    }

    EndpointAddress endpointAddress(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        String endpoint = firstConfigValue(config, "endpoint", "endPoint", "end_point");
        if (endpoint == null) {
            return null;
        }
        try {
            URI uri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
            String host = uri.getHost();
            int port = uri.getPort();
            if (port < 0) {
                port = "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
            }
            return host == null ? null : new EndpointAddress(host, port);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String firstConfigValue(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null && trimToNull(String.valueOf(value)) != null) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static class EndpointAddress {
        final String host;
        final Integer port;

        EndpointAddress(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
    }
}
