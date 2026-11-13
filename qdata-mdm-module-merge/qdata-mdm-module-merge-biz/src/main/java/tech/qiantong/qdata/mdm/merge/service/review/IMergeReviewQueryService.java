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
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewPageReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;

import java.util.List;

/**
 * 审核列表与详情查询，不承担审核写事务。
 *
 * @author chentangwen
 */
public interface IMergeReviewQueryService {

    /** 按审核范围分页查询候选组。 */
    PageResult<MergeCandidateGroupDO> page(MergeReviewPageReqVO reqVO);

    /** 批量补充当前页候选组的目标来源和匹配摘要。 */
    void fillSummary(List<MergeGroupRespVO> rows);

    /** 查询候选组、固定成员版本、匹配依据和审核日志，支持历史组追溯。 */
    MergeReviewDetailRespVO detail(Long groupId);
}
