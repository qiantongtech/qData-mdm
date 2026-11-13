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
import java.util.List;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelColumnDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 审计追溯模型字段只读查询。 */
public interface AuditModelColumnMapper extends BaseMapperX<AuditModelColumnDO> {
    /** 仅在模型发布快照缺失时读取当前模型的名称和编码字段配置。 */
    default List<AuditModelColumnDO> selectIdentityColumns(Long modelId) {
        return selectList(new LambdaQueryWrapper<AuditModelColumnDO>()
                .select(AuditModelColumnDO::getCode, AuditModelColumnDO::getNameFlag,
                        AuditModelColumnDO::getEncodingMode, AuditModelColumnDO::getEncodingVersionId)
                .eq(AuditModelColumnDO::getModelId, modelId)
                .eq(AuditModelColumnDO::getDelFlag, "0")
                .orderByAsc(AuditModelColumnDO::getId));
    }

    default List<AuditModelColumnDO> selectForModels(List<Long> modelIds) {
        return selectList(new LambdaQueryWrapper<AuditModelColumnDO>()
                .in(AuditModelColumnDO::getModelId, modelIds)
                .eq(AuditModelColumnDO::getDelFlag, "0")
                .orderByAsc(AuditModelColumnDO::getId));
    }
    default List<AuditModelColumnDO> selectNameColumns(Long modelId) {
        return selectList(new LambdaQueryWrapper<AuditModelColumnDO>()
                .select(AuditModelColumnDO::getCode)
                .eq(AuditModelColumnDO::getModelId, modelId)
                .eq(AuditModelColumnDO::getNameFlag, "1")
                .eq(AuditModelColumnDO::getDelFlag, "0")
                .orderByAsc(AuditModelColumnDO::getId));
    }

    default List<AuditModelColumnDO> selectLabels(Long modelId) {
        return selectList(new LambdaQueryWrapper<AuditModelColumnDO>()
                .select(AuditModelColumnDO::getCode, AuditModelColumnDO::getName)
                .eq(AuditModelColumnDO::getModelId, modelId)
                .eq(AuditModelColumnDO::getDelFlag, "0"));
    }
}
