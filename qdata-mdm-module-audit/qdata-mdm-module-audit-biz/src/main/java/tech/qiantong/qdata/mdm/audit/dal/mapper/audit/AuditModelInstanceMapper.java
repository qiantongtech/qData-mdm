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
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelInstanceDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.audit.dal.dto.AuditOperationFactQuery;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 发布模型快照只读查询。 */
public interface AuditModelInstanceMapper extends BaseMapperX<AuditModelInstanceDO> {
    default Page<AuditModelInstanceDO> selectOperationPage(Page<AuditModelInstanceDO> page, AuditOperationFactQuery filters) {
        MPJLambdaWrapper<AuditModelInstanceDO> query = new MPJLambdaWrapper<>();
        query.selectAll(AuditModelInstanceDO.class)
                .leftJoin(AuditModelNameDO.class, AuditModelNameDO::getId, AuditModelInstanceDO::getModelId)
                .eq(AuditModelInstanceDO::getStatus, "SUCCEEDED")
                .in(AuditModelInstanceDO::getChangeStatus, "FIRST_PUBLISH", "STRUCTURE_CHANGE", "METADATA_ONLY", "DISABLE", "MIXED");
        if (filters.getModelName() != null) query.like(AuditModelNameDO::getName, filters.getModelName());
        if (filters.getModelCode() != null) query.like(AuditModelNameDO::getCode, filters.getModelCode());
        if (!filters.getModelIds().isEmpty()) query.in(AuditModelInstanceDO::getModelId, filters.getModelIds());
        if (!filters.getTypes().isEmpty()) {
            if (filters.getTypes().contains("DISABLE")) query.eq(AuditModelInstanceDO::getChangeStatus, "DISABLE");
            else query.ne(AuditModelInstanceDO::getChangeStatus, "DISABLE");
        }
        if (filters.getOperatorName() != null) query.like(AuditModelInstanceDO::getCreateBy, filters.getOperatorName());
        if (filters.getBeginTime() != null) query.ge(AuditModelInstanceDO::getFinishedTime, filters.getBeginTime());
        if (filters.getEndTime() != null) query.lt(AuditModelInstanceDO::getFinishedTime, filters.getEndTime());
        if ("id".equals(filters.getOrderBy())) query.orderBy(true, filters.isAscending(), AuditModelInstanceDO::getId);
        else query.orderBy(true, filters.isAscending(), AuditModelInstanceDO::getFinishedTime)
                .orderBy(true, filters.isAscending(), AuditModelInstanceDO::getId);
        return selectJoinPage(page, AuditModelInstanceDO.class, query);
    }

    default List<AuditModelInstanceDO> selectPublishedSnapshots(List<Long> versionIds) {
        return selectList(new LambdaQueryWrapper<AuditModelInstanceDO>()
                .select(AuditModelInstanceDO::getId, AuditModelInstanceDO::getModelId,
                        AuditModelInstanceDO::getModelVersionId, AuditModelInstanceDO::getModelSnapshot)
                .in(AuditModelInstanceDO::getModelVersionId, versionIds)
                .eq(AuditModelInstanceDO::getStatus, "SUCCEEDED")
                .isNotNull(AuditModelInstanceDO::getModelSnapshot)
                .orderByDesc(AuditModelInstanceDO::getId));
    }
}
