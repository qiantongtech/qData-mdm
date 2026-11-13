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
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelNameDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 审计追溯模型名称只读查询。 */
public interface AuditModelNameMapper extends BaseMapperX<AuditModelNameDO> {
    default List<AuditModelNameDO> selectOperationModels(String name, String code) {
        LambdaQueryWrapper<AuditModelNameDO> query = new LambdaQueryWrapper<AuditModelNameDO>()
                .select(AuditModelNameDO::getId);
        if (name != null && !name.trim().isEmpty()) query.like(AuditModelNameDO::getName, name.trim());
        if (code != null && !code.trim().isEmpty()) query.like(AuditModelNameDO::getCode, code.trim());
        return selectList(query);
    }

    default List<AuditModelNameDO> selectOperationRelations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        return selectList(new LambdaQueryWrapper<AuditModelNameDO>().in(AuditModelNameDO::getId, ids));
    }

    default List<AuditModelNameDO> selectValidNames() {
        return selectList(new LambdaQueryWrapper<AuditModelNameDO>()
                .select(AuditModelNameDO::getName)
                .eq(AuditModelNameDO::getValidFlag, "1")
                .orderByAsc(AuditModelNameDO::getName));
    }
}
