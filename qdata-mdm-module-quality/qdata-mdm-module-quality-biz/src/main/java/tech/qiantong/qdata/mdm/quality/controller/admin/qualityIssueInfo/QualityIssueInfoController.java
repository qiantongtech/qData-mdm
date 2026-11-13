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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.convert.qualityIssueInfo.QualityIssueInfoConvert;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueInfo.IQualityIssueInfoService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 问题数据明细Controller
 *
 * @author qdata
 * @date 2026-09-23
 */
@Tag(name = "问题数据明细")
@RestController
@RequestMapping("/quality/qualityIssueInfo")
@Validated
public class QualityIssueInfoController extends BaseController {
    @Resource
    private IQualityIssueInfoService qualityIssueInfoService;

    @Operation(summary = "查询问题数据明细列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityIssueInfoRespVO>> list(QualityIssueInfoPageReqVO qualityIssueInfo) {
        PageResult<QualityIssueInfoDO> page = qualityIssueInfoService.getQualityIssueInfoPage(qualityIssueInfo);
        return CommonResult.success(BeanUtils.toBean(page, QualityIssueInfoRespVO.class));
    }

    @Operation(summary = "导出问题数据明细列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:export')")
    @Log(title = "问题数据明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityIssueInfoPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityIssueInfoDO> list = (List<QualityIssueInfoDO>) qualityIssueInfoService.getQualityIssueInfoPage(exportReqVO).getRows();
        ExcelUtil<QualityIssueInfoRespVO> util = new ExcelUtil<>(QualityIssueInfoRespVO.class);
        util.exportExcel(response, QualityIssueInfoConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入问题数据明细列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:import')")
    @Log(title = "问题数据明细", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityIssueInfoRespVO> util = new ExcelUtil<>(QualityIssueInfoRespVO.class);
        List<QualityIssueInfoRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = qualityIssueInfoService.importQualityIssueInfo(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取问题数据明细详细信息")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityIssueInfoRespVO> getInfo(@PathVariable("id") Long id) {
        QualityIssueInfoDO qualityIssueInfoDO = qualityIssueInfoService.getQualityIssueInfoById(id);
        return CommonResult.success(BeanUtils.toBean(qualityIssueInfoDO, QualityIssueInfoRespVO.class));
    }

    @Operation(summary = "新增问题数据明细")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:add')")
    @Log(title = "问题数据明细", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityIssueInfoSaveReqVO qualityIssueInfo) {
        qualityIssueInfo.setCreatorId(getUserId());
        qualityIssueInfo.setCreateBy(getNickName());
        qualityIssueInfo.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(qualityIssueInfoService.createQualityIssueInfo(qualityIssueInfo));
    }

    @Operation(summary = "修改问题数据明细")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:edit')")
    @Log(title = "问题数据明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityIssueInfoSaveReqVO qualityIssueInfo) {
        qualityIssueInfo.setUpdatorId(getUserId());
        qualityIssueInfo.setUpdateBy(getNickName());
        qualityIssueInfo.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(qualityIssueInfoService.updateQualityIssueInfo(qualityIssueInfo));
    }

    @Operation(summary = "删除问题数据明细")
    @PreAuthorize("@ss.hasPermi('quality:qualityissueinfo:remove')")
    @Log(title = "问题数据明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(qualityIssueInfoService.removeQualityIssueInfo(Arrays.asList(ids)));
    }

}
