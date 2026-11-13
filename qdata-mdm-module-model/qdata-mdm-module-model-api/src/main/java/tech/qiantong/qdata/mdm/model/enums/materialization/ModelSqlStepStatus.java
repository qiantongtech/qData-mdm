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
 * 单条SQL执行汇总状态；持久化及JSON使用枚举名称，不使用序号。
 */
public enum ModelSqlStepStatus {
    /**
     * 已计划但尚未发送SQL
     */
    PENDING,
    /**
     * 已记录开始，执行或核验尚未完成
     */
    RUNNING,
    /**
     * SQL已执行成功；整批结构是否通过另由发布状态表示
     */
    SUCCEEDED,
    /**
     * 已确认本次失败，是否重试须重新核验
     */
    FAILED,
    /**
     * 无法确认实际效果，禁止盲目重发
     */
    UNKNOWN,
    /**
     * 经判断无需执行，如恢复确认已完成
     */
    SKIPPED,
    /**
     * 前序失败等原因导致本步未执行
     */
    NOT_EXECUTED
}

