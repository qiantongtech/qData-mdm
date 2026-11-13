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

package tech.qiantong.qdata.mdm.audit.dal.mapper.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import java.util.List;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditSourceGroup;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditSourceRelationDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 来源追溯关系只读查询。 */
public interface AuditSourceRelationMapper extends BaseMapperX<AuditSourceRelationDO> {
    default List<AuditSourceGroup> selectActiveGroups(Long masterRecordId) {
        MPJLambdaWrapper<AuditSourceRelationDO> query = new MPJLambdaWrapper<>();
        query.select(AuditSourceRelationDO::getModelId, AuditSourceRelationDO::getMasterRecordId)
                .selectMin(AuditSourceRelationDO::getId, AuditSourceGroup::getSourceRelId)
                .eq(AuditSourceRelationDO::getStatus, "1")
                .eq(AuditSourceRelationDO::getDelFlag, "0")
                .eq(masterRecordId != null, AuditSourceRelationDO::getMasterRecordId, masterRecordId)
                .groupBy(AuditSourceRelationDO::getModelId, AuditSourceRelationDO::getMasterRecordId);
        return selectJoinList(AuditSourceGroup.class, query);
    }

    default List<AuditSourceRelationDO> selectActive(Long modelId, Long masterRecordId) {
        return selectList(new LambdaQueryWrapper<AuditSourceRelationDO>()
                .eq(AuditSourceRelationDO::getModelId, modelId)
                .eq(AuditSourceRelationDO::getMasterRecordId, masterRecordId)
                .eq(AuditSourceRelationDO::getStatus, "1")
                .eq(AuditSourceRelationDO::getDelFlag, "0")
                .orderByAsc(AuditSourceRelationDO::getId));
    }
}
