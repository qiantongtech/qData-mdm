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

package tech.qiantong.qdata.mdm.model.service.encoding;

import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateReqDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateRespDTO;

/**
 * 其他模块只需依赖model-api并注入本接口。
 */
public interface ModelEncodingApi {
    /**
     * 按已发布方案正式分配一条编码，独立事务提交后返回；外层事务回滚、业务删除均不回收号码。
     *
     * @param request 方案版本、使用模型及按配置值匹配的业务字段和参考映射值
     * @return 完整编码及本次各流水段分配的数字值
     */
    ModelEncodingGenerateRespDTO generate(ModelEncodingGenerateReqDTO request);

}
