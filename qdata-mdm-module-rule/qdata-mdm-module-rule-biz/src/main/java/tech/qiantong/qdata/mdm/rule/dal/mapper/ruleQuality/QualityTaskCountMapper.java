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
import java.util.Collection;
import java.util.List;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityTaskCountDO;

/** 按模型读取未删除的质量任务。 */
public interface QualityTaskCountMapper extends BaseMapperX<QualityTaskCountDO> {
    default List<QualityTaskCountDO> selectByModels(Collection<Long> modelIds) {
        return selectList(new LambdaQueryWrapper<QualityTaskCountDO>()
                .select(QualityTaskCountDO::getId, QualityTaskCountDO::getModelId)
                .in(QualityTaskCountDO::getModelId, modelIds)
                .eq(QualityTaskCountDO::getDeleted, false));
    }
}
