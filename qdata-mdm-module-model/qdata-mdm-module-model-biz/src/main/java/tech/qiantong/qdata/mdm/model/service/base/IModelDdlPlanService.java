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

package tech.qiantong.qdata.mdm.model.service.base;

import tech.qiantong.qdata.mdm.model.domain.ModelPublishContext;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishExecutionPlan;

/**
 * 统一整理DDL动作、生成SQL计划，并执行、恢复及回撤平台保存的计划。
 */
public interface IModelDdlPlanService {

    /**
     * 重新选择动作、核对目标并保存计划；失败保留旧成功版本。
     * <p>
     * 内部先调用结构服务完成CONFIG/BUILD/DIFF，再完成CHANGE/TARGET；无物理变更时保存空步骤计划，仍保留完整发布过程。
     */
    ModelPublishExecutionPlan preparePlan(ModelPublishContext context);

    /**
     * 从平台读取已保存计划，逐条执行并把结果暂存内存，结束或失败时统一保存结果和全流程日志。
     * 先选择整批DDL事务或普通执行模式，最后统一核验；全部通过仅进入待提交状态，不提前更新模型为已发布。
     * 旧计划保留原进度方式；中断实例需先经过恢复核实，不能直接重发整批SQL。
     */
    ModelPublishExecutionPlan executePlan(ModelPublishContext context);

    /**
     * 从保存的实例恢复核验；新计划逐项核实实际结构，符合要求的跳过，缺失的按原计划继续。
     * 注释重新写入，事务中断先确认原事务回滚或提交结果；每条SQL本次最多发送一次。
     *
     * <p>
     * 统一发布入口发现未完成实例时调用；先查旧效果，满足恢复条件才发送下一次尝试，不重复发送已生效SQL。
     * @param modelId 主模型ID，用于核对实例归属
     * @param instanceId 要恢复的发布实例ID
     * @return 从平台记录重新读取的发布上下文，调用方应检查其中的执行状态
     */
    ModelPublishContext recoverPlan(Long modelId, Long instanceId);

    /**
     * 读取待提交实例并统一保存正式结果，不重新发送DDL。
     * 同一次发布复用上下文中的完整核验结果；跨请求补保存没有结果时才重新采集。
     *
     * @param context 带有模型ID、实例ID及本次完整核验结果的发布上下文
     * @return 正式保存成功的发布上下文；已成功的实例直接返回原结果
     */
    ModelPublishContext commitPlan(ModelPublishContext context);

    /**
     * 按逆序评估失败实例的全部效果；只撤销已证明安全的本次新增对象，其余保留并说明原因。
     * 每个对象本次最多发送一次补偿，仍使用原实例记录，不影响上次成功版本。
     * <p>
     * 用于未采用整批DDL事务的失败实例；逆序评估本次新增对象，已有表加列和扩长等无法安全撤销的效果保留并记录。
     */
    ModelPublishContext compensatePlan(Long modelId, Long instanceId);
}
