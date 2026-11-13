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
 * 实例或步骤的补偿状态；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelCompensationStatus {
    /**
     * 无需补偿
     */
    NOT_REQUIRED,
    /**
     * 等待补偿评估或执行
     */
    PENDING,
    /**
     * 补偿执行或核验中
     */
    RUNNING,
    /**
     * 安全补偿及核验成功
     */
    SUCCEEDED,
    /**
     * 补偿失败
     */
    FAILED,
    /**
     * 不安全或不允许撤销，保留实际效果及原因
     */
    RETAINED
}

