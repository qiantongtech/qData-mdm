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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor;

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
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorSaveReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.SurvivorMergeRecordRespVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeRecordDO;
import tech.qiantong.qdata.mdm.rule.convert.ruleSurvivor.RuleSurvivorConvert;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
import tech.qiantong.qdata.mdm.rule.service.ruleSurvivor.IRuleSurvivorService;

/**
 * 生存规则Controller
 *
 * @author qdata
 * @date 2026-09-17
 */
@Tag(name = "生存规则")
@RestController
@RequestMapping("/rule/rulesurvivor")
@Validated
public class RuleSurvivorController extends BaseController {
    @Resource
    private IRuleSurvivorService ruleSurvivorService;

    @Operation(summary = "查询生存规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<RuleSurvivorRespVO>> list(RuleSurvivorPageReqVO ruleSurvivor) {
        PageResult<RuleSurvivorDO> page = ruleSurvivorService.getRuleSurvivorPage(ruleSurvivor);
        return CommonResult.success(BeanUtils.toBean(page, RuleSurvivorRespVO.class));
    }

    @Operation(summary = "查询生存规则关联的合并管理记录")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:query') and @ss.hasPermi('merge:manage:list')")
    @GetMapping("/{id}/merge-records")
    public CommonResult<PageResult<SurvivorMergeRecordRespVO>> mergeRecords(@PathVariable("id") Long id, PageParam page) {
        PageResult<SurvivorMergeRecordDO> result = ruleSurvivorService.getMergeRecords(id, page);
        return CommonResult.success(BeanUtils.toBean(result, SurvivorMergeRecordRespVO.class));
    }

    @Operation(summary = "导出生存规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:export')")
    @Log(title = "生存规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RuleSurvivorPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RuleSurvivorDO> list = (List<RuleSurvivorDO>) ruleSurvivorService.getRuleSurvivorPage(exportReqVO).getRows();
        ExcelUtil<RuleSurvivorRespVO> util = new ExcelUtil<>(RuleSurvivorRespVO.class);
        util.exportExcel(response, RuleSurvivorConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入生存规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:import')")
    @Log(title = "生存规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<RuleSurvivorRespVO> util = new ExcelUtil<>(RuleSurvivorRespVO.class);
        List<RuleSurvivorRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ruleSurvivorService.importRuleSurvivor(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取生存规则详细信息")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<RuleSurvivorRespVO> getInfo(@PathVariable("id") Long id) {
        RuleSurvivorDO ruleSurvivorDO = ruleSurvivorService.getRuleSurvivorById(id);
        return CommonResult.success(BeanUtils.toBean(ruleSurvivorDO, RuleSurvivorRespVO.class));
    }

    @Operation(summary = "新增生存规则")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:add')")
    @Log(title = "生存规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody RuleSurvivorSaveReqVO ruleSurvivor) {
        ruleSurvivor.setCreatorId(getUserId());
        ruleSurvivor.setCreateBy(getNickName());
        ruleSurvivor.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ruleSurvivorService.createRuleSurvivor(ruleSurvivor));
    }

    @Operation(summary = "修改生存规则")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:edit')")
    @Log(title = "生存规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody RuleSurvivorSaveReqVO ruleSurvivor) {
        ruleSurvivor.setUpdatorId(getUserId());
        ruleSurvivor.setUpdateBy(getNickName());
        ruleSurvivor.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ruleSurvivorService.updateRuleSurvivor(ruleSurvivor));
    }

    @Operation(summary = "删除生存规则")
    @PreAuthorize("@ss.hasPermi('rule:rulesurvivor:remove')")
    @Log(title = "生存规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ruleSurvivorService.removeRuleSurvivor(Arrays.asList(ids)));
    }

}
