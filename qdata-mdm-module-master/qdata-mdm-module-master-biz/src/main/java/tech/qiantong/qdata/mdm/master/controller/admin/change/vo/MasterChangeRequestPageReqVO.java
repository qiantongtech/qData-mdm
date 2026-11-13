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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import tech.qiantong.qdata.mdm.common.core.page.PageParam;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 权威主数据直接维护审批分页筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MasterChangeRequestPageReqVO extends PageParam {
    @Size(max = 128, message = "申请名称不能超过128字")
    private String requestName;
    @Size(max = 19, message = "主数据ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "主数据ID格式无效")
    private String masterDataId;
    @Pattern(regexp = "^$|CREATE|UPDATE|BATCH_UPDATE|BATCH_IMPORT|STATUS_CHANGE|VERSION_RESTORE",
            message = "维护操作类型无效")
    private String changeType;
    @Pattern(regexp = "^$|[0-4]", message = "审批状态无效")
    private String approvalStatus;
    /** 分类树节点的 ID；分类模型按当前节点及其后代筛选审批单。 */
    @Size(max = 19, message = "分类节点ID长度不能超过19位")
    @Pattern(regexp = "^$|^[1-9]\\d*$", message = "分类节点ID格式无效")
    private String treeId;

    public MasterChangeRequestPageReqVO() {
        setPageSize(10);
    }
}
