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

package tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeFieldDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeRecordDO;

/** 分页读取实际关联生存规则的有效合并记录。 */
public interface SurvivorMergeRecordMapper extends BaseMapperX<SurvivorMergeRecordDO> {
    default PageResult<SurvivorMergeRecordDO> selectPageByRule(Long ruleId, PageParam page) {
        MPJLambdaWrapper<SurvivorMergeRecordDO> query = new MPJLambdaWrapper<>();
        // 删除标记统一使用下面的参数化条件，避免数据库方言生成重复的逻辑删除字面量。
        query.disableLogicDel().disableSubLogicDel();
        query.distinct().select(SurvivorMergeRecordDO::getId, SurvivorMergeRecordDO::getModelId,
                        SurvivorMergeRecordDO::getStatus, SurvivorMergeRecordDO::getCreateTime, SurvivorMergeRecordDO::getUpdateTime)
                .innerJoin(SurvivorMergeFieldDO.class, SurvivorMergeFieldDO::getPendingId, SurvivorMergeRecordDO::getId)
                .eq(SurvivorMergeFieldDO::getSurvivorRuleId, ruleId)
                .eq(SurvivorMergeFieldDO::getValidFlag, "1")
                .eq(SurvivorMergeFieldDO::getDelFlag, "0")
                .eq(SurvivorMergeRecordDO::getValidFlag, "1")
                .eq(SurvivorMergeRecordDO::getDelFlag, "0");
        boolean asc = "asc".equalsIgnoreCase(page.getIsAsc());
        boolean validDirection = asc || "desc".equalsIgnoreCase(page.getIsAsc());
        if (validDirection && "id".equals(page.getOrderByColumn())) {
            query.orderBy(true, asc, SurvivorMergeRecordDO::getId);
        } else if (validDirection && ("update_time".equals(page.getOrderByColumn()) || "updateTime".equals(page.getOrderByColumn()))) {
            query.orderBy(true, asc, SurvivorMergeRecordDO::getUpdateTime)
                    .orderByDesc(SurvivorMergeRecordDO::getId);
        } else {
            query.orderByDesc(SurvivorMergeRecordDO::getCreateTime, SurvivorMergeRecordDO::getId);
        }
        return selectJoinPage(page, SurvivorMergeRecordDO.class, query);
    }
}
