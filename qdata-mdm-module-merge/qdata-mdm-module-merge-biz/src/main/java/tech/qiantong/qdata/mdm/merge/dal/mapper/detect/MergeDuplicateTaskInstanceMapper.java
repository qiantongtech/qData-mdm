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

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskPageReqVO;

import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/**
 * 重复检测任务实例 Mapper
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface MergeDuplicateTaskInstanceMapper extends BaseMapperX<MergeDuplicateTaskInstanceDO> {

    /** 按模型、任务和状态分页查询检测实例，根据请求决定是否查询快照字段。 */
    default PageResult<MergeDuplicateTaskInstanceDO> selectPage(MergeTaskPageReqVO reqVO) {
        LambdaQueryWrapperX<MergeDuplicateTaskInstanceDO> wrapper = new LambdaQueryWrapperX<MergeDuplicateTaskInstanceDO>();
        wrapper.eq(reqVO.getModelId() != null, MergeDuplicateTaskInstanceDO::getModelId, reqVO.getModelId())
                .eq(reqVO.getTaskId() != null, MergeDuplicateTaskInstanceDO::getTaskId, reqVO.getTaskId())
                .eq(reqVO.getStatus() != null && !reqVO.getStatus().isEmpty(), MergeDuplicateTaskInstanceDO::getStatus, reqVO.getStatus());
        applyInstanceOrder(wrapper, reqVO);
        if (!Boolean.TRUE.equals(reqVO.getIncludeSnapshot())) {
            // 普通实例列表排除规则及来源快照，避免加载大字段。
            wrapper.select(MergeDuplicateTaskInstanceDO::getId, MergeDuplicateTaskInstanceDO::getTaskId,
                    MergeDuplicateTaskInstanceDO::getInstanceNo, MergeDuplicateTaskInstanceDO::getModelId,
                    MergeDuplicateTaskInstanceDO::getModelVersionId, MergeDuplicateTaskInstanceDO::getStatus,
                    MergeDuplicateTaskInstanceDO::getSourceCount, MergeDuplicateTaskInstanceDO::getGroupCount,
                    MergeDuplicateTaskInstanceDO::getSingleCount, MergeDuplicateTaskInstanceDO::getHistorySourceCount,
                    MergeDuplicateTaskInstanceDO::getSourceSnapshotTime, MergeDuplicateTaskInstanceDO::getStartTime,
                    MergeDuplicateTaskInstanceDO::getEndTime, MergeDuplicateTaskInstanceDO::getErrorMessage,
                    MergeDuplicateTaskInstanceDO::getValidFlag);
        }
        return selectPage(reqVO, wrapper);
    }

    /** 仅允许 ID 和开始时间排序，默认按开始时间和 ID 降序。 */
    default void applyInstanceOrder(LambdaQueryWrapperX<MergeDuplicateTaskInstanceDO> wrapper, MergeTaskPageReqVO reqVO) {
        boolean asc = "asc".equalsIgnoreCase(reqVO.getIsAsc());
        String column = reqVO.getOrderByColumn();
        if ("id".equals(column)) {
            wrapper.orderBy(true, asc, MergeDuplicateTaskInstanceDO::getId);
        } else if ("start_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeDuplicateTaskInstanceDO::getStartTime)
                    .orderBy(true, asc, MergeDuplicateTaskInstanceDO::getId);
        } else {
            wrapper.orderByDesc(MergeDuplicateTaskInstanceDO::getStartTime)
                    .orderByDesc(MergeDuplicateTaskInstanceDO::getId);
        }
    }
}
