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

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStage;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelVersionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次主模型发布要用的数据集合（上下文），用来在各步骤之间传递配置、结构和结果；不对应独立数据库表。
 */
@Data
@NoArgsConstructor
public class ModelPublishContext {

    /**
     * 本次整体发布实例ID
     */
    private Long instanceId;

    /**
     * 根模型ID
     */
    private Long modelId;

    /**
     * 当前根工作版本ID，与发布执行状态分开
     */
    private Long modelVersionId;

    /**
     * 本次指定的数据源ID，不能使用前端传来的类型或连接信息替代查询。
     */
    private Long datasourceId;

    /**
     * 上次成功实例的非敏感连接身份，用于阻止数据源被换成另一个库。
     */
    private ModelPublishTarget baseTarget;

    /**
     * TARGET已核对的本次连接身份，不含账号密码。
     */
    private ModelPublishTarget target;

    /**
     * 当前根工作版本状态，不能与发布实例状态混用
     */
    private ModelVersionStatus modelVersionStatus;

    /**
     * 上次成功实例ID，首次为空
     */
    private Long baseInstanceId;

    /**
     * 本次发布执行状态
     */
    private ModelPublishStatus status = ModelPublishStatus.PREPARING;

    /**
     * 当前流程阶段
     */
    private ModelPublishStage currentStage = ModelPublishStage.READ_CONFIG;

    /**
     * 本次独立保存的完整模型配置
     */
    private ModelPublishSnapshot snapshot;

    /**
     * 上次成功配置，首次为空
     */
    private ModelPublishSnapshot baseSnapshot;

    /**
     * 上次成功的完整结构，不从目标库反推替代
     */
    private ModelMaterializedStructure baseStructure;

    /**
     * TARGET查到的历史表、字段和索引；缺失对象不在此清单中，用于补建和回滚，不覆盖历史成功结构。
     */
    private ModelMaterializedStructure existingStructure;

    /**
     * 本次完整目标结构
     */
    private ModelMaterializedStructure targetStructure;

    /**
     * 完整比较结果
     */
    private ModelPublishStructureDiff diff;

    /**
     * 保存到实例EXECUTION_PLAN的有序计划
     */
    private ModelPublishExecutionPlan executionPlan;

    /**
     * 全部真实核验后完整结构；只有此结构可提交成功记录
     */
    private ModelMaterializedStructure verifiedStructure;

    /**
     * 各阶段规则收集的错误，保留具体阶段、规则和对象位置，方便定位发布失败原因
     */
    private List<ModelPublishValidationIssue> validationIssues = new ArrayList<>();

    /**
     * 失败恢复证据，不污染旧成功版本
     */
    private List<ModelPublishRemainingEffect> remainingEffects = new ArrayList<>();
}

