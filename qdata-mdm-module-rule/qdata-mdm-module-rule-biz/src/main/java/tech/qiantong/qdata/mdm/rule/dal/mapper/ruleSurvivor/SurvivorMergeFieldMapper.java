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

package tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import java.util.Collection;
import java.util.List;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeFieldDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeRecordDO;

/** 查询规则与有效合并记录的去重关联。 */
public interface SurvivorMergeFieldMapper extends BaseMapperX<SurvivorMergeFieldDO> {
    default List<SurvivorMergeFieldDO> selectDistinctAssociations(Collection<Long> ruleIds) {
        MPJLambdaWrapper<SurvivorMergeFieldDO> query = new MPJLambdaWrapper<>();
        // 删除标记统一使用下面的参数化条件，避免数据库方言生成重复的逻辑删除字面量。
        query.disableLogicDel().disableSubLogicDel();
        query.distinct().select(SurvivorMergeFieldDO::getSurvivorRuleId, SurvivorMergeFieldDO::getPendingId)
                .innerJoin(SurvivorMergeRecordDO.class, SurvivorMergeRecordDO::getId, SurvivorMergeFieldDO::getPendingId)
                .in(SurvivorMergeFieldDO::getSurvivorRuleId, ruleIds)
                .eq(SurvivorMergeFieldDO::getValidFlag, "1")
                .eq(SurvivorMergeFieldDO::getDelFlag, "0")
                .eq(SurvivorMergeRecordDO::getValidFlag, "1")
                .eq(SurvivorMergeRecordDO::getDelFlag, "0");
        return selectJoinList(SurvivorMergeFieldDO.class, query);
    }
}
