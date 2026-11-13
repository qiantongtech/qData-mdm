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

package tech.qiantong.qdata.mdm.master.dal.dto.change;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 当前登录人已提交申请的查询条件。
 */
@Data
public class MasterMyRequestQueryDTO {

    /** 当前登录人 ID。 */
    private Long applicantId;
    /** 下拉选择的申请人 ID，在当前登录人的申请范围内进一步筛选。 */
    private Long selectedApplicantId;
    /** 已转义的模糊查询参数，包含前后百分号。 */
    private String keywordLike;
    private String requestNoLike;
    private String targetNameLike;
    private String applicantNameLike;
    /** 通过已发布模型名称、编码命中的模型ID，每组不超过500个。 */
    private List<List<Long>> keywordModelIdGroups = Collections.emptyList();
    /** 申请变更类型。 */
    private String changeType;
    /** 主数据模型 ID。 */
    private Long modelId;
    /** 所选主数据域。 */
    private Long projectId;
    /** 当前目录及其下级目录 ID。 */
    private List<Long> layerIds;
    /** 审批状态。 */
    private String approvalStatus;
    /** 状态分组：ALL、PROCESSING 或 ENDED。 */
    private String statusGroup;
}
