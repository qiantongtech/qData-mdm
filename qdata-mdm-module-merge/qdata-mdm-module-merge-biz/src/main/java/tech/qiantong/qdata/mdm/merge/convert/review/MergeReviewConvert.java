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

package tech.qiantong.qdata.mdm.merge.convert.review;

import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewLogRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewMatchRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewMemberRespVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateMatchDetailDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateReviewLogDO;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;

/**
 * 审核展示对象转换，仅组装字段，不执行查询或审核规则。
 *
 * @author chentangwen
 */
public final class MergeReviewConvert {

    private MergeReviewConvert() {
    }

    /** 转换候选组基本信息。 */
    public static MergeReviewDetailRespVO toDetail(MergeCandidateGroupDO group) {
        MergeReviewDetailRespVO detail = new MergeReviewDetailRespVO();
        detail.setId(group.getId());
        detail.setTaskInstanceId(group.getTaskInstanceId());
        detail.setGroupNo(group.getGroupNo());
        detail.setModelId(group.getModelId());
        detail.setType(group.getType());
        detail.setStatus(group.getStatus());
        detail.setMaxMatchScore(group.getMaxMatchScore());
        detail.setParentGroupId(group.getParentGroupId());
        detail.setValidFlag(group.getValidFlag());
        detail.setCreateTime(group.getCreateTime());
        return detail;
    }

    /** 转换成员与固定快照，快照缺失时保留成员追溯信息。 */
    public static MergeReviewMemberRespVO toMember(MergeCandidateGroupMemberDO member, SourceSnapshot snapshot) {
        MergeReviewMemberRespVO view = new MergeReviewMemberRespVO();
        view.setMemberId(member.getId());
        view.setSourceRecordId(member.getSourceRecordId());
        view.setSourceVersionId(member.getSourceVersionId());
        view.setMemberRole(member.getMemberRole());
        view.setJoinType(member.getJoinType());
        if (snapshot != null) {
            view.setStableRecordId(snapshot.getStableRecordId());
            view.setSystemCode(snapshot.getSystemCode());
            view.setSystemName(snapshot.getSystemName());
            view.setBusinessName(snapshot.getBusinessName());
            view.setValues(snapshot.getValues());
        }
        return view;
    }

    /** 转换成员间匹配依据。 */
    public static MergeReviewMatchRespVO toMatch(MergeCandidateMatchDetailDO match) {
        MergeReviewMatchRespVO view = new MergeReviewMatchRespVO();
        view.setLeftMemberId(match.getLeftMemberId());
        view.setRightMemberId(match.getRightMemberId());
        view.setMatchRuleId(match.getMatchRuleId());
        view.setMatchScore(match.getMatchScore());
        view.setMatchFields(match.getMatchFields());
        view.setMatchReason(match.getMatchReason());
        return view;
    }

    /** 转换审核日志及前后快照。 */
    public static MergeReviewLogRespVO toLog(MergeCandidateReviewLogDO log) {
        MergeReviewLogRespVO view = new MergeReviewLogRespVO();
        view.setId(log.getId());
        view.setActionType(log.getActionType());
        view.setReviewerId(log.getReviewerId());
        view.setReviewerName(log.getReviewerName());
        view.setReviewTime(log.getReviewTime());
        view.setReason(log.getReason());
        view.setBeforeData(log.getBeforeData());
        view.setAfterData(log.getAfterData());
        return view;
    }
}
