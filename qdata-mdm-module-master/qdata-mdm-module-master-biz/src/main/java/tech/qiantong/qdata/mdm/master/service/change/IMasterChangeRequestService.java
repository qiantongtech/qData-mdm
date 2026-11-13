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

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestDetailRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestBatchSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeBatchValidateRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeValidateRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterRecordRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSubmitRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeImportParseRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestModelRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * MASTER_CHANGE_REQUEST 申请查询、校验、提交与审批业务。
 */
public interface IMasterChangeRequestService {

    /**
     * 分页查询当前登录人的已提交申请。
     *
     * @param reqVO 分页、筛选及排序参数
     * @return 当前登录人的申请分页列表及统计信息
     */
    MasterMyRequestPageRespVO getMyRequestPage(MasterMyRequestPageReqVO reqVO);

    /**
     * 查询当前登录人申请涉及的模型选项。
     *
     * @return 查询结果集合
     */
    List<MasterMyRequestModelRespVO> getMyRequestModelOptions();

    /**
     * 查询本人申请详情，严格校验申请人归属。
     *
     * @param requestId 变更申请 ID
     * @return 申请详情及有效申请明细
     */
    MasterChangeRequestDetailRespVO getMyRequestDetail(Long requestId);

    /** 撤回本人尚未进入执行阶段的审批中申请。 */
    void withdrawMyRequest(Long requestId);

    /** 基于本人已驳回、已撤回或已终止的申请生成一张新申请。 */
    MasterChangeRequestSubmitRespVO reapplyMyRequest(Long requestId);

    /**
     * 分页查询指定主数据关联的申请。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 分页及排序参数
     * @return 与指定主数据关联的申请分页列表
     */
    MasterRecordRequestPageRespVO getRecordApprovalPage(Long instanceId, Long masterDataId,
                                                        MasterChangePageReqVO reqVO);

    /**
     * 查询申请中属于指定主数据的明细。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param requestId 变更申请 ID
     * @return 申请详情及有效申请明细
     */
    MasterChangeRequestDetailRespVO getRecordApprovalDetail(Long instanceId, Long masterDataId, Long requestId);

    /**
     * 分页查询当前模型的维护审批申请。
     *
     * @param instanceId 模型物化实例 ID
     * @param req 查询参数
     * @return 当前模型的审批申请分页列表及统计信息
     */
    MasterChangeRequestPageRespVO getApprovalPage(Long instanceId, MasterChangeRequestPageReqVO req);

    /**
     * 查询当前模型中的指定申请详情。
     *
     * @param instanceId 模型物化实例 ID
     * @param requestId 变更申请 ID
     * @return 申请详情及有效申请明细
     */
    MasterChangeRequestDetailRespVO getApprovalDetail(Long instanceId, Long requestId);

    /**
     * 只读校验单条维护申请。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @return 校验结果及问题明细
     */
    MasterChangeValidateRespVO validateMaintenance(Long instanceId,
            MasterChangeRequestSaveReqVO request);

    /**
     * 只读校验整批维护申请；提交时在写事务中再次校验。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @return 校验结果及问题明细
     */
    MasterChangeBatchValidateRespVO validateBatch(Long instanceId, MasterChangeRequestBatchSaveReqVO request);

    /**
     * 同一事务内校验并建立单条申请及明细。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @param applicantId 申请人 ID
     * @param applicantName 申请人姓名
     * @return 申请提交结果
     */
    MasterChangeRequestSubmitRespVO submitSingle(Long instanceId,
            MasterChangeRequestSaveReqVO request, Long applicantId, String applicantName);

    /** 指定来源提交单条申请，支持MANUAL和MERGE。 */
    MasterChangeRequestSubmitRespVO submitSingle(Long instanceId,
            MasterChangeRequestSaveReqVO request, Long applicantId, String applicantName, String changeSource);

    /**
     * 同一事务内校验并建立批量申请及明细。
     *
     * @param instanceId 模型物化实例 ID
     * @param request 业务请求参数
     * @param applicantId 申请人 ID
     * @param applicantName 申请人姓名
     * @return 申请提交结果
     */
    MasterChangeRequestSubmitRespVO submitBatch(Long instanceId,
            MasterChangeRequestBatchSaveReqVO request, Long applicantId, String applicantName);

    /**
     * 解析导入文件，不写入业务表。
     *
     * @param instanceId 模型物化实例 ID
     * @param file 待解析的 CSV 或 XLSX 文件
     * @return 解析后的表头、数据行及文件格式问题
     */
    MasterChangeImportParseRespVO parseImportFile(Long instanceId, MultipartFile file);

    /**
     * 按当前模型字段生成导入模板。
     *
     * @param instanceId 模型物化实例 ID
     * @param response 用于写入导出文件的 HTTP 响应
     */
    void writeImportTemplate(Long instanceId, HttpServletResponse response) throws IOException;
}
