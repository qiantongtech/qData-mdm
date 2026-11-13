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

package tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import cn.hutool.core.date.DateUtil;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessPageReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessSaveReqVO;
import tech.qiantong.qdata.mdm.system.convert.flowMyBusiness.SystemFlowMyBusinessConvert;
import tech.qiantong.qdata.mdm.system.dal.dataobject.flowMyBusiness.SystemFlowMyBusinessDO;
import tech.qiantong.qdata.mdm.system.service.flowMyBusiness.ISystemFlowMyBusinessService;

/**
 * 审批记录Controller
 *
 * @author qdata
 * @date 2025-04-25
 */
@Tag(name = "审批记录")
@RestController
@RequestMapping("/system/flowMyBusiness")
@Validated
public class SystemFlowMyBusinessController extends BaseController {
    @Resource
    private ISystemFlowMyBusinessService systemFlowMyBusinessService;

    @Operation(summary = "查询审批记录列表")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<SystemFlowMyBusinessRespVO>> list(SystemFlowMyBusinessPageReqVO systemFlowMyBusiness) {
        PageResult<SystemFlowMyBusinessDO> page = systemFlowMyBusinessService.getSystemFlowMyBusinessPage(systemFlowMyBusiness);
        return CommonResult.success(BeanUtils.toBean(page, SystemFlowMyBusinessRespVO.class));
    }

    @Operation(summary = "导出审批记录列表")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:export')")
    @Log(title = "审批记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SystemFlowMyBusinessPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SystemFlowMyBusinessDO> list = (List<SystemFlowMyBusinessDO>) systemFlowMyBusinessService.getSystemFlowMyBusinessPage(exportReqVO).getRows();
        ExcelUtil<SystemFlowMyBusinessRespVO> util = new ExcelUtil<>(SystemFlowMyBusinessRespVO.class);
        util.exportExcel(response, SystemFlowMyBusinessConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入审批记录列表")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:import')")
    @Log(title = "审批记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<SystemFlowMyBusinessRespVO> util = new ExcelUtil<>(SystemFlowMyBusinessRespVO.class);
        List<SystemFlowMyBusinessRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = systemFlowMyBusinessService.importSystemFlowMyBusiness(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取审批记录详细信息")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<SystemFlowMyBusinessRespVO> getInfo(@PathVariable("id") Long id) {
        SystemFlowMyBusinessDO systemFlowMyBusinessDO = systemFlowMyBusinessService.getSystemFlowMyBusinessById(id);
        return CommonResult.success(BeanUtils.toBean(systemFlowMyBusinessDO, SystemFlowMyBusinessRespVO.class));
    }

    @Operation(summary = "新增审批记录")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:add')")
    @Log(title = "审批记录", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody SystemFlowMyBusinessSaveReqVO systemFlowMyBusiness) {
        systemFlowMyBusiness.setCreatorId(getUserId());
        systemFlowMyBusiness.setCreateBy(getNickName());
        systemFlowMyBusiness.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(systemFlowMyBusinessService.createSystemFlowMyBusiness(systemFlowMyBusiness));
    }

    @Operation(summary = "修改审批记录")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:edit')")
    @Log(title = "审批记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody SystemFlowMyBusinessSaveReqVO systemFlowMyBusiness) {
        systemFlowMyBusiness.setUpdatorId(getUserId());
        systemFlowMyBusiness.setUpdateBy(getNickName());
        systemFlowMyBusiness.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(systemFlowMyBusinessService.updateSystemFlowMyBusiness(systemFlowMyBusiness));
    }

    @Operation(summary = "删除审批记录")
    @PreAuthorize("@ss.hasPermi('system:flowMyBusiness:flowmybusiness:remove')")
    @Log(title = "审批记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(systemFlowMyBusinessService.removeSystemFlowMyBusiness(Arrays.asList(ids)));
    }

}
