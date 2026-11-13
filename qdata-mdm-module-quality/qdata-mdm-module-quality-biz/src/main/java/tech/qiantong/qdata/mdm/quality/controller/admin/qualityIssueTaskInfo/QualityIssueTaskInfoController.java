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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTaskInfo.vo.QualityIssueTaskInfoRespVO;
import tech.qiantong.qdata.mdm.quality.convert.qualityIssueTaskInfo.QualityIssueTaskInfoConvert;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTaskInfo.QualityIssueTaskInfoDO;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueTaskInfo.IQualityIssueTaskInfoService;

import java.util.List;
import javax.annotation.Resource;

/**
 * 查询整改任务明细。明细由整改流程写入。
 */
@Tag(name = "质量问题整改任务明细")
@RestController
@RequestMapping("/quality/qualityIssueTaskInfo")
@Validated
public class QualityIssueTaskInfoController extends BaseController {
    @Resource
    private IQualityIssueTaskInfoService qualityIssueTaskInfoService;

    @Operation(summary = "查询整改任务的问题项明细列表")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:query')")
    @GetMapping("/list")
    public CommonResult<List<QualityIssueTaskInfoRespVO>> list(@RequestParam("taskId") Long taskId) {
        List<QualityIssueTaskInfoDO> list = qualityIssueTaskInfoService.listByTaskId(taskId);
        return CommonResult.success(QualityIssueTaskInfoConvert.INSTANCE.convertToRespVOList(list));
    }

    @Operation(summary = "获取问题项明细详情")
    @PreAuthorize("@ss.hasPermi('quality:qualityissuetask:query')")
    @GetMapping("/{id}")
    public CommonResult<QualityIssueTaskInfoRespVO> getInfo(@PathVariable("id") Long id) {
        QualityIssueTaskInfoDO detail = qualityIssueTaskInfoService.getById(id);
        return CommonResult.success(QualityIssueTaskInfoConvert.INSTANCE.convertToRespVO(detail));
    }

}
