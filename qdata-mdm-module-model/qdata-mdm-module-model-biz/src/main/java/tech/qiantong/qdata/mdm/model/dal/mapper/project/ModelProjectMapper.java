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

package tech.qiantong.qdata.mdm.model.dal.mapper.project;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * 主数据域Mapper接口
 *
 * @author qdata
 * @date 2026-09-15
 */
public interface ModelProjectMapper extends BaseMapperX<ModelProjectDO> {

    /** 查询存在可用主物化实例的主数据域。 */
    List<ModelProjectDO> selectAvailableMainInstanceProjects();

    default PageResult<ModelProjectDO> selectPage(ModelProjectPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelProjectDO>()
                .eqIfPresent(ModelProjectDO::getCode, reqVO.getCode())
                .likeIfPresent(ModelProjectDO::getName, reqVO.getName())
                .eqIfPresent(ModelProjectDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelProjectDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(ModelProjectDO::getValidFlag, reqVO.getValidFlag())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelProjectDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
