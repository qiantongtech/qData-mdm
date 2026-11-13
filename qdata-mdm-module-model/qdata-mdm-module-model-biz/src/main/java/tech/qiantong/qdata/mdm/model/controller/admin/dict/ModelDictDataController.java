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
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataUsageRespVO;
import tech.qiantong.qdata.mdm.model.convert.dict.ModelDictDataConvert;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictDataDO;
import tech.qiantong.qdata.mdm.model.service.dict.IModelDictDataService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 参考数据项Controller
 *
 * @author qdata
 * @date 2026-09-16
 */
@Tag(name = "参考数据项")
@RestController
@RequestMapping("/model/dict/data")
@Validated
public class ModelDictDataController extends BaseController {
    @Resource
    private IModelDictDataService modelDictDataService;

    @Operation(summary = "查询参考数据项列表")
    @PreAuthorize("@ss.hasPermi('model:dict:data:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelDictDataRespVO>> list(ModelDictDataPageReqVO modelDictData) {
        PageResult<ModelDictDataDO> page = modelDictDataService.getModelDictDataPage(modelDictData);
        return CommonResult.success(BeanUtils.toBean(page, ModelDictDataRespVO.class));
    }

    /**
     * 查询有效参考数据项，参数结构与普通列表一致，仅固定过滤 validFlag=1。
     */
    @Operation(summary = "查询有效参考数据项列表")
    @PreAuthorize("@ss.hasPermi('model:dict:data:list')")
    @GetMapping("/valid-list")
    public CommonResult<List<ModelDictDataRespVO>> validList(ModelDictDataPageReqVO modelDictData) {
        modelDictData.setPageSize(PageParam.PAGE_SIZE_NONE);
        // 复用普通列表的分页、排序和类型过滤，只覆盖状态条件，确保下拉框不展示无效数据项。
        modelDictData.setValidFlag(Boolean.TRUE);
        PageResult<ModelDictDataDO> page = modelDictDataService.getModelDictDataPage(modelDictData);
        return CommonResult.success(BeanUtils.toBean(page.getRows(), ModelDictDataRespVO.class));
    }

    @Operation(summary = "查询参考数据引用的模型基础信息及字段信息")
    @PreAuthorize("@ss.hasPermi('model:dict:data:list')")
    @GetMapping("/reference-usage")
    public CommonResult<List<ModelDictDataUsageRespVO>> referenceUsage(@RequestParam Long typeId) {
        // 每个引用模型返回一次，fields 仅包含引用当前参考数据类型的字段。
        return CommonResult.success(modelDictDataService.getModelDictDataUsage(typeId));
    }

    @Operation(summary = "导出参考数据项列表")
    @PreAuthorize("@ss.hasPermi('model:dict:data:export')")
    @Log(title = "参考数据项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelDictDataPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelDictDataDO> list = (List<ModelDictDataDO>) modelDictDataService.getModelDictDataPage(exportReqVO).getRows();
        ExcelUtil<ModelDictDataRespVO> util = new ExcelUtil<>(ModelDictDataRespVO.class);
        util.exportExcel(response, ModelDictDataConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入参考数据项列表")
    @PreAuthorize("@ss.hasPermi('model:dict:data:import')")
    @Log(title = "参考数据项", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelDictDataRespVO> util = new ExcelUtil<>(ModelDictDataRespVO.class);
        List<ModelDictDataRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = modelDictDataService.importModelDictData(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取参考数据项详细信息")
    @PreAuthorize("@ss.hasPermi('model:dict:data:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelDictDataRespVO> getInfo(@PathVariable("id") Long id) {
        ModelDictDataDO modelDictDataDO = modelDictDataService.getModelDictDataById(id);
        return CommonResult.success(BeanUtils.toBean(modelDictDataDO, ModelDictDataRespVO.class));
    }

    @Operation(summary = "新增参考数据项")
    @PreAuthorize("@ss.hasPermi('model:dict:data:add')")
    @Log(title = "参考数据项", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelDictDataSaveReqVO modelDictData) {
        if (!modelDictDataService.checkCodeUnique(modelDictData.getCode(), null))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增参考数据项'" + modelDictData.getCode() + "'失败，编码已存在");
        modelDictData.setCreatorId(getUserId());
        modelDictData.setCreateBy(getNickName());
        modelDictData.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelDictDataService.createModelDictData(modelDictData));
    }

    @Operation(summary = "修改参考数据项")
    @PreAuthorize("@ss.hasPermi('model:dict:data:edit')")
    @Log(title = "参考数据项", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelDictDataSaveReqVO modelDictData) {
        if (!modelDictDataService.checkCodeUnique(modelDictData.getCode(), modelDictData.getId()))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改参考数据项'" + modelDictData.getCode() + "'失败，编码已存在");
        modelDictData.setUpdatorId(getUserId());
        modelDictData.setUpdateBy(getNickName());
        modelDictData.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelDictDataService.updateModelDictData(modelDictData));
    }

    @Operation(summary = "修改参考数据项状态")
    @PreAuthorize("@ss.hasPermi('model:dict:data:edit')")
    @Log(title = "参考数据项", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus/{id}/{status}")
    public CommonResult<Integer> updateStatus(@PathVariable("id") Long id, @PathVariable("status") Boolean status) {
        return CommonResult.toAjax(modelDictDataService.updateModelDictDataStatus(id, status));
    }

    @Operation(summary = "删除参考数据项")
    @PreAuthorize("@ss.hasPermi('model:dict:data:remove')")
    @Log(title = "参考数据项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        boolean hasEnabledData = modelDictDataService.lambdaQuery().in(ModelDictDataDO::getId, Arrays.asList(ids)).eq(ModelDictDataDO::getValidFlag, Boolean.TRUE).count() > 0;
        if (hasEnabledData) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除参考数据失败,请先停用!");
        }
        return CommonResult.toAjax(modelDictDataService.removeModelDictData(Arrays.asList(ids)));
    }

}
