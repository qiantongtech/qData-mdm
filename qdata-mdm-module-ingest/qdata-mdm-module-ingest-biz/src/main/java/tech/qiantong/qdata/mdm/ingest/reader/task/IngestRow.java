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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 单条来源记录，定位信息独立于来源值，不参与字段映射。
 *
 * @author chentangwen
 */
@Getter
@RequiredArgsConstructor
public class IngestRow {
    /**
     * 以来源字段名为键的原始数据。
     */
    private final Map<String, Object> values;
    /**
     * 错误定位信息，例如文件物理行号、API 页内序号或数据库读取序号。
     */
    private final String location;
    /**
     * 按任务唯一字段顺序提取的标识值，尚未计算来源记录 ID。
     */
    private final Map<String, Object> sourceId;
    /**
     * 数据库增量字段的当前值，非数据库增量读取时为空。
     */
    private final Object incrementValue;
}
