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

package tech.qiantong.qdata.mdm.system.service.monitor.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.system.core.config.MonitorProperties;
import tech.qiantong.qdata.mdm.system.service.monitor.IResourceServerService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @program: qData-mdm
 * @author: zyg
 * @create: 2026-07-01 15:08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class IResourceServerServiceImpl implements IResourceServerService {
    @Resource
    private MonitorProperties monitorProperties;

    // 缓存DS token
    private String dsToken;
    private long tokenExpireTime;

    /**
     * DS\SPARK\FLINK\MQ资源监控返回
     */
    /**
     * DS\SPARK\MQ资源监控统一返回
     */
    @Override
    public Map<String, Object> resourceResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("collectTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 并行采集
        CompletableFuture<Map<String, Object>> dsFuture = CompletableFuture.supplyAsync(this::collectDsInfo);
        CompletableFuture<Map<String, Object>> mqFuture = CompletableFuture.supplyAsync(this::collectRabbitMqInfo);
        CompletableFuture<Map<String, Object>> sparkFuture = CompletableFuture.supplyAsync(this::collectSparkInfo);

        result.put("dolphinScheduler", dsFuture.join());
        result.put("rabbitMq", mqFuture.join());
        result.put("spark", sparkFuture.join());

        return result;
    }

    // ====================== 1. DolphinScheduler 监控采集（4个monitor接口） ======================
    private Map<String, Object> collectDsInfo() {
        Map<String, Object> dsResult = new HashMap<>();
        MonitorProperties.DsConfig dsConfig = monitorProperties.getDs();
        Integer timeout = dsConfig.getTimeout() == null ? 5000 : dsConfig.getTimeout();

        // 基础配置校验
        if (StrUtil.isBlank(dsConfig.getHost()) || StrUtil.isBlank(dsConfig.getPort()) || StrUtil.isBlank(dsConfig.getToken())) {
            dsResult.put("health", "WARN");
            dsResult.put("errMsg", "DolphinScheduler配置缺失：host/port/token不能为空");
            return dsResult;
        }
        String baseUrl = String.format("http://%s:%s/dolphinscheduler/monitor/", dsConfig.getHost(), dsConfig.getPort());
        String tokenHeaderValue = dsConfig.getToken();

        try {
            // 并行请求4个监控接口
            CompletableFuture<String> masterFuture = sendDsMonitorRequest(baseUrl + "MASTER", tokenHeaderValue, timeout);
            CompletableFuture<String> workerFuture = sendDsMonitorRequest(baseUrl + "WORKER", tokenHeaderValue, timeout);
            CompletableFuture<String> alertFuture = sendDsMonitorRequest(baseUrl + "ALERT_SERVER", tokenHeaderValue, timeout);
            CompletableFuture<String> dbFuture = sendDsMonitorRequest(baseUrl + "databases", tokenHeaderValue, timeout);

            // 封装接口返回JSON对象存入结果
            dsResult.put("health", "NORMAL");
            dsResult.put("errMsg", "");
            dsResult.put("master", JSON.parseObject(masterFuture.join()));
            dsResult.put("worker", JSON.parseObject(workerFuture.join()));
            dsResult.put("alertServer", JSON.parseObject(alertFuture.join()));
            dsResult.put("database", JSON.parseObject(dbFuture.join()));
        } catch (Exception e) {
            log.error("DolphinScheduler监控接口采集异常", e);
            dsResult.put("health", "DOWN");
            dsResult.put("errMsg", "DS采集失败：" + e.getMessage());
        }
        return dsResult;
    }

    /**
     * 封装DS monitor接口统一GET请求
     * @param url 接口完整地址
     * @param token 请求头token值
     * @param timeout 超时时间
     * @return 接口原始响应文本
     */
    private CompletableFuture<String> sendDsMonitorRequest(String url, String token, Integer timeout) {
        return CompletableFuture.supplyAsync(() -> {
            HttpRequest request = HttpUtil.createRequest(Method.GET, url)
                    .header("token", token)
                    .timeout(timeout);
            return request.execute().body();
        });
    }

    // ====================== 2. RabbitMQ Prometheus监控指标采集 ======================
    private Map<String, Object> collectRabbitMqInfo() {
        Map<String, Object> mqResult = new HashMap<>();
        MonitorProperties.RabbitMqConfig mqConfig = monitorProperties.getRabbitmq();
        Integer timeout = mqConfig.getTimeout() == null ? 5000 : mqConfig.getTimeout();

        // 配置校验
        if (StrUtil.isBlank(mqConfig.getHost()) || StrUtil.isBlank(mqConfig.getUsername())) {
            mqResult.put("health", "WARN");
            mqResult.put("errMsg", "RabbitMQ配置缺失：host/username不能为空");
            return mqResult;
        }
        // 调用管理API overview接口
        String overviewUrl = String.format("http://%s:%d/api/overview", mqConfig.getHost(), mqConfig.getPort());

        try {
            // Basic Auth 编码，自动处理密码里^特殊符号
            String authRaw = mqConfig.getUsername() + ":" + mqConfig.getPassword();
            String base64Auth = Base64.encode(authRaw);

            String respBody = HttpUtil.createRequest(Method.GET, overviewUrl)
                    .header("Authorization", "Basic " + base64Auth)
                    .timeout(timeout)
                    .execute()
                    .body();

            // 判断返回是否为认证错误
            if (respBody.contains("\"error\":\"not_authorized\"")) {
                mqResult.put("health", "DOWN");
                mqResult.put("errMsg", "RabbitMQ账号密码鉴权失败");
                return mqResult;
            }

            // FastJSON 解析字符串为JSONObject
            JSONObject jsonObj = JSON.parseObject(respBody);

            mqResult.put("health", "NORMAL");
            mqResult.put("errMsg", "");
            // 基础版本信息
            mqResult.put("rabbitmqVersion", jsonObj.getString("rabbitmq_version"));
            mqResult.put("clusterName", jsonObj.getString("cluster_name"));
            mqResult.put("node", jsonObj.getString("node"));

            // 对象总数：连接、通道、队列、消费者
            JSONObject objectTotals = jsonObj.getJSONObject("object_totals");
            mqResult.put("connections", objectTotals.getInteger("connections"));
            mqResult.put("channels", objectTotals.getInteger("channels"));
            mqResult.put("queues", objectTotals.getInteger("queues"));
            mqResult.put("consumers", objectTotals.getInteger("consumers"));

            // 队列堆积消息
            JSONObject queueTotals = jsonObj.getJSONObject("queue_totals");
            mqResult.put("totalMessages", queueTotals.getLong("messages"));
            mqResult.put("readyMessages", queueTotals.getLong("messages_ready"));
            mqResult.put("unAckMessages", queueTotals.getLong("messages_unacknowledged"));

            // 消息收发统计
            JSONObject msgStats = jsonObj.getJSONObject("message_stats");
            mqResult.put("publishCount", msgStats.getLong("publishing"));
            mqResult.put("deliverCount", msgStats.getLong("deliver"));
            mqResult.put("ackCount",msgStats.getLong("ack"));
            mqResult.put("diskWrites", msgStats.getLong("disk_writes"));

        } catch (Exception e) {
            log.error("RabbitMQ Management API采集异常", e);
            mqResult.put("health", "DOWN");
            mqResult.put("errMsg", "MQ采集失败：" + e.getMessage());
        }
        return mqResult;
    }

    /**
     * 解析RabbitMQ Prometheus metrics文本，提取数值指标
     */
    private Map<String, Double> parseMqMetrics(String metricsText) {
        Map<String, Double> metrics = new HashMap<>();
        String[] lines = metricsText.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 过滤注释、空行
            if (StrUtil.isBlank(line) || line.startsWith("#")) {
                continue;
            }
            // 格式示例：rabbitmq_connections 12
            String[] kv = line.split(" ");
            if (kv.length >= 2) {
                String metricName = kv[0];
                try {
                    Double value = Double.parseDouble(kv[1]);
                    metrics.put(metricName, value);
                } catch (NumberFormatException ignore) {
                    // 非数值指标直接跳过
                }
            }
        }
        return metrics;
    }

    // ====================== 3. Spark 采集 ======================
    private Map<String, Object> collectSparkInfo() {
        Map<String, Object> sparkResult = new HashMap<>();
        try {
            String webUrl = monitorProperties.getSpark().getMasterWebUrl();
            Integer timeout = monitorProperties.getSpark().getTimeout() == null ? 3000 : monitorProperties.getSpark().getTimeout();

            if (webUrl == null || webUrl.trim().isEmpty()) {
                sparkResult.put("health", "WARN");
                sparkResult.put("errMsg", "Spark Master WebUI address is not configured.");
                return sparkResult;
            }

            String normalizedWebUrl = webUrl.replaceAll("/+$", "");

            // Spark Master WebUI exposes cluster state through /json.
            HttpRequest masterStateReq = HttpUtil.createRequest(Method.GET, normalizedWebUrl + "/json/")
                    .timeout(timeout);
            String masterStateResp = masterStateReq.execute().body();
            JSONObject masterState = JSON.parseObject(masterStateResp);

            sparkResult.put("health", "NORMAL");
            sparkResult.put("errMsg", "");
            sparkResult.put("clusterMode", "STANDALONE");
            sparkResult.put("masterUrl", masterState.getString("url"));
            sparkResult.put("status", masterState.getString("status"));
            sparkResult.put("aliveWorkers", masterState.getInteger("aliveworkers"));
            sparkResult.put("cores", masterState.getInteger("cores"));
            sparkResult.put("coresUsed", masterState.getInteger("coresused"));
            sparkResult.put("memory", masterState.getInteger("memory"));
            sparkResult.put("memoryUsed", masterState.getInteger("memoryused"));
            sparkResult.put("workerNodes", masterState.getJSONArray("workers"));
            sparkResult.put("runningApplications", masterState.getJSONArray("activeapps"));
            sparkResult.put("completedApplications", masterState.getJSONArray("completedapps"));
            sparkResult.put("runningDrivers", masterState.getJSONArray("activedrivers"));
            sparkResult.put("completedDrivers", masterState.getJSONArray("completeddrivers"));

        } catch (Exception e) {
            sparkResult.put("health", "DOWN");
            sparkResult.put("errMsg", "Collection failed: " + e.getMessage());
        }
        return sparkResult;
    }
}
