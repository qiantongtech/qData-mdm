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

package tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTask;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * 主数据质量任务Mapper接口
 *
 * @author qdata
 * @date 2026-09-22
 */
public interface QualityTaskMapper extends BaseMapperX<QualityTaskDO> {

    default PageResult<QualityTaskDO> selectPage(QualityTaskPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<QualityTaskDO>()
                .eqIfPresent(QualityTaskDO::getTaskCode, reqVO.getTaskCode())
                .likeIfPresent(QualityTaskDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(QualityTaskDO::getResponsibleUserId, reqVO.getResponsibleUserId())
                .eqIfPresent(QualityTaskDO::getModelId, reqVO.getModelId())
                .inIfPresent(QualityTaskDO::getModelId, reqVO.getModelIds())
                .eqIfPresent(QualityTaskDO::getModelVersion, reqVO.getModelVersion())
                .eqIfPresent(QualityTaskDO::getDataScopeType, reqVO.getDataScopeType())
                .eqIfPresent(QualityTaskDO::getDataScopeConfig, reqVO.getDataScopeConfig())
                .eqIfPresent(QualityTaskDO::getScheduleFlag, reqVO.getScheduleFlag())
                .eqIfPresent(QualityTaskDO::getScheduleConfig, reqVO.getScheduleConfig())
                .eqIfPresent(QualityTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QualityTaskDO::getLastRunTime, reqVO.getLastRunTime())
                .eqIfPresent(QualityTaskDO::getLastRunStatus, reqVO.getLastRunStatus())
                .eqIfPresent(QualityTaskDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(QualityTaskDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(QualityTaskDO::getUpdateTime, reqVO.getUpdateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(QualityTaskDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
