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
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditModelLayerDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
public interface AuditModelLayerMapper extends BaseMapperX<AuditModelLayerDO> {
    default List<AuditModelLayerDO> selectOperationRelations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return selectList(new LambdaQueryWrapper<AuditModelLayerDO>()
                .in(AuditModelLayerDO::getId, ids));
    }
}
