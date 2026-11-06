/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.common.enums;

import lombok.Getter;

import java.util.Locale;

/**
 * <P>
 * Purpose: Category table enumeration
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-11 16:00
 **/

public enum TaskCatEnum {

    ATT_TASK_CAT("1", "离线数据集成任务"),
    CAT("2", "实时任务"),
    ATT_DATA_DEV_CAT("3", "数据开发任务"),
    ATT_JOB_CAT("4", "作业任务");

    @Getter
    private String type;

    @Getter
    private String name;

    TaskCatEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public static TaskCatEnum findEnumByType(String type) {
        for (TaskCatEnum taskCatEnum : TaskCatEnum.values()) {
            if (taskCatEnum.getType().toUpperCase(Locale.ROOT).equals(type.toUpperCase(Locale.ROOT))) {
                return taskCatEnum;
            }
        }
        return null;
    }

}
