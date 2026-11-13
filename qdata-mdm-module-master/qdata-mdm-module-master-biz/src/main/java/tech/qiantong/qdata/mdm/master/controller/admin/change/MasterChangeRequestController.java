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

package tech.qiantong.qdata.mdm.master.controller.admin.change;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestDetailRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestBatchSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeBatchValidateRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeImportParseRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeValidateRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterRecordRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeStatusPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSubmitRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestModelRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterMyRequestPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeRequestItemService;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeRequestService;

import java.io.IOException;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * MASTER_CHANGE_REQUEST 变更申请入口。
 *
 * <p>统一承载申请查询、维护预校验、申请提交及审批记录查询；不在 Controller 中直接访问数据表。</p>
 */
@Tag(name = "主数据变更申请")
@RestController
@RequestMapping("/master/change/request")
@Validated
public class MasterChangeRequestController extends BaseController {

    @Resource
    private IMasterChangeRequestService masterChangeRequestService;

    @Resource
    private IMasterChangeRequestItemService masterChangeRequestItemService;

    @Operation(summary = "查询当前登录人的已提交主数据申请")
    @PreAuthorize("@ss.hasPermi('master:change:list')")
    @GetMapping("/list")
    public CommonResult<MasterMyRequestPageRespVO> list(
            @Valid MasterMyRequestPageReqVO reqVO) {
        return CommonResult.success(masterChangeRequestService.getMyRequestPage(reqVO));
    }

    @Operation(summary = "查询当前登录人的申请模型选项")
    @PreAuthorize("@ss.hasPermi('master:change:list')")
    @GetMapping("/modelOptions")
    public CommonResult<List<MasterMyRequestModelRespVO>> modelOptions() {
        return CommonResult.success(masterChangeRequestService.getMyRequestModelOptions());
    }

    @Operation(summary = "查询当前登录人的已提交申请详情")
    @PreAuthorize("@ss.hasPermi('master:change:query')")
    @GetMapping("/{requestId}")
    public CommonResult<MasterChangeRequestDetailRespVO> detail(@PathVariable("requestId") Long requestId) {
        return CommonResult.success(masterChangeRequestService.getMyRequestDetail(requestId));
    }

    @Operation(summary = "撤回当前登录人的审批中申请")
    @PreAuthorize("@ss.hasPermi('master:change:withdraw')")
    @Log(title = "撤回主数据维护申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{requestId}/withdraw")
    public CommonResult<Boolean> withdraw(@PathVariable("requestId") Long requestId) {
        masterChangeRequestService.withdrawMyRequest(requestId);
        return CommonResult.success(true);
    }

    @Operation(summary = "基于已结束的申请重新发起主数据维护申请")
    @PreAuthorize("@ss.hasPermi('master:change:reapply')")
    @Log(title = "重新发起主数据维护申请", businessType = BusinessType.INSERT)
    @PostMapping("/{requestId}/reapply")
    public CommonResult<MasterChangeRequestSubmitRespVO> reapply(@PathVariable("requestId") Long requestId) {
        return CommonResult.success(masterChangeRequestService.reapplyMyRequest(requestId));
    }

    @Operation(summary = "维护申请提交前预校验（不落库、不发起审批）")
    @PreAuthorize("(#p1.mode == 'CREATE' and @ss.hasPermi('master:instance:add'))"
            + " or (#p1.mode == 'UPDATE' and @ss.hasPermi('master:instance:edit'))"
            + " or ({'FREEZE','UNFREEZE','INVALIDATE','RECOVER','ARCHIVE'}.contains(#p1.mode)"
            + " and @ss.hasPermi('master:instance:status'))")
    @PostMapping("/maintenance/validate/{instanceId}")
    public CommonResult<MasterChangeValidateRespVO> validateMaintenance(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody MasterChangeRequestSaveReqVO request) {
        return CommonResult.success(masterChangeRequestService.validateMaintenance(instanceId, request));
    }

    @Operation(summary = "批量修改、状态和导入整批预校验（只读）")
    @PreAuthorize("(#p1.mode == 'BATCH_UPDATE' and @ss.hasPermi('master:instance:edit'))"
            + " or (#p1.mode == 'BATCH_IMPORT' and @ss.hasPermi('master:instance:import'))"
            + " or (#p1.mode == 'BATCH_STATUS' and @ss.hasPermi('master:instance:status'))")
    @PostMapping("/maintenance/batch/validate/{instanceId}")
    public CommonResult<MasterChangeBatchValidateRespVO> validateBatchMaintenance(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody MasterChangeRequestBatchSaveReqVO request) {
        return CommonResult.success(masterChangeRequestService.validateBatch(instanceId, request));
    }

    @Operation(summary = "提交单条维护申请并建立正式申请记录")
    @PreAuthorize("(#p1.mode == 'CREATE' and @ss.hasPermi('master:instance:add'))"
            + " or (#p1.mode == 'UPDATE' and @ss.hasPermi('master:instance:edit'))"
            + " or ({'FREEZE','UNFREEZE','INVALIDATE','RECOVER','ARCHIVE'}.contains(#p1.mode)"
            + " and @ss.hasPermi('master:instance:status'))")
    @Log(title = "提交主数据维护申请", businessType = BusinessType.INSERT)
    @PostMapping("/maintenance/submit/{instanceId}")
    public CommonResult<MasterChangeRequestSubmitRespVO> submitMaintenance(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody MasterChangeRequestSaveReqVO request) {
        return CommonResult.success(masterChangeRequestService.submitSingle(
                instanceId, request, getUserId(), getUsername()));
    }

