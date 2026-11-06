/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.system.dal.mapper.updater;

import tech.qiantong.qdata.mdm.system.dal.dataobject.updater.SystemVersionTrackDO;
import java.util.Arrays;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import tech.qiantong.qdata.mdm.system.controller.admin.updater.vo.SystemVersionTrackPageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 版本跟踪Mapper interface
 *
 * @author qdata
 * @date 2026-08-12
 */
public interface SystemVersionTrackMapper extends BaseMapperX<SystemVersionTrackDO> {

    default PageResult<SystemVersionTrackDO> selectPage(SystemVersionTrackPageReqVO reqVO) {
        // Define the sorting field (prevent SQL injection, consistent with the database field name)
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // Construct dynamic query conditions
        return selectPage(reqVO, new LambdaQueryWrapperX<SystemVersionTrackDO>()
                .likeIfPresent(SystemVersionTrackDO::getName, reqVO.getName())
                .eqIfPresent(SystemVersionTrackDO::getCurrVersion, reqVO.getCurrVersion())
                .eqIfPresent(SystemVersionTrackDO::getDescription, reqVO.getDescription())
                .eqIfPresent(SystemVersionTrackDO::getAuthor, reqVO.getAuthor())
                .eqIfPresent(SystemVersionTrackDO::getCreateBy, reqVO.getCreateBy())
                .eqIfPresent(SystemVersionTrackDO::getCreatorId, reqVO.getCreatorId())
                .eqIfPresent(SystemVersionTrackDO::getCreateTime, reqVO.getCreateTime())
                // If reqVO.getName() is not empty, add an exact matching condition for name (name = '<name>')
                // .likeIfPresent(SystemVersionTrackDO::getName, reqVO.getName())
                // Sort by createTime field in descending order
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
