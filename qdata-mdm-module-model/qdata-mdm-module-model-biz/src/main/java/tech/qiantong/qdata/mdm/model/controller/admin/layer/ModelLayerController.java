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

package tech.qiantong.qdata.mdm.model.controller.admin.layer;

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
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.exception.enums.GlobalErrorCodeConstants;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.layer.vo.ModelLayerSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.layer.ModelLayerDO;
import tech.qiantong.qdata.mdm.model.service.layer.IModelLayerService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

/**
 * 模型目录层级Controller
 *
 * @author qdata
 * @date 2026-09-15
 */
@Tag(name = "模型目录层级")
@RestController
@RequestMapping("/model/layer")
@Validated
public class ModelLayerController extends BaseController {
    @Resource
    private IModelLayerService modelLayerService;

    @Operation(summary = "查询模型目录层级列表")
    @PreAuthorize("@ss.hasPermi('model:layer:list')")
    @GetMapping("/list")
    public CommonResult<List<ModelLayerRespVO>> list(ModelLayerPageReqVO modelLayer) {
        modelLayer.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelLayerDO> list = (List<ModelLayerDO>) modelLayerService.getModelLayerPage(modelLayer).getRows();
        return CommonResult.success(BeanUtils.toBean(list, ModelLayerRespVO.class));
    }

    @Operation(summary = "查询模型目录层级树")
    @PreAuthorize("@ss.hasPermi('model:layer:list')")
    @GetMapping("/tree")
    public CommonResult<List<ModelLayerRespVO>> tree(ModelLayerPageReqVO modelLayer) {
        modelLayer.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelLayerDO> allLayers = (List<ModelLayerDO>) modelLayerService
                .getModelLayerPage(modelLayer).getRows();
        Map<Long, List<ModelLayerDO>> childrenMap = new LinkedHashMap<>();
        for (ModelLayerDO layer : allLayers) {
            if (Boolean.TRUE.equals(layer.getValidFlag())) {
                childrenMap.computeIfAbsent(layer.getParentId(), key -> new ArrayList<>()).add(layer);
            }
        }

        List<ModelLayerDO> tree = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        appendTree(0L, childrenMap, tree, visited);
        return CommonResult.success(BeanUtils.toBean(tree, ModelLayerRespVO.class));
    }

    private void appendTree(Long parentId, Map<Long, List<ModelLayerDO>> childrenMap,
                            List<ModelLayerDO> tree, Set<Long> visited) {
        for (ModelLayerDO layer : childrenMap.getOrDefault(parentId, new ArrayList<>())) {
            if (layer.getId() == null || !visited.add(layer.getId())) {
                continue;
            }
            tree.add(layer);
            appendTree(layer.getId(), childrenMap, tree, visited);
        }
    }

    @Operation(summary = "获取模型目录层级详细信息")
    @PreAuthorize("@ss.hasPermi('model:layer:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelLayerRespVO> getInfo(@PathVariable("id") Long id) {
        ModelLayerDO modelLayerDO = modelLayerService.getModelLayerById(id);
        return CommonResult.success(BeanUtils.toBean(modelLayerDO, ModelLayerRespVO.class));
    }

    @Operation(summary = "新增模型目录层级")
    @PreAuthorize("@ss.hasPermi('model:layer:add')")
    @Log(title = "模型目录层级", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelLayerSaveReqVO modelLayer) {
        if (!modelLayerService.checkCodeUnique(modelLayer.getCode(), null))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增模型目录层级'" + modelLayer.getCode() + "'失败，编码已存在");
        modelLayer.setCreatorId(getUserId());
        modelLayer.setCreateBy(getNickName());
        modelLayer.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelLayerService.createModelLayer(modelLayer));
    }

    @Operation(summary = "修改模型目录层级")
    @PreAuthorize("@ss.hasPermi('model:layer:edit')")
    @Log(title = "模型目录层级", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelLayerSaveReqVO modelLayer) {
        if (!modelLayerService.checkCodeUnique(modelLayer.getCode(), modelLayer.getId()))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改模型目录层级'" + modelLayer.getCode() + "'失败，编码已存在");
        modelLayer.setUpdatorId(getUserId());
        modelLayer.setUpdateBy(getNickName());
        modelLayer.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelLayerService.updateModelLayer(modelLayer));
    }

    @Operation(summary = "修改模型目录层级状态")
    @PreAuthorize("@ss.hasPermi('model:layer:edit')")
    @Log(title = "模型目录层级", businessType = BusinessType.UPDATE)
    @GetMapping("/updateStatus/{id}/{status}")
    public CommonResult<Integer> updateStatus(@PathVariable("id") Long id, @PathVariable("status") Boolean status) {
        return CommonResult.toAjax(modelLayerService.updateModelLayerStatus(id, status));
    }

    @Operation(summary = "删除模型目录层级")
    @PreAuthorize("@ss.hasPermi('model:layer:remove')")
    @Log(title = "模型目录层级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        boolean hasEnabledLayer = modelLayerService.lambdaQuery().in(ModelLayerDO::getId, Arrays.asList(ids)).eq(ModelLayerDO::getValidFlag, Boolean.TRUE).count() > 0;
        if (hasEnabledLayer) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除模型目录失败,请先停用!");
        }
        List<Long> layerIds = Arrays.asList(ids);
        if (modelLayerService.hasChildByModelLayerId(layerIds)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,存在子模型目录层级!");
        }
        if (modelLayerService.hasModelReference(layerIds)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,模型目录被主数据模型引用!");
        }
        int result = 0;
        for (Long id : ids) result += modelLayerService.removeModelLayer(id);
        return CommonResult.toAjax(result);
    }

}
