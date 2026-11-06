/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.system.controller.admin.monitor;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

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
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerPageReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerSaveReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.monitor.ContainerServerDO;
import tech.qiantong.qdata.mdm.system.service.monitor.IContainerServerService;

/**
 * 容器管理-服务器配置Controller
 *
 * @author CHAOS
 * @date 2025-09-02
 */
@Tag(name = "容器管理-服务器配置")
@RestController
@RequestMapping("/system/containerServer")
@Validated
public class ContainerServerController extends BaseController {
    @Resource
    private IContainerServerService containerServerService;

    @Operation(summary = "查询容器管理-服务器配置列表")
    @PreAuthorize("@ss.hasPermi('system:monitor:containerServer:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ContainerServerRespVO>> list(ContainerServerPageReqVO containerServer) {
        PageResult<ContainerServerDO> page = containerServerService.getContainerServerPage(containerServer);
        return CommonResult.success(BeanUtils.toBean(page, ContainerServerRespVO.class));
    }

    @Operation(summary = "获取容器管理-服务器配置详细信息")
    @PreAuthorize("@ss.hasPermi('system:monitor:containerServer:query')")
    @GetMapping(value = "/info")
    public CommonResult<ContainerServerRespVO> getInfo(@RequestParam Long id, @RequestParam(defaultValue = "true") boolean all) {
        ContainerServerRespVO res = containerServerService.getContainerServerById(id, all);
        return CommonResult.success(res);
    }

    @Operation(summary = "新增容器管理-服务器配置")
    @PreAuthorize("@ss.hasPermi('system:monitor:containerServer:add')")
    @Log(title = "容器管理-服务器配置", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ContainerServerSaveReqVO containerServer) {
        containerServer.setCreatorId(getUserId());
        containerServer.setCreateBy(getNickName());
        containerServer.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(containerServerService.createContainerServer(containerServer));
    }

    @Operation(summary = "修改容器管理-服务器配置")
    @PreAuthorize("@ss.hasPermi('system:monitor:containerServer:edit')")
    @Log(title = "容器管理-服务器配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ContainerServerSaveReqVO containerServer) {
        containerServer.setUpdatorId(getUserId());
        containerServer.setUpdateBy(getNickName());
        containerServer.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(containerServerService.updateContainerServer(containerServer));
    }

    @Operation(summary = "删除容器管理-服务器配置")
    @PreAuthorize("@ss.hasPermi('system:monitor:containerServer:remove')")
    @Log(title = "容器管理-服务器配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(containerServerService.removeContainerServer(Arrays.asList(ids)));
    }

}
