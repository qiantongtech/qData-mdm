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
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergeFieldAdjustReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingActionReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingApprovalRecordRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingLogPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingRespVO;

/**
 * 合并管理
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface IMergeManageService {

    /**
     * 分页查询待处理合并结果。
     * @param reqVO 分页及状态筛选条件
     * @return 待处理结果分页列表
     */
    PageResult<MergePendingRespVO> page(MergePendingPageReqVO reqVO);

    /**
     * 查询待处理合并结果详情。
     * @param pendingId 待处理结果编号
     * @return 结果详情
     */
    MergePendingDetailRespVO detail(Long pendingId);

    /**
     * 查询指定待合并结果的审批与流转记录。
     *
     * @param pendingId 待合并结果编号
     * @param reqVO 分页参数
     * @return 审批与流转记录分页数据
     */
    PageResult<MergePendingApprovalRecordRespVO> approvalRecords(
            Long pendingId, MergePendingLogPageReqVO reqVO);

    /**
     * 调整待处理结果字段。
     * @param reqVO 字段调整参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void adjust(MergeFieldAdjustReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 确认合并结果并触发质量检测。
     * @param reqVO 确认参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void confirm(MergePendingActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 重新执行质量检测。
     * @param reqVO 复检参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void recheck(MergePendingActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 提交质量通过的合并结果。
     * @param reqVO 提交参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void submit(MergePendingActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 撤回审批中的合并结果。
     * @param reqVO 撤回参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void withdraw(MergePendingActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 取消审批中的合并结果。
     * @param reqVO 取消参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void cancel(MergePendingActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 重新提交合并结果。
     * @param reqVO 提交参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void resubmit(MergePendingActionReqVO reqVO, Long operatorId, String operatorName);
}
