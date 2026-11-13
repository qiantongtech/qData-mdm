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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskStatusCountRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo.QualityIssueTaskInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTask.QualityIssueTaskDO;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTask.IQualityIssueTaskService;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 问题整改任务Controller
 *
 * @author qdata
 * @date 2026-09-23
 */
@Tag(name = "问题整改任务")
@RestController
@RequestMapping("/quality/qualityIssueTask")
@Validated
public class QualityIssueTaskController extends BaseController {
    @Resource
    private IQualityIssueTaskService qualityIssueTaskService;

    @Operation(summary = "查询问题整改任务列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityIssueTaskRespVO>> list(@Valid QualityIssueTaskPageReqVO qualityIssueTask) {
        PageResult<QualityIssueTaskDO> page = qualityIssueTaskService.getQualityIssueTaskPage(qualityIssueTask);
        return CommonResult.success(BeanUtils.toBean(page, QualityIssueTaskRespVO.class));
    }

    @Operation(summary = "查询整改任务状态数量")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:list')")
    @GetMapping("/statusCount")
    public CommonResult<QualityIssueTaskStatusCountRespVO> statusCount(@Valid QualityIssueTaskPageReqVO request) {
        return CommonResult.success(qualityIssueTaskService.getQualityIssueTaskStatusCount(request));
    }

    @Operation(summary = "导出问题整改任务列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:export')")
    @Log(title = "问题整改任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityIssueTaskPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityIssueTaskRespVO> list = BeanUtils.toBean(qualityIssueTaskService.getQualityIssueTaskPage(exportReqVO).getRows(), QualityIssueTaskRespVO.class);
        ExcelUtil<QualityIssueTaskRespVO> util = new ExcelUtil<>(QualityIssueTaskRespVO.class);
        util.exportExcel(response, list, "整改任务");
    }

    @Operation(summary = "获取问题整改任务详细信息")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityIssueTaskRespVO> getInfo(@PathVariable("id") Long id) {
        QualityIssueTaskDO taskDO = qualityIssueTaskService.getQualityIssueTaskDetail(id);
        return CommonResult.success(BeanUtils.toBean(taskDO, QualityIssueTaskRespVO.class));
    }

    @Operation(summary = "从问题项下发整改任务")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:add')")
    @Log(title = "问题整改任务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Validated(QualityIssueTaskSaveReqVO.Create.class) @RequestBody QualityIssueTaskSaveReqVO qualityIssueTask) {
        qualityIssueTask.setCreatorId(getUserId());
        qualityIssueTask.setCreateBy(getNickName());
        qualityIssueTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(qualityIssueTaskService.createQualityIssueTask(qualityIssueTask));
    }

    @Operation(summary = "批量删除尚未开始整改的任务")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:remove')")
    @Log(title = "问题整改任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable("ids") List<Long> ids) {
        return CommonResult.toAjax(qualityIssueTaskService.deleteQualityIssueTasks(ids, getUserId(), getNickName()));
    }

    @Operation(summary = "开始或完成单个问题项整改")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:edit')")
    @Log(title = "问题整改任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> updateStatus(@Valid @RequestBody QualityIssueTaskInfoSaveReqVO request) {
        request.setUpdatorId(getUserId());
        request.setUpdateBy(getNickName());
        request.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(qualityIssueTaskService.updateQualityIssueTaskStatus(request));
    }

    @Operation(summary = "提交整改任务审批（仅更新状态）")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:edit')")
    @Log(title = "问题整改任务", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/submitApproval")
    public CommonResult<Integer> submitApproval(@PathVariable("id") Long id) {
        return CommonResult.toAjax(qualityIssueTaskService.updateApprovalStatus(id, true, getUserId(), getNickName()));
    }

    @Operation(summary = "撤回整改任务审批（仅更新状态）")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:edit')")
    @Log(title = "问题整改任务", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/withdrawApproval")
    public CommonResult<Integer> withdrawApproval(@PathVariable("id") Long id) {
        return CommonResult.toAjax(qualityIssueTaskService.updateApprovalStatus(id, false, getUserId(), getNickName()));
    }

}
