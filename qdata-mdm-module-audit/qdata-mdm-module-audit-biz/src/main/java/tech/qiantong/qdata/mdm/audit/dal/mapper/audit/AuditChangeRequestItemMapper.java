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

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.List;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestDO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditChangeRequestItemDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 生命周期执行结果只读查询。 */
public interface AuditChangeRequestItemMapper extends BaseMapperX<AuditChangeRequestItemDO> {
    default List<AuditChangeRequestItemDO> selectEffectiveStatesForRecords(Long modelId, List<Long> recordIds) {
        MPJLambdaWrapper<AuditChangeRequestItemDO> query = new MPJLambdaWrapper<>();
        query.select(AuditChangeRequestItemDO::getMasterRecordId, AuditChangeRequestItemDO::getResultMasterRecordId,
                        AuditChangeRequestItemDO::getTargetStatus, AuditChangeRequestItemDO::getExecuteTime)
                .innerJoin(AuditChangeRequestDO.class, AuditChangeRequestDO::getId, AuditChangeRequestItemDO::getRequestId)
                .eq(AuditChangeRequestDO::getModelId, modelId)
                .and(condition -> condition.in(AuditChangeRequestItemDO::getMasterRecordId, recordIds)
                        .or().in(AuditChangeRequestItemDO::getResultMasterRecordId, recordIds))
                .eq(AuditChangeRequestItemDO::getExecuteStatus, "2")
                .in(AuditChangeRequestItemDO::getChangeType, Arrays.asList("FREEZE", "UNFREEZE", "INVALIDATE", "RECOVER", "ARCHIVE"))
                .eq(AuditChangeRequestItemDO::getDelFlag, "0")
                .orderByDesc(AuditChangeRequestItemDO::getExecuteTime, AuditChangeRequestItemDO::getId);
        return selectJoinList(AuditChangeRequestItemDO.class, query);
    }
    default List<AuditChangeRequestItemDO> selectExecutedLifecycleItems(Long id) {
        LambdaQueryWrapper<AuditChangeRequestItemDO> query = new LambdaQueryWrapper<AuditChangeRequestItemDO>()
                .eq(AuditChangeRequestItemDO::getDelFlag, "0")
                .in(AuditChangeRequestItemDO::getChangeType, Arrays.asList("FREEZE", "UNFREEZE", "INVALIDATE", "RECOVER", "ARCHIVE"))
                .in(AuditChangeRequestItemDO::getExecuteStatus, Arrays.asList("2", "3"));
        if (id != null) query.eq(AuditChangeRequestItemDO::getId, id);
        return selectList(query);
    }

    default List<AuditChangeRequestItemDO> selectEffectiveStates(Long modelId, Long recordId) {
        MPJLambdaWrapper<AuditChangeRequestItemDO> query = new MPJLambdaWrapper<>();
        query.select(AuditChangeRequestItemDO::getTargetStatus, AuditChangeRequestItemDO::getExecuteTime)
                .innerJoin(AuditChangeRequestDO.class, AuditChangeRequestDO::getId, AuditChangeRequestItemDO::getRequestId)
                .eq(AuditChangeRequestDO::getModelId, modelId)
                .and(condition -> condition.eq(AuditChangeRequestItemDO::getMasterRecordId, recordId)
                        .or().eq(AuditChangeRequestItemDO::getResultMasterRecordId, recordId))
                .eq(AuditChangeRequestItemDO::getExecuteStatus, "2")
                .in(AuditChangeRequestItemDO::getChangeType, Arrays.asList("FREEZE", "UNFREEZE", "INVALIDATE", "RECOVER", "ARCHIVE"))
                .eq(AuditChangeRequestItemDO::getDelFlag, "0")
                .orderByDesc(AuditChangeRequestItemDO::getExecuteTime, AuditChangeRequestItemDO::getId);
        return selectJoinList(AuditChangeRequestItemDO.class, query);
    }
}
