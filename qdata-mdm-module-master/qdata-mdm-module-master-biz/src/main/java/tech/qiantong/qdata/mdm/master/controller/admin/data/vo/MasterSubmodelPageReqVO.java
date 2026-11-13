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

package tech.qiantong.qdata.mdm.master.controller.admin.data.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import javax.validation.constraints.Positive;

/** 主数据详情中子模型记录的分页及排序参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MasterSubmodelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Positive(message = "子模型ID必须为正数")
    private Long submodelId;

    public MasterSubmodelPageReqVO() {
        setPageSize(10);
    }
}
