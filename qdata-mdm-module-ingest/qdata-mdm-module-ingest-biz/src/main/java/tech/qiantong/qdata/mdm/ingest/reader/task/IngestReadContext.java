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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import lombok.Getter;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskMappingVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.INCREMENTAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_FIXED;

/**
 * 单次读取的配置与进度，避免在 Spring 单例读取器中共享任务状态。
 *
 * @author chentangwen
 */
@Getter
public class IngestReadContext {
    private final IngestTaskSaveReqVO task;
    /**
     * 最大回调条数，小于或等于 0 表示不限量。
     */
    private final int limit;
    /** 已持久化的排他起点；预览及首次采集为空。 */
    private final String currentIncrementValue;
    /** 按增量字段排序后最后一条完成回调的原始值。 */
    private Object lastIncrementValue;
    /**
     * 已正常完成回调的记录数，不等同于业务入库成功数。
     */
    private long readCount;
    /**
     * 读取阶段跳过的空记录数，不占用回调条数限制。
     */
    private long skipCount;

    /**
     * 创建本次读取独享的上下文。
     *
     * @param task 接入任务配置
     * @param limit 最大回调条数，小于或等于 0 时不限制
     */
    public IngestReadContext(IngestTaskSaveReqVO task, int limit) {
        this(task, limit, null);
    }

    public IngestReadContext(IngestTaskSaveReqVO task, int limit, String currentIncrementValue) {
        if (task == null) {
            throw new ServiceException("缺少接入任务配置");
        }
        this.task = task;
        this.limit = limit;
        this.currentIncrementValue = currentIncrementValue;
    }

    /**
     * 汇总映射、唯一标识和增量判断所需的来源列，固定值映射无需读取来源列。
     *
     * @return 去重且保持配置顺序的字段集合
     */
    public Set<String> requiredFields() {
        Set<String> fields = new LinkedHashSet<>();
        if (task.getMappings() != null) {
            for (IngestTaskMappingVO mapping : task.getMappings()) {
                if (!TRANSFORM_FIXED.equals(mapping.getTransformType()) && mapping.getSourceFieldName() != null) {
                    fields.add(mapping.getSourceFieldName());
                }
            }
        }
        if (task.getUniqueFields() != null) {
            fields.addAll(task.getUniqueFields());
        }
        if (INCREMENTAL.equals(task.getIngestMode()) && task.getDbConfig() != null) {
            fields.add(task.getDbConfig().getIncrementField());
        }
        return fields;
    }

    /**
     * 数据库或文件取得完整字段列表后，在回调前检查配置字段是否存在。
     *
     * @param fields 来源实际字段列表
     */
    public void validateFields(List<String> fields) {
        Set<String> missing = requiredFields();
        if (missing.contains(null) || missing.stream().anyMatch(field -> field.trim().isEmpty())) {
            throw new ServiceException("来源字段配置不能为空");
        }
        missing.removeAll(fields);
        if (!missing.isEmpty()) {
            throw new ServiceException("来源字段不存在：" + String.join("、", missing));
        }
    }

    /**
     * 判断已回调记录数是否达到本次读取上限。
     */
    public boolean reachedLimit() {
        return limit > 0 && readCount >= limit;
    }

    /**
     * 累计一条由读取器跳过的空记录，供执行服务汇总运行统计。
     */
    public void skip() {
        skipCount++;
    }

    /**
     * 检查中断、提取标识及增量值并同步回调；回调抛出异常时不增加读取计数。
     * 调用方负责在调用前检查条数限制。
     *
     * @param values 来源字段及其值
     * @param location 来源位置描述
     * @param consumer 逐条处理回调
     */
    public void emit(Map<String, Object> values, String location, Consumer<IngestRow> consumer) {
        if (Thread.currentThread().isInterrupted()) {
            throw new ServiceException("读取已中断，位置：" + location);
        }
        Map<String, Object> identity = new LinkedHashMap<>();
        for (String field : task.getUniqueFields() == null ? Collections.<String>emptyList() : task.getUniqueFields()) {
            identity.put(field, values.get(field));
        }
        Object increment = INCREMENTAL.equals(task.getIngestMode()) && task.getDbConfig() != null
                ? values.get(task.getDbConfig().getIncrementField()) : null;
        consumer.accept(new IngestRow(values, location, identity, increment));
        lastIncrementValue = increment;
        readCount++;
    }
}
