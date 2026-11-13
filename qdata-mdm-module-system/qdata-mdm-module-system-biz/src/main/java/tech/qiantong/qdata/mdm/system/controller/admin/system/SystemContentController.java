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

package tech.qiantong.qdata.mdm.system.controller.admin.system;

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
import tech.qiantong.qdata.mdm.system.domain.SystemContentDO;
import tech.qiantong.qdata.mdm.system.domain.vo.SystemContentPageReqVO;
import tech.qiantong.qdata.mdm.system.domain.vo.SystemContentRespVO;
import tech.qiantong.qdata.mdm.system.domain.vo.SystemContentSaveReqVO;
import tech.qiantong.qdata.mdm.system.service.ISystemContentService;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 系统配置Controller
 *
 * @author qdata
 * @date 2024-12-31
 */
@Tag(name = "系统配置")
@RestController
@Validated
public class SystemContentController extends BaseController {
    @Resource
    private ISystemContentService systemContentService;

    @Operation(summary = "查询系统配置列表")
    @PreAuthorize("@ss.hasPermi('system:system:content:list')")
    @GetMapping("/system/content/list")
    public CommonResult<PageResult<SystemContentRespVO>> list(SystemContentPageReqVO systemContent) {
        PageResult<SystemContentDO> page = systemContentService.getSystemContentPage(systemContent);
        return CommonResult.success(BeanUtils.toBean(page, SystemContentRespVO.class));
    }

    @Operation(summary = "获取系统配置详细信息")
    //首页不登录时要获取logo信息
//    @PreAuthorize("@ss.hasPermi('system:system:content:query')")
    @GetMapping(value = "sys/content/{id}")
    public CommonResult<SystemContentRespVO> getInfo(@PathVariable("id") Long id) {
        SystemContentDO systemContentDO = systemContentService.getSystemContentById(id);
        return CommonResult.success(BeanUtils.toBean(systemContentDO, SystemContentRespVO.class));
    }

    @Operation(summary = "修改系统配置")
    @PreAuthorize("@ss.hasPermi('system:system:content:edit')")
    @Log(title = "系统配置", businessType = BusinessType.UPDATE)
    @PostMapping("/system/content/edit")
    public CommonResult<Integer> edit(@Valid @RequestBody SystemContentSaveReqVO systemContent) {
        systemContent.setUpdatorId(getUserId());
        systemContent.setUpdateBy(getNickName());
        systemContent.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(systemContentService.updateSystemContent(systemContent));
    }

//    @Operation(summary = "删除系统配置")
//    @PreAuthorize("@ss.hasPermi('system:system:content:remove')")
//    @Log(title = "系统配置", businessType = BusinessType.DELETE)
//    @DeleteMapping("/{ids}")
//    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
//        return CommonResult.toAjax(systemContentService.removeSystemContent(Arrays.asList(ids)));
//    }

}
