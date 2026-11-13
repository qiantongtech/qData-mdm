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

package tech.qiantong.qdata.mdm.quality.enums.task;

/**
 * 整改任务及问题项状态常量，取值与数据库字段保持一致。
 */
public final class QualityIssueTaskConstants {

    /** 整改任务状态：待审批、审批中、已完成、已取消、处理中。 */
    public static final String TASK_STATUS_PENDING_APPROVAL = "1";
    public static final String TASK_STATUS_IN_APPROVAL = "2";
    public static final String TASK_STATUS_COMPLETED = "3";
    public static final String TASK_STATUS_CANCELLED = "4";
    public static final String TASK_STATUS_RECTIFYING = "5";

    /** 整改问题项状态：待整改、整改中、已整改。 */
    public static final String INFO_STATUS_PENDING = "1";
    public static final String INFO_STATUS_PROCESSING = "2";
    public static final String INFO_STATUS_COMPLETED = "3";

    /** 整改说明最大长度。 */
    public static final int NOTE_MAX_LENGTH = 512;

    private QualityIssueTaskConstants() {
    }
}
