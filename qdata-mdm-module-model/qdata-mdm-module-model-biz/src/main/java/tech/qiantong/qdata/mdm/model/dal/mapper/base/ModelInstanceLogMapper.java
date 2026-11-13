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

package tech.qiantong.qdata.mdm.model.dal.mapper.base;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceLogReqDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceLogDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Param;

/**
 * 模型发布全流程日志 Mapper。
 */
public interface ModelInstanceLogMapper extends BaseMapperX<ModelInstanceLogDO> {

    /**
     * 读取发布实例唯一的全流程日志，后续阶段在原内容后追加并更新这一行。
     *
     * @param instanceId 发布实例ID
     * @return 该发布实例的全流程日志
     */
    ModelInstanceLogDO selectByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 分页查询未逻辑删除的全流程日志；阶段和状态表示最近一次记录，详细过程保存在LOG_CONTENT。
     *
     * @param reqDTO 分页、筛选及排序参数，排序字段限于白名单
     * @return 符合条件的分页记录
     */
    default PageResult<ModelInstanceLogDO> selectPage(ModelInstanceLogReqDTO reqDTO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        // eqIfPresent只在请求参数有值时加筛选条件；排序字段还要经过白名单检查，避免直接拼入任意列名。
        return selectPage(reqDTO, new LambdaQueryWrapperX<ModelInstanceLogDO>()
                .eqIfPresent(ModelInstanceLogDO::getId, reqDTO.getId())
                .eqIfPresent(ModelInstanceLogDO::getInstanceId, reqDTO.getInstanceId())
                .eqIfPresent(ModelInstanceLogDO::getStage, reqDTO.getStage())
                .eqIfPresent(ModelInstanceLogDO::getStatus, reqDTO.getStatus())
                .eqIfPresent(ModelInstanceLogDO::getStepNo, reqDTO.getStepNo())
                .eqIfPresent(ModelInstanceLogDO::getExecutionDirection, reqDTO.getExecutionDirection())
                .eqIfPresent(ModelInstanceLogDO::getAttemptNo, reqDTO.getAttemptNo())
                .eqIfPresent(ModelInstanceLogDO::getValidFlag, reqDTO.getValidFlag())
                .orderBy(reqDTO.getOrderByColumn(), reqDTO.getIsAsc(), allowedColumns));
    }
}
