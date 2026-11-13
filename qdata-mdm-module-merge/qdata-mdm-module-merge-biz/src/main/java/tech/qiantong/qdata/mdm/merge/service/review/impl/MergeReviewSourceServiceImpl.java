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

package tech.qiantong.qdata.mdm.merge.service.review.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.dal.mapper.detect.MergeDuplicateTaskInstanceMapper;
import tech.qiantong.qdata.mdm.merge.domain.review.MergeReviewSourceContextBO;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewSourceService;
import tech.qiantong.qdata.mdm.merge.service.source.IMergeSourceSnapshotService;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor.RuleSurvivorMapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;

/**
 * 审核来源服务，准备固定版本快照及结果生成上下文。
 *
 * @author chentangwen
 */
@Service
public class MergeReviewSourceServiceImpl implements IMergeReviewSourceService {

    @Resource
    private MergeDuplicateTaskInstanceMapper mergeDuplicateTaskInstanceMapper;

    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Resource
    private RuleSurvivorMapper ruleSurvivorMapper;

    @Resource
    private IMergeSourceSnapshotService mergeSourceSnapshotService;

    /**
     * 准备审核所需模型字段及固定版本快照，缺失快照保留 null。
     */
    @Override
    public MergeReviewSourceContextBO loadContext(Long modelId, List<MergeCandidateGroupMemberDO> members) {
        MergeReviewSourceContextBO context = new MergeReviewSourceContextBO();
        if (members.isEmpty()) {
            return context;
        }
        List<ModelColumnDO> columns = modelColumnMapper.selectList(
                new LambdaQueryWrapper<ModelColumnDO>()
                        .eq(ModelColumnDO::getModelId, modelId));
        context.setColumns(columns);
        context.setSnapshots(mergeSourceSnapshotService.loadSnapshots(members, columns));
        return context;
    }

    /**
     * 拆分产生的多个子组复用模型字段、固定版本与生存规则上下文。
     */
    @Override
    public MergeReviewSourceContextBO prepareResult(MergeCandidateGroupDO group,
            List<MergeCandidateGroupMemberDO> members, boolean useSurvivorRules) {
        MergeReviewSourceContextBO context = loadContext(group.getModelId(), members);
        MergeDuplicateTaskInstanceDO instance = group.getTaskInstanceId() == null ? null
                : mergeDuplicateTaskInstanceMapper.selectById(group.getTaskInstanceId());
        context.setModelVersionId(instance == null ? null : instance.getModelVersionId());
        for (MergeCandidateGroupMemberDO member : members) {
            context.getSourceSnapshots().put(member.getSourceRecordId(), context.getSnapshots().get(member.getId()));
        }
        if (useSurvivorRules) {
            List<RuleSurvivorDO> rules = ruleSurvivorMapper.selectList(
                    new LambdaQueryWrapper<RuleSurvivorDO>()
                            .eq(RuleSurvivorDO::getModelId, group.getModelId())
                            .eq(RuleSurvivorDO::getValidFlag, Boolean.TRUE));
            List<RuleSurvivorDO> executable = new ArrayList<>();
            for (RuleSurvivorDO rule : rules) {
                if (!"1".equals(rule.getAbnormalFlag())) {
                    executable.add(rule);
                }
            }
            context.setSurvivorRules(executable);
        }
        return context;
    }

}
