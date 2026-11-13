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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance;

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
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo.QualityTaskInstanceSaveReqVO;
import tech.qiantong.qdata.mdm.quality.convert.qualityTaskInstance.QualityTaskInstanceConvert;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance.QualityTaskInstanceDO;
import tech.qiantong.qdata.mdm.quality.service.qualityTaskInstance.IQualityTaskInstanceService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 主数据质量任务执行实例Controller
 *
 * @author qdata
 * @date 2026-09-22
 */
@Tag(name = "主数据质量任务执行实例")
@RestController
@RequestMapping("/quality/qualitytaskinstance")
@Validated
public class QualityTaskInstanceController extends BaseController {
    @Resource
    private IQualityTaskInstanceService qualityTaskInstanceService;

    @Operation(summary = "手动执行质量任务并生成运行记录")
    @PreAuthorize("@ss.hasPermi('quality:qualitytask:edit') and @ss.hasPermi('quality:qualitytaskinstance:add')")
    @Log(title = "执行质量任务", businessType = BusinessType.INSERT)
    @PostMapping("/execute/{taskId}")
    public CommonResult<QualityTaskInstanceRespVO> execute(@PathVariable("taskId") Long taskId) {
        return CommonResult.success(BeanUtils.toBean(
                qualityTaskInstanceService.executeOnce(taskId, getUserId(), getNickName()), QualityTaskInstanceRespVO.class));
    }

    @Operation(summary = "查询主数据质量任务执行实例列表")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityTaskInstanceRespVO>> list(QualityTaskInstancePageReqVO qualityTaskInstance) {
        PageResult<QualityTaskInstanceDO> page = qualityTaskInstanceService.getQualityTaskInstancePage(qualityTaskInstance);
        return CommonResult.success(BeanUtils.toBean(page, QualityTaskInstanceRespVO.class));
    }

    @Operation(summary = "导出主数据质量任务执行实例列表")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:export')")
    @Log(title = "主数据质量任务执行实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityTaskInstancePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityTaskInstanceDO> list = (List<QualityTaskInstanceDO>) qualityTaskInstanceService.getQualityTaskInstancePage(exportReqVO).getRows();
        ExcelUtil<QualityTaskInstanceRespVO> util = new ExcelUtil<>(QualityTaskInstanceRespVO.class);
        util.exportExcel(response, QualityTaskInstanceConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入主数据质量任务执行实例列表")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:import')")
    @Log(title = "主数据质量任务执行实例", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityTaskInstanceRespVO> util = new ExcelUtil<>(QualityTaskInstanceRespVO.class);
        List<QualityTaskInstanceRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = qualityTaskInstanceService.importQualityTaskInstance(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取主数据质量任务执行实例详细信息")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityTaskInstanceRespVO> getInfo(@PathVariable("id") Long id) {
        QualityTaskInstanceDO qualityTaskInstanceDO = qualityTaskInstanceService.getQualityTaskInstanceById(id);
        return CommonResult.success(BeanUtils.toBean(qualityTaskInstanceDO, QualityTaskInstanceRespVO.class));
    }

    @Operation(summary = "新增主数据质量任务执行实例")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:add')")
    @Log(title = "主数据质量任务执行实例", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityTaskInstanceSaveReqVO qualityTaskInstance) {
        qualityTaskInstance.setCreatorId(getUserId());
        qualityTaskInstance.setCreateBy(getNickName());
        qualityTaskInstance.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(qualityTaskInstanceService.createQualityTaskInstance(qualityTaskInstance));
    }

    @Operation(summary = "修改主数据质量任务执行实例")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:edit')")
    @Log(title = "主数据质量任务执行实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityTaskInstanceSaveReqVO qualityTaskInstance) {
        qualityTaskInstance.setUpdatorId(getUserId());
        qualityTaskInstance.setUpdateBy(getNickName());
        qualityTaskInstance.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(qualityTaskInstanceService.updateQualityTaskInstance(qualityTaskInstance));
    }

    @Operation(summary = "删除主数据质量任务执行实例")
    @PreAuthorize("@ss.hasPermi('quality:qualitytaskinstance:remove')")
    @Log(title = "主数据质量任务执行实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(qualityTaskInstanceService.removeQualityTaskInstance(Arrays.asList(ids)));
    }

}
