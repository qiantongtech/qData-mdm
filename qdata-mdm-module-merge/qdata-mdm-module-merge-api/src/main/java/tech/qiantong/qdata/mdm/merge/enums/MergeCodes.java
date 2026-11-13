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

package tech.qiantong.qdata.mdm.merge.enums;

/**
 * 多源融合状态
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public final class MergeCodes {
    private MergeCodes() {
    }

    public static final String YES = "1";
    public static final String NO = "0";

    public static final String GROUP_TYPE_CANDIDATE = "1";
    public static final String GROUP_TYPE_SINGLE = "2";

    public static final String GROUP_STATUS_WAIT = "0";
    public static final String GROUP_STATUS_REVIEW = "1";
    public static final String GROUP_STATUS_SUSPEND = "2";
    public static final String GROUP_STATUS_CONFIRMED = "3";
    public static final String GROUP_STATUS_SPLIT = "4";
    public static final String GROUP_STATUS_SINGLE = "5";

    public static final String MEMBER_ROLE_TARGET = "1";
    public static final String MEMBER_ROLE_HISTORY = "2";
    public static final String JOIN_AUTO = "1";
    public static final String JOIN_MANUAL = "2";

    public static final String REVIEW_CONFIRM = "1";
    public static final String REVIEW_SPLIT = "2";
    public static final String REVIEW_SUSPEND = "3";
    public static final String REVIEW_RESUME = "4";
    public static final String REVIEW_ADD = "5";
    public static final String REVIEW_DELETE = "6";

    public static final String TASK_RUNNING = "1";
    public static final String TASK_FAILED = "2";
    public static final String TASK_PENDING = "0";
    public static final String TASK_SUCCESS = "4";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String SCOPE_MODEL = "1";
    public static final String SCOPE_CUSTOM = "2";

    public static final String PROCESS_SINGLE = "1";
    public static final String PROCESS_MULTI = "2";

    public static final String PENDING_WAIT_CONFIRM = "0";
    public static final String PENDING_WAIT_QUALITY = "1";
    public static final String PENDING_QUALITY_FAIL = "2";
    public static final String PENDING_WAIT_SUBMIT = "3";
    public static final String PENDING_APPROVING = "4";
    public static final String PENDING_REJECTED = "5";
    public static final String PENDING_WITHDRAWN = "6";
    public static final String PENDING_CANCELLED = "7";
    public static final String PENDING_WAIT_EFFECT = "8";
    public static final String PENDING_EFFECTIVE = "9";

    public static final String QUALITY_NONE = "0";
    public static final String QUALITY_RUNNING = "1";
    public static final String QUALITY_FAIL = "2";
    public static final String QUALITY_PASS = "4";
    public static final String QUALITY_STALE = "5";

    public static final String VALUE_SINGLE_DEFAULT = "1";
    public static final String VALUE_SURVIVOR = "2";
    public static final String VALUE_FALLBACK = "3";
    public static final String VALUE_PICK_SOURCE = "4";
    public static final String VALUE_MANUAL = "5";
    public static final String VALUE_AUTO_CODE = "6";

    public static final String LOG_ADJUST = "1";
    public static final String LOG_CONFIRM = "2";
    public static final String LOG_SUBMIT = "3";
    public static final String LOG_WITHDRAW = "4";
    public static final String LOG_CANCEL = "5";

    public static final String GOVERNANCE_PENDING = "PENDING";
    public static final String GOVERNANCE_PROCESSING = "PROCESSING";
    public static final String GOVERNANCE_MERGED = "MERGED";
}
