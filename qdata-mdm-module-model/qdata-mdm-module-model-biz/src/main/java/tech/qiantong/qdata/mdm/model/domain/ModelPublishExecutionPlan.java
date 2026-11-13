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

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlTransactionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * EXECUTION_PLAN的JSON契约；完整目标、逐条SQL及执行状态一起保存，不新增步骤表。
 */
@Data
@NoArgsConstructor
public class ModelPublishExecutionPlan {

    /**
     * 结构格式版本，恢复必须识别版本后再读取
     */
    private Integer formatVersion = 1;

    /** 新计划逐条结果先存内存，结束时统一保存；旧计划未配置时仍按原逐条保存方式处理。 */
    private Boolean deferredProgress = Boolean.FALSE;

    /** 本次调用产生的实际执行事件，不写入计划JSON，结束时统一追加到全流程日志。 */
    @JSONField(serialize = false, deserialize = false)
    private List<JSONObject> executionEvents = new ArrayList<>();

    /** 只在中断恢复时使用，表示已核对原计划对应的真实结构，不作为数据库执行证明。 */
    @JSONField(serialize = false, deserialize = false)
    private Boolean recoveryReconciled = Boolean.FALSE;

    /** 原DDL事务已由数据库回滚并核验，允许用原计划重新开始一批事务。 */
    @JSONField(serialize = false, deserialize = false)
    private Boolean rollbackVerified = Boolean.FALSE;

    /** 业务库整批DDL事务阶段，保存在原执行计划中，不新增平台表字段。 */
    private ModelDdlTransactionStatus ddlTransactionStatus = ModelDdlTransactionStatus.NONE;

    /**
     * 同一业务库连接的非敏感目标描述
     */
    private ModelPublishTarget target;

    /**
     * TARGET查到的历史表、字段和索引；缺失对象不在此清单中，用于补建和回滚，不覆盖历史成功结构。
     */
    private ModelMaterializedStructure existingStructure;

    /**
     * 完整目标结构；不等同于未经核验即可提交的成功结构
     */
    private ModelMaterializedStructure targetStructure = new ModelMaterializedStructure();

    /**
     * 严格按stepNo顺序排列的单条SQL；元数据发布允许为空
     */
    private List<ModelSqlExecutionStep> steps = new ArrayList<>();

    /**
     * 正向最多重试4次，不包含首次执行；每次重试前仍需核对实际执行结果。
     */
    private Integer forwardMaxRetries = 4;

    /**
     * 补偿最多重试3次，不包含首次执行；满足安全条件后才允许补偿。
     */
    private Integer compensationMaxRetries = 3;
}

