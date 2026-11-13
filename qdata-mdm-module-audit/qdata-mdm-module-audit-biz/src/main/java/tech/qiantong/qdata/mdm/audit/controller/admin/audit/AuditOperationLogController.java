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

package tech.qiantong.qdata.mdm.audit.controller.admin.audit;

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
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogPageReqVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogSaveReqVO;
import tech.qiantong.qdata.mdm.audit.convert.audit.AuditOperationLogConvert;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditOperationLogService;

/**
 * 业务操作审计Controller
 *
 * @author zhangyanyan
 * @date 2026-09-22
 */
@Tag(name = "业务操作审计")
@RestController
@RequestMapping("/tech/qiantong/qdata/mdm/audit/LOG")
@Validated
public class AuditOperationLogController extends BaseController {
    @Resource
    private IAuditOperationLogService auditOperationLogService;

    @Operation(summary = "查询业务操作审计列表")
    @PreAuthorize("@ss.hasPermi('audit:log:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AuditOperationLogRespVO>> list(AuditOperationLogPageReqVO auditOperationLog) {
        PageResult<AuditOperationLogDO> page = auditOperationLogService.getAuditOperationLogPage(auditOperationLog);
        return CommonResult.success(BeanUtils.toBean(page, AuditOperationLogRespVO.class));
    }

    @Operation(summary = "导出业务操作审计列表")
    @PreAuthorize("@ss.hasPermi('audit:log:export')")
    @Log(title = "业务操作审计", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AuditOperationLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AuditOperationLogDO> list = (List<AuditOperationLogDO>) auditOperationLogService.getAuditOperationLogPage(exportReqVO).getRows();
        ExcelUtil<AuditOperationLogRespVO> util = new ExcelUtil<>(AuditOperationLogRespVO.class);
        util.exportExcel(response, AuditOperationLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入业务操作审计列表")
    @PreAuthorize("@ss.hasPermi('audit:log:import')")
    @Log(title = "业务操作审计", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AuditOperationLogRespVO> util = new ExcelUtil<>(AuditOperationLogRespVO.class);
        List<AuditOperationLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = auditOperationLogService.importAuditOperationLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取业务操作审计详细信息")
    @PreAuthorize("@ss.hasPermi('audit:log:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AuditOperationLogRespVO> getInfo(@PathVariable("id") Long id) {
        AuditOperationLogDO auditOperationLogDO = auditOperationLogService.getAuditOperationLogById(id);
        return CommonResult.success(BeanUtils.toBean(auditOperationLogDO, AuditOperationLogRespVO.class));
    }

    @Operation(summary = "新增业务操作审计")
    @PreAuthorize("@ss.hasPermi('audit:log:add')")
    @Log(title = "业务操作审计", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AuditOperationLogSaveReqVO auditOperationLog) {
        auditOperationLog.setCreatorId(getUserId());
        auditOperationLog.setCreateBy(getNickName());
        auditOperationLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(auditOperationLogService.createAuditOperationLog(auditOperationLog));
    }

    @Operation(summary = "修改业务操作审计")
    @PreAuthorize("@ss.hasPermi('audit:log:edit')")
    @Log(title = "业务操作审计", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AuditOperationLogSaveReqVO auditOperationLog) {
        auditOperationLog.setUpdatorId(getUserId());
        auditOperationLog.setUpdateBy(getNickName());
        auditOperationLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(auditOperationLogService.updateAuditOperationLog(auditOperationLog));
    }

    @Operation(summary = "删除业务操作审计")
    @PreAuthorize("@ss.hasPermi('audit:log:remove')")
    @Log(title = "业务操作审计", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(auditOperationLogService.removeAuditOperationLog(Arrays.asList(ids)));
    }

}

