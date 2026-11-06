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

package tech.qiantong.qdata.mdm.system.dal.mapper.monitor;

import tech.qiantong.qdata.mdm.system.dal.dataobject.monitor.ContainerServerDO;
import java.util.Arrays;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerPageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 容器管理-服务器配置Mapper接口
 *
 * @author CHAOS
 * @date 2025-09-02
 */
public interface ContainerServerMapper extends BaseMapperX<ContainerServerDO> {

    default PageResult<ContainerServerDO> selectPage(ContainerServerPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ContainerServerDO>()
                .eqIfPresent(ContainerServerDO::getId, reqVO.getId())
                .likeIfPresent(ContainerServerDO::getServerName, reqVO.getServerName())
                .eqIfPresent(ContainerServerDO::getSshHost, reqVO.getSshHost())
                .eqIfPresent(ContainerServerDO::getSshPort, reqVO.getSshPort())
                .likeIfPresent(ContainerServerDO::getUserName, reqVO.getUserName())
                .eqIfPresent(ContainerServerDO::getServerStatus, reqVO.getServerStatus())
                .eqIfPresent(ContainerServerDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ContainerServerDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
