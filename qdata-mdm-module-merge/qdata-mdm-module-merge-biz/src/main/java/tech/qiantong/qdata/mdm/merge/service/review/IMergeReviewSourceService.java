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

import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.domain.review.MergeReviewSourceContextBO;

import java.util.List;

/**
 * 审核来源固定版本与结果上下文查询。
 *
 * @author chentangwen
 */
public interface IMergeReviewSourceService {

    /**
     * 加载成员固定版本；缺失或无法解析的内容在快照映射中保留 null。
     *
     * @param modelId 模型编号
     * @param members 候选组成员
     * @return 模型字段及按成员编号索引的快照
     */
    MergeReviewSourceContextBO loadContext(Long modelId, List<MergeCandidateGroupMemberDO> members);

    /**
     * 准备结果生成的数据，不写入结果、不改变来源治理状态。
     *
     * @param group 原候选组
     * @param members 待处理成员
     * @param useSurvivorRules 是否加载多成员结果使用的生存规则
     * @return 固定模型版本、字段、快照及规则
     */
    MergeReviewSourceContextBO prepareResult(MergeCandidateGroupDO group,
            List<MergeCandidateGroupMemberDO> members, boolean useSurvivorRules);
}
