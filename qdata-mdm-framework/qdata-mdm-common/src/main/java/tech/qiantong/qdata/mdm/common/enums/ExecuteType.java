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



package tech.qiantong.qdata.mdm.common.enums;

/**
 * execute type
 */
public enum ExecuteType {

    /**
     * operation type
     * 1 repeat running
     * 2 resume pause
     * 3 resume failure
     * 4 stop
     * 5 pause
     */
    NONE(0, "NONE"),

    // ******************************* Workflow ***************************
    REPEAT_RUNNING(1, "REPEAT_RUNNING"),
    RECOVER_SUSPENDED_PROCESS(2, "RECOVER_SUSPENDED_PROCESS"),
    START_FAILURE_TASK_PROCESS(3, "START_FAILURE_TASK_PROCESS"),
    STOP(4, "STOP"),
    PAUSE(5, "PAUSE"),
    // ******************************* Workflow ***************************

    // ******************************* Task *******************************
    EXECUTE_TASK(6, "EXECUTE_TASK"),
    // ******************************* Task *******************************
    ;

    private final int code;
    private final String desc;

    ExecuteType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ExecuteType getEnum(int value) {
        for (ExecuteType e : ExecuteType.values()) {
            if (e.getCode() == value) {
                return e;
            }
        }
        return NONE;
    }
}
