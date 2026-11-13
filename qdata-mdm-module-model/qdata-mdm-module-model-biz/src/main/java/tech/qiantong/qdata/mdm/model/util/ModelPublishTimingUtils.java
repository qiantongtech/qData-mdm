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

import lombok.extern.slf4j.Slf4j;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedStructure;
import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedTableDefinition;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishContext;
import tech.qiantong.qdata.mdm.model.domain.ModelSqlExecutionStep;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 只统计当前同步发布的耗时，不参与业务判断、不写平台表，也不保存SQL原文或连接参数。
 * 使用nanoTime计算经过时间；阶段包含下层调用，不能把父子阶段耗时直接相加。
 */
@Slf4j
public final class ModelPublishTimingUtils {

    /**
     * 每个发布线程独立统计，入口结束后移除，避免线程复用时串到下一次发布。
     */
    private static final ThreadLocal<ModelPublishTimingStatistics> CURRENT = new ThreadLocal<>();

    /**
     * 非发布流程调用公共工具时直接跳过计时，不产生独立统计或日志。
     */
    private static final ModelPublishTiming EMPTY = new ModelPublishTiming(null, null, null, null, false);

    /**
     * 工具类不需要创建对象。
     */
    private ModelPublishTimingUtils() {
    }

    /**
     * 从统一入口开始一次统计；只有创建统计的这一层负责输出汇总并清理线程数据。
     */
    public static ModelPublishTiming begin(Long modelId, Long datasourceId) {
        if (CURRENT.get() != null) {
            return start("嵌套发布调用", modelId, null);
        }
        ModelPublishTimingStatistics statistics = new ModelPublishTimingStatistics();
        statistics.modelId = modelId;
        statistics.datasourceId = datasourceId;
        CURRENT.set(statistics);
        log.info("[模型发布开始] modelId={}, datasourceId={}", modelId, datasourceId);
        return new ModelPublishTiming(statistics, "发布总流程", null, null, true);
    }

    /**
     * 用当前发布上下文记录一个阶段；实例ID可能在准备过程中才生成，所以结束时再同步一次。
     */
    public static ModelPublishTiming start(String phase, ModelPublishContext context) {
        bind(context);
        return start(phase, context == null ? null : context.getModelId(), context == null ? null : context.getInstanceId());
    }

    /**
     * 按已知的模型和实例记录阶段，不依赖平台查询，也不触发事务。
     */
    public static ModelPublishTiming start(String phase, Long modelId, Long instanceId) {
        ModelPublishTimingStatistics statistics = CURRENT.get();
        if (statistics == null) {
            return EMPTY;
        }
        if (statistics.modelId == null) {
            statistics.modelId = modelId;
        }
        if (instanceId != null) {
            statistics.instanceId = instanceId;
        }
        return new ModelPublishTiming(statistics, phase, null, null, false);
    }

    /**
     * 平台记录步骤可以早于上下文创建，直接借用实例信息计时；不会修改这个实例对象。
     */
    public static ModelPublishTiming startRecord(String phase, ModelInstanceDO record) {
        ModelPublishTiming timing = start(phase, record == null ? null : record.getModelId(), record == null ? null : record.getId());
        if (timing.statistics != null) {
            timing.record = record;
        }
        return timing;
    }

    /**
     * 记录单条SQL的调用耗时；只输出步骤号、方向和动作类型，不输出语句或业务数据。
     */
    public static ModelPublishTiming startStep(String phase, ModelPublishContext context, ModelSqlExecutionStep step) {
        bind(context);
        ModelPublishTimingStatistics statistics = CURRENT.get();
        if (statistics == null) {
            return EMPTY;
        }
        return new ModelPublishTiming(statistics, phase, step == null ? null : step.getStepNo(),
                step == null || step.getOperation() == null ? null : step.getOperation().name(), false);
    }

    /**
     * 保存当前上下文引用以便汇总数量和最终状态；不复制或序列化整份配置和执行计划。
     */
    public static void bind(ModelPublishContext context) {
        ModelPublishTimingStatistics statistics = CURRENT.get();
        if (statistics != null && context != null) {
            statistics.context = context;
            if (context.getInstanceId() != null) {
                statistics.instanceId = context.getInstanceId();
            }
            if (context.getDatasourceId() != null) {
                statistics.datasourceId = context.getDatasourceId();
            }
        }
    }

    /**
     * 一个阶段的计时句柄。放在finally里关闭，提前返回或抛异常时也会记录耗时。
     * 它只负责日志，不持有数据库资源，不提交或回滚事务。
     */
    public static class ModelPublishTiming implements AutoCloseable {

