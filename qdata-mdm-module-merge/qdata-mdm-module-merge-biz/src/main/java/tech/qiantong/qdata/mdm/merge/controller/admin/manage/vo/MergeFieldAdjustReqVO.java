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

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人工调整待提交字段
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeFieldAdjustReqVO {
    @NotNull(message = "待提交结果不能为空")
    private Long pendingId;
    /** 本次字段调整入口：start 首次处理，reprocess 重新处理。 */
    private String processingMode;
    @Valid
    @NotEmpty(message = "调整字段不能为空")
    private List<MergeFieldAdjustItemVO> fields;
}
