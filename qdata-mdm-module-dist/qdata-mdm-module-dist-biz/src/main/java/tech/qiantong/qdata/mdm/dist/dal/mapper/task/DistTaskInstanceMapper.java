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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.mapper.task;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 分发任务执行实例Mapper接口
 *
 * @author qdata
 * @date 2026-10-13
 */
public interface DistTaskInstanceMapper extends BaseMapperX<DistTaskInstanceDO> {

    /**
     * 根据任务、批次和执行状态等条件分页查询执行实例。
     *
     * @param reqVO 筛选与分页参数
     * @return 执行实例分页结果
     */
    default PageResult<DistTaskInstanceDO> selectPage(DistTaskInstancePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "start_time", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<DistTaskInstanceDO>()
                .eqIfPresent(DistTaskInstanceDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(DistTaskInstanceDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(DistTaskInstanceDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(DistTaskInstanceDO::getRunStatus, reqVO.getRunStatus())
                .eqIfPresent(DistTaskInstanceDO::getStartTime, reqVO.getStartTime())
                .eqIfPresent(DistTaskInstanceDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(DistTaskInstanceDO::getTotalCount, reqVO.getTotalCount())
                .eqIfPresent(DistTaskInstanceDO::getSuccessCount, reqVO.getSuccessCount())
                .eqIfPresent(DistTaskInstanceDO::getFailureCount, reqVO.getFailureCount())
                .eqIfPresent(DistTaskInstanceDO::getErrorMessage, reqVO.getErrorMessage())
                .eqIfPresent(DistTaskInstanceDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
