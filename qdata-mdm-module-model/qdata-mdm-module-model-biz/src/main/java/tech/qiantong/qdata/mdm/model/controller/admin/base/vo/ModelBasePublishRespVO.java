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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 原发布入口的处理结果，只返回状态和对象定位信息，不返回SQL、连接凭证或完整配置。
 */
@Data
public class ModelBasePublishRespVO {
    /**
     * 本次处理的主模型ID。
     */
    private Long modelId;
    /**
     * 本次新发布或恢复的原实例ID。
     */
    private Long instanceId;
    /**
     * 本次尝试发布的模型版本ID，失败时不代表该版本已生效。
     */
    private Long modelVersionId;
    /**
     * 本次成功生效的版本ID；本次失败或待提交时为空，不冒充已发布版本。
     */
    private Long publishedModelVersionId;
    /**
     * 本次发布之前的成功实例ID，首次发布为空。
     */
    private Long baseInstanceId;
    /**
     * SUCCEEDED才表示本次成功；失败、待提交及结果不明保持各自原状态。
     */
    private String status;
    /**
     * 当前处理阶段；补偿后为COMPENSATE，不能当作原始失败阶段。
     */
    private String currentStage;
    /**
     * 原始失败阶段，能够从本次上下文确定时返回。
     */
    private String failureStage;
    /**
     * 第一条失败或结果不明的SQL步骤序号，非SQL失败为空。
     */
    private Integer failedStepNo;
    /**
     * 可向用户展示的失败提示，不直接透传驱动异常。
     */
    private String failureReason;
    /**
     * 事务型DDL处理状态；无执行计划时为空。
     */
    private String ddlTransactionStatus;
    /**
     * 每条SQL的执行与补偿结果，不包含SQL正文及默认值。
     */
    private List<ModelPublishStepRespVO> steps = new ArrayList<>();
    /**
     * 尚未消除或无法确认的物理效果，不包含采集到的结构内容。
     */
    private List<ModelPublishRemainingEffectRespVO> remainingEffects = new ArrayList<>();

    /**
     * 用步骤序号对应实例中的原计划，正向执行结果与补偿结果分别展示。
     */
    @Data
    public static class ModelPublishStepRespVO {
        /**
         * 原计划中的SQL顺序号。
         */
        private Integer stepNo;
        /**
         * 正向执行状态。
         */
        private String status;
        /**
         * 安全补偿状态，不能把无需补偿理解为已回撤。
         */
        private String compensationStatus;
    }

    /**
     * 只指出哪个对象仍需处理，不返回完整数据库结构。
     */
    @Data
    public static class ModelPublishRemainingEffectRespVO {
        /**
         * 原计划步骤序号。
         */
        private Integer stepNo;
        /**
         * 涉及的表名。
         */
        private String tableName;
        /**
         * 涉及的字段名。
         */
        private String columnName;
        /**
         * 涉及的索引名。
         */
        private String indexName;
        /**
         * 是否已核实物理效果。
         */
        private String verificationStatus;
    }
}
