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

package tech.qiantong.qdata.mdm.ingest.controller.admin.ingest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataDetailRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataModelColumnRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataModelTreeRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.ingest.vo.SourceDataPageRespVO;
import tech.qiantong.qdata.mdm.ingest.service.ingest.SourceDataService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 来源数据接口。
 */
@Tag(name = "来源数据")
@RestController
@Validated
@RequestMapping("/ingest/source-data")
public class SourceDataController {

    @Resource
    private SourceDataService sourceDataService;

    @Operation(summary = "查询来源数据列表")
    @PreAuthorize("@ss.hasPermi('ingest:sourceData:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<SourceDataPageRespVO>> list(SourceDataPageReqVO pageReqVO) {
        return CommonResult.success(sourceDataService.getPage(pageReqVO));
    }

    @Operation(summary = "查询来源数据主数据模型树")
    @PreAuthorize("@ss.hasPermi('ingest:sourceData:list')")
    @GetMapping("/model-tree")
    public CommonResult<List<SourceDataModelTreeRespVO>> getModelTree() {
        return CommonResult.success(sourceDataService.getModelTree());
    }

    @Operation(summary = "查询来源数据页面的模型业务字段")
    @PreAuthorize("@ss.hasPermi('ingest:sourceData:list')")
    @GetMapping("/model/{modelId}/columns")
    public CommonResult<List<SourceDataModelColumnRespVO>> getModelColumns(@PathVariable Long modelId) {
        return CommonResult.success(sourceDataService.getModelColumns(modelId));
    }

    @Operation(summary = "查询来源数据详情")
    @PreAuthorize("@ss.hasPermi('ingest:sourceData:query')")
    @GetMapping("/{id}")
    public CommonResult<SourceDataDetailRespVO> getDetail(@PathVariable Long id, @RequestParam Long modelId) {
        return CommonResult.success(sourceDataService.getDetail(modelId, id));
    }

    @Operation(summary = "删除来源数据")
    @PreAuthorize("@ss.hasPermi('ingest:sourceData:remove')")
    @Log(title = "来源数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable Long id, @RequestParam Long modelId) {
        sourceDataService.deleteRecord(modelId, id);
        return CommonResult.success(Boolean.TRUE);
    }
}
