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

package tech.qiantong.qdata.mdm.ingest.enums.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 接入任务配置及运行状态常量，状态码与数据库字段取值保持一致。
 *
 * @author chentangwen
 */
public final class IngestTaskConstants {

    /**
     * 读取全部来源记录，不限制行数。
     */
    public static final int READ_ALL_ROWS = -1;

    /**
     * 未逻辑删除。
     */
    public static final String NOT_DELETED = "0";

    /**
     * 运行中、失败、成功。
     */
    public static final String RUNNING = "1";
    public static final String RUN_FAILED = "2";
    public static final String RUN_SUCCESS = "4";

    /**
     * 来源记录等待治理。
     */
    public static final String GOVERNANCE_PENDING = "PENDING";

    /**
     * 来源字段是否参与唯一标识。
     */
    public static final String UNIQUE_KEY_YES = "1";
    public static final String UNIQUE_KEY_NO = "0";

    public static final String TASK_CODE_PREFIX = "task";

    public static final String DATABASE = "DATABASE";
    public static final String FILE = "FILE";
    public static final String API = "API";
    public static final String ENABLED = "1";
    public static final String DISABLED = "0";
    public static final String VALID_FLAG_YES = "1";
    public static final String MANUAL = "MANUAL";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String FULL = "FULL";
    public static final String INCREMENTAL = "INCREMENTAL";

    /**
     * 处理类型写入 CHAR(1)：0不处理 1类型转换 2去空格 3空值 4固定值 5值映射 6日期。
     */
    public static final String TRANSFORM_NONE = "0";
    public static final String TRANSFORM_TYPE = "1";
    public static final String TRANSFORM_TRIM = "2";
    public static final String TRANSFORM_NULL = "3";
    public static final String TRANSFORM_FIXED = "4";
    public static final String TRANSFORM_VALUE_MAP = "5";
    public static final String TRANSFORM_DATE = "6";

    public static final List<String> TRANSFORM_TYPES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            TRANSFORM_NONE,
                            TRANSFORM_TYPE,
                            TRANSFORM_TRIM,
                            TRANSFORM_NULL,
                            TRANSFORM_FIXED,
                            TRANSFORM_VALUE_MAP,
                            TRANSFORM_DATE));

    private IngestTaskConstants() {
    }
}
