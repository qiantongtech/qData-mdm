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

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分发任务Mapper接口
 *
 * @author qdata
 * @date 2026-10-13
 */
public interface DistTaskMapper extends BaseMapperX<DistTaskDO> {

    /**
     * 批量读取当前页任务的关联名称、人员电话和最近执行结果。
     *
     * @param ids 当前页任务 ID
     * @return 列表和详情页使用的关联信息
     */
    List<DistTaskRespVO> selectListDetails(@Param("ids") List<Long> ids);

    /**
     * 组合任务字段、目标系统名称及最近执行状态条件进行分页查询。
     *
     * @param reqVO 筛选与分页参数
     * @return 任务分页结果
     */
    default PageResult<DistTaskDO> selectPage(DistTaskPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 先保留项目扩展的 Wrapper 类型；apply 返回基础类型，不能再链式调用 eqIfPresent。
        LambdaQueryWrapperX<DistTaskDO> wrapper = new LambdaQueryWrapperX<DistTaskDO>()
                .likeIfPresent(DistTaskDO::getName, reqVO.getName())
                .likeIfPresent(DistTaskDO::getCode, reqVO.getCode())
                .eqIfPresent(DistTaskDO::getTargetSystemId, reqVO.getTargetSystemId())
                .eqIfPresent(DistTaskDO::getModelId, reqVO.getModelId())
                .eqIfPresent(DistTaskDO::getDataMode, reqVO.getDataMode())
                .eqIfPresent(DistTaskDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(DistTaskDO::getStatus, reqVO.getStatus())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        // 目标系统名称通过子查询过滤，参数由 MyBatis-Plus 绑定，避免拼接用户输入。
        if (reqVO.getTargetSystemName() != null && !reqVO.getTargetSystemName().trim().isEmpty()) {
            wrapper.apply("TARGET_SYSTEM_ID IN (SELECT ID FROM DIST_TARGET_SYSTEM WHERE NAME LIKE {0})",
                    "%" + reqVO.getTargetSystemName().trim() + "%");
        }
        if (reqVO.getLastRunStatus() != null && !reqVO.getLastRunStatus().trim().isEmpty()) {
            wrapper.apply("ID IN (SELECT i.TASK_ID FROM DIST_TASK_INSTANCE i "
                            + "JOIN (SELECT TASK_ID, MAX(ID) AS LATEST_ID FROM DIST_TASK_INSTANCE GROUP BY TASK_ID) latest "
                            + "ON latest.LATEST_ID = i.ID WHERE i.RUN_STATUS = {0})",
                    reqVO.getLastRunStatus());
        }
        return selectPage(reqVO, wrapper);
    }
}
