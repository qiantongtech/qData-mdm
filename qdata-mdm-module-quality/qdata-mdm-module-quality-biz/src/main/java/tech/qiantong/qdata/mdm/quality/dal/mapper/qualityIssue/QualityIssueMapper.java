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

package tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssue;

import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssuePageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 问题数据Mapper接口
 *
 * @author qdata
 * @date 2026-09-23
 */
public interface QualityIssueMapper extends BaseMapperX<QualityIssueDO> {

    default Long selectIssueCount(QualityIssuePageReqVO reqVO, String status) {
        return selectCount(buildQuery(reqVO).eqIfPresent(QualityIssueDO::getStatus, status));
    }

    default PageResult<QualityIssueDO> selectPage(QualityIssuePageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time", "last_found_time"));
        return selectPage(reqVO, buildQuery(reqVO)
                .eqIfPresent(QualityIssueDO::getStatus, reqVO.getStatus())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    /** 列表与状态统计共用筛选条件，状态由各自入口指定。 */
    default LambdaQueryWrapperX<QualityIssueDO> buildQuery(QualityIssuePageReqVO reqVO) {
        return new LambdaQueryWrapperX<QualityIssueDO>()
                .eqIfPresent(QualityIssueDO::getIssueCode, reqVO.getIssueCode())
                .eqIfPresent(QualityIssueDO::getModelId, reqVO.getModelId())
                .inIfPresent(QualityIssueDO::getModelId, reqVO.getModelIds())
                .eqIfPresent(QualityIssueDO::getMasterRecordId, reqVO.getMasterRecordId())
                .eqIfPresent(QualityIssueDO::getFirstFoundTime, reqVO.getFirstFoundTime())
                .eqIfPresent(QualityIssueDO::getLastFoundTime, reqVO.getLastFoundTime())
                .eqIfPresent(QualityIssueDO::getCloseTime, reqVO.getCloseTime())
                .eqIfPresent(QualityIssueDO::getCloserId, reqVO.getCloserId())
                .eqIfPresent(QualityIssueDO::getRectificationStatus, reqVO.getRectificationStatus())
                .eqIfPresent(QualityIssueDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(QualityIssueDO::getCreateTime, reqVO.getCreateTime());
    }
}
