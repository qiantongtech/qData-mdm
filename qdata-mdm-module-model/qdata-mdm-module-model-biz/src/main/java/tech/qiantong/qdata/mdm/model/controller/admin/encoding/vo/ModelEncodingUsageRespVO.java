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

package tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseRespVO;

import java.util.LinkedHashMap;
import java.util.Map;

/** 编码方案版本引用情况：模型基础信息及引用该版本的字段。 */
@Schema(description = "编码方案引用模型及字段，fields 仅包含引用当前编码方案版本的字段")
public class ModelEncodingUsageRespVO extends ModelBaseRespVO {
    private static final long serialVersionUID = 1L;

    /** 键为引用字段 ID。 */
    @Getter
    @Schema(description = "各引用字段中已填写编码的主数据记录数")
    private final Map<Long, Long> fieldUsageCounts = new LinkedHashMap<>();
}
