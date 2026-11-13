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

package tech.qiantong.qdata.mdm.model.controller.admin.base;

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
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.*;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.service.base.IModelBaseService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 主数据模型基本信息Controller
 *
 * @author qdata
 * @date 2026-09-18
 */
@Tag(name = "主数据模型基本信息")
@RestController
@RequestMapping("/model/base")
@Validated
public class ModelBaseController extends BaseController {
    @Resource
    private IModelBaseService modelBaseService;

    @Operation(summary = "查询主数据模型基本信息列表")
    @PreAuthorize("@ss.hasPermi('model:base:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelBaseRespVO>> list(ModelBasePageReqVO modelBase) {
        PageResult<ModelBaseDO> page = modelBaseService.getModelBasePage(modelBase);
        return CommonResult.success(BeanUtils.toBean(page, ModelBaseRespVO.class));
    }

    @Operation(summary = "查询主数据域下的关联模型")
    @PreAuthorize("@ss.hasPermi('model:base:list')")
    @GetMapping("/related")
    public CommonResult<List<ModelBaseRespVO>> related(ModelBasePageReqVO reqVO) {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setIncludeDescendantLayers(reqVO.getLayerId() != null);
        PageResult<ModelBaseDO> page = modelBaseService.getModelBasePage(reqVO);
        return CommonResult.success(BeanUtils.toBean(page.getRows(), ModelBaseRespVO.class));
    }

    @Operation(summary = "获取主数据模型基本信息详细信息")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelBaseRespVO> getInfo(@PathVariable("id") Long id) {
        ModelBaseDO modelBaseDO = modelBaseService.getModelBaseById(id);
        return CommonResult.success(BeanUtils.toBean(modelBaseDO, ModelBaseRespVO.class));
    }

    /**
     * 查询模型关系图谱。查询参数包含 ID 时从指定模型开始查询；未传 ID 时按其他条件选择起始模型。
     */
    @Operation(summary = "查询模型关系图谱")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping("/graph")
    public CommonResult<ModelRelationGraphRespVO> graph(ModelRelationGraphReqVO reqVO) {
        return CommonResult.success(modelBaseService.getModelRelationGraph(reqVO));
    }

    @Operation(summary = "新增主数据模型基本信息")
    @PreAuthorize("@ss.hasPermi('model:base:add')")
    @Log(title = "主数据模型基本信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelBaseSaveReqVO modelBase) {
        if (!modelBaseService.checkCodeUnique(modelBase.getCode(), null))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增主数据模型'" + modelBase.getCode() + "'失败，编码已存在");
        modelBase.setCreatorId(getUserId());
        modelBase.setCreateBy(getNickName());
        modelBase.setCreateTime(DateUtil.date());
        return CommonResult.success(modelBaseService.createModelBase(modelBase));
    }

    @Operation(summary = "修改主数据模型基本信息")
    @PreAuthorize("@ss.hasPermi('model:base:edit')")
    @Log(title = "主数据模型基本信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelBaseSaveReqVO modelBase) {
        if (!modelBaseService.checkCodeUnique(modelBase.getCode(), modelBase.getId()))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改主数据模型'" + modelBase.getCode() + "'失败，编码已存在");
        modelBase.setUpdatorId(getUserId());
        modelBase.setUpdateBy(getNickName());
        modelBase.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelBaseService.updateModelBase(modelBase));
    }

    /**
     * 沿用原发布入口，成功返回实例结果；失败由现有异常处理器返回错误码及结果信息。
     */
    @Operation(summary = "发布主数据模型")
    @PreAuthorize("@ss.hasPermi('model:base:edit')")
    @Log(title = "主数据模型", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public CommonResult<ModelBasePublishRespVO> publish(@PathVariable Long id,
                                                        @Valid @RequestBody ModelBasePublishReqVO publishReqVO) {
        // 入口只接收主模型ID、目标数据源及当前操作人；主子模型和两套表由后端一起发布。
        return CommonResult.success(modelBaseService.publishModelBase(id, publishReqVO, getUserId(), getNickName()));
    }

    @Operation(summary = "停用模型")
    @PreAuthorize("@ss.hasPermi('model:base:edit')")
    @Log(title = "主数据模型", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/disable")
    public CommonResult<Integer> disable(@PathVariable Long id) {
        return CommonResult.toAjax(modelBaseService.disableModelBase(id, getUserId(), getNickName()));
    }

    @Operation(summary = "批量发布主数据模型")
    @PreAuthorize("@ss.hasPermi('model:base:edit')")
    @Log(title = "批量发布主数据模型", businessType = BusinessType.UPDATE)
    @PostMapping("/batch/publish")
    public CommonResult<ModelBaseBatchRespVO> batchPublish(@Valid @RequestBody ModelBaseBatchPublishReqVO request) {
        return CommonResult.success(modelBaseService.batchPublishModelBase(request, getUserId(), getNickName()));
    }

    @Operation(summary = "批量停用主数据模型")
    @PreAuthorize("@ss.hasPermi('model:base:edit')")
    @Log(title = "批量停用主数据模型", businessType = BusinessType.UPDATE)
    @PostMapping("/batch/disable")
    public CommonResult<ModelBaseBatchRespVO> batchDisable(@Valid @RequestBody ModelBaseBatchReqVO request) {
        return CommonResult.success(modelBaseService.batchDisableModelBase(request, getUserId(), getNickName()));
    }

    @Operation(summary = "批量删除主数据模型草稿")
    @PreAuthorize("@ss.hasPermi('model:base:remove')")
    @Log(title = "批量删除主数据模型草稿", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public CommonResult<ModelBaseBatchRespVO> batchDelete(@Valid @RequestBody ModelBaseBatchReqVO request) {
        return CommonResult.success(modelBaseService.batchRemoveModelBase(request));
    }

    @Operation(summary = "删除主数据模型基本信息")
    @PreAuthorize("@ss.hasPermi('model:base:remove')")
    @Log(title = "主数据模型基本信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(modelBaseService.removeModelBase(Arrays.asList(ids)));
    }

}
