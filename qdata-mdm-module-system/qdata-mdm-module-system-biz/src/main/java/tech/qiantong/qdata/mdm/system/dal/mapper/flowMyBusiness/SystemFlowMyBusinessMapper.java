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

package tech.qiantong.qdata.mdm.system.dal.mapper.flowMyBusiness;

import tech.qiantong.qdata.mdm.system.dal.dataobject.flowMyBusiness.SystemFlowMyBusinessDO;
import java.util.Arrays;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessPageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 审批记录Mapper接口
 *
 * @author qdata
 * @date 2025-04-25
 */
public interface SystemFlowMyBusinessMapper extends BaseMapperX<SystemFlowMyBusinessDO> {

    default PageResult<SystemFlowMyBusinessDO> selectPage(SystemFlowMyBusinessPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<SystemFlowMyBusinessDO>()
                .eqIfPresent(SystemFlowMyBusinessDO::getProcessDefinitionKey, reqVO.getProcessDefinitionKey())
                .eqIfPresent(SystemFlowMyBusinessDO::getProcessDefinitionId, reqVO.getProcessDefinitionId())
                .inIfPresent(SystemFlowMyBusinessDO::getProcessInstanceId, reqVO.getProcessInstanceIdList())
                .eqIfPresent(SystemFlowMyBusinessDO::getProcessInstanceId, reqVO.getProcessInstanceId())
                .eqIfPresent(SystemFlowMyBusinessDO::getTitle, reqVO.getTitle())
                .eqIfPresent(SystemFlowMyBusinessDO::getDataId, reqVO.getDataId())
                .eqIfPresent(SystemFlowMyBusinessDO::getType, reqVO.getType())
                .likeIfPresent(SystemFlowMyBusinessDO::getServiceImplName, reqVO.getServiceImplName())
                .eqIfPresent(SystemFlowMyBusinessDO::getProposer, reqVO.getProposer())
                .eqIfPresent(SystemFlowMyBusinessDO::getActStatus, reqVO.getActStatus())
                .eqIfPresent(SystemFlowMyBusinessDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(SystemFlowMyBusinessDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(SystemFlowMyBusinessDO::getTaskNameId, reqVO.getTaskNameId())
                .eqIfPresent(SystemFlowMyBusinessDO::getTodoUsers, reqVO.getTodoUsers())
                .eqIfPresent(SystemFlowMyBusinessDO::getDoneUsers, reqVO.getDoneUsers())
                .eqIfPresent(SystemFlowMyBusinessDO::getPrority, reqVO.getPrority())
                .eqIfPresent(SystemFlowMyBusinessDO::getFlowValues, reqVO.getFlowValues())
                .eqIfPresent(SystemFlowMyBusinessDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(SystemFlowMyBusinessDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
