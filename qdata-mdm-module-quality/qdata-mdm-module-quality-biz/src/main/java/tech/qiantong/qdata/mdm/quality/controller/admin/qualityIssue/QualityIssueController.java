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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue;

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
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssuePageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssue.vo.QualityIssueSaveReqVO;
import tech.qiantong.qdata.mdm.quality.convert.qualityIssue.QualityIssueConvert;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.quality.service.qualityIssue.IQualityIssueService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 问题数据Controller
 *
 * @author qdata
 * @date 2026-09-23
 */
@Tag(name = "问题数据")
@RestController
@RequestMapping("/quality/qualityIssue")
@Validated
public class QualityIssueController extends BaseController {
    @Resource
    private IQualityIssueService qualityIssueService;

    @Operation(summary = "查询问题数据列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityIssueRespVO>> list(QualityIssuePageReqVO qualityIssue) {
        PageResult<QualityIssueDO> page = qualityIssueService.getQualityIssuePage(qualityIssue);
        return CommonResult.success(BeanUtils.toBean(page, QualityIssueRespVO.class));
    }

    @Operation(summary = "查询问题数据状态数量")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:list')")
    @GetMapping("/statusCount")
    public CommonResult<Map<String, Long>> statusCount(QualityIssuePageReqVO qualityIssue) {
        return CommonResult.success(qualityIssueService.getQualityIssueStatusCount(qualityIssue));
    }

    @Operation(summary = "导出问题数据列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:export')")
    @Log(title = "问题数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityIssuePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityIssueDO> list = (List<QualityIssueDO>) qualityIssueService.getQualityIssuePage(exportReqVO).getRows();
        ExcelUtil<QualityIssueRespVO> util = new ExcelUtil<>(QualityIssueRespVO.class);
        util.exportExcel(response, QualityIssueConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入问题数据列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:import')")
    @Log(title = "问题数据", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityIssueRespVO> util = new ExcelUtil<>(QualityIssueRespVO.class);
        List<QualityIssueRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = qualityIssueService.importQualityIssue(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取问题数据详细信息")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityIssueRespVO> getInfo(@PathVariable("id") Long id) {
        QualityIssueDO qualityIssueDO = qualityIssueService.getQualityIssueById(id);
        return CommonResult.success(BeanUtils.toBean(qualityIssueDO, QualityIssueRespVO.class));
    }

    @Operation(summary = "新增问题数据")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:add')")
    @Log(title = "问题数据", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityIssueSaveReqVO qualityIssue) {
        qualityIssue.setCreatorId(getUserId());
        qualityIssue.setCreateBy(getNickName());
        qualityIssue.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(qualityIssueService.createQualityIssue(qualityIssue));
    }

    @Operation(summary = "修改问题数据")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:edit')")
    @Log(title = "问题数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityIssueSaveReqVO qualityIssue) {
        qualityIssue.setUpdatorId(getUserId());
        qualityIssue.setUpdateBy(getNickName());
        qualityIssue.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(qualityIssueService.updateQualityIssue(qualityIssue));
    }

    @Operation(summary = "删除问题数据")
    @PreAuthorize("@ss.hasPermi('quality:qualityissue:remove')")
    @Log(title = "问题数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(qualityIssueService.removeQualityIssue(Arrays.asList(ids)));
    }

}
