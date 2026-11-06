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

package tech.qiantong.qdata.mdm.common.constant;

/**
 * Task scheduling common constants
 *
 * @author qdata
 */
public class ScheduleConstants
{
    /** Quartz scheduler */
    public static final String QUARTZ = "QUARTZ";

    /** DolphinScheduler scheduler */
    public static final String DOLPHINSCHEDULER = "DOLPHINSCHEDULER";

    public static final String TASK_CLASS_NAME = "TASK_CLASS_NAME";

    public static final String QUARTZ_JOB_NAMESPACE = "_QUARTZ_JOB_";

    /** Execute target key */
    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    /** default */
    public static final String MISFIRE_DEFAULT = "0";

    /** Trigger execution immediately */
    public static final String MISFIRE_IGNORE_MISFIRES = "1";

    /** Trigger an execution */
    public static final String MISFIRE_FIRE_AND_PROCEED = "2";

    /** Does not trigger immediate execution */
    public static final String MISFIRE_DO_NOTHING = "3";

    public enum Status
    {
        /**
         * Normal
         */
        NORMAL("0"),
        /**
         * Pause
         */
        PAUSE("1");

        private String value;

        private Status(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
    }
}
