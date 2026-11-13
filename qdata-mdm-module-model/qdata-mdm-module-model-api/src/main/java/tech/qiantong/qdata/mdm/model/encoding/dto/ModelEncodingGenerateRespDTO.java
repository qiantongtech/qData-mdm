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
 * 正式分配结果，返回时流水状态已独立提交。
 */
@Data
public class ModelEncodingGenerateRespDTO {
    /**
     * 编码方案主体ID。
     */
    private Long encodingId;
    /**
     * 本次使用的已发布编码方案版本ID。
     */
    private Long encodingVersionId;
    /**
     * 使用模型ID，不同模型独立计数。
     */
    private Long modelId;
    /**
     * 所有规则段按顺序拼接的完整编码。
     */
    private String code;
    /**
     * 本次各流水段的分配信息，无流水段时为空列表。
     */
    private List<SerialValue> serialValues = new ArrayList<>();

    /** 单个流水规则段的正式分配结果，数字值与当前周期持久化进度一致。 */
    @Data
    public static class SerialValue {
        /**
         * 本次规则段ID。
         */
        private Long itemId;
        /**
         * 规则排序位置，用于跨版本续号。
         */
        private Long sortOrder;
        /**
         * 周期标识，例如NONE或DAY:20261019。
         */
        private String periodKey;
        /**
         * 本次正式分配的数字，未补零。
         */
        private Long value;
    }
}
