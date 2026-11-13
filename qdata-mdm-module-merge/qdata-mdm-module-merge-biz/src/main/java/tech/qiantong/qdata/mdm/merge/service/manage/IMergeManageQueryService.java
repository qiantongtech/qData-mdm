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

package tech.qiantong.qdata.mdm.merge.service.manage;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingApprovalRecordRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingLogPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingRespVO;

/**
 * 合并管理列表及详情查询服务。
 *
 * @author chentangwen
 */
public interface IMergeManageQueryService {

    /**
     * 分页查询合并结果及关联展示数据。
     *
     * @param reqVO 页面查询条件
     * @return 分页结果
     */
    PageResult<MergePendingRespVO> page(MergePendingPageReqVO reqVO);

    /**
     * 查询结果详情、固定来源候选值和质量结果。
     *
     * @param pendingId 待提交结果编号
     * @return 结果详情
     */
    MergePendingDetailRespVO detail(Long pendingId);

    /**
     * 分页查询指定待合并结果的审批与流转记录。
     *
     * @param pendingId 待合并结果编号
     * @param reqVO 分页参数
     * @return 审批与流转记录分页数据
     */
    PageResult<MergePendingApprovalRecordRespVO> approvalRecords(
            Long pendingId, MergePendingLogPageReqVO reqVO);
}
