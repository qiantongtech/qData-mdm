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

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 合并管理的聚合查询范围，接口筛选值与数据库结果状态分别管理。
 *
 * @author chentangwen
 */
@Getter
public enum MergeManageQueryEnum {

    /** 未指定状态时展示待确认、待质检、质检失败和待提交结果。 */
    DEFAULT("", MergeCodes.PENDING_WAIT_CONFIRM, MergeCodes.PENDING_WAIT_QUALITY,
            MergeCodes.PENDING_QUALITY_FAIL, MergeCodes.PENDING_WAIT_SUBMIT),

    /** 查询全部有效结果，不限制状态。 */
    ALL("*"),

    /** 待确认页的聚合范围，保持原有接口含义。 */
    PENDING_CONFIRM("pendingConfirm", MergeCodes.PENDING_WAIT_CONFIRM,
            MergeCodes.PENDING_WAIT_QUALITY, MergeCodes.PENDING_WAIT_SUBMIT),

    /** 审批中及审批通过后待生效的结果。 */
    APPROVING("approving", MergeCodes.PENDING_APPROVING, MergeCodes.PENDING_WAIT_EFFECT);

    private final String value;
    private final List<String> statuses;

    MergeManageQueryEnum(String value, String... statuses) {
        this.value = value;
        this.statuses = Collections.unmodifiableList(Arrays.asList(statuses));
    }

    /**
     * 展开页面状态范围；普通状态作为单一筛选值，全部状态返回 null。
     *
     * @param value 页面状态值
     * @return 持久化状态范围，null 表示不限制
     */
    public static List<String> resolveStatuses(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT.statuses;
        }
        for (MergeManageQueryEnum query : values()) {
            if (query.value.equals(value)) {
                return query == ALL ? null : query.statuses;
            }
        }
        return Collections.singletonList(value);
    }
}