        /**
         * 本次发布的统计；为空表示当前不在发布入口内。
         */
        private ModelPublishTimingStatistics statistics;
        /**
         * 阶段名称，或正向/逆向SQL调用。
         */
        private String phase;
        /**
         * 单条SQL的计划步骤号；普通阶段为空。
         */
        private Integer stepNo;
        /**
         * SQL动作类型，例如CREATE_TABLE；不包含SQL原文。
         */
        private String operation;
        /**
         * 是否由当前句柄创建了整次发布统计。
         */
        private boolean owner;
        /**
         * 计时开始点，只用于计算差值，不是业务时间戳。
         */
        private long startedNanos;
        /**
         * 防止重复关闭时重复计数。
         */
        private boolean closed;
        /**
         * 阶段直接抛出的异常类型，只记录类名，不记录可能带凭证的异常正文。
         */
        private String failureType;
        /**
         * 早期步骤创建的发布记录，结束时从中补取生成的实例ID。
         */
        private ModelInstanceDO record;

        /**
         * 内部统一创建计时句柄。
         */
        private ModelPublishTiming(ModelPublishTimingStatistics statistics, String phase, Integer stepNo,
                                   String operation, boolean owner) {
            this.statistics = statistics;
            this.phase = phase;
            this.stepNo = stepNo;
            this.operation = operation;
            this.owner = owner;
            this.startedNanos = System.nanoTime();
        }

        /**
         * 标记该调用以异常退出；保留原异常的传播，不把计时结果当成业务发布状态。
         */
        public void failed(Throwable failure) {
            if (statistics != null && failure != null) {
                failureType = failure.getClass().getSimpleName();
            }
        }

        /**
         * 输出调用耗时；入口句柄最后统一输出数量、各阶段累计耗时，并清理ThreadLocal。
         */
        @Override
        public void close() {
            if (statistics == null || closed) {
                return;
            }
            closed = true;
            try {
                if (record != null && record.getId() != null) {
                    statistics.instanceId = record.getId();
                }
                long nanos = System.nanoTime() - startedNanos;
                if (owner) {
                    printSummary(nanos);
                } else {
                    ModelPublishPhaseTiming total = statistics.phases.computeIfAbsent(phase, key -> new ModelPublishPhaseTiming());
                    total.calls++;
                    total.nanos += nanos;
                    if (failureType != null) {
                        // 失败始终可见，避免关闭细分debug后漏掉异常发生的阶段或SQL步骤。
                        log.warn("[模型发布调用异常] modelId={}, instanceId={}, phase={}, stepNo={}, operation={}, elapsedMs={}, exit={}",
                                statistics.modelId, statistics.instanceId, phase, stepNo, operation,
                                TimeUnit.NANOSECONDS.toMillis(nanos), failureType);
                    } else if (stepNo != null) {
                        log.debug("[模型发布SQL耗时] modelId={}, instanceId={}, phase={}, stepNo={}, operation={}, elapsedMs={}, exit={}",
                                statistics.modelId, statistics.instanceId, phase, stepNo, operation,
                                TimeUnit.NANOSECONDS.toMillis(nanos), failureType == null ? "正常返回" : failureType);
                    } else if ("READ_PREPARE".equals(phase) || "PLAN".equals(phase) || "EXECUTE".equals(phase)
                            || "VERIFY_FINAL".equals(phase) || "COMMIT".equals(phase)) {
                        // 每个主要阶段只输出完成摘要，日常排查无需逐次打印进度保存和目标连接调用。
                        log.info("[模型发布阶段完成] modelId={}, instanceId={}, phase={}, elapsedMs={}",
                                statistics.modelId, statistics.instanceId, phase, TimeUnit.NANOSECONDS.toMillis(nanos));
                    } else {
                        log.debug("[模型发布调用耗时] modelId={}, instanceId={}, phase={}, elapsedMs={}, exit={}",
                                statistics.modelId, statistics.instanceId, phase, TimeUnit.NANOSECONDS.toMillis(nanos),
                                failureType == null ? "正常返回" : failureType);
                    }
                }
            } catch (RuntimeException ignored) {
                // 计时或日志异常不能盖住原业务异常，也不能改变已完成的发布结果。
            } finally {
                if (owner) {
                    CURRENT.remove();
                }
            }
        }

        /**
         * 返回某类调用次数；未进入的阶段按0统计。
         */
        private long calls(String name) {
            ModelPublishPhaseTiming total = statistics.phases.get(name);
            return total == null ? 0 : total.calls;
        }

        /** 读取已有阶段统计，不为打印摘要增加查询；父子阶段存在包含关系，不能直接相加。 */
        private long millis(String phase) {
            ModelPublishPhaseTiming timing = statistics.phases.get(phase);
            return timing == null ? 0 : TimeUnit.NANOSECONDS.toMillis(timing.nanos);
        }

