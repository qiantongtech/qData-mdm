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

package tech.qiantong.qdata.mdm.quality.dal.mapper.qualityTaskInstance;

import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
import java.util.Arrays;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;


/**
 * 主数据质量任务执行实例Mapper接口
 *
 * @author qdata
 * @date 2026-09-22
 */
public interface QualityTaskInstanceMapper extends BaseMapperX<QualityTaskInstanceDO> {

    List<QualityTaskInstanceDO> selectLatestByTaskIds(@Param("taskIds") Collection<Long> taskIds);

    default PageResult<QualityTaskInstanceDO> selectPage(QualityTaskInstancePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<QualityTaskInstanceDO>()
                .eqIfPresent(QualityTaskInstanceDO::getQualityTaskId, reqVO.getQualityTaskId())
                .eqIfPresent(QualityTaskInstanceDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(QualityTaskInstanceDO::getModelId, reqVO.getModelId())
                .eqIfPresent(QualityTaskInstanceDO::getModelVersion, reqVO.getModelVersion())
                .eqIfPresent(QualityTaskInstanceDO::getExecutionNo, reqVO.getExecutionNo())
                .eqIfPresent(QualityTaskInstanceDO::getPersonCharge, reqVO.getPersonCharge())
                .eqIfPresent(QualityTaskInstanceDO::getContactNumber, reqVO.getContactNumber())
                .eqIfPresent(QualityTaskInstanceDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(QualityTaskInstanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QualityTaskInstanceDO::getStartTime, reqVO.getStartTime())
                .eqIfPresent(QualityTaskInstanceDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(QualityTaskInstanceDO::getTotalCount, reqVO.getTotalCount())
                .eqIfPresent(QualityTaskInstanceDO::getPassCount, reqVO.getPassCount())
                .eqIfPresent(QualityTaskInstanceDO::getFailCount, reqVO.getFailCount())
                .eqIfPresent(QualityTaskInstanceDO::getErrorMessage, reqVO.getErrorMessage())
                .eqIfPresent(QualityTaskInstanceDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(QualityTaskInstanceDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(QualityTaskInstanceDO::getUpdateTime, reqVO.getUpdateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(QualityTaskInstanceDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
