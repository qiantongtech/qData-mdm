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

package tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 单字段调整
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeFieldAdjustItemVO {
    @NotNull(message = "字段不能为空")
    private Long fieldId;
    private String finalValue;
    private String valueSourceType;
    private Long sourceVersionId;
    /** 是否由用户明确选择系统推荐项；为空时兼容旧调用，按最终值判断。 */
    private Boolean systemRecommended;
    private String adjustReason;
}
