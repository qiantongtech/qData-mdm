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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality;

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
import tech.qiantong.qdata.mdm.common.exception.enums.GlobalErrorCodeConstants;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.QualityMergeRecordRespVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergePendingDO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualitySaveReqVO;
import tech.qiantong.qdata.mdm.rule.convert.ruleQuality.RuleQualityConvert;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import tech.qiantong.qdata.mdm.rule.service.ruleQuality.IRuleQualityService;

/**
 * 质量规则Controller
 *
 * @author qdata
 * @date 2026-09-18
 */
@Tag(name = "质量规则")
@RestController
@RequestMapping("/rule/rulequality")
@Validated
public class RuleQualityController extends BaseController {
    @Resource
    private IRuleQualityService ruleQualityService;

    @Operation(summary = "查询质量规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<RuleQualityRespVO>> list(RuleQualityPageReqVO ruleQuality) {
        PageResult<RuleQualityDO> page = ruleQualityService.getRuleQualityPage(ruleQuality);
        return CommonResult.success(BeanUtils.toBean(page, RuleQualityRespVO.class));
    }

    @Operation(summary = "查询质量规则关联的合并记录")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:query') and @ss.hasPermi('merge:manage:list')")
    @GetMapping("/{id}/merge-records")
    public CommonResult<PageResult<QualityMergeRecordRespVO>> mergeRecords(@PathVariable Long id, PageParam page) {
        PageResult<QualityMergePendingDO> result = ruleQualityService.getMergeRecords(id, page);
        return CommonResult.success(BeanUtils.toBean(result, QualityMergeRecordRespVO.class));
    }

    @Operation(summary = "导出质量规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:export')")
    @Log(title = "质量规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RuleQualityPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RuleQualityDO> list = (List<RuleQualityDO>) ruleQualityService.getRuleQualityPage(exportReqVO).getRows();
        ExcelUtil<RuleQualityRespVO> util = new ExcelUtil<>(RuleQualityRespVO.class);
        util.exportExcel(response, RuleQualityConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入质量规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:import')")
    @Log(title = "质量规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<RuleQualityRespVO> util = new ExcelUtil<>(RuleQualityRespVO.class);
        List<RuleQualityRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ruleQualityService.importRuleQuality(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取质量规则详细信息")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<RuleQualityRespVO> getInfo(@PathVariable("id") Long id) {
        RuleQualityDO ruleQualityDO = ruleQualityService.getRuleQualityById(id);
        return CommonResult.success(BeanUtils.toBean(ruleQualityDO, RuleQualityRespVO.class));
    }

    @Operation(summary = "新增质量规则")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:add')")
    @Log(title = "质量规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody RuleQualitySaveReqVO ruleQuality) {
        ruleQuality.setCreatorId(getUserId());
        ruleQuality.setCreateBy(getNickName());
        ruleQuality.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ruleQualityService.createRuleQuality(ruleQuality));
    }

    @Operation(summary = "修改质量规则")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:edit')")
    @Log(title = "质量规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody RuleQualitySaveReqVO ruleQuality) {
        ruleQuality.setUpdatorId(getUserId());
        ruleQuality.setUpdateBy(getNickName());
        ruleQuality.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ruleQualityService.updateRuleQuality(ruleQuality));
    }

    @Operation(summary = "删除质量规则")
    @PreAuthorize("@ss.hasPermi('rule:rulequality:remove')")
    @Log(title = "质量规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ruleQualityService.removeRuleQuality(Arrays.asList(ids)));
    }

}
