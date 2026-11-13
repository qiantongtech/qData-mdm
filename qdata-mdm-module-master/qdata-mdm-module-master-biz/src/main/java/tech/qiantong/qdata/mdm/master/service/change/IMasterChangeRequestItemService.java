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

package tech.qiantong.qdata.mdm.master.service.change;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeStatusPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestItemRespVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;

import java.util.List;

/**
 * MASTER_CHANGE_REQUEST_ITEM 申请明细查询、状态记录及持久化。
 */
public interface IMasterChangeRequestItemService {

    /**
     * 查询已通过且执行成功的申请明细状态记录。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 分页及排序参数
     * @return 已执行成功的状态变更分页列表
     */
    MasterChangeStatusPageRespVO getStatusPage(Long instanceId, Long masterDataId,
                                               MasterChangePageReqVO reqVO);

    /**
     * 查询申请明细，按申请内序号排序。
     *
     * @param requestId 变更申请 ID
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemRespVO> getApplicationItems(Long requestId);

    /**
     * 查询申请中与指定主数据关联的明细。
     *
     * @param requestId 变更申请 ID
     * @param masterRecordId 稳定主数据 ID
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemQueryDTO> getApplicationItemsForRecord(
            Long requestId, Long masterRecordId);

    /**
     * 批量读取申请明细，用于汇总展示。
     *
     * @param requestIds 变更申请 ID 集合
     * @return 查询结果集合
     */
    List<MasterChangeRequestItemQueryDTO> getItemsForRequests(List<Long> requestIds);

    /** 读取一张申请的有效明细实体，用于安全派生新申请。 */
    List<MasterChangeRequestItemDO> getItemsForReapply(Long requestId);

    /**
     * 统计记录尚未结束的申请明细。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 稳定主数据 ID
     * @return 符合条件的记录数
     */
    Long getPendingCountForRecord(Long modelId, Long masterRecordId);

    /**
     * 写入单条明细，参加申请提交的外层事务。
     *
     * @param item 申请明细
     * @return 受影响的记录数
     */
    int insertItem(MasterChangeRequestItemDO item);

    /**
     * 批量写入明细，参加申请提交的外层事务。
     *
     * @param items 申请明细集合
     * @return 是否满足操作条件
     */
    boolean insertItems(List<MasterChangeRequestItemDO> items);

    /**
     * 初始化明细执行状态及审计字段，业务字段由调用方设置。
     *
     * @param item 待保存的申请明细
     * @param applicantId 申请人 ID
     * @param applicantName 申请人姓名
     */
    void initializeItem(MasterChangeRequestItemDO item, Long applicantId, String applicantName);
}
