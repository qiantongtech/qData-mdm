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

package tech.qiantong.qdata.mdm.model.controller.admin.project;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.exception.enums.GlobalErrorCodeConstants;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.project.vo.ModelProjectSaveReqVO;
import tech.qiantong.qdata.mdm.model.convert.project.ModelProjectConvert;
import tech.qiantong.qdata.mdm.model.dal.dataobject.project.ModelProjectDO;
import tech.qiantong.qdata.mdm.model.service.project.IModelProjectService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 主数据域Controller
 *
 * @author qdata
 * @date 2026-09-15
 */
@Tag(name = "主数据域")
@RestController
@RequestMapping("/model/project")
@Validated
public class ModelProjectController extends BaseController {
    @Resource
    private IModelProjectService modelProjectService;

    @Operation(summary = "查询主数据域列表")
    @PreAuthorize("@ss.hasPermi('model:project:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelProjectRespVO>> list(ModelProjectPageReqVO modelProject) {
        PageResult<ModelProjectDO> page = modelProjectService.getModelProjectPage(modelProject);
        return CommonResult.success(BeanUtils.toBean(page, ModelProjectRespVO.class));
    }

    @Operation(summary = "查询主数据域树列表")
    @PreAuthorize("@ss.hasPermi('model:project:list')")
    @GetMapping("/tree")
    public CommonResult<List<ModelProjectRespVO>> tree(ModelProjectPageReqVO modelProject) {
        modelProject.setValidFlag(true);
        modelProject.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelProjectDO> list = (List<ModelProjectDO>) modelProjectService
                .getModelProjectPage(modelProject).getRows();
        return CommonResult.success(BeanUtils.toBean(list, ModelProjectRespVO.class));
    }

    @Operation(summary = "导出主数据域列表")
    @PreAuthorize("@ss.hasPermi('model:project:export')")
    @Log(title = "主数据域", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelProjectPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelProjectDO> list = (List<ModelProjectDO>) modelProjectService.getModelProjectPage(exportReqVO).getRows();
        ExcelUtil<ModelProjectRespVO> util = new ExcelUtil<>(ModelProjectRespVO.class);
        util.exportExcel(response, ModelProjectConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取主数据域详细信息")
    @PreAuthorize("@ss.hasPermi('model:project:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelProjectRespVO> getInfo(@PathVariable("id") Long id) {
        ModelProjectDO modelProjectDO = modelProjectService.getModelProjectById(id);
        return CommonResult.success(BeanUtils.toBean(modelProjectDO, ModelProjectRespVO.class));
    }

    @Operation(summary = "新增主数据域")
    @PreAuthorize("@ss.hasPermi('model:project:add')")
    @Log(title = "主数据域", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelProjectSaveReqVO modelProject) {
        if (!modelProjectService.checkCodeUnique(modelProject.getCode(), null))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增主数据域'" + modelProject.getCode() + "'失败，编码已存在");
        modelProject.setCreatorId(getUserId());
        modelProject.setCreateBy(getNickName());
        modelProject.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelProjectService.createModelProject(modelProject));
    }

    @Operation(summary = "修改主数据域")
    @PreAuthorize("@ss.hasPermi('model:project:edit')")
    @Log(title = "主数据域", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelProjectSaveReqVO modelProject) {
        if (!modelProjectService.checkCodeUnique(modelProject.getCode(), modelProject.getId()))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改主数据域'" + modelProject.getCode() + "'失败，编码已存在");
        modelProject.setUpdatorId(getUserId());
        modelProject.setUpdateBy(getNickName());
        modelProject.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelProjectService.updateModelProject(modelProject));
    }

    @Operation(summary = "修改主数据域状态")
    @PreAuthorize("@ss.hasPermi('model:project:edit')")
    @Log(title = "主数据域", businessType = BusinessType.UPDATE)
    @GetMapping("/updateStatus/{id}/{status}")
    public CommonResult<Integer> updateStatus(@PathVariable("id") Long id, @PathVariable("status") Boolean status) {
        return CommonResult.toAjax(modelProjectService.updateModelProjectStatus(id, status));
    }

    @Operation(summary = "删除主数据域")
    @PreAuthorize("@ss.hasPermi('model:project:remove')")
    @Log(title = "主数据域", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        boolean hasEnabledLayer = modelProjectService.lambdaQuery().in(ModelProjectDO::getId, Arrays.asList(ids)).eq(ModelProjectDO::getValidFlag, Boolean.TRUE).count() > 0;
        if (hasEnabledLayer) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除主数据域,请先停用!");
        }
        List<Long> projectIds = Arrays.asList(ids);
        if (modelProjectService.hasLayerReference(projectIds)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,主数据域被模型目录引用!");
        }
        if (modelProjectService.hasModelReference(projectIds)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,主数据域被主数据模型引用!");
        }
        return CommonResult.toAjax(modelProjectService.removeModelProject(Arrays.asList(ids)));
    }

}
