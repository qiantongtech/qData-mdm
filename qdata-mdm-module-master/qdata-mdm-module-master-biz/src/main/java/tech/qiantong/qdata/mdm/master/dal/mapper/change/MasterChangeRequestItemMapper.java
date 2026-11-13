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

package tech.qiantong.qdata.mdm.master.dal.mapper.change;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeStatusDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.List;

/**
 * MASTER_CHANGE_REQUEST_ITEM 主数据变更申请明细 Mapper。
 */
public interface MasterChangeRequestItemMapper extends BaseMapperX<MasterChangeRequestItemDO> {

    /** 查询申请执行所需的有效明细实体。 */
    List<MasterChangeRequestItemDO> selectByRequestIdForExecute(@Param("requestId") Long requestId);

    /**
     * 查询一张申请单的全部有效明细。
     *
     * @param requestId 变更申请 ID
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemQueryDTO> selectApplicationItems(@Param("requestId") Long requestId);

    /**
     * 查询一张申请单中属于指定主数据记录的有效明细。
     *
     * @param requestId 变更申请 ID
     * @param masterRecordId 稳定主数据 ID
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemQueryDTO> selectApplicationItemsForRecord(
            @Param("requestId") Long requestId, @Param("masterRecordId") Long masterRecordId);

    /**
     * 查询多张申请单的有效明细，用于当前页目标摘要。
     *
     * @param requestIds 变更申请 ID 集合
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemQueryDTO> selectItemsForRequests(
            @Param("requestIds") List<Long> requestIds);

    /**
     * 统计指定主数据记录仍在审批或等待执行完成的有效申请明细。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 稳定主数据 ID
     * @return 符合条件的记录数
     */
    Long selectPendingCountForRecord(@Param("modelId") Long modelId,
                                     @Param("masterRecordId") Long masterRecordId);

    /**
     * 批量查询一组主数据记录最近的申请及审批展示信息。
     *
     * @param modelId 主数据模型 ID
     * @param recordIds 主数据记录 ID 集合
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemQueryDTO> selectLatestApprovals(
            @Param("modelId") Long modelId, @Param("recordIds") List<Long> recordIds);

    /**
     * 查询指定模型最近一条已提交申请符合审批状态的主数据记录 ID。
     *
     * @param modelId 主数据模型 ID
     * @param approvalStatus 审批状态
     * @return 主数据记录 ID 集合
     */
    List<Long> selectLatestApprovalRecordIds(@Param("modelId") Long modelId,
                                             @Param("approvalStatus") String approvalStatus);

    /**
     * 分页查询已执行成功的主数据状态变更记录。
     *
     * @param page 分页参数
     * @param modelId 主数据模型 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 分页及排序参数，XML 仅开放 id，其他字段采用默认排序
     * @return 符合查询条件的分页记录
     */
    IPage<MasterChangeStatusDTO> selectStatusPage(
            Page<MasterChangeStatusDTO> page,
            @Param("modelId") Long modelId,
            @Param("masterDataId") Long masterDataId,
            @Param("req") MasterChangePageReqVO reqVO);
}
