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

package tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch;

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
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchSaveReqVO;
import tech.qiantong.qdata.mdm.rule.convert.ruleMatch.RuleMatchConvert;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
import tech.qiantong.qdata.mdm.rule.service.ruleMatch.IRuleMatchService;

/**
 * 匹配规则Controller
 *
 * @author qdata
 * @date 2026-09-15
 */
@Tag(name = "匹配规则")
@RestController
@RequestMapping("/rule/rulematch")
@Validated
public class RuleMatchController extends BaseController {
    @Resource
    private IRuleMatchService ruleMatchService;

    @Operation(summary = "查询匹配规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<RuleMatchRespVO>> list(RuleMatchPageReqVO ruleMatch) {
        PageResult<RuleMatchDO> page = ruleMatchService.getRuleMatchPage(ruleMatch);
        return CommonResult.success(BeanUtils.toBean(page, RuleMatchRespVO.class));
    }

    @Operation(summary = "导出匹配规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:export')")
    @Log(title = "匹配规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RuleMatchPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RuleMatchDO> list = (List<RuleMatchDO>) ruleMatchService.getRuleMatchPage(exportReqVO).getRows();
        ExcelUtil<RuleMatchRespVO> util = new ExcelUtil<>(RuleMatchRespVO.class);
        util.exportExcel(response, RuleMatchConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入匹配规则列表")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:import')")
    @Log(title = "匹配规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<RuleMatchRespVO> util = new ExcelUtil<>(RuleMatchRespVO.class);
        List<RuleMatchRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ruleMatchService.importRuleMatch(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取匹配规则详细信息")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<RuleMatchRespVO> getInfo(@PathVariable("id") Long id) {
        RuleMatchDO ruleMatchDO = ruleMatchService.getRuleMatchById(id);
        return CommonResult.success(BeanUtils.toBean(ruleMatchDO, RuleMatchRespVO.class));
    }

    @Operation(summary = "新增匹配规则")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:add')")
    @Log(title = "匹配规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody RuleMatchSaveReqVO ruleMatch) {
        ruleMatch.setCreatorId(getUserId());
        ruleMatch.setCreateBy(getNickName());
        ruleMatch.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ruleMatchService.createRuleMatch(ruleMatch));
    }

    @Operation(summary = "修改匹配规则")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:edit')")
    @Log(title = "匹配规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody RuleMatchSaveReqVO ruleMatch) {
        ruleMatch.setUpdatorId(getUserId());
        ruleMatch.setUpdateBy(getNickName());
        ruleMatch.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ruleMatchService.updateRuleMatch(ruleMatch));
    }

    @Operation(summary = "删除匹配规则")
    @PreAuthorize("@ss.hasPermi('rule:rulematch:remove')")
    @Log(title = "匹配规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ruleMatchService.removeRuleMatch(Arrays.asList(ids)));
    }

}
