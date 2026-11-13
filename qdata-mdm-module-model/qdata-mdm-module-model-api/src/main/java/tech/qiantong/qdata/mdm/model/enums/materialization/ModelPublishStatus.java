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
 * 发布实例执行状态，与MODEL_VERSION.STATUS分开；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelPublishStatus {
    /**
     * 正在读取配置、构建、校验和生成计划
     */
    PREPARING,
    /**
     * 正在执行或核验目标结构
     */
    EXECUTING,
    /**
     * 正在评估及执行安全补偿
     */
    COMPENSATING,
    /**
     * 整体核验及平台成功提交均已完成
     */
    SUCCEEDED,
    /**
     * 失败且已确认没有未处理物理效果
     */
    FAILED,
    /**
     * 失败且存在已确认残留效果
     */
    PARTIAL_FAILED,
    /**
     * 实际执行或提交效果暂时无法确认，必须先核验
     */
    UNKNOWN,
    /**
     * DDL全部完成，平台成功结构尚待提交
     */
    COMMIT_PENDING
}

