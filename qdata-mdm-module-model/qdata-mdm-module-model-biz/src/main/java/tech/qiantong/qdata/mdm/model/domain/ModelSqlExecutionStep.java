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

package tech.qiantong.qdata.mdm.model.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelCompensationStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlOperation;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSqlStepStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * EXECUTION_PLAN中的单条SQL计划和汇总；每次实际尝试另写MODEL_INSTANCE_LOG。
 */
@Data
@NoArgsConstructor
public class ModelSqlExecutionStep {

    /**
     * 从1开始的全计划唯一序号，依赖顺序执行
     */
    private Integer stepNo;

    /**
     * 所属动作固定标识，同一动作的结构SQL及附属注释使用同一个标识。
     */
    private String actionKey;

    /**
     * 必须先完成的SQL步骤序号；动作依赖在拆分后换成真实步骤依赖。
     */
    private List<Integer> dependencyStepNos = new ArrayList<>();

    /**
     * 步骤对应模型、角色及物理对象
     */
    private ModelSchemaObjectRef object;

    /**
     * 正向操作种类，不允许以停用/删除生成DROP
     */
    private ModelDdlOperation operation;

    /**
     * 由common生成的单条正向SQL
     */
    private String forwardSql;

    /**
     * 恢复及回滚时核对原状态的条件；正常执行不逐条查询
     */
    private List<ModelVerificationCondition> preconditions = new ArrayList<>();

    /**
     * 恢复未完成SQL时核对效果的条件；正常执行只做整批结构核验
     */
    private List<ModelVerificationCondition> postconditions = new ArrayList<>();

    /**
     * 正向执行汇总状态
     */
    private ModelSqlStepStatus status = ModelSqlStepStatus.PENDING;

    /** 中断恢复时确认预期效果已存在，因此跳过SQL；不虚构原执行次数和原执行时间。 */
    private Boolean recoveredEffect = Boolean.FALSE;

    /**
     * 已记录的正向实际SQL尝试次数，不等于日志条数
     */
    private Integer attemptCount = 0;

    /**
     * 最近一次发送是否明确结束。正常返回或数据库明确拒绝才记true；超时、断线及未收到结果都记false。
     * 查不到物理效果还不够，重试前也要确认上一条SQL没有继续在数据库里运行。
     */
    private Boolean lastAttemptCompleted = false;

    /**
     * 上次是否属于明确可重试的临时错误，例如数据库报告死锁回滚或锁暂不可用。
     * 语法、权限、约束错误及无法识别的异常都不机械重试；本标识不代表可以省略真实结构核验。
     */
    private Boolean lastAttemptRetryable = false;

    /**
     * 本条执行准备的开始时间；用于合并保存开始事件和发送意图，不写入执行计划。
     */
    @JSONField(serialize = false, deserialize = false)
    private Date checkStartedTime;

    /**
     * 本步骤主要失败原因
     */
    private String failureReason;

    /**
     * 仅限安全可撤销对象的单条逆向SQL；不可逆时为空
     */
    private String compensationSql;

    /**
     * 无安全逆向时的原因，已有加列/扩长不能自动破坏性撤销
     */
    private String irreversibleReason;

    /**
     * 补偿前须确认对象属于本次新增、未生效、无数据和依赖等条件
     */
    private List<ModelVerificationCondition> compensationPreconditions = new ArrayList<>();

    /**
     * 补偿完成后核验条件
     */
    private List<ModelVerificationCondition> compensationPostconditions = new ArrayList<>();

    /**
     * 本步补偿汇总状态
     */
    private ModelCompensationStatus compensationStatus = ModelCompensationStatus.NOT_REQUIRED;

    /**
     * 已记录的补偿实际SQL尝试次数
     */
    private Integer compensationAttemptCount = 0;

    /**
     * 最近一次补偿SQL是否明确结束；断线、超时或结果未保存时为false，不能盲目再发DROP。
     */
    private Boolean compensationLastAttemptCompleted = false;

    /**
     * 最近一次补偿是否因明确的临时错误结束；重新核验安全条件后才可使用剩余重试次数。
     */
    private Boolean compensationLastAttemptRetryable = false;

    /**
     * 补偿失败或保留效果的原因
     */
    private String compensationFailureReason;
}

