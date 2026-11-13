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

package tech.qiantong.qdata.mdm.auth.controller.admin.authorization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.auth.api.dataauthorization.dto.DataAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.DataAuthorizationPageReqVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.BatchAuthorizationResultVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.DataAuthorizationRespVO;
import tech.qiantong.qdata.mdm.auth.service.DataAuthorizationService;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import javax.annotation.Resource;
import java.util.List;

/** 数据授权配置接口。 */
@Tag(name = "数据授权")
@RestController
@RequestMapping("/auth/data-grants")
@Validated
public class DataAuthorizationController extends BaseController {
    @Resource
    private DataAuthorizationService dataAuthorizationService;

    @Operation(summary = "新增数据授权")
    @PreAuthorize("@ss.hasPermi('auth:data:add')")
    @PostMapping
    public CommonResult<Long> create(@RequestBody DataAuthorizationSaveReqDTO request) {
        return CommonResult.success(dataAuthorizationService.save(request));
    }

    @Operation(summary = "批量新增数据授权")
    @PreAuthorize("@ss.hasPermi('auth:data:add')")
    @PostMapping("/batch-grant")
    public CommonResult<BatchAuthorizationResultVO> batchGrant(@RequestBody DataAuthorizationSaveReqDTO request) {
        return CommonResult.success(dataAuthorizationService.saveBatch(request));
    }

    @Operation(summary = "修改数据授权")
    @PreAuthorize("@ss.hasPermi('auth:data:edit')")
    @PutMapping
    public CommonResult<Long> update(@RequestBody DataAuthorizationSaveReqDTO request) {
        if (request.getId() == null) {
            throw new ServiceException("编辑授权时缺少授权编号");
        }
        return CommonResult.success(dataAuthorizationService.save(request));
    }

    @Operation(summary = "查询数据授权列表")
    @PreAuthorize("@ss.hasPermi('auth:data:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<DataAuthorizationRespVO>> list(DataAuthorizationPageReqVO request) {
        return CommonResult.success(dataAuthorizationService.page(request));
    }

    @Operation(summary = "查询数据授权详情")
    @PreAuthorize("@ss.hasPermi('auth:data:query')")
    @GetMapping("/info")
    public CommonResult<DataAuthorizationRespVO> info(@RequestParam Long id) {
        return CommonResult.success(dataAuthorizationService.getDetail(id));
    }

    @Operation(summary = "撤销数据授权")
    @PreAuthorize("@ss.hasPermi('auth:data:edit')")
    @PostMapping("/{id}/revoke")
    public CommonResult<Boolean> revoke(@PathVariable Long id) {
        dataAuthorizationService.revoke(id, String.valueOf(getUserId()));
        return CommonResult.success(true);
    }

    @Operation(summary = "批量撤销数据授权")
    @PreAuthorize("@ss.hasPermi('auth:data:edit')")
    @PostMapping("/batch-revoke")
    public CommonResult<Boolean> batchRevoke(@RequestBody List<Long> ids) {
        dataAuthorizationService.revokeBatch(ids, String.valueOf(getUserId()));
        return CommonResult.success(true);
    }

    @Operation(summary = "批量删除数据授权")
    @PreAuthorize("@ss.hasPermi('auth:data:remove')")
    @DeleteMapping("/batch")
    public CommonResult<Boolean> batchDelete(@RequestBody List<Long> ids) {
        dataAuthorizationService.deleteBatch(ids, String.valueOf(getUserId()));
        return CommonResult.success(true);
    }
}
