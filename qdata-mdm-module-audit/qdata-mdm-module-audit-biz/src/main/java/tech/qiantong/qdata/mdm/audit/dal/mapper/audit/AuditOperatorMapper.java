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
import java.util.Collection;
import java.util.List;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperatorDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/** 操作审计使用的用户联系方式只读查询。 */
public interface AuditOperatorMapper extends BaseMapperX<AuditOperatorDO> {
    default List<AuditOperatorDO> selectCurrentPhones(Collection<Long> userIds) {
        return selectList(new LambdaQueryWrapper<AuditOperatorDO>()
                .select(AuditOperatorDO::getUserId, AuditOperatorDO::getPhoneNumber)
                .in(AuditOperatorDO::getUserId, userIds)
                .eq(AuditOperatorDO::getDelFlag, "0"));
    }
}
