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

package tech.qiantong.qdata.mdm.dist.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFieldMappingDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFilterDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceLogDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskFieldMappingMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskFilterMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskInstanceLogMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskInstanceMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskMapper;
import tech.qiantong.qdata.mdm.dist.source.DistTaskRecordSource;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskExecutionService;
import tech.qiantong.qdata.mdm.dist.service.task.model.DeliveryResult;

/** 分发任务人工执行。每条记录独立请求，单条失败不会中断整批统计。 */
@Service
public class DistTaskExecutionService implements IDistTaskExecutionService {
    @Resource
    private DistTaskMapper taskMapper;
    @Resource
    private DistTaskFilterMapper filterMapper;
    @Resource
    private DistTaskFieldMappingMapper mappingMapper;
    @Resource
    private DistTaskInstanceMapper instanceMapper;
    @Resource
    private DistTaskInstanceLogMapper logMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private List<DistTaskRecordSource> recordSources;

    private final Set<Long> runningTasks = ConcurrentHashMap.newKeySet();

    /**
     * 人工执行一次任务，逐条发送主数据并记录执行实例和日志。
     *
     * @param taskId 分发任务 ID
     * @param operatorId 操作人 ID
     * @param operator 操作人姓名
     * @return 本次执行实例
     */
    @Override
    public DistTaskInstanceRespVO execute(Long taskId, Long operatorId, String operator) {
        if (taskId == null || !runningTasks.add(taskId)) throw new ServiceException("任务正在执行，请稍后查看执行记录");
        try {
            DistTaskDO task = taskMapper.selectById(taskId);
            if (task == null) throw new ServiceException("分发任务不存在");
            if (Long.valueOf(-1L).equals(task.getModelId()))
                throw new ServiceException("示例模型不能执行真实分发，请改选已发布主数据模型");
            if (!"ENABLED".equals(task.getStatus())) throw new ServiceException("未上线的任务不能执行");
            if (!"POST".equals(task.getHttpMethod())) throw new ServiceException("当前仅支持 POST 分发");
            List<DistTaskFieldMappingDO> mappings = mappingMapper.selectList(new LambdaQueryWrapper<DistTaskFieldMappingDO>()
                    .eq(DistTaskFieldMappingDO::getTaskId, taskId).orderByAsc(DistTaskFieldMappingDO::getSortOrder));
            if (mappings.isEmpty()) throw new ServiceException("请先配置分发字段");
            List<DistTaskFilterDO> filters = filterMapper.selectList(new LambdaQueryWrapper<DistTaskFilterDO>()
                    .eq(DistTaskFilterDO::getTaskId, taskId).orderByAsc(DistTaskFilterDO::getSortOrder));
            if ("CONDITION".equals(task.getDataScopeType()) && filters.isEmpty())
                throw new ServiceException("请先配置数据范围筛选条件");
            // 根据模型 ID 选择真实主数据取数适配器。
            DistTaskRecordSource source = recordSources.stream().filter(item -> item.supports(task.getModelId()))
                    .findFirst().orElseThrow(() -> new ServiceException("当前主数据模型不可用"));
            Map<String, String> headers = requestHeaders(task);
            JsonNode successCondition = json(task.getSuccessCondition(), "成功判定配置");
            Date started = new Date();
            DistTaskInstanceDO instance = new DistTaskInstanceDO();
            instance.setTaskId(taskId);
            instance.setBatchNo("DIST" + UUID.randomUUID().toString().replace("-", ""));
            instance.setTriggerType("MANUAL");
            instance.setRunStatus("RUNNING");
            instance.setStartTime(started);
            instance.setTotalCount(0L);
            instance.setSuccessCount(0L);
            instance.setFailureCount(0L);
            instance.setValidFlag("1");
            instance.setDelFlag("0");
            instance.setCreatorId(operatorId);
            instance.setCreateBy(operator);
            instance.setCreateTime(started);
            instanceMapper.insert(instance);

            long total = 0, succeeded = 0, failed = 0;
            try {
                for (DistTaskRecordSource.Record record : source.read(task, started)) {
                    if (!inRange(task, record, started) || !matches(filters, record)) continue;
                    total++;
                    String body = null;
                    DeliveryResult result;
                    try {
                        body = requestBody(mappings, record);
                        result = send(task.getApiUrl(), headers, body, successCondition);
                    } catch (Exception ex) {
                        result = new DeliveryResult(false, null,
                                ex.getMessage() == null ? "分发记录处理失败" : ex.getMessage());
                    }
                    if (result.isSuccess()) succeeded++; else failed++;
                    DistTaskInstanceLogDO log = new DistTaskInstanceLogDO();
                    log.setInstanceId(instance.getId());
                    log.setMasterRecordId(record.getId());
                    log.setMasterCode(record.getCode());
                    log.setMasterName(record.getName());
                    log.setDistributeStatus(result.isSuccess() ? "SUCCESS" : "FAILURE");
                    log.setDistributeTime(new Date());
                    log.setRequestData(body);
                    log.setResponseCode(result.getCode());
                    log.setResponseMessage(limit(result.getMessage(), 500));
                    log.setFailReason(result.isSuccess() ? null : limit(result.getMessage(), 500));
                    log.setRetryCount(0L);
                    log.setValidFlag("1");
                    log.setDelFlag("0");
                    log.setCreatorId(operatorId);
                    log.setCreateBy(operator);
                    log.setCreateTime(new Date());
                    logMapper.insert(log);
                }
                instance.setRunStatus(failed == 0 ? "SUCCESS" : "FAILED");
                if (failed > 0) instance.setErrorMessage("失败 " + failed + " 条，请查看失败明细");
            } catch (Exception ex) {
                instance.setRunStatus("FAILED");
                instance.setErrorMessage(limit(ex.getMessage() == null ? "任务执行异常" : ex.getMessage(), 500));
            }
            instance.setTotalCount(total);
            instance.setSuccessCount(succeeded);
            instance.setFailureCount(failed);
            instance.setEndTime(new Date());
            if ("SUCCESS".equals(instance.getRunStatus()) && "INCREMENTAL".equals(task.getDataMode())) {
                try {
                    int updated = taskMapper.update(null, new LambdaUpdateWrapper<DistTaskDO>()
                            .eq(DistTaskDO::getId, taskId).set(DistTaskDO::getLastSuccessTime, started));
                    if (updated != 1) throw new ServiceException("增量水位保存失败");
                } catch (Exception ex) {
                    instance.setRunStatus("FAILED");
                    instance.setErrorMessage("增量水位保存失败，请检查任务记录");
                }
            }
            instanceMapper.updateById(instance);
            return BeanUtils.toBean(instance, DistTaskInstanceRespVO.class);
        } finally {
            runningTasks.remove(taskId);
        }
    }

