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

package tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergePendingDO;

/** 读取质量规则快照中关联的有效合并记录。 */
public interface QualityMergePendingMapper extends BaseMapperX<QualityMergePendingDO> {
    default PageResult<QualityMergePendingDO> selectAssociatedPage(Collection<Long> ids, PageParam page) {
        List<Long> allIds = new ArrayList<>(ids);
        LambdaQueryWrapper<QualityMergePendingDO> query = new LambdaQueryWrapper<>();
        query.select(QualityMergePendingDO::getId, QualityMergePendingDO::getModelId,
                QualityMergePendingDO::getStatus, QualityMergePendingDO::getUpdateTime)
            .eq(QualityMergePendingDO::getValidFlag, "1")
            .eq(QualityMergePendingDO::getDeleted, "0")
            .and(group -> {
                for (int start = 0; start < allIds.size(); start += 500) {
                    if (start > 0) group.or();
                    group.in(QualityMergePendingDO::getId, allIds.subList(start, Math.min(start + 500, allIds.size())));
                }
            });
        String sort = page.getOrderByColumn();
        boolean asc = "asc".equalsIgnoreCase(page.getIsAsc());
        boolean validDirection = asc || "desc".equalsIgnoreCase(page.getIsAsc());
        if (validDirection && "id".equals(sort)) {
            query.orderBy(true, asc, QualityMergePendingDO::getId);
        } else if (validDirection && ("update_time".equals(sort) || "updateTime".equals(sort))) {
            query.orderBy(true, asc, QualityMergePendingDO::getUpdateTime).orderByDesc(QualityMergePendingDO::getId);
        } else {
            query.orderByDesc(QualityMergePendingDO::getId);
        }
        return selectPage(page, query);
    }
}
