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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo;

import java.util.*;
import lombok.Data;

/**
 * 接入任务来源数据预览响应对象。
 *
 * @author chentangwen
 */
@Data
public class IngestTaskPreviewRespVO {

    /**
     * 预览数据的来源字段名称列表。
     */
    private List<String> fields;

    /**
     * 预览记录列表，每条记录以来源字段名称为键。
     */
    private List<Map<String, Object>> rows;
}
