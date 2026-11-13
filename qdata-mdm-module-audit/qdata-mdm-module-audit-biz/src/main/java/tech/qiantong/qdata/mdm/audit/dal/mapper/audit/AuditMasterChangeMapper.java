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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import java.util.List;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditMasterChangeDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.audit.dal.dto.AuditOperationFactQuery;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 审计追溯主数据变更查询。 */
public interface AuditMasterChangeMapper extends BaseMapperX<AuditMasterChangeDO> {
    default Page<AuditMasterChangeDO> selectOperationPage(Page<AuditMasterChangeDO> page, AuditOperationFactQuery filters) {
        return selectJoinPage(page, AuditMasterChangeDO.class, operationQuery(filters));
    }

    default List<AuditMasterChangeDO> selectOperationCandidates(AuditOperationFactQuery filters) {
        return selectJoinList(AuditMasterChangeDO.class, operationQuery(filters));
    }

    default MPJLambdaWrapper<AuditMasterChangeDO> operationQuery(AuditOperationFactQuery filters) {
        MPJLambdaWrapper<AuditMasterChangeDO> query = new MPJLambdaWrapper<>();
        query.selectAll(AuditMasterChangeDO.class)
                .leftJoin(AuditModelNameDO.class, AuditModelNameDO::getId, AuditMasterChangeDO::getModelId)
                .eq(AuditMasterChangeDO::getDelFlag, "0")
                .eq(AuditMasterChangeDO::getProcessResult, "1");
        if (filters.getModelName() != null) query.like(AuditModelNameDO::getName, filters.getModelName());
        if (filters.getModelCode() != null) query.like(AuditModelNameDO::getCode, filters.getModelCode());
        if (!filters.getModelIds().isEmpty()) query.in(AuditMasterChangeDO::getModelId, filters.getModelIds());
        if (!filters.getTypes().isEmpty()) query.in(AuditMasterChangeDO::getType, filters.getTypes());
        if (filters.getOperatorName() != null) query.like(AuditMasterChangeDO::getOperatorName, filters.getOperatorName());
        if (filters.getRecordId() != null) query.eq(AuditMasterChangeDO::getMasterRecordId, filters.getRecordId());
        if (filters.getBeginTime() != null) query.ge(AuditMasterChangeDO::getChangeTime, filters.getBeginTime());
        if (filters.getEndTime() != null) query.lt(AuditMasterChangeDO::getChangeTime, filters.getEndTime());
        if ("id".equals(filters.getOrderBy())) {
            query.orderBy(true, filters.isAscending(), AuditMasterChangeDO::getId);
        } else {
            query.orderBy(true, filters.isAscending(), AuditMasterChangeDO::getChangeTime)
                    .orderBy(true, filters.isAscending(), AuditMasterChangeDO::getId);
        }
        return query;
    }

    default List<AuditMasterChangeDO> selectOperationRelations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        return selectList(new LambdaQueryWrapper<AuditMasterChangeDO>().in(AuditMasterChangeDO::getId, ids));
    }

    default List<AuditMasterChangeDO> selectEffectiveStatesForRecords(Long modelId, List<Long> recordIds) {
        return selectList(new LambdaQueryWrapper<AuditMasterChangeDO>()
                .select(AuditMasterChangeDO::getMasterRecordId, AuditMasterChangeDO::getAfterStatus, AuditMasterChangeDO::getChangeTime)
                .eq(AuditMasterChangeDO::getModelId, modelId)
                .in(AuditMasterChangeDO::getMasterRecordId, recordIds)
                .eq(AuditMasterChangeDO::getProcessResult, "1")
                .isNotNull(AuditMasterChangeDO::getAfterStatus)
                .eq(AuditMasterChangeDO::getDelFlag, "0")
                .orderByDesc(AuditMasterChangeDO::getChangeTime, AuditMasterChangeDO::getId));
    }
    default List<AuditMasterChangeDO> selectTraceChanges() {
        return selectList(new LambdaQueryWrapper<AuditMasterChangeDO>().eq(AuditMasterChangeDO::getDelFlag, "0"));
    }

    default List<AuditMasterChangeDO> selectByRequestItemIds(List<Long> itemIds) {
        return selectList(new LambdaQueryWrapper<AuditMasterChangeDO>()
                .select(AuditMasterChangeDO::getRequestItemId)
                .in(AuditMasterChangeDO::getRequestItemId, itemIds)
                .eq(AuditMasterChangeDO::getDelFlag, "0"));
    }

    default List<AuditMasterChangeDO> selectEffectiveStates(Long modelId, Long recordId) {
        return selectList(new LambdaQueryWrapper<AuditMasterChangeDO>()
                .select(AuditMasterChangeDO::getAfterStatus, AuditMasterChangeDO::getChangeTime)
                .eq(AuditMasterChangeDO::getModelId, modelId)
                .eq(AuditMasterChangeDO::getMasterRecordId, recordId)
                .eq(AuditMasterChangeDO::getProcessResult, "1")
                .isNotNull(AuditMasterChangeDO::getAfterStatus)
                .eq(AuditMasterChangeDO::getDelFlag, "0")
                .orderByDesc(AuditMasterChangeDO::getChangeTime, AuditMasterChangeDO::getId));
    }
}
