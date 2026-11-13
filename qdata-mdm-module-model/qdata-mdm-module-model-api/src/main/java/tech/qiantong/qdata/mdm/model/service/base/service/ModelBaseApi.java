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

package tech.qiantong.qdata.mdm.model.service.base.service;

import tech.qiantong.qdata.mdm.model.base.dto.ModelBaseRespDTO;

import java.util.Collection;
import java.util.List;

/** 向其他模块提供当前模型定义的展示信息，不加载字段、子模型或物化配置。 */
public interface ModelBaseApi {

    /** 按 ID 批量查询模型、版本和归属位置；不存在或已删除的模型不返回。 */
    List<ModelBaseRespDTO> getModelBaseList(Collection<Long> modelIds);
}
