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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceHistoryRespDTO;

/**
 * 模型详情页发布历史的返回信息，数据来自真实发布实例。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelInstanceHistoryRespVO extends ModelInstanceHistoryRespDTO {
}