    /**
     * 判断记录是否位于本次增量执行的时间范围内。
     *
     * @param task 当前任务配置
     * @param record 待分发记录
     * @param cutoff 本批次开始时间
     * @return 全量记录或处于增量范围内时为 true
     */
    private boolean inRange(DistTaskDO task, DistTaskRecordSource.Record record, Date cutoff) {
        if (!"INCREMENTAL".equals(task.getDataMode())) return true;
        Date from = task.getLastSuccessTime() == null ? task.getIncrementalStartTime() : task.getLastSuccessTime();
        Date updated = record.getUpdateTime();
        return updated != null && (from == null || updated.after(from)) && !updated.after(cutoff);
    }

    /**
     * 按任务配置的全部筛选条件判断记录是否需要分发。
     *
     * @param filters 任务筛选条件
     * @param record 待分发记录
     * @return 全部条件满足时为 true
     */
    private boolean matches(List<DistTaskFilterDO> filters, DistTaskRecordSource.Record record) {
        for (DistTaskFilterDO filter : filters) {
            Object value = record.getFields().get(filter.getModelColumnId());
            String expected = filter.getFilterValue();
            String operator = filter.getOperator();
            if ("IS_NULL".equals(operator)) { if (value != null) return false; continue; }
            if ("IS_NOT_NULL".equals(operator)) { if (value == null) return false; continue; }
            if (value == null || expected == null) return false;
            String actual = String.valueOf(value);
            int order;
            try {
                order = value instanceof Number ? new BigDecimal(actual).compareTo(new BigDecimal(expected))
                        : actual.compareTo(expected);
            } catch (NumberFormatException ex) { return false; }
            switch (operator) {
                case "EQ": if (order != 0) return false; break;
                case "NE": if (order == 0) return false; break;
                case "CONTAINS": if (!actual.contains(expected)) return false; break;
                case "NOT_CONTAINS": if (actual.contains(expected)) return false; break;
                case "GT": if (order <= 0) return false; break;
                case "GE": if (order < 0) return false; break;
                case "LT": if (order >= 0) return false; break;
                case "LE": if (order > 0) return false; break;
                default: throw new ServiceException("筛选条件不受支持");
            }
        }
        return true;
    }

