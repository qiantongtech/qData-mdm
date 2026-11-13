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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTask.vo.QualityTaskSaveReqVO;
import tech.qiantong.qdata.mdm.quality.convert.qualityTask.QualityTaskConvert;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTask.QualityTaskDO;
import tech.qiantong.qdata.mdm.quality.service.qualityTask.IQualityTaskService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 主数据质量任务Controller
 *
 * @author qdata
 * @date 2026-09-22
 */
@Tag(name = "主数据质量任务")
@RestController
@RequestMapping("/quality/qualitytask")
@Validated
public class QualityTaskController extends BaseController {
    @Resource
    private IQualityTaskService qualityTaskService;

    @Operation(summary = "查询主数据质量任务列表")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityTaskRespVO>> list(@Valid QualityTaskPageReqVO qualityTask) {
        PageResult<QualityTaskDO> page = qualityTaskService.getQualityTaskPage(qualityTask);
        return CommonResult.success(BeanUtils.toBean(page, QualityTaskRespVO.class));
    }

    @Operation(summary = "导出主数据质量任务列表")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:export')")
    @Log(title = "主数据质量任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityTaskPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityTaskDO> list = (List<QualityTaskDO>) qualityTaskService.getQualityTaskPage(exportReqVO).getRows();
        ExcelUtil<QualityTaskRespVO> util = new ExcelUtil<>(QualityTaskRespVO.class);
        util.exportExcel(response, QualityTaskConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入主数据质量任务列表")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:import')")
    @Log(title = "主数据质量任务", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityTaskRespVO> util = new ExcelUtil<>(QualityTaskRespVO.class);
        List<QualityTaskRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = qualityTaskService.importQualityTask(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取主数据质量任务详细信息")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityTaskRespVO> getInfo(@PathVariable("id") Long id) {
        QualityTaskDO qualityTaskDO = qualityTaskService.getQualityTaskById(id);
        return CommonResult.success(BeanUtils.toBean(qualityTaskDO, QualityTaskRespVO.class));
    }

    @Operation(summary = "新增主数据质量任务")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:add')")
    @Log(title = "主数据质量任务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityTaskSaveReqVO qualityTask) {
        qualityTask.setCreatorId(getUserId());
        qualityTask.setCreateBy(getNickName());
        qualityTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(qualityTaskService.createQualityTask(qualityTask));
    }

    @Operation(summary = "修改主数据质量任务")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:edit')")
    @Log(title = "主数据质量任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityTaskSaveReqVO qualityTask) {
        qualityTask.setUpdatorId(getUserId());
        qualityTask.setUpdateBy(getNickName());
        qualityTask.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(qualityTaskService.updateQualityTask(qualityTask));
    }

    @Operation(summary = "删除主数据质量任务")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:remove')")
    @Log(title = "主数据质量任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(qualityTaskService.removeQualityTask(Arrays.asList(ids)));
    }

}
