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

package tech.qiantong.qdata.mdm.model.enums.materialization;

/**
 * 发布过程阶段，对应实例进度及阶段日志；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelPublishStage {
    /**
     * 读取并保存完整配置
     */
    READ_CONFIG,
    /**
     * 逻辑配置基础校验
     */
    CONFIG,
    /**
     * 构建完整来源及权威结构
     */
    BUILD,
    /**
     * 比较上次成功结构及本次结构
     */
    DIFF,
    /**
     * 校验变更边界
     */
    CHANGE,
    /**
     * 核验目标身份、实际结构及数据条件
     */
    TARGET,
    /**
     * 生成并保存完整执行计划
     */
    PLAN,
    /**
     * 顺序执行SQL
     */
    EXECUTE,
    /**
     * 核验真实执行结果
     */
    VERIFY,
    /**
     * 失败评估及安全补偿
     */
    COMPENSATE,
    /**
     * 平台短事务提交完整成功结构
     */
    COMMIT,
    /**
     * 关闭本次发布使用的自有资源
     */
    CLOSE
}

