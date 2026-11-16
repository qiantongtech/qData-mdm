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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.mapper.service;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServicePageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.util.StringUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;

/**
 * 数据服务Mapper，复用框架分页和逻辑删除。
 *
 * @author chentangwen
 *
 */
public interface DistServiceMapper extends BaseMapperX<DistServiceDO> {
    /**
     * 批量查询创建人电话，仅返回用户ID和电话。
     */
    List<DistServiceRespVO> selectCreatorPhonesByIds(@Param("ids") Collection<Long> ids);

    default PageResult<DistServiceDO> selectPage(DistServicePageReqVO reqVO) {
        LambdaQueryWrapperX<DistServiceDO> query = new LambdaQueryWrapperX<DistServiceDO>()
                .likeIfPresent(DistServiceDO::getName, reqVO.getName())
                .likeIfPresent(DistServiceDO::getCode, reqVO.getCode())
                .likeIfPresent(DistServiceDO::getServicePath, reqVO.getServicePath())
                .eqIfPresent(DistServiceDO::getModelId, reqVO.getModelId())
                .eqIfPresent(DistServiceDO::getApiType, reqVO.getApiType())
                .eqIfPresent(DistServiceDO::getPublishStatus, reqVO.getPublishStatus());
        if (DistServiceConstants.STATUS_DISABLED.equals(reqVO.getStatus())) {
            query.and(w -> w.in(DistServiceDO::getStatus, DistServiceConstants.STATUS_DISABLED,
                    DistServiceConstants.LEGACY_STATUS_DISABLED).or().isNull(DistServiceDO::getStatus));
        } else {
            query.eqIfPresent(DistServiceDO::getStatus, reqVO.getStatus());
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            query.and(w -> w.like(DistServiceDO::getName, keyword).or().like(DistServiceDO::getServicePath, keyword));
        }
        if (StringUtils.hasText(reqVO.getOrderByColumn())) {
            query.orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(),
                    new HashSet<>(Arrays.asList("id", "create_time", "update_time")));
        } else {
            query.orderByDesc(DistServiceDO::getCreateTime);
        }
        return selectPage(reqVO, query);
    }
}
