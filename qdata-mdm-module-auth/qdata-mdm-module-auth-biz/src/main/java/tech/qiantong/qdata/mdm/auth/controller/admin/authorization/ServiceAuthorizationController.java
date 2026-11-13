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
import tech.qiantong.qdata.mdm.auth.api.serviceauthorization.dto.ServiceAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.ServiceAuthorizationPageReqVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.BatchAuthorizationResultVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.ServiceAuthorizationRespVO;
import tech.qiantong.qdata.mdm.auth.service.ServiceAuthorizationService;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;

import javax.annotation.Resource;
import java.util.List;

/** 服务授权配置接口。 */
@Tag(name = "服务授权")
@RestController
@RequestMapping("/auth/service-accesses")
@Validated
public class ServiceAuthorizationController extends BaseController {
    @Resource
    private ServiceAuthorizationService serviceAuthorizationService;

    @Operation(summary = "新增服务授权")
    @PreAuthorize("@ss.hasPermi('auth:service:add')")
    @PostMapping
    public CommonResult<Long> create(@RequestBody ServiceAuthorizationSaveReqDTO request) {
        return CommonResult.success(serviceAuthorizationService.save(request));
    }

    @Operation(summary = "批量新增服务授权")
    @PreAuthorize("@ss.hasPermi('auth:service:add')")
    @PostMapping("/batch-grant")
    public CommonResult<BatchAuthorizationResultVO> batchGrant(@RequestBody ServiceAuthorizationSaveReqDTO request) {
        return CommonResult.success(serviceAuthorizationService.saveBatch(request));
    }

    @Operation(summary = "修改服务授权")
    @PreAuthorize("@ss.hasPermi('auth:service:edit')")
    @PutMapping
    public CommonResult<Long> update(@RequestBody ServiceAuthorizationSaveReqDTO request) {
        if (request.getId() == null) {
            throw new ServiceException("编辑授权时缺少授权编号");
        }
        return CommonResult.success(serviceAuthorizationService.save(request));
    }

    @Operation(summary = "查询服务授权列表")
    @PreAuthorize("@ss.hasPermi('auth:service:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ServiceAuthorizationRespVO>> list(ServiceAuthorizationPageReqVO request) {
        return CommonResult.success(serviceAuthorizationService.page(request));
    }

    /**
     * 根据数据服务 ID 查询授权记录列表，不分页。
     *
     * @param serviceId 数据服务 ID
     * @return 授权记录列表
     */
    @Operation(summary = "根据数据服务ID查询服务授权列表")
    @PreAuthorize("@ss.hasAnyPermi('auth:service:list,dist:service:query')")
    @GetMapping("/list-by-service-id")
    public CommonResult<List<ServiceAuthorizationRespVO>> listByServiceId(@RequestParam Long serviceId) {
        return CommonResult.success(serviceAuthorizationService.listByServiceId(serviceId));
    }

    @Operation(summary = "查询服务授权详情")
    @PreAuthorize("@ss.hasPermi('auth:service:query')")
    @GetMapping("/info")
    public CommonResult<ServiceAuthorizationRespVO> info(@RequestParam Long id) {
        return CommonResult.success(serviceAuthorizationService.getDetail(id));
    }

    @Operation(summary = "撤销服务授权")
    @PreAuthorize("@ss.hasPermi('auth:service:edit')")
    @PostMapping("/{id}/revoke")
    public CommonResult<Boolean> revoke(@PathVariable Long id) {
        serviceAuthorizationService.revoke(id, String.valueOf(getUserId()));
        return CommonResult.success(true);
    }

    @Operation(summary = "批量撤销服务授权")
    @PreAuthorize("@ss.hasPermi('auth:service:edit')")
    @PostMapping("/batch-revoke")
    public CommonResult<Boolean> batchRevoke(@RequestBody List<Long> ids) {
        serviceAuthorizationService.revokeBatch(ids, String.valueOf(getUserId()));
        return CommonResult.success(true);
    }

    @Operation(summary = "批量删除服务授权")
    @PreAuthorize("@ss.hasPermi('auth:service:remove')")
    @DeleteMapping("/batch")
    public CommonResult<Boolean> batchDelete(@RequestBody List<Long> ids) {
        serviceAuthorizationService.deleteBatch(ids, String.valueOf(getUserId()));
        return CommonResult.success(true);
    }
}
