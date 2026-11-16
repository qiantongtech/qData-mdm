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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.service;

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
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.*;
import tech.qiantong.qdata.mdm.dist.service.service.IDistServiceService;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Date;

/**
 * 数据服务配置接口
 *
 * @author chentangwen
 */
@Tag(name = "数据服务")
@RestController
@RequestMapping("/dist/service")
@Validated
public class DistServiceController extends BaseController {
    @Resource
    private IDistServiceService distServiceService;

    @Operation(summary = "分页查询数据服务")
    @PreAuthorize("@ss.hasPermi('dist:service:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<DistServiceRespVO>> list(@Valid DistServicePageReqVO request) {
        return CommonResult.success(distServiceService.getDistServicePage(request));
    }

    @Operation(summary = "查询数据服务及字段配置")
    @PreAuthorize("@ss.hasPermi('dist:service:query')")
    @GetMapping("/{id}")
    public CommonResult<DistServiceRespVO> getInfo(@PathVariable Long id) {
        return CommonResult.success(distServiceService.getDistServiceById(id));
    }

    @Operation(summary = "新增数据服务及字段配置")
    @PreAuthorize("@ss.hasPermi('dist:service:add')")
    @Log(title = "数据服务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody DistServiceSaveReqVO request) {
        request.setCreatorId(getUserId());
        request.setCreateBy(getNickName());
        request.setCreateTime(new Date());
        return CommonResult.toAjax(distServiceService.createDistService(request));
    }

    @Operation(summary = "修改数据服务及字段配置")
    @PreAuthorize("@ss.hasPermi('dist:service:edit')")
    @Log(title = "数据服务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody DistServiceSaveReqVO request) {
        request.setUpdatorId(getUserId());
        request.setUpdateBy(getNickName());
        request.setUpdateTime(new Date());
        return CommonResult.toAjax(distServiceService.updateDistService(request));
    }

    @Operation(summary = "启用或停用数据服务")
    @PreAuthorize("@ss.hasPermi('dist:service:edit')")
    @Log(title = "数据服务状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public CommonResult<Integer> changeStatus(@Valid @RequestBody DistServiceStatusReqVO request) {
        request.setUpdatorId(getUserId());
        request.setUpdateBy(getNickName());
        return CommonResult.toAjax(distServiceService.updateDistServiceStatus(request));
    }

    @Operation(summary = "发布已启用的数据服务")
    @PreAuthorize("@ss.hasPermi('dist:service:edit')")
    @Log(title = "数据服务发布", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/publish")
    public CommonResult<Integer> publish(@PathVariable @Positive Long id) {
        return CommonResult.toAjax(distServiceService.publishDistService(id, getUserId(), getNickName()));
    }

    @Operation(summary = "批量删除数据服务及字段配置")
    @PreAuthorize("@ss.hasPermi('dist:service:remove')")
    @Log(title = "数据服务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable @NotEmpty Long[] ids) {
        return CommonResult.toAjax(distServiceService.removeDistService(Arrays.asList(ids)));
    }
}
