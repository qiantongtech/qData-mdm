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

package tech.qiantong.qdata.mdm.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelVerificationStatus;
import com.alibaba.fastjson2.JSONObject;

/**
 * 失败及补偿后的已确认或未知物理效果，不写入成功物化结构。
 */
@Data
@NoArgsConstructor
public class ModelPublishRemainingEffect {

    /**
     * 关联SQL计划步骤
     */
    private Integer stepNo;

    /**
     * 受影响对象
     */
    private ModelSchemaObjectRef object;

    /**
     * 是否已确认实际效果，无法确认使用UNKNOWN
     */
    private ModelVerificationStatus verificationStatus;

    /**
     * 已读取的实际结构，读取失败时不可伪造为空对象
     */
    private JSONObject observedDefinition;

    /**
     * 残留或无法判定原因
     */
    private String reason;
}

