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
 * 匹配审核的聚合查询范围；查询值用于接口筛选，不作为候选组数据库状态。
 *
 * @author chentangwen
 */
@Getter
public enum MergeReviewQueryEnum {

    /** 全部审核状态，不包含待转入组和单条结果组。 */
    ALL("*", "全部", MergeCodes.GROUP_STATUS_REVIEW, MergeCodes.GROUP_STATUS_SUSPEND,
            MergeCodes.GROUP_STATUS_CONFIRMED, MergeCodes.GROUP_STATUS_SPLIT),

    /** 已完成审核，包括已确认和已拆分的组。 */
    COMPLETED("completed", "已完成", MergeCodes.GROUP_STATUS_CONFIRMED, MergeCodes.GROUP_STATUS_SPLIT);

    /** 接口查询值。 */
    private final String value;

    /** 查询范围名称。 */
    private final String name;

    /** 该查询范围包含的数据库状态，禁止外部修改。 */
    private final List<String> statuses;

    MergeReviewQueryEnum(String value, String name, String... statuses) {
        this.value = value;
        this.name = name;
        this.statuses = Collections.unmodifiableList(Arrays.asList(statuses));
    }

    /**
     * 按接口查询值查找聚合范围，普通状态或未知值返回 null。
     *
     * @param value 接口查询值
     * @return 对应的聚合范围
     */
    public static MergeReviewQueryEnum getByValue(String value) {
        for (MergeReviewQueryEnum query : values()) {
            if (query.value.equals(value)) {
                return query;
            }
        }
        return null;
    }
}
