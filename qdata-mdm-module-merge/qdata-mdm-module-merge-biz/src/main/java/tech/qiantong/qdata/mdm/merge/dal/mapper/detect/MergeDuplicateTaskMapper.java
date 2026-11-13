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

package tech.qiantong.qdata.mdm.merge.dal.mapper.detect;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import java.util.List;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskCreatorVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskPageReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 重复检测任务 Mapper
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface MergeDuplicateTaskMapper extends BaseMapperX<MergeDuplicateTaskDO> {

    /** 根据本页任务的创建账号 ID 批量查询电话，包含已停用的历史账号。 */
    @Select("<script>SELECT USER_ID, PHONENUMBER FROM SYSTEM_USER WHERE USER_ID IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    @Results({
            @Result(property = "userId", column = "USER_ID"),
            @Result(property = "phonenumber", column = "PHONENUMBER")
    })
    List<MergeTaskCreatorVO> selectCreators(@Param("ids") List<Long> ids);

    /** 锁定未删除的任务，同一任务的执行、更新和删除按事务串行处理。 */
    @Select("SELECT * FROM MERGE_DUPLICATE_TASK WHERE ID = #{id} AND DEL_FLAG = '0' FOR UPDATE")
    MergeDuplicateTaskDO selectForUpdate(@Param("id") Long id);

    /** 按模型、任务编号、任务名称、检测范围和状态分页查询重复检测任务。 */
    default PageResult<MergeDuplicateTaskDO> selectPage(MergeTaskPageReqVO reqVO) {
        String keyword = reqVO.getKeyword() == null ? null : reqVO.getKeyword().trim();
        String taskName = reqVO.getTaskName() == null ? null : reqVO.getTaskName().trim();
        LambdaQueryWrapperX<MergeDuplicateTaskDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(reqVO.getModelId() != null, MergeDuplicateTaskDO::getModelId, reqVO.getModelId())
                .inIfPresent(MergeDuplicateTaskDO::getModelId, reqVO.getModelIds())
                .like(keyword != null && !keyword.isEmpty(), MergeDuplicateTaskDO::getTaskNo, keyword)
                .like(taskName != null && !taskName.isEmpty(), MergeDuplicateTaskDO::getTaskName, taskName)
                .eq(reqVO.getScopeType() != null && !reqVO.getScopeType().isEmpty(),
                        MergeDuplicateTaskDO::getScopeType, reqVO.getScopeType())
                .eq(reqVO.getStatus() != null && !reqVO.getStatus().isEmpty(),
                        MergeDuplicateTaskDO::getStatus, reqVO.getStatus());
        applyTaskOrder(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    /** 仅允许 ID、最近执行时间和创建时间排序，默认按创建时间和 ID 降序。 */
    default void applyTaskOrder(LambdaQueryWrapperX<MergeDuplicateTaskDO> wrapper, MergeTaskPageReqVO reqVO) {
        boolean asc = "asc".equalsIgnoreCase(reqVO.getIsAsc());
        String column = reqVO.getOrderByColumn();
        if ("id".equals(column)) {
            wrapper.orderBy(true, asc, MergeDuplicateTaskDO::getId);
        } else if ("last_execute_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeDuplicateTaskDO::getLastExecuteTime)
                    .orderBy(true, asc, MergeDuplicateTaskDO::getId);
        } else if ("create_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeDuplicateTaskDO::getCreateTime)
                    .orderBy(true, asc, MergeDuplicateTaskDO::getId);
        } else {
            wrapper.orderByDesc(MergeDuplicateTaskDO::getCreateTime)
                    .orderByDesc(MergeDuplicateTaskDO::getId);
        }
    }
}