    /**
     * 按字段映射生成目标 API 接收的平铺 JSON 请求体。
     *
     * @param mappings 任务字段映射
     * @param record 待分发记录
     * @return JSON 请求体
     */
    private String requestBody(List<DistTaskFieldMappingDO> mappings, DistTaskRecordSource.Record record) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (DistTaskFieldMappingDO mapping : mappings) {
            if (!record.getFields().containsKey(mapping.getModelColumnId())) {
                throw new ServiceException("分发字段与主数据模型不一致");
            }
            body.put(mapping.getTargetFieldName(), record.getFields().get(mapping.getModelColumnId()));
        }
        try { return objectMapper.writeValueAsString(body); }
        catch (Exception ex) { throw new ServiceException("分发请求体生成失败"); }
    }

    /**
     * 组合自定义请求头与任务配置的认证请求头。
     *
     * @param task 当前任务配置
     * @return 发送请求所需的请求头
     */
    private Map<String, String> requestHeaders(DistTaskDO task) {
        Map<String, String> headers = new LinkedHashMap<>();
        JsonNode configured = json(task.getHeaderConfig(), "请求头配置");
        if (configured != null) {
            if (!configured.isArray()) throw new ServiceException("请求头配置格式不正确");
            for (JsonNode item : configured) headers.put(item.path("key").asText(), item.path("value").asText());
        }
        JsonNode auth = json(task.getAuthConfig(), "认证配置");
        if ("BEARER".equals(task.getAuthType())) {
            if (auth == null || auth.path("token").asText().isEmpty()) throw new ServiceException("认证配置不完整");
            headers.put("Authorization", "Bearer " + auth.path("token").asText());
        } else if ("API_KEY".equals(task.getAuthType())) {
            if (auth == null || auth.path("headerName").asText().isEmpty() || auth.path("key").asText().isEmpty())
                throw new ServiceException("认证配置不完整");
            headers.put(auth.path("headerName").asText(), auth.path("key").asText());
        }
        return headers;
    }

    /**
     * 解析任务配置中的 JSON，格式错误时转换为业务异常。
     *
     * @param value 待解析内容
     * @param label 配置名称
     * @return 解析后的 JSON 节点，空内容返回 null
     */
    private JsonNode json(String value, String label) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return objectMapper.readTree(value); }
        catch (Exception ex) { throw new ServiceException(label + "格式不正确"); }
    }

    /**
     * 向目标 API 发送单条记录，并判定 HTTP 和业务响应结果。
     *
     * @param address 目标 API 地址
     * @param headers 请求头
     * @param body JSON 请求体
     * @param condition 业务成功条件
     * @return 单条记录的发送结果
     */
    private DeliveryResult send(String address, Map<String, String> headers, String body, JsonNode condition) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            for (Map.Entry<String, String> header : headers.entrySet())
                connection.setRequestProperty(header.getKey(), header.getValue());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String response = readResponse(stream);
            if (status < 200 || status >= 300) return new DeliveryResult(false, String.valueOf(status),
                    "目标 API 返回 HTTP " + status + (response.isEmpty() ? "" : "：" + response));
            if (condition != null) {
                JsonNode result = json(response, "目标 API 响应");
                String field = condition.path("field").asText();
                String expected = condition.path("value").asText();
                String actual = result == null ? "" : result.path(field).asText();
                boolean equal = expected.equals(actual);
                if ("EQ".equals(condition.path("operator").asText()) ? !equal : equal)
                    return new DeliveryResult(false, String.valueOf(status), "目标 API 业务成功条件未满足：" + field);
            }
            return new DeliveryResult(true, String.valueOf(status), response);
        } catch (Exception ex) {
            return new DeliveryResult(false, null, "目标 API 调用失败：" + ex.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * 读取响应内容，最多保留前 4096 字节用于执行日志。
     *
     * @param stream 目标 API 响应流
     * @return UTF-8 响应文本
     * @throws Exception 响应读取失败时抛出
     */
    private String readResponse(InputStream stream) throws Exception {
        if (stream == null) return "";
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int total = 0, read;
            while (total < 4096 && (read = input.read(buffer, 0, Math.min(buffer.length, 4096 - total))) != -1) {
                output.write(buffer, 0, read);
                total += read;
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 将日志文本截断到数据库字段允许的长度。
     *
     * @param value 原始文本
     * @param length 最大字符数
     * @return 截断后的文本
     */
    private String limit(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }

}
