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

package tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import java.util.Collection;
import java.util.List;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergeLogDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergePendingDO;

/** 读取有效合并结果的质量规则快照。 */
public interface QualityMergeLogMapper extends BaseMapperX<QualityMergeLogDO> {
    default PageResult<QualityMergeLogDO> selectSnapshotPage(Long modelId, PageParam page) {
        MPJLambdaWrapper<QualityMergeLogDO> query = new MPJLambdaWrapper<>();
        query.disableLogicDel().disableSubLogicDel();
        query.select(QualityMergeLogDO::getId, QualityMergeLogDO::getPendingMasterId, QualityMergeLogDO::getRuleSnapshot)
                .innerJoin(QualityMergePendingDO.class, QualityMergePendingDO::getId, QualityMergeLogDO::getPendingMasterId)
                .eq(QualityMergePendingDO::getModelId, modelId)
                .eq(QualityMergePendingDO::getValidFlag, "1")
                .eq(QualityMergePendingDO::getDeleted, "0")
                .eq(QualityMergeLogDO::getValidFlag, "1")
                .eq(QualityMergeLogDO::getDeleted, "0")
                .orderByAsc(QualityMergeLogDO::getId);
        return selectJoinPage(page, QualityMergeLogDO.class, query);
    }
    default List<QualityMergeLogDO> selectSnapshotsByModels(Collection<Long> modelIds) {
        MPJLambdaWrapper<QualityMergeLogDO> query = new MPJLambdaWrapper<>();
        query.disableLogicDel().disableSubLogicDel();
        query.select(QualityMergeLogDO::getPendingMasterId, QualityMergeLogDO::getRuleSnapshot)
                .innerJoin(QualityMergePendingDO.class, QualityMergePendingDO::getId, QualityMergeLogDO::getPendingMasterId)
                .in(QualityMergePendingDO::getModelId, modelIds)
                .eq(QualityMergePendingDO::getValidFlag, "1")
                .eq(QualityMergePendingDO::getDeleted, "0")
                .eq(QualityMergeLogDO::getValidFlag, "1")
                .eq(QualityMergeLogDO::getDeleted, "0");
        return selectJoinList(QualityMergeLogDO.class, query);
    }
}
