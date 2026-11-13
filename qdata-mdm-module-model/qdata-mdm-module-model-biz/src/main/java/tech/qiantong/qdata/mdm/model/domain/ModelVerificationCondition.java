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
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelVerificationType;
import com.alibaba.fastjson2.JSONObject;

/**
 * SQL前后及补偿核验的声明式条件；正向条件由校验服务解释，不能把读取异常当成条件满足。
 */
@Data
@NoArgsConstructor
public class ModelVerificationCondition {

    /**
     * 本步骤内稳定条件标识
     */
    private String conditionKey;

    /**
     * 核验条件类型
     */
    private ModelVerificationType type;

    /**
     * 被核验的稳定对象
     */
    private ModelSchemaObjectRef object;

    /**
     * 规范化预期结构或数据条件参数，不存任意可执行脚本
     */
    private JSONObject expected = new JSONObject();

    /**
     * 最近一次核验结果
     */
    private ModelVerificationStatus status = ModelVerificationStatus.PENDING;

    /**
     * 未满足或无法核验的原因
     */
    private String failureReason;
}

