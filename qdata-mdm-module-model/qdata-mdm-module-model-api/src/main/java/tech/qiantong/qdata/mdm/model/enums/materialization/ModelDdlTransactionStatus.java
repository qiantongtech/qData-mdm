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

/** 整批DDL的业务库事务阶段，与平台发布状态分开保存，避免把事务内效果当作已提交。 */
public enum ModelDdlTransactionStatus {
    /** 没有使用事务型DDL，沿用逐条执行及安全补偿。 */
    NONE,
    /** 已开始业务库事务，SQL效果尚未提交。 */
    ACTIVE,
    /** 已保存提交意图；中断后不能假定提交成功或回滚成功。 */
    COMMITTING,
    /** 业务库提交已明确完成，仍需平台正式保存。 */
    COMMITTED,
    /** 正在回滚；中断后保留未知，不执行逆向删除SQL。 */
    ROLLING_BACK,
    /** 回滚正常返回且原物理结构已重新核验。 */
    ROLLED_BACK
}
