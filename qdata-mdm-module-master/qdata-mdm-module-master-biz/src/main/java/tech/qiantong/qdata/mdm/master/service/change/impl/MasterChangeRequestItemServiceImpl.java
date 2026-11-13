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

package tech.qiantong.qdata.mdm.master.service.change.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestItemRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeStatusPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeStatusRespVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestItemDO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeStatusDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.mapper.change.MasterChangeRequestItemMapper;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeRequestItemService;
import tech.qiantong.qdata.mdm.master.util.MasterRecordUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MASTER_CHANGE_REQUEST_ITEM 申请明细查询、状态记录及持久化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterChangeRequestItemServiceImpl implements IMasterChangeRequestItemService {

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository dataQueryRepository;
    private final MasterChangeRequestItemMapper changeRequestItemMapper;
    private final ISysUserService sysUserService;

    /**
     * 查询已通过且执行成功的申请明细状态记录。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 分页及排序参数
     * @return 已执行成功的状态变更分页列表
     */
    @Override
    public MasterChangeStatusPageRespVO getStatusPage(Long instanceId, Long masterDataId,
                                                      MasterChangePageReqVO reqVO) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        MasterRecordUtils.requireRecord(dataQueryRepository, model, masterDataId);
        int page = reqVO == null || reqVO.getPageNum() == null ? 1 : Math.max(1, reqVO.getPageNum());
        int size = reqVO == null || reqVO.getPageSize() == null ? 10
                : Math.max(1, Math.min(100, reqVO.getPageSize()));
        try {
            IPage<MasterChangeStatusDTO> dataPage = changeRequestItemMapper.selectStatusPage(
                    new Page<>(page, size), model.getModelId(), masterDataId, reqVO);
            MasterChangeStatusPageRespVO response = new MasterChangeStatusPageRespVO();
            response.setTotal(dataPage.getTotal());
            Map<Long, String> applicantNames = new HashMap<>();
            List<MasterChangeStatusRespVO> rows = new ArrayList<>();
            for (MasterChangeStatusDTO row : dataPage.getRecords()) {
                Long applicantId = row.getApplicantId();
                if (applicantId != null && !applicantNames.containsKey(applicantId)) {
                    SysUser user = sysUserService.selectUserById(applicantId);
                    String name = user == null ? null : user.getNickName();
                    applicantNames.put(applicantId, name == null || name.trim().isEmpty() ? null : name);
                }
                rows.add(toStatusResp(row, applicantNames.get(applicantId)));
            }
            response.setRows(rows);
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据状态记录失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("主数据状态记录查询失败");
        }
    }

    /**
     * 查询申请明细，按申请内序号排序。
     *
     * @param requestId 变更申请 ID
     * @return 查询结果集合
     */
    @Override
    public List<MasterChangeRequestItemRespVO> getApplicationItems(Long requestId) {
        return changeRequestItemMapper.selectApplicationItems(requestId).stream()
                .map(this::toItemResp).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询申请中与指定主数据关联的明细。
     *
     * @param requestId 变更申请 ID
     * @param masterRecordId 稳定主数据 ID
     * @return 查询结果集合
     */
    @Override
    public List<MasterChangeRequestItemQueryDTO> getApplicationItemsForRecord(
            Long requestId, Long masterRecordId) {
        return changeRequestItemMapper.selectApplicationItemsForRecord(requestId, masterRecordId);
    }

    /**
     * 批量读取申请明细，用于汇总展示。
     *
     * @param requestIds 变更申请 ID 集合
     * @return 查询结果集合
     */
    @Override
    public List<MasterChangeRequestItemQueryDTO> getItemsForRequests(List<Long> requestIds) {
        return requestIds == null || requestIds.isEmpty()
                ? Collections.emptyList() : changeRequestItemMapper.selectItemsForRequests(requestIds);
    }

    @Override
    public List<MasterChangeRequestItemDO> getItemsForReapply(Long requestId) {
        return requestId == null ? Collections.emptyList()
                : changeRequestItemMapper.selectByRequestIdForExecute(requestId);
    }

    /**
     * 统计记录尚未结束的申请明细。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 稳定主数据 ID
     * @return 符合条件的记录数
     */
    @Override
    public Long getPendingCountForRecord(Long modelId, Long masterRecordId) {
        return changeRequestItemMapper.selectPendingCountForRecord(modelId, masterRecordId);
    }

    /**
     * 写入单条明细，参加申请提交的外层事务。
     *
     * @param item 申请明细
     * @return 受影响的记录数
     */
    @Override
    public int insertItem(MasterChangeRequestItemDO item) {
        return changeRequestItemMapper.insert(item);
    }

    /**
     * 批量写入明细，参加申请提交的外层事务。
     *
     * @param items 申请明细集合
     * @return 是否满足操作条件
     */
    @Override
    public boolean insertItems(List<MasterChangeRequestItemDO> items) {
        return changeRequestItemMapper.insertBatch(items);
    }

    private MasterChangeRequestItemRespVO toItemResp(MasterChangeRequestItemQueryDTO row) {
        MasterChangeRequestItemRespVO item = new MasterChangeRequestItemRespVO();
        item.setItemId(toId(row.getId()));
        item.setItemNo(row.getItemNo());
        item.setMasterRecordId(toId(row.getMasterRecordId()));
        item.setResultMasterRecordId(toId(row.getResultMasterRecordId()));
        item.setChangeType(row.getChangeType());
        item.setBeforeStatus(row.getBeforeStatus());
        item.setTargetStatus(row.getTargetStatus());
        item.setCurrentVersionNo(row.getCurrentVersionNo());
        item.setTargetVersionNo(row.getTargetVersionNo());
        item.setBeforeData(row.getBeforeData());
        item.setProposedData(row.getProposedData());
        item.setExecuteStatus(row.getExecuteStatus());
        item.setExecuteMessage(row.getExecuteMessage());
        return item;
    }

    private MasterChangeStatusRespVO toStatusResp(MasterChangeStatusDTO row, String applicantName) {
        MasterChangeStatusRespVO item = new MasterChangeStatusRespVO();
        item.setItemId(toId(row.getItemId()));
        item.setChangeType(row.getChangeType());
        item.setBeforeStatus(row.getBeforeStatus());
        item.setTargetStatus(row.getTargetStatus());
        item.setExecuteTime(row.getExecuteTime());
        item.setApplicantName(applicantName);
        item.setRequestNo(row.getRequestNo());
        item.setApplyReason(row.getApplyReason());
        item.setApprovalStatus(row.getApprovalStatus());
        return item;
    }

    private String toId(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 初始化执行状态及审计字段；实际写入参加申请事务。 */
    @Override
    public void initializeItem(MasterChangeRequestItemDO item, Long applicantId, String applicantName) {
        item.setExecuteStatus("0");
        item.setValidFlag(true);
        item.setDelFlag(false);
        String operatorName = applicantName == null ? null : applicantName.substring(0, Math.min(applicantName.length(), 32));
        item.setCreateBy(operatorName);
        item.setCreatorId(applicantId);
        item.setUpdateBy(operatorName);
        item.setUpdatorId(applicantId);
    }

}