        /**
         * 从本次已有上下文汇总规模；失败在构建前时数量为0，不额外查数据库补统计。
         */
        private void printSummary(long nanos) {
            ModelPublishContext context = statistics.context;
            ModelMaterializedStructure structure = context == null ? null : context.getTargetStructure();
            int sqlCount = 0;
            if (context != null && context.getExecutionPlan() != null) {
                structure = context.getExecutionPlan().getTargetStructure();
                sqlCount = context.getExecutionPlan().getSteps() == null ? 0 : context.getExecutionPlan().getSteps().size();
            }
            int tableCount = 0;
            int columnCount = 0;
            if (structure != null && structure.getTables() != null) {
                tableCount = structure.getTables().size();
                for (ModelMaterializedTableDefinition table : structure.getTables()) {
                    columnCount += table.getColumns() == null ? 0 : table.getColumns().size();
                }
            }
            log.info("[模型发布耗时汇总] modelId={}, instanceId={}, datasourceId={}, elapsedMs={}, exit={}, publishStatus={}, tableCount={}, columnCount={}, sqlCount={}, targetOpenCalls={}, targetCreateCalls={}, structureReadCalls={}, forwardSqlCalls={}, compensationSqlCalls={}",
                    statistics.modelId, statistics.instanceId, statistics.datasourceId, TimeUnit.NANOSECONDS.toMillis(nanos),
                    failureType == null ? "正常返回" : failureType, context == null ? null : context.getStatus(), tableCount, columnCount, sqlCount, calls("TARGET_OPEN"), calls("TARGET_CREATE"),
                    calls("READ_COLUMNS") + calls("READ_TABLE_METADATA") + calls("CHECK_INDEXES"),
                    calls("SQL_FORWARD"), calls("SQL_COMPENSATE"));
            log.info("[模型发布主要耗时] modelId={}, instanceId={}, progressSaveMs={}, sqlExecuteMs={}, targetValidateMs={}, finalVerifyMs={}",
                    statistics.modelId, statistics.instanceId, millis("SAVE_PROGRESS_TX"), millis("SQL_FORWARD"),
                    millis("VALIDATE_TARGET"), millis("VERIFY_FINAL"));
            // 只在入口结束时各输出一行细分汇总，避免每张表、每个字段都刷INFO日志。
            // 这些时间属于父阶段的一部分；包含重复调用累计值，不可再与父阶段相加。
            log.info("[模型发布TARGET细分耗时] modelId={}, instanceId={}, readExistingMs={}, checkLimitsMs={}, checkIndexNamesMs={}, checkExistingTableMs={}, checkAddedColumnsMs={}",
                    statistics.modelId, statistics.instanceId, millis("TARGET_READ_EXISTING"), millis("TARGET_CHECK_LIMITS"),
                    millis("TARGET_CHECK_INDEX_NAMES"), millis("TARGET_CHECK_EXISTING_TABLE"), millis("TARGET_CHECK_ADDED_COLUMNS"));
            log.info("[模型发布正式保存细分耗时] modelId={}, instanceId={}, readPendingMs={}, readInstanceMs={}, validateInstanceMs={}, parsePlanMs={}, validateStructureMs={}, readBindingsMs={}, saveMaterializedMs={}, saveVersionPointerMs={}, saveResultLogMs={}, commitBodyMs={}, commitTotalMs={}",
                    statistics.modelId, statistics.instanceId, millis("COMMIT_READ_PENDING"), millis("COMMIT_READ_INSTANCE"),
                    millis("COMMIT_VALIDATE_INSTANCE"), millis("COMMIT_PARSE_PLAN"), millis("COMMIT_VALIDATE_STRUCTURE"),
                    millis("COMMIT_READ_BINDINGS"), millis("SAVE_MATERIALIZED_BODY"), millis("COMMIT_SAVE_VERSION_POINTER"),
                    millis("COMMIT_SAVE_RESULT_LOG"), millis("COMMIT_BODY"), millis("COMMIT"));
            for (Map.Entry<String, ModelPublishPhaseTiming> entry : statistics.phases.entrySet()) {
                log.debug("[模型发布阶段耗时] modelId={}, instanceId={}, phase={}, calls={}, totalMs={}",
                        statistics.modelId, statistics.instanceId, entry.getKey(), entry.getValue().calls,
                        TimeUnit.NANOSECONDS.toMillis(entry.getValue().nanos));
            }
        }
    }

    /**
     * 只在当前发布线程内使用，入口结束即释放，不作为模型业务状态或持久化数据。
     */
    private static class ModelPublishTimingStatistics {
        /**
         * 本次主模型ID。
         */
        private Long modelId;
        /**
         * 本次发布实例ID，读取或创建后补齐。
         */
        private Long instanceId;
        /**
         * 本次请求的数据源ID。
         */
        private Long datasourceId;
        /**
         * 当前发布上下文引用，只读取标识、数量和最终状态。
         */
        private ModelPublishContext context;
        /**
         * 按阶段累计调用次数与耗时，保持首次出现的顺序。
         */
        private Map<String, ModelPublishPhaseTiming> phases = new LinkedHashMap<>();
    }

    /**
     * 一个阶段多次调用的累计值；父阶段包含子阶段，不与子阶段相加计算发布总耗时。
     */
    private static class ModelPublishPhaseTiming {
        /**
         * 阶段调用次数，包含异常退出的调用。
         */
        private long calls;
        /**
         * 累计经过时间，纳秒。
         */
        private long nanos;
    }
}
