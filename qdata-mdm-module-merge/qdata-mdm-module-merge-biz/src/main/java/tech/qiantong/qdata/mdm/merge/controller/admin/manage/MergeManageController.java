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

package tech.qiantong.qdata.mdm.merge.controller.admin.manage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergeFieldAdjustReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingActionReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingApprovalRecordRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingLogPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingRespVO;
import tech.qiantong.qdata.mdm.merge.service.manage.IMergeManageService;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 合并管理
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Tag(name = "合并管理")
@RestController
@RequestMapping("/merge/manage")
@Validated
public class MergeManageController extends BaseController {
    @Resource
    private IMergeManageService mergeManageService;

    @Operation(summary = "查询待提交结果")
    @PreAuthorize("@ss.hasPermi('merge:manage:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<MergePendingRespVO>> list(MergePendingPageReqVO reqVO) {
        return CommonResult.success(mergeManageService.page(reqVO));
    }

    @Operation(summary = "查询待提交结果详情")
    @PreAuthorize("@ss.hasPermi('merge:manage:query')")
    @GetMapping("/{id}")
    public CommonResult<MergePendingDetailRespVO> get(@PathVariable("id") Long id) {
        return CommonResult.success(mergeManageService.detail(id));
    }

    /**
     * 分页查询当前结果的审批与流转记录。
     *
     * @param id 待合并结果编号
     * @param reqVO 分页参数
     * @return 审批与流转记录分页结果
     */
    @Operation(summary = "查询合并结果审批记录")
    @PreAuthorize("@ss.hasPermi('merge:manage:query')")
    @GetMapping("/{id}/approval-records")
    public CommonResult<PageResult<MergePendingApprovalRecordRespVO>> approvalRecords(
            @PathVariable("id") Long id, MergePendingLogPageReqVO reqVO) {
        return CommonResult.success(mergeManageService.approvalRecords(id, reqVO));
    }

    @Operation(summary = "调整字段")
    @PreAuthorize("@ss.hasPermi('merge:manage:adjust')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PutMapping("/field")
    public CommonResult<Boolean> adjust(@Valid @RequestBody MergeFieldAdjustReqVO reqVO) {
        mergeManageService.adjust(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "确认待提交结果")
    @PreAuthorize("@ss.hasPermi('merge:manage:confirm')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PostMapping("/confirm")
    public CommonResult<Boolean> confirm(@Valid @RequestBody MergePendingActionReqVO reqVO) {
        mergeManageService.confirm(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "重新执行质量检测")
    @PreAuthorize("@ss.hasPermi('merge:manage:quality')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PostMapping("/quality")
    public CommonResult<Boolean> quality(@Valid @RequestBody MergePendingActionReqVO reqVO) {
        mergeManageService.recheck(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "提交审批")
    @PreAuthorize("@ss.hasPermi('merge:manage:submit')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PostMapping("/submit")
    public CommonResult<Boolean> submit(@Valid @RequestBody MergePendingActionReqVO reqVO) {
        mergeManageService.submit(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "撤回审批")
    @PreAuthorize("@ss.hasPermi('merge:manage:submit')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PostMapping("/withdraw")
    public CommonResult<Boolean> withdraw(@Valid @RequestBody MergePendingActionReqVO reqVO) {
        mergeManageService.withdraw(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "取消审批")
    @PreAuthorize("@ss.hasPermi('merge:manage:submit')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel")
    public CommonResult<Boolean> cancel(@Valid @RequestBody MergePendingActionReqVO reqVO) {
        mergeManageService.cancel(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "重新提交审批")
    @PreAuthorize("@ss.hasPermi('merge:manage:submit')")
    @Log(title = "合并管理", businessType = BusinessType.UPDATE)
    @PostMapping("/resubmit")
    public CommonResult<Boolean> resubmit(@Valid @RequestBody MergePendingActionReqVO reqVO) {
        mergeManageService.resubmit(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }
}
