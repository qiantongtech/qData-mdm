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

package tech.qiantong.qdata.mdm.merge.service.review;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewActionReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewAddReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewSplitReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;

import java.util.List;

/**
 * 匹配审核
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface IMergeReviewService {

    /**
     * 分页查询待审核候选组。
     * @param reqVO 分页及筛选条件
     * @return 候选组分页列表
     */
    PageResult<MergeCandidateGroupDO> page(MergeReviewPageReqVO reqVO);

    /**
     * 批量补充候选组摘要信息。
     * @param rows 候选组列表
     */
    void fillSummary(List<MergeGroupRespVO> rows);

    /**
     * 查询候选组审核详情。
     * @param groupId 候选组编号
     * @return 审核详情
     */
    MergeReviewDetailRespVO detail(Long groupId);

    /**
     * 确认候选组并生成待提交结果。
     * @param reqVO 审核操作参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void confirm(MergeReviewActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 拆分候选组并生成子组。
     * @param reqVO 拆分参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void split(MergeReviewSplitReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 暂缓候选组审核。
     * @param reqVO 审核操作参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void suspend(MergeReviewActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 恢复暂缓候选组审核。
     * @param reqVO 审核操作参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void resume(MergeReviewActionReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 手工补入来源记录到候选组。
     * @param reqVO 补入参数
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void add(MergeReviewAddReqVO reqVO, Long operatorId, String operatorName);

    /**
     * 删除待审核或暂缓处理的候选组，并释放未被其他有效组占用的来源。
     * @param groupId 候选组编号
     * @param operatorId 操作人编号
     * @param operatorName 操作人名称
     */
    void delete(Long groupId, Long operatorId, String operatorName);
}