    @Operation(summary = "提交批量维护申请并建立正式申请记录")
    @PreAuthorize("(#p1.mode == 'BATCH_UPDATE' and @ss.hasPermi('master:instance:edit'))"
            + " or (#p1.mode == 'BATCH_IMPORT' and @ss.hasPermi('master:instance:import'))"
            + " or (#p1.mode == 'BATCH_STATUS' and @ss.hasPermi('master:instance:status'))")
    @Log(title = "提交批量主数据维护申请", businessType = BusinessType.INSERT)
    @PostMapping("/maintenance/batch/submit/{instanceId}")
    public CommonResult<MasterChangeRequestSubmitRespVO> submitBatchMaintenance(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody MasterChangeRequestBatchSaveReqVO request) {
        return CommonResult.success(masterChangeRequestService.submitBatch(
                instanceId, request, getUserId(), getUsername()));
    }

    @Operation(summary = "下载当前模型动态字段的 XLSX 导入模板")
    @PreAuthorize("@ss.hasPermi('master:instance:import')")
    @GetMapping("/maintenance/batch/import/template/{instanceId}")
    public void downloadImportTemplate(@PathVariable("instanceId") Long instanceId, HttpServletResponse response)
            throws IOException {
        masterChangeRequestService.writeImportTemplate(instanceId, response);
    }

    @Operation(summary = "解析 CSV/XLSX 导入文件，不保存或校验业务数据")
    @PreAuthorize("@ss.hasPermi('master:instance:import')")
    @PostMapping("/maintenance/batch/import/parse/{instanceId}")
    public CommonResult<MasterChangeImportParseRespVO> parseImportFile(
            @PathVariable("instanceId") Long instanceId, @RequestParam("file") MultipartFile file) {
        return CommonResult.success(masterChangeRequestService.parseImportFile(instanceId, file));
    }

    @Operation(summary = "查询单条主数据已执行的直接维护状态记录")
    @PreAuthorize("@ss.hasPermi('master:instance:status:list')")
    @GetMapping("/status/list/{instanceId}/{masterDataId}")
    public CommonResult<MasterChangeStatusPageRespVO> statuses(
            @PathVariable("instanceId") Long instanceId, @PathVariable("masterDataId") Long masterDataId,
            @Valid MasterChangePageReqVO reqVO) {
        return CommonResult.success(masterChangeRequestItemService.getStatusPage(
                instanceId, masterDataId, reqVO));
    }

    @Operation(summary = "查询单条主数据关联的直接维护审批记录")
    @PreAuthorize("@ss.hasPermi('master:instance:approval:list')")
    @GetMapping("/recordApproval/list/{instanceId}/{masterDataId}")
    public CommonResult<MasterRecordRequestPageRespVO> recordApprovals(
            @PathVariable("instanceId") Long instanceId, @PathVariable("masterDataId") Long masterDataId,
            @Valid MasterChangePageReqVO reqVO) {
        return CommonResult.success(masterChangeRequestService.getRecordApprovalPage(
                instanceId, masterDataId, reqVO));
    }

    @Operation(summary = "查询单条主数据关联的直接维护审批明细")
    @PreAuthorize("@ss.hasPermi('master:instance:approval:list')")
    @GetMapping("/recordApproval/detail/{instanceId}/{masterDataId}/{requestId}")
    public CommonResult<MasterChangeRequestDetailRespVO> recordApprovalDetail(
            @PathVariable("instanceId") Long instanceId, @PathVariable("masterDataId") Long masterDataId,
            @PathVariable("requestId") Long requestId) {
        return CommonResult.success(masterChangeRequestService.getRecordApprovalDetail(
                instanceId, masterDataId, requestId));
    }

    @Operation(summary = "查询主数据直接维护审批列表")
    @PreAuthorize("@ss.hasPermi('master:instance:approval:list')")
    @GetMapping("/approval/list/{instanceId}")
    public CommonResult<MasterChangeRequestPageRespVO> approvals(
            @PathVariable("instanceId") Long instanceId, @Valid MasterChangeRequestPageReqVO reqVO) {
        return CommonResult.success(masterChangeRequestService.getApprovalPage(instanceId, reqVO));
    }

    @Operation(summary = "查询主数据直接维护审批详情")
    @PreAuthorize("@ss.hasPermi('master:instance:approval:list')")
    @GetMapping("/approval/detail/{instanceId}/{requestId}")
    public CommonResult<MasterChangeRequestDetailRespVO> approvalDetail(
            @PathVariable("instanceId") Long instanceId, @PathVariable("requestId") Long requestId) {
        return CommonResult.success(masterChangeRequestService.getApprovalDetail(instanceId, requestId));
    }
}
