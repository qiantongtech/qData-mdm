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

package tech.qiantong.qdata.mdm.model.controller.admin.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBaseRespVO;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 参考数据引用情况：每个模型返回一次，fields 仅包含引用指定参考数据类型的字段。
 * 模型基础信息及字段信息复用模型响应结构。
 */
@Schema(description = "参考数据引用模型及引用字段，fields 仅包含引用当前参考数据的字段")
public class ModelDictDataUsageRespVO extends ModelBaseRespVO {
    private static final long serialVersionUID = 1L;

    /** 外层键为引用字段 ID，内层键为原始数据项编码。 */
    @Getter
    @Schema(description = "各数据项在引用字段中的主数据使用数量")
    private final Map<Long, Map<String, Long>> fieldUsageCounts = new LinkedHashMap<>();
}
