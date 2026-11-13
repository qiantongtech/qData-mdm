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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskLog.vo.QualityIssueTaskLogRespVO;
import tech.qiantong.qdata.mdm.quality.convert.qualityIssueTaskLog.QualityIssueTaskLogConvert;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskLog.QualityIssueTaskLogDO;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskLog.IQualityIssueTaskLogService;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 问题整改记录日志Controller
 *
 * @author qdata
 * @date 2026-09-23
 */
@Tag(name = "问题整改记录日志")
@RestController
@RequestMapping("/quality/qualityIssueTaskLog")
@Validated
public class QualityIssueTaskLogController extends BaseController {
    @Resource
    private IQualityIssueTaskLogService qualityIssueTaskLogService;

    @Operation(summary = "查询问题整改记录日志列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:query')")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityIssueTaskLogRespVO>> list(@Valid QualityIssueTaskLogPageReqVO qualityIssueTaskLog) {
        PageResult<QualityIssueTaskLogDO> page = qualityIssueTaskLogService.getQualityIssueTaskLogPage(qualityIssueTaskLog);
        return CommonResult.success(BeanUtils.toBean(page, QualityIssueTaskLogRespVO.class));
    }

    @Operation(summary = "导出问题整改记录日志列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetasklog:export')")
    @Log(title = "问题整改记录日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityIssueTaskLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityIssueTaskLogDO> list = (List<QualityIssueTaskLogDO>) qualityIssueTaskLogService.getQualityIssueTaskLogPage(exportReqVO).getRows();
        ExcelUtil<QualityIssueTaskLogRespVO> util = new ExcelUtil<>(QualityIssueTaskLogRespVO.class);
        util.exportExcel(response, QualityIssueTaskLogConvert.INSTANCE.convertToRespVOList(list), "整改处理记录");
    }

    @Operation(summary = "获取问题整改记录日志详细信息")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityIssueTaskLogRespVO> getInfo(@PathVariable("id") Long id) {
        QualityIssueTaskLogDO qualityIssueTaskLogDO = qualityIssueTaskLogService.getQualityIssueTaskLogById(id);
        return CommonResult.success(BeanUtils.toBean(qualityIssueTaskLogDO, QualityIssueTaskLogRespVO.class));
    }

}
