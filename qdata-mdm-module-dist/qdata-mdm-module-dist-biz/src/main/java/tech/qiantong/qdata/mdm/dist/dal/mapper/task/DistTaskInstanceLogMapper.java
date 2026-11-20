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
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceLogPageReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceLogDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 分发任务执行日志Mapper接口
 *
 * @author qdata
 * @date 2026-10-13
 */
public interface DistTaskInstanceLogMapper extends BaseMapperX<DistTaskInstanceLogDO> {

    /**
     * 根据执行实例、主数据记录和分发结果分页查询逐条日志。
     *
     * @param reqVO 筛选与分页参数
     * @return 执行日志分页结果
     */
    default PageResult<DistTaskInstanceLogDO> selectPage(DistTaskInstanceLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<DistTaskInstanceLogDO>()
                .eqIfPresent(DistTaskInstanceLogDO::getInstanceId, reqVO.getInstanceId())
                .eqIfPresent(DistTaskInstanceLogDO::getMasterRecordId, reqVO.getMasterRecordId())
                .eqIfPresent(DistTaskInstanceLogDO::getMasterCode, reqVO.getMasterCode())
                .likeIfPresent(DistTaskInstanceLogDO::getMasterName, reqVO.getMasterName())
                .eqIfPresent(DistTaskInstanceLogDO::getDistributeStatus, reqVO.getDistributeStatus())
                .eqIfPresent(DistTaskInstanceLogDO::getDistributeTime, reqVO.getDistributeTime())
                .eqIfPresent(DistTaskInstanceLogDO::getRequestData, reqVO.getRequestData())
                .eqIfPresent(DistTaskInstanceLogDO::getResponseCode, reqVO.getResponseCode())
                .eqIfPresent(DistTaskInstanceLogDO::getResponseMessage, reqVO.getResponseMessage())
                .eqIfPresent(DistTaskInstanceLogDO::getFailReason, reqVO.getFailReason())
                .eqIfPresent(DistTaskInstanceLogDO::getRetryCount, reqVO.getRetryCount())
                .eqIfPresent(DistTaskInstanceLogDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
