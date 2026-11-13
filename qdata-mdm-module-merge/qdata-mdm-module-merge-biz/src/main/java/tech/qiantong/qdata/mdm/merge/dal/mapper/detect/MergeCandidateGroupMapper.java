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
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewPageReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 重复检测候选组 Mapper
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface MergeCandidateGroupMapper extends BaseMapperX<MergeCandidateGroupDO> {
    /** 锁定未删除的候选组，串行化同一候选组的处理操作。 */
    @Select("SELECT * FROM MERGE_CANDIDATE_GROUP WHERE ID = #{id} AND DEL_FLAG = '0' FOR UPDATE")
    MergeCandidateGroupDO selectForUpdate(@Param("id") Long id);

    /** 按模型、检测实例、类型、状态及组编号分页查询检测候选组。 */
    default PageResult<MergeCandidateGroupDO> selectPage(MergeGroupPageReqVO reqVO) {
        LambdaQueryWrapperX<MergeCandidateGroupDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(reqVO.getModelId() != null, MergeCandidateGroupDO::getModelId, reqVO.getModelId())
                .eq(reqVO.getTaskInstanceId() != null, MergeCandidateGroupDO::getTaskInstanceId,
                        reqVO.getTaskInstanceId())
                .eq(reqVO.getType() != null && !reqVO.getType().isEmpty(), MergeCandidateGroupDO::getType,
                        reqVO.getType())
                .eq(reqVO.getStatus() != null && !reqVO.getStatus().isEmpty(),
                        MergeCandidateGroupDO::getStatus, reqVO.getStatus())
                .like(reqVO.getGroupNo() != null && !reqVO.getGroupNo().isEmpty(),
                        MergeCandidateGroupDO::getGroupNo, reqVO.getGroupNo());
        applyGroupOrder(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    /** 仅允许指定字段排序；时间相同时按 ID 排序，未指定合法字段时按 ID 降序。 */
    default void applyGroupOrder(LambdaQueryWrapperX<MergeCandidateGroupDO> wrapper, MergeGroupPageReqVO reqVO) {
        boolean asc = "asc".equalsIgnoreCase(reqVO.getIsAsc());
        String column = reqVO.getOrderByColumn();
        if ("id".equals(column)) {
            wrapper.orderBy(true, asc, MergeCandidateGroupDO::getId);
        } else if ("transfer_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeCandidateGroupDO::getTransferTime)
                    .orderBy(true, asc, MergeCandidateGroupDO::getId);
        } else if ("create_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeCandidateGroupDO::getCreateTime)
                    .orderBy(true, asc, MergeCandidateGroupDO::getId);
        } else {
            wrapper.orderByDesc(MergeCandidateGroupDO::getId);
        }
    }

    /**
     * 按已解析的数据库状态分页查询有效审核候选组。
     *
     * @param reqVO 分页及筛选条件
     * @param statuses 审核服务解析后的非空状态集合
     * @return 候选组分页结果
     */
    default PageResult<MergeCandidateGroupDO> selectReviewPage(MergeReviewPageReqVO reqVO, List<String> statuses) {
        LambdaQueryWrapperX<MergeCandidateGroupDO> wrapper = new LambdaQueryWrapperX<>();
        String taskName = reqVO.getTaskName() == null ? "" : reqVO.getTaskName().trim();
        wrapper.eq(MergeCandidateGroupDO::getValidFlag, MergeCodes.YES)
                .in(MergeCandidateGroupDO::getStatus, statuses)
                .eq(reqVO.getModelId() != null, MergeCandidateGroupDO::getModelId, reqVO.getModelId())
                .inIfPresent(MergeCandidateGroupDO::getModelId, reqVO.getModelIds())
                .ge(reqVO.getTransferStartTime() != null, MergeCandidateGroupDO::getTransferTime,
                        reqVO.getTransferStartTime())
                .le(reqVO.getTransferEndTime() != null, MergeCandidateGroupDO::getTransferTime,
                        reqVO.getTransferEndTime())
                .eq(reqVO.getTaskInstanceId() != null, MergeCandidateGroupDO::getTaskInstanceId,
                        reqVO.getTaskInstanceId())
                .like(reqVO.getGroupNo() != null && !reqVO.getGroupNo().isEmpty(),
                        MergeCandidateGroupDO::getGroupNo, reqVO.getGroupNo());
        wrapper.apply(!taskName.isEmpty(), "TASK_INSTANCE_ID IN (SELECT i.ID FROM MERGE_DUPLICATE_TASK_INSTANCE i "
                + "JOIN MERGE_DUPLICATE_TASK t ON t.ID = i.TASK_ID "
                + "WHERE i.DEL_FLAG = '0' AND t.DEL_FLAG = '0' AND t.TASK_NAME LIKE {0})", "%" + taskName + "%");
        applyOrder(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    /** 审核列表只允许指定字段排序，默认按创建时间和 ID 降序。 */
    default void applyOrder(LambdaQueryWrapperX<MergeCandidateGroupDO> wrapper, MergeReviewPageReqVO reqVO) {
        boolean asc = "asc".equalsIgnoreCase(reqVO.getIsAsc());
        String column = reqVO.getOrderByColumn();
        if ("id".equals(column)) {
            wrapper.orderBy(true, asc, MergeCandidateGroupDO::getId);
        } else if ("transfer_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeCandidateGroupDO::getTransferTime)
                    .orderBy(true, asc, MergeCandidateGroupDO::getId);
        } else if ("create_time".equals(column)) {
            wrapper.orderBy(true, asc, MergeCandidateGroupDO::getCreateTime)
                    .orderBy(true, asc, MergeCandidateGroupDO::getId);
        } else {
            wrapper.orderByDesc(MergeCandidateGroupDO::getCreateTime)
                    .orderByDesc(MergeCandidateGroupDO::getId);
        }
    }
}
