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

package tech.qiantong.qdata.mdm.model.encoding.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动编码输入。每个业务字段/参考映射规则段必须提供一条参数，按配置值匹配而非数组顺序。
 */
@Data
public class ModelEncodingGenerateReqDTO {
    /**
     * 已发布编码方案版本ID。
     */
    private Long encodingVersionId;
    /**
     * 使用模型ID，同一方案在不同模型下独立计数。
     */
    private Long modelId;
    /**
     * 每个业务字段规则段一条输入，按configValue匹配配置。
     */
    private List<FieldValue> businessFields = new ArrayList<>();
    /**
     * 每个参考映射规则段一条输入，configValue为参考数据类型ID字符串。
     */
    private List<FieldValue> referenceMappings = new ArrayList<>();

    /** 单个动态规则段的输入，按配置值匹配；同名配置重复时，每个规则段仍需提供一条值。 */
    @Data
    public static class FieldValue {
        /**
         * 规则段fixedValue：业务字段参数名，或参考数据类型ID的字符串。
         */
        private String configValue;
        /**
         * 调用方从相应业务字段取得的实际值，参考映射同样直接使用此值。
         */
        private String value;
    }
}
