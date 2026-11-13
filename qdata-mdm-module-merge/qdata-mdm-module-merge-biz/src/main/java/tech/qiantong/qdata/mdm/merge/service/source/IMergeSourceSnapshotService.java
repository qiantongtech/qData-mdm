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

package tech.qiantong.qdata.mdm.merge.service.source;

import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.List;
import java.util.Map;

/**
 * 审核与合并管理共用的固定来源快照加载服务。
 *
 * @author chentangwen
 */
public interface IMergeSourceSnapshotService {

    /**
     * 按成员编号索引快照，来源缺失或内容损坏时对应值为 null。
     *
     * @param members 成员及其固定来源版本
     * @param columns 已加载的模型字段
     * @return 按成员顺序构造的快照索引
     */
    Map<Long, SourceSnapshot> loadSnapshots(List<MergeCandidateGroupMemberDO> members, List<ModelColumnDO> columns);

    /**
     * 加载候选组的有效来源快照，缺失快照不作为可选值。
     *
     * @param groupId 候选组编号
     * @param columns 已加载的模型字段
     * @return 按成员顺序排列的有效快照
     */
    List<SourceSnapshot> loadGroupSnapshots(Long groupId, List<ModelColumnDO> columns);
}
