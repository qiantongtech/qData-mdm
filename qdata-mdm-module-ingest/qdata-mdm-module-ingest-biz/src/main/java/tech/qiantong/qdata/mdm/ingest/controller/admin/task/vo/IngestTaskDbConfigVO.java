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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import lombok.Data;

/**
 * 接入任务数据库来源配置对象，支持来源对象选择及增量读取配置。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskDbConfigVO {

    /**
     * 来源数据库连接 ID。
     */
    private Long datasourceId;

    /**
     * 来源对象所属的数据库模式。
     */
    private String sourceSchema;

    /**
     * 待采集的来源表或视图名称。
     */
    private String sourceObject;

    /**
     * 增量读取字段，增量模式下用于确定读取位置。
     */
    private String incrementField;

    /**
     * 首次增量读取的起始值，后续执行位置由服务端维护。
     */
    private String initialIncrementValue;
}
