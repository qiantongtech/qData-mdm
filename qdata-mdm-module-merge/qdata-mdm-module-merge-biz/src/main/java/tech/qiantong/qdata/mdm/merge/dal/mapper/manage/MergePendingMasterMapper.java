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

package tech.qiantong.qdata.mdm.merge.dal.mapper.manage;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingPageReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterDO;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergePendingQueryBO;
import tech.qiantong.qdata.mdm.merge.domain.manage.MergeEffectiveResultBO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 融合待提交结果
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface MergePendingMasterMapper extends BaseMapperX<MergePendingMasterDO> {
    /** 按本次结果版本查询正式快照，不使用该记录的最新版本替代历史结果。 */
    MergeEffectiveResultBO selectEffectiveResult(@Param("pendingId") Long pendingId);

    /** 锁定候选组已有的有效待提交结果，供结果生成时检查复用。 */
    @Select("SELECT * FROM MERGE_PENDING_MASTER WHERE OBJECT_GROUP_ID = #{groupId} "
            + "AND VALID_FLAG = '1' AND DEL_FLAG = '0' FOR UPDATE")
    MergePendingMasterDO selectByGroupForUpdate(@Param("groupId") Long groupId);

    /**
     * 查询并锁定其他有效治理结果占用的目标来源记录，防止重复生成结果。
     *
     * @param groupId 当前候选组 ID，查询时排除该组
     * @param sourceIds 待检查的来源记录 ID，调用方须保证非空
     * @return 已被其他有效结果占用的来源记录 ID
     */
    List<Long> selectOccupiedSourceIds(@Param("groupId") Long groupId, @Param("sourceIds") List<Long> sourceIds);

    /** 按有效结果编号加锁，写操作在同一事务中读取最新状态并完成更新。 */
    @Select("SELECT * FROM MERGE_PENDING_MASTER WHERE ID = #{pendingId} "
            + "AND VALID_FLAG = '1' AND DEL_FLAG = '0' FOR UPDATE")
    MergePendingMasterDO selectByIdForUpdate(@Param("pendingId") Long pendingId);

    /**
     * 按业务层已经解析的筛选范围查询有效结果。
     *
     * @param reqVO 分页、模型及排序参数
     * @param query 已解析的关联编号、状态和文本条件
     * @return 待提交结果分页数据
     */
    default PageResult<MergePendingMasterDO> selectPage(MergePendingPageReqVO reqVO, MergePendingQueryBO query) {
        LambdaQueryWrapperX<MergePendingMasterDO> wrapper = new LambdaQueryWrapperX<>();
        String taskName = reqVO.getTaskName() == null ? "" : reqVO.getTaskName().trim();
        wrapper.eq(MergePendingMasterDO::getValidFlag, MergeCodes.YES)
                .eq(reqVO.getModelId() != null, MergePendingMasterDO::getModelId, reqVO.getModelId())
                .inIfPresent(MergePendingMasterDO::getModelId, reqVO.getModelIds())
                .in(query.getRecordIds() != null, MergePendingMasterDO::getMergeRecordId, query.getRecordIds())
                .in(query.getGroupIds() != null, MergePendingMasterDO::getObjectGroupId, query.getGroupIds())
                .like(!query.getBusinessName().isEmpty(), MergePendingMasterDO::getResultData, query.getBusinessName())
                .in(query.getStatuses() != null, MergePendingMasterDO::getStatus, query.getStatuses());
        wrapper.apply(!taskName.isEmpty(), "OBJECT_GROUP_ID IN (SELECT g.ID FROM MERGE_CANDIDATE_GROUP g "
                + "JOIN MERGE_DUPLICATE_TASK_INSTANCE i ON i.ID = g.TASK_INSTANCE_ID "
                + "JOIN MERGE_DUPLICATE_TASK t ON t.ID = i.TASK_ID "
                + "WHERE g.VALID_FLAG = '1' AND g.DEL_FLAG = '0' AND i.DEL_FLAG = '0' "
                + "AND t.DEL_FLAG = '0' AND t.TASK_NAME LIKE {0})", "%" + taskName + "%");
        wrapper.apply(reqVO.getTransferStartTime() != null,
                "OBJECT_GROUP_ID IN (SELECT g.ID FROM MERGE_CANDIDATE_GROUP g "
                + "WHERE g.VALID_FLAG = '1' AND g.DEL_FLAG = '0' AND g.TRANSFER_TIME >= {0})",
                reqVO.getTransferStartTime());
        wrapper.apply(reqVO.getTransferEndTime() != null,
                "OBJECT_GROUP_ID IN (SELECT g.ID FROM MERGE_CANDIDATE_GROUP g "
                + "WHERE g.VALID_FLAG = '1' AND g.DEL_FLAG = '0' AND g.TRANSFER_TIME <= {0})",
                reqVO.getTransferEndTime());
        applyOrder(wrapper, reqVO);
        if (!query.getLegacyKeyword().isEmpty()) {
            wrapper.and(item -> {
                if (!query.getKeywordGroupIds().isEmpty()) {
                    item.in(MergePendingMasterDO::getObjectGroupId, query.getKeywordGroupIds()).or();
                }
                item.like(MergePendingMasterDO::getResultData, query.getLegacyKeyword());
            });
        }
        return selectPage(reqVO, wrapper);
    }

    /**
     * 仅允许 ID、更新时间和创建时间排序，默认按创建时间和 ID 降序。
     */
    default void applyOrder(LambdaQueryWrapperX<MergePendingMasterDO> wrapper, MergePendingPageReqVO reqVO) {
        boolean asc = "asc".equalsIgnoreCase(reqVO.getIsAsc());
        String column = reqVO.getOrderByColumn();
        if ("id".equals(column)) {
            wrapper.orderBy(true, asc, MergePendingMasterDO::getId);
        } else if ("update_time".equals(column)) {
            wrapper.orderBy(true, asc, MergePendingMasterDO::getUpdateTime)
                    .orderBy(true, asc, MergePendingMasterDO::getId);
        } else if ("create_time".equals(column)) {
            wrapper.orderBy(true, asc, MergePendingMasterDO::getCreateTime)
                    .orderBy(true, asc, MergePendingMasterDO::getId);
        } else {
            wrapper.orderByDesc(MergePendingMasterDO::getCreateTime)
                    .orderByDesc(MergePendingMasterDO::getId);
        }
    }
}
