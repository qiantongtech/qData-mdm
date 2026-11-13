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

package tech.qiantong.qdata.mdm.model.controller.admin.encoding;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.exception.enums.GlobalErrorCodeConstants;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingUsageRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingDO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateReqDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateRespDTO;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 编码方案Controller
 *
 * @author qdata
 * @date 2026-09-16
 */
@Tag(name = "编码方案")
@RestController
@RequestMapping("/model/encoding")
@Validated
public class ModelEncodingController extends BaseController {
    @Resource
    private IModelEncodingService modelEncodingService;

    @Operation(summary = "分配自动编码")
    @PreAuthorize("@ss.hasPermi('model:encoding:add')")
    @PostMapping("/generate")
    public CommonResult<ModelEncodingGenerateRespDTO> generate(@RequestBody ModelEncodingGenerateReqDTO request) {
        return CommonResult.success(modelEncodingService.generate(request));
    }

    @Operation(summary = "查询编码方案列表")
    @PreAuthorize("@ss.hasPermi('model:encoding:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelEncodingRespVO>> list(ModelEncodingPageReqVO modelEncoding) {
        PageResult<ModelEncodingDO> page = modelEncodingService.getModelEncodingPage(modelEncoding);
        return CommonResult.success(BeanUtils.toBean(page, ModelEncodingRespVO.class));
    }

    /**
     * 查询字段配置可选的编码方案（当前版本已发布且存在规则段）。
     */
    @Operation(summary = "查询可用编码方案列表")
    @PreAuthorize("@ss.hasPermi('model:encoding:list')")
    @GetMapping("/available-list")
    public CommonResult<List<ModelEncodingRespVO>> availableList(ModelEncodingPageReqVO modelEncoding) {
        PageResult<ModelEncodingDO> page = modelEncodingService.getAvailableModelEncodingPage(modelEncoding);
        return CommonResult.success(BeanUtils.toBean(page.getRows(), ModelEncodingRespVO.class));
    }

    @Operation(summary = "获取编码方案详细信息")
    @PreAuthorize("@ss.hasPermi('model:encoding:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelEncodingRespVO> getInfo(@PathVariable("id") Long id) {
        ModelEncodingDO modelEncodingDO = modelEncodingService.getModelEncodingById(id);
        return CommonResult.success(BeanUtils.toBean(modelEncodingDO, ModelEncodingRespVO.class));
    }

    /**
     * 查询指定编码方案版本被模型字段使用的情况。
     *
     * @param encodingVersionId 编码方案版本 ID
     * @return 使用该版本的模型字段列表
     */
    @Operation(summary = "查询编码方案版本使用情况")
    @PreAuthorize("@ss.hasPermi('model:encoding:query')")
    @GetMapping("/usage")
    public CommonResult<List<ModelEncodingUsageRespVO>> usage(@RequestParam Long encodingVersionId) {
        return CommonResult.success(modelEncodingService.getUsageByVersionId(encodingVersionId));
    }

    @Operation(summary = "新增编码方案")
    @PreAuthorize("@ss.hasPermi('model:encoding:add')")
    @Log(title = "编码方案", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelEncodingSaveReqVO modelEncoding) {
        if (!modelEncodingService.checkCodeUnique(modelEncoding.getCode(), null))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增编码方案'" + modelEncoding.getCode() + "'失败，编码已存在");
        modelEncoding.setCreatorId(getUserId());
        modelEncoding.setCreateBy(getNickName());
        modelEncoding.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelEncodingService.createModelEncoding(modelEncoding));
    }

    @Operation(summary = "修改编码方案")
    @PreAuthorize("@ss.hasPermi('model:encoding:edit')")
    @Log(title = "编码方案", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelEncodingSaveReqVO modelEncoding) {
        if (!modelEncodingService.checkCodeUnique(modelEncoding.getCode(), modelEncoding.getId()))
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改编码方案'" + modelEncoding.getCode() + "'失败，编码已存在");
        modelEncoding.setUpdatorId(getUserId());
        modelEncoding.setUpdateBy(getNickName());
        modelEncoding.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelEncodingService.updateModelEncoding(modelEncoding));
    }

    @Operation(summary = "删除编码方案")
    @PreAuthorize("@ss.hasPermi('model:encoding:remove')")
    @Log(title = "编码方案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        List<Long> encodingIds = Arrays.asList(ids);
        if (modelEncodingService.hasPublishedVersion(encodingIds)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,请先停用版本");
        }
        if (modelEncodingService.hasModelReference(encodingIds)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "删除失败,编码方案被主数据模型引用!");
        }
        return CommonResult.toAjax(modelEncodingService.removeModelEncoding(Arrays.asList(ids)));
    }

}
