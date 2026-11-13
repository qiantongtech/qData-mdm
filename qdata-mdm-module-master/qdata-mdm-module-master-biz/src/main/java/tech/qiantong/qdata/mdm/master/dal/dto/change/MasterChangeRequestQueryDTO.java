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

import java.util.List;

/**
 * 主数据维护审批分页的 Mapper 查询条件；排序列及方向由 XML 固定分支选择。
 */
@Data
public class MasterChangeRequestQueryDTO {
    /** 主数据模型 ID。 */
    private Long modelId;
    /** 申请变更类型。 */
    private String changeType;
    /** 审批状态。 */
    private String approvalStatus;
    /** 已转义并包含前后百分号的申请名称模糊参数。 */
    private String requestNameLike;
    /** 申请明细关联的主数据记录 ID。 */
    private Long masterDataId;
    /** 分类分支记录 ID 分批列表，每批最多 500 个。 */
    private List<List<Long>> categoryRecordIdBatches;
}
