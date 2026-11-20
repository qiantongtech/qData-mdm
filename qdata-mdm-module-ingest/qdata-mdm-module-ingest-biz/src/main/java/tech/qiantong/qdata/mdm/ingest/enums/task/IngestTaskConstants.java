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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.enums.task;

/**
 * 接入任务配置、运行状态及字段转换类型常量。
 *
 * @author chentangwen
 */
public final class IngestTaskConstants {

    /**
     * 读取全部来源记录，不限制行数。
     */
    public static final int READ_ALL_ROWS = -1;

    /**
     * 逻辑删除标志：未删除。
     */
    public static final String NOT_DELETED = "0";

    /**
     * 运行实例状态：运行中，包含排队等待。
     */
    public static final String RUNNING = "1";

    /**
     * 运行实例状态：失败。
     */
    public static final String RUN_FAILED = "2";

    /**
     * 运行实例状态：成功。
     */
    public static final String RUN_SUCCESS = "4";

    /**
     * 来源记录治理状态：待治理。
     */
    public static final String GOVERNANCE_PENDING = "PENDING";

    /**
     * 来源字段参与记录唯一标识的组合。
     */
    public static final String UNIQUE_KEY_YES = "1";

    /**
     * 来源字段不参与记录唯一标识的组合。
     */
    public static final String UNIQUE_KEY_NO = "0";

    /**
     * 接入任务编码前缀。
     */
    public static final String TASK_CODE_PREFIX = "task";

    /**
     * 接入类型：数据库。
     */
    public static final String DATABASE = "DATABASE";

    /**
     * 接入类型：文件。
     */
    public static final String FILE = "FILE";

    /**
     * 接入类型：API 接口。
     */
    public static final String API = "API";

    /**
     * 任务启停状态或调度开关：启用。
     */
    public static final String ENABLED = "1";

    /**
     * 任务启停状态或调度开关：停用。
     */
    public static final String DISABLED = "0";

    /**
     * 有效标志：有效。
     */
    public static final String VALID_FLAG_YES = "1";

    /**
     * 任务触发方式：人工执行。
     */
    public static final String MANUAL = "MANUAL";

    /**
     * 任务触发方式：定时执行。
     */
    public static final String SCHEDULED = "SCHEDULED";

    /**
     * 数据库接入模式：全量读取。
     */
    public static final String FULL = "FULL";

    /**
     * 数据库接入模式：增量读取。
     */
    public static final String INCREMENTAL = "INCREMENTAL";

    /**
     * 字段转换类型：不处理，保留原值。
     */
    public static final String TRANSFORM_NONE = "0";

    /**
     * 字段转换类型：数据类型转换。
     */
    public static final String TRANSFORM_TYPE = "1";

    /**
     * 字段转换类型：去除字符串首尾空白字符。
     */
    public static final String TRANSFORM_TRIM = "2";

    /**
     * 字段转换类型：空值处理。
     */
    public static final String TRANSFORM_NULL = "3";

    /**
     * 字段转换类型：固定值。
     */
    public static final String TRANSFORM_FIXED = "4";

    /**
     * 字段转换类型：值映射。
     */
    public static final String TRANSFORM_VALUE_MAP = "5";

    /**
     * 字段转换类型：日期处理。
     */
    public static final String TRANSFORM_DATE = "6";

    private IngestTaskConstants() {
    }
}
