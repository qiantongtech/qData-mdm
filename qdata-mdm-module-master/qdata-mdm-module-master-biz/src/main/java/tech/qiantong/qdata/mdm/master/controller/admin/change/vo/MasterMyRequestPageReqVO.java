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

import java.util.List;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/** 当前登录人已提交申请的分页筛选参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MasterMyRequestPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "关键词不能超过100字")
    private String keyword;

    @Size(max = 100, message = "申请编号不能超过100字")
    private String requestNo;

    @Size(max = 100, message = "目标主数据名称不能超过100字")
    private String targetName;

    /** 下拉选择的申请人用户 ID。 */
    @Positive(message = "申请人ID必须为正数")
    private Long applicantId;

    /** 申请人名称，对应列表展示的用户昵称。 */
    @Size(max = 100, message = "申请人名称不能超过100字")
    private String applicantName;

    @Pattern(regexp = "(?i)\\s*(?:CREATE|UPDATE|BATCH_UPDATE|BATCH_IMPORT|STATUS_CHANGE|VERSION_RESTORE)?\\s*",
            message = "申请类型无效")
    private String changeType;

    @Positive(message = "模型ID必须为正数")
    private Long modelId;

    /** 所选主数据域，不传时查询全部域。 */
    @Positive(message = "主数据域ID必须为正数")
    private Long projectId;

    /** 当前目录及其所有下级目录的 ID。 */
    private List<Long> layerIds;

    @Pattern(regexp = "\\s*[0-4]?\\s*", message = "审批状态无效")
    private String approvalStatus;

    @Pattern(regexp = "(?i)\\s*(?:ALL|PROCESSING|ENDED)?\\s*", message = "状态分组无效")
    private String statusGroup;

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? null : keyword.trim();
    }

    public MasterMyRequestPageReqVO() {
        setPageSize(10);
    }
}
