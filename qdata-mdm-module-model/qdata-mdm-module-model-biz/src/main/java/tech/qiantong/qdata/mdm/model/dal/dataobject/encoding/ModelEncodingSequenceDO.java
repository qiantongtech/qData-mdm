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

package tech.qiantong.qdata.mdm.model.dal.dataobject.encoding;

import lombok.Data;

/**
 * 不允许逻辑删除或随方案版本删除的已分配流水状态。
 */
@Data
public class ModelEncodingSequenceDO {
    /**
     * 编码方案主体ID
     */
    private Long encodingId;
    /**
     * 使用模型ID
     */
    private Long modelId;
    /**
     * 流水规则段排序位置，与方案、模型和周期共同标识流水；跨版本应保持位置稳定。
     */
    private Long sortOrder;
    /**
     * 计数周期标识：NONE、DAY:yyyyMMdd、MONTH:yyyyMM或YEAR:yyyy，历史周期记录保留。
     */
    private String periodKey;
    /**
     * 当前周期最后一次正式分配的数字值，不含补零
     */
    private Long currentValue;
    /**
     * 最后一次分配时使用的编码方案版本ID
     */
    private Long encodingVersionId;
    /**
     * 最后一次分配时使用的流水规则段ID
     */
    private Long itemId;
    /**
     * 最后分配时规则的完整JSON快照，保留位数、起始值、步长、补位及重置配置。
     */
    private String configSnapshot;
}
