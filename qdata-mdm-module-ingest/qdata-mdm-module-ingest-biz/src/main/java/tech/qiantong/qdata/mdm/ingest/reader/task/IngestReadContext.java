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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.Getter;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskMappingVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.INCREMENTAL;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.TRANSFORM_FIXED;

/**
 * 单次来源读取的配置、回调进度和候选增量位置。
 *
 * <p>由数据库、文件和 API 读取器共用，负责来源字段收集、字段存在性校验、记录封装和读取统计。
 * 字段转换、入库成功统计、逐行错误策略及增量位置持久化由执行服务负责。</p>
 *
 * <p>本类包含可变状态，仅供一次同步读取使用，不支持并行回调。
 * 任务配置按引用保存，调用方须在读取开始前完成配置，读取期间不得修改配置。
 * 重用上下文不会清零计数或增量位置，是否允许重试由调用方决定。</p>
 *
 * @author chentangwen
 */
@Getter
public class IngestReadContext {

    /** 本次读取使用的任务配置，持有原始引用而非配置快照。 */
    private final IngestTaskSaveReqVO task;
    /**
     * 最大回调条数，小于或等于 0 表示不限量。
     */
    private final int limit;
    /** 是否为页面预览，仅影响配置提示级别。 */
    private final boolean preview;
    /** 已持久化的排他起点；预览及首次采集为空。 */
    private final String currentIncrementValue;
    /** 最后一条完成回调记录的增量原值，仅为候选位置，不代表入库成功或已持久化。 */
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
     * @throws ServiceException 任务配置为空时抛出
     */
    public IngestReadContext(IngestTaskSaveReqVO task, int limit) {
        this(task, limit, null);
    }

    /**
     * 创建带已持久化增量起点的读取上下文，所有统计从零开始。
     *
     * @param task 读取期间不再修改的任务配置
     * @param limit 最大回调条数，小于或等于 0 时不限制
     * @param currentIncrementValue 已保存的排他增量起点；无已保存位置时为 {@code null}
     * @throws ServiceException 任务配置为空时抛出
     */
    public IngestReadContext(IngestTaskSaveReqVO task, int limit, String currentIncrementValue) {
        this(task, limit, currentIncrementValue, false);
    }

    public IngestReadContext(IngestTaskSaveReqVO task, int limit, String currentIncrementValue, boolean preview) {
        if (task == null) {
            throw new ServiceException("缺少接入任务配置");
        }
        this.task = task;
        this.limit = limit;
        this.preview = preview;
        this.currentIncrementValue = currentIncrementValue;
    }

    /**
     * 汇总映射、唯一标识和增量判断所需的来源字段。
     *
     * <p>仅已选择目标字段的固定值映射不依赖来源；未选择目标字段时不执行转换规则，
     * 仍保留已配置的来源字段。字段名保持原值，不自动裁剪空格或转换大小写。</p>
     *
     * @return 去重且保持配置顺序的独立可变集合，修改它不会影响任务配置
     * @throws ServiceException 映射项为空或所需字段名为空白时抛出
     */
    public Set<String> requiredFields() {
        Set<String> fields = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(task.getMappings())) {
            for (IngestTaskMappingVO mapping : task.getMappings()) {
                if (ObjectUtil.isNull(mapping)) {
                    throw new ServiceException("字段映射配置不能为空");
                }
                boolean fixed = ObjectUtil.isNotNull(mapping.getTargetFieldId())
                        && TRANSFORM_FIXED.equals(mapping.getTransformType());
                if (!fixed && (ObjectUtil.isNotNull(mapping.getTargetFieldId())
                        || ObjectUtil.isNotNull(mapping.getSourceFieldName()))) {
                    fields.add(mapping.getSourceFieldName());
                }
            }
        }
        if (CollectionUtils.isNotEmpty(task.getUniqueFields())) {
            fields.addAll(task.getUniqueFields());
        }
        if (INCREMENTAL.equals(task.getIngestMode()) && ObjectUtil.isNotNull(task.getDbConfig())) {
            fields.add(task.getDbConfig().getIncrementField());
        }
        if (fields.contains(null) || fields.stream().anyMatch(field -> field.trim().isEmpty())) {
            throw new ServiceException("来源字段配置不能为空");
        }
        return fields;
    }

    /**
     * 数据库或文件取得完整字段列表后，在回调前检查配置字段是否存在。
     * API 记录结构可能变化，其字段取值校验由逐行处理完成，不应使用本方法校验路径。
     *
     * @param fields 来源实际字段列表
     * @throws ServiceException 字段列表为空引用、包含空字段，或配置所需字段不存在时抛出
     */
    public void validateFields(List<String> fields) {
        if (ObjectUtil.isNull(fields)
                || fields.stream().anyMatch(field -> ObjectUtil.isNull(field) || field.trim().isEmpty())) {
            throw new ServiceException("来源字段列表不能为空或包含空字段");
        }
        Set<String> missing = requiredFields();
        missing.removeAll(fields);
        if (CollectionUtils.isNotEmpty(missing)) {
            throw new ServiceException("来源字段不存在：" + String.join("、", missing));
        }
    }

    /**
     * 判断已回调记录数是否达到本次读取上限。
     *
     * @return 设置了正数上限且已完成回调数达到上限时返回 {@code true}
     */
    public boolean reachedLimit() {
        return limit > 0 && readCount >= limit;
    }

    /**
     * 累计一条由读取器跳过的空记录，供执行服务汇总运行统计。
     * 不增加回调计数，不更新候选增量位置，不占用读取条数上限。
     */
    public void skip() {
        skipCount++;
    }

    /**
     * 检查中断并提取增量原值，封装来源记录后同步回调。
     *
     * <p>回调正常返回后才增加读取计数并更新候选增量位置，异常原样上抛且不推进状态。
     * 回调正常返回不等于入库成功；执行服务须结合整体运行结果决定是否保存候选增量位置。</p>
     *
     * <p>调用方负责在调用前检查条数限制。本方法不复制来源 Map，回调应按只读记录使用。</p>
     *
     * @param values 来源字段 Map，不可为 {@code null}，允许无字段
     * @param location 来源位置描述，允许为 {@code null}
     * @param consumer 非空的逐条处理回调
     * @throws ServiceException 来源记录或回调为空，或读取线程已中断
     */
    public void emit(Map<String, Object> values, String location, Consumer<IngestRow> consumer) {
        if (Thread.currentThread().isInterrupted()) {
            throw new ServiceException("读取已中断，位置：" + location);
        }
        if (ObjectUtil.isNull(values) || ObjectUtil.isNull(consumer)) {
            throw new ServiceException("来源记录和处理回调不能为空");
        }
        Object increment = INCREMENTAL.equals(task.getIngestMode()) && ObjectUtil.isNotNull(task.getDbConfig())
                ? values.get(task.getDbConfig().getIncrementField()) : null;
        consumer.accept(new IngestRow(values, location, increment));
        // 回调异常时不会执行到这里，避免把未完成处理的记录计入进度。
        lastIncrementValue = increment;
        readCount++;
    }
}
