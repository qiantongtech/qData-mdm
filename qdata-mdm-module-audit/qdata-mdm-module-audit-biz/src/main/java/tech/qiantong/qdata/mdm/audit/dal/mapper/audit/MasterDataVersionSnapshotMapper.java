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
import java.util.List;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.MasterDataVersionSnapshotDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 审计追溯只读使用的权威主数据版本 Mapper。 */
public interface MasterDataVersionSnapshotMapper extends BaseMapperX<MasterDataVersionSnapshotDO> {
    default List<MasterDataVersionSnapshotDO> selectOperationRelations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        return selectList(new LambdaQueryWrapper<MasterDataVersionSnapshotDO>()
                .in(MasterDataVersionSnapshotDO::getId, ids));
    }

    default Page<MasterDataVersionSnapshotDO> selectTracePage(Page<MasterDataVersionSnapshotDO> page, String order) {
        LambdaQueryWrapper<MasterDataVersionSnapshotDO> query = new LambdaQueryWrapper<>();
        if ("ascending".equals(order)) {
            query.orderByAsc(MasterDataVersionSnapshotDO::getId);
        } else if ("descending".equals(order)) {
            query.orderByDesc(MasterDataVersionSnapshotDO::getId);
        } else {
            query.orderByDesc(MasterDataVersionSnapshotDO::getGenerateTime, MasterDataVersionSnapshotDO::getId);
        }
        return selectPage(page, query);
    }
    default List<MasterDataVersionSnapshotDO> selectTraceVersions() {
        return selectList(new LambdaQueryWrapper<MasterDataVersionSnapshotDO>());
    }

    default List<MasterDataVersionSnapshotDO> selectObjectVersions(Long modelId, Long masterRecordId) {
        return selectList(new LambdaQueryWrapper<MasterDataVersionSnapshotDO>()
                .eq(MasterDataVersionSnapshotDO::getModelId, modelId)
                .eq(MasterDataVersionSnapshotDO::getMasterRecordId, masterRecordId)
                .orderByAsc(MasterDataVersionSnapshotDO::getVersionSeq, MasterDataVersionSnapshotDO::getId));
    }

    default MasterDataVersionSnapshotDO selectLatestSnapshot(Long modelId, Long masterRecordId) {
        LambdaQueryWrapper<MasterDataVersionSnapshotDO> query = new LambdaQueryWrapper<MasterDataVersionSnapshotDO>()
                .select(MasterDataVersionSnapshotDO::getModelVersionId, MasterDataVersionSnapshotDO::getSnapshotData)
                .eq(MasterDataVersionSnapshotDO::getModelId, modelId)
                .eq(MasterDataVersionSnapshotDO::getMasterRecordId, masterRecordId)
                .orderByDesc(MasterDataVersionSnapshotDO::getCurrentFlag,
                        MasterDataVersionSnapshotDO::getVersionSeq, MasterDataVersionSnapshotDO::getId);
        Page<MasterDataVersionSnapshotDO> page = selectPage(new Page<>(1, 1, false), query);
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0);
    }
}
