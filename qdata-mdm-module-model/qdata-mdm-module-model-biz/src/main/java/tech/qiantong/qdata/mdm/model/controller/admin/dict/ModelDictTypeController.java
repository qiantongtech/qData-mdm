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

package tech.qiantong.qdata.mdm.model.controller.admin.dict;

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
import tech.qiantong.qdata.mdm.common.exception.enums.GlobalErrorCodeConstants;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypePageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictTypeDO;
import tech.qiantong.qdata.mdm.model.service.dict.IModelDictTypeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 参考数据Controller
 *
 * @author qdata
 * @date 2026-09-16
 */
@Tag(name = "参考数据")
@RestController
@RequestMapping("/model/dict/type")
@Validated
public class ModelDictTypeController extends BaseController {
    @Resource
    private IModelDictTypeService modelDictTypeService;

    @Operation(summary = "查询参考数据列表")
    @PreAuthorize("@ss.hasPermi('model:dict:type:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelDictTypeRespVO>> list(ModelDictTypePageReqVO modelDictType) {
        PageResult<ModelDictTypeDO> page = modelDictTypeService.getModelDictTypePage(modelDictType);
        return CommonResult.success(BeanUtils.toBean(page, ModelDictTypeRespVO.class));
    }

    /**
     * 查询有效参考数据类型列表，参数结构与普通列表一致，仅固定过滤 validFlag=1。
     */
    @Operation(summary = "查询有效参考数据列表")
    @PreAuthorize("@ss.hasPermi('model:dict:type:list')")
    @GetMapping("/valid-list")
    public CommonResult<List<ModelDictTypeRespVO>> validList(ModelDictTypePageReqVO modelDictType) {
        modelDictType.setPageSize(PageParam.PAGE_SIZE_NONE);
        // 复用原列表的分页、排序和其他查询条件，只覆盖状态条件，避免前端传入无效状态。
        modelDictType.setValidFlag(Boolean.TRUE);
        PageResult<ModelDictTypeDO> page = modelDictTypeService.getModelDictTypePage(modelDictType);
        return CommonResult.success(BeanUtils.toBean(page.getRows(), ModelDictTypeRespVO.class));
    }

    @Operation(summary = "导出参考数据列表")
    @PreAuthorize("@ss.hasPermi('model:dict:type:export')")
    @Log(title = "参考数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelDictTypePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelDictTypeDO> list = (List<ModelDictTypeDO>) modelDictTypeService.getModelDictTypePage(exportReqVO).getRows();
        ExcelUtil<ModelDictTypeRespVO> util = new ExcelUtil<>(ModelDictTypeRespVO.class);
        util.exportExcel(response, BeanUtils.toBean(list, ModelDictTypeRespVO.class), "应用管理数据");
    }

    @Operation(summary = "导入参考数据列表")
    @PreAuthorize("@ss.hasPermi('model:dict:type:import')")
    @Log(title = "参考数据", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelDictTypeRespVO> util = new ExcelUtil<>(ModelDictTypeRespVO.class);
        List<ModelDictTypeRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = modelDictTypeService.importModelDictType(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取参考数据详细信息")
    @PreAuthorize("@ss.hasPermi('model:dict:type:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelDictTypeRespVO> getInfo(@PathVariable("id") Long id) {
        ModelDictTypeDO modelDictTypeDO = modelDictTypeService.getModelDictTypeById(id);
        return CommonResult.success(BeanUtils.toBean(modelDictTypeDO, ModelDictTypeRespVO.class));
    }

    @Operation(summary = "新增参考数据")
    @PreAuthorize("@ss.hasPermi('model:dict:type:add')")
    @Log(title = "参考数据", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelDictTypeSaveReqVO modelDictType) {
        if (!modelDictTypeService.checkCodeUnique(modelDictType.getCode(), null))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增'" + modelDictType.getCode() + "'失败，编码已存在");
        modelDictType.setCreatorId(getUserId());
        modelDictType.setCreateBy(getNickName());
        modelDictType.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelDictTypeService.createModelDictType(modelDictType));
    }

    @Operation(summary = "修改参考数据")
    @PreAuthorize("@ss.hasPermi('model:dict:type:edit')")
    @Log(title = "参考数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelDictTypeSaveReqVO modelDictType) {
        if (!modelDictTypeService.checkCodeUnique(modelDictType.getCode(), modelDictType.getId()))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改'" + modelDictType.getCode() + "'失败，编码已存在");
        modelDictType.setUpdatorId(getUserId());
        modelDictType.setUpdateBy(getNickName());
        modelDictType.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelDictTypeService.updateModelDictType(modelDictType));
    }

    @Operation(summary = "修改参考数据状态")
    @PreAuthorize("@ss.hasPermi('model:dict:type:edit')")
    @Log(title = "参考数据", businessType = BusinessType.UPDATE)
    @GetMapping("/updateStatus/{id}/{status}")
    public CommonResult<Integer> updateStatus(@PathVariable("id") Long id, @PathVariable("status") Boolean status) {
        return CommonResult.toAjax(modelDictTypeService.updateModelDictTypeStatus(id, status));
    }

    /**
     * 批量启用或停用参考数据
     */
    @PreAuthorize("@ss.hasPermi('model:dict:type:edit')")
    @Log(title = "参考数据", businessType = BusinessType.UPDATE)
    @PutMapping("/batchStatus/{status}")
    public CommonResult<Integer> batchStatus(@RequestBody Long[] ids, @PathVariable Boolean status) {
        return CommonResult.toAjax(modelDictTypeService.updateModelDictTypeStatusBatch(Arrays.asList(ids), status));
    }

    @Operation(summary = "删除参考数据")
    @PreAuthorize("@ss.hasPermi('model:dict:type:remove')")
    @Log(title = "参考数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        boolean hasEnabledType = modelDictTypeService.lambdaQuery().in(ModelDictTypeDO::getId, Arrays.asList(ids)).eq(ModelDictTypeDO::getValidFlag, Boolean.TRUE).count() > 0;
        if (hasEnabledType) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除参考数据失败,请先停用!");
        }
        if (modelDictTypeService.getDataCount(Arrays.asList(ids)) > 0) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,参考数据已被字段引用!");
        }
        return CommonResult.toAjax(modelDictTypeService.removeModelDictType(Arrays.asList(ids)));
    }

}
