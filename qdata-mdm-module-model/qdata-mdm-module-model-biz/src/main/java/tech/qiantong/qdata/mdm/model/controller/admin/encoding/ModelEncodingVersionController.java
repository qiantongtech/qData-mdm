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
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingVersionDO;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingVersionService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

/**
 * 编码方案版本Controller
 *
 * @author qdata
 * @date 2026-09-17
 */
@Tag(name = "编码方案版本")
@RestController
@RequestMapping("/model/encoding/version")
@Validated
public class ModelEncodingVersionController extends BaseController {
    @Resource
    private IModelEncodingVersionService modelEncodingVersionService;

    @Operation(summary = "查询编码方案版本列表")
    @PreAuthorize("@ss.hasPermi('model:encoding:version:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelEncodingVersionRespVO>> list(ModelEncodingVersionPageReqVO modelEncodingVersion) {
        PageResult<ModelEncodingVersionDO> page = modelEncodingVersionService.getModelEncodingVersionPage(modelEncodingVersion);
        return CommonResult.success(BeanUtils.toBean(page, ModelEncodingVersionRespVO.class));
    }

    @Operation(summary = "获取编码方案版本详细信息")
    @PreAuthorize("@ss.hasPermi('model:encoding:version:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelEncodingVersionRespVO> getInfo(@PathVariable("id") Long id) {
        ModelEncodingVersionDO modelEncodingVersionDO = modelEncodingVersionService.getModelEncodingVersionById(id);
        return CommonResult.success(BeanUtils.toBean(modelEncodingVersionDO, ModelEncodingVersionRespVO.class));
    }

    @Operation(summary = "新增编码方案版本")
    @PreAuthorize("@ss.hasPermi('model:encoding:version:add')")
    @Log(title = "编码方案版本", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelEncodingVersionSaveReqVO modelEncodingVersion) {
        modelEncodingVersion.setCreatorId(getUserId());
        modelEncodingVersion.setCreateBy(getNickName());
        modelEncodingVersion.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelEncodingVersionService.createModelEncodingVersion(modelEncodingVersion));
    }

    @Operation(summary = "修改编码方案版本")
    @PreAuthorize("@ss.hasPermi('model:encoding:version:edit')")
    @Log(title = "编码方案版本", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelEncodingVersionSaveReqVO modelEncodingVersion) {
        modelEncodingVersion.setUpdatorId(getUserId());
        modelEncodingVersion.setUpdateBy(getNickName());
        modelEncodingVersion.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelEncodingVersionService.updateModelEncodingVersion(modelEncodingVersion));
    }

    /**
     * 根据版本ID和状态值修改编码方案版本状态。
     */
    @Operation(summary = "修改编码方案版本状态")
    @PreAuthorize("@ss.hasPermi('model:encoding:version:edit')")
    @Log(title = "编码方案版本", businessType = BusinessType.UPDATE)
    @GetMapping("/updateStatus/{id}/{status}")
    public CommonResult<Integer> updateStatus(@PathVariable("id") Long id,
                                              @PathVariable("status") Integer status) {
        return CommonResult.toAjax(modelEncodingVersionService.updateModelEncodingVersionStatus(id, status));
    }

    @Operation(summary = "删除编码方案版本")
    @PreAuthorize("@ss.hasPermi('model:encoding:version:remove')")
    @Log(title = "编码方案版本", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(modelEncodingVersionService.removeModelEncodingVersion(Arrays.asList(ids)));
    }

}
