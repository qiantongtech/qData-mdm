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
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterMyRequestRowDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestItemQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestApprovalDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestPendingCountDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRequestStatusCountDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterMyRequestQueryDTO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterMyRequestStatusCountDTO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.List;

/**
 * MASTER_CHANGE_REQUEST 主数据变更申请 Mapper。
 */
public interface MasterChangeRequestMapper extends BaseMapperX<MasterChangeRequestDO> {

    /** 在项目库申请事务中锁定模型行，串行化同模型的占用检查和申请保存。 */
    Long lockModelForMaintenance(@Param("modelId") Long modelId);

    /** 查询指定记录未完成的申请，包含待人工确认的执行结果。 */
    List<MasterChangeRequestItemQueryDTO> selectPendingRecordRequests(
            @Param("modelId") Long modelId, @Param("recordIds") List<Long> recordIds);

    /**
     * 按模型统计仍在审批中的人工维护申请。
     *
     * @param modelIds 主数据模型 ID 集合
     * @return MODEL_ID、PENDING_COUNT 统计行
     */
    List<MasterChangeRequestPendingCountDTO> selectPendingCountsByModelIds(
            @Param("modelIds") List<Long> modelIds);

    /**
     * 统计指定模型仍在审批中的人工维护申请。
     *
     * @param modelId 主数据模型 ID
     * @return 待审批申请数量
     */
    Long selectPendingCountByModelId(@Param("modelId") Long modelId);

    /**
     * 分页查询指定记录关联的直接维护申请。
     *
     * @param page 分页参数
     * @param modelId 主数据模型 ID
     * @param masterRecordId 稳定主数据 ID
     * @param reqVO 分页及排序参数，XML 仅开放 id，其他字段采用默认排序
     * @return 符合查询条件的分页记录
     */
    IPage<MasterChangeRequestApprovalDTO> selectRecordApprovalPage(
            Page<MasterChangeRequestApprovalDTO> page, @Param("modelId") Long modelId,
            @Param("masterRecordId") Long masterRecordId,
            @Param("req") MasterChangePageReqVO reqVO);

    /**
     * 查询指定记录关联的一张直接维护申请头信息。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 稳定主数据 ID
     * @param requestId 变更申请 ID
     * @return 查询结果映射
     */
    MasterChangeRequestApprovalDTO selectRecordApprovalDetail(
            @Param("modelId") Long modelId, @Param("masterRecordId") Long masterRecordId,
            @Param("requestId") Long requestId);

    /**
     * 按筛选条件分页查询模型的直接维护申请。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @param reqVO 分页及排序请求
     * @return 符合查询条件的分页记录
     */
    IPage<MasterChangeRequestApprovalDTO> selectApprovalPage(
            Page<MasterChangeRequestApprovalDTO> page,
            @Param("query") MasterChangeRequestQueryDTO query,
            @Param("req") MasterChangeRequestPageReqVO reqVO);

    /**
     * 查询模型下的一张直接维护申请头信息。
     *
     * @param modelId 主数据模型 ID
     * @param requestId 变更申请 ID
     * @return 查询结果映射
     */
    MasterChangeRequestApprovalDTO selectApprovalDetail(@Param("modelId") Long modelId,
                                                         @Param("requestId") Long requestId);

    /**
     * 按审批状态统计模型的有效直接维护申请。
     *
     * @param modelId 主数据模型 ID
     * @return 查询结果集合
     */
    List<MasterChangeRequestStatusCountDTO> selectApprovalStatusCounts(@Param("modelId") Long modelId);

    /**
     * 按审批状态统计当前申请人的已提交申请。
     *
     * @param query 查询条件
     * @return 查询结果集合
     */
    List<MasterMyRequestStatusCountDTO> selectMyRequestStatusCounts(
            @Param("query") MasterMyRequestQueryDTO query);

    /**
     * 分页查询当前申请人的已提交申请。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @param reqVO 分页及排序请求
     * @return 符合查询条件的分页记录
     */
    IPage<MasterMyRequestRowDTO> selectMyRequestPage(
            Page<MasterMyRequestRowDTO> page,
            @Param("query") MasterMyRequestQueryDTO query,
            @Param("req") MasterMyRequestPageReqVO reqVO);

    /**
     * 查询当前申请人已提交申请涉及的模型选项。
     *
     * @param applicantId 申请人 ID
     * @return 查询结果集合
     */
    List<Long> selectMyRequestModelIds(
            @Param("applicantId") Long applicantId);

    /**
     * 查询当前申请人的已提交申请详情。
     *
     * @param requestId 变更申请 ID
     * @param applicantId 申请人 ID
     * @return 申请详情及有效申请明细
     */
    MasterChangeRequestApprovalDTO selectMyRequestDetail(
            @Param("requestId") Long requestId,
            @Param("applicantId") Long applicantId);

}
