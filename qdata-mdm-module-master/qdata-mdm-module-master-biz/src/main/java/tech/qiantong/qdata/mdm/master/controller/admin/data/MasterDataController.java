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

package tech.qiantong.qdata.mdm.master.controller.admin.data;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.*;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataExportService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataService;
import tech.qiantong.qdata.mdm.master.service.data.IMasterSubmodelService;
import tech.qiantong.qdata.mdm.master.service.data.navigation.MasterClassificationNavigationService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.io.IOException;
import java.util.List;

/**
 * 主数据查询 Controller。
 */
@Tag(name = "主数据")
@RestController
@RequestMapping("/master/data")
@Validated
public class MasterDataController extends BaseController {

    @Resource
    private IMasterDataService masterDataService;

    @Resource
    private IMasterDataExportService exportService;

    @Resource
    private IMasterSubmodelService submodelService;

    @Resource
    private MasterClassificationNavigationService classificationNavigationService;

    @Operation(summary = "查询普通模型的完整分类导航")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/classifications/{instanceId}")
    public CommonResult<List<MasterClassificationFieldRespVO>> classifications(
            @PathVariable @Positive Long instanceId, @Valid MasterDataPageReqVO request) {
        return CommonResult.success(classificationNavigationService.navigation(instanceId, request.getModelId(), request.getModelVersionId()));
    }

    @Operation(summary = "查询分类维护控件的完整选项")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/classificationOptions/{instanceId}")
    public CommonResult<MasterClassificationTreeRespVO> classificationOptions(
            @PathVariable @Positive Long instanceId,
            @Valid MasterClassificationOptionsReqVO request) {
        return CommonResult.success(classificationNavigationService.options(instanceId, request));
    }


    @Operation(summary = "查询主数据动态列表")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/{instanceId}")
    public CommonResult<MasterDataRespVO> data(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId, @Valid MasterDataPageReqVO reqVO) {
        return CommonResult.success(masterDataService.getData(instanceId, reqVO));
    }

    @Operation(summary = "查询分类模型树形列表数据，不分页")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/category/{instanceId}")
    public CommonResult<MasterDataRespVO> categoryData(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId, @Valid MasterDataPageReqVO reqVO) {
        return CommonResult.success(masterDataService.getCategoryData(instanceId, reqVO));
    }

    @Operation(summary = "查询当前模型关联的来源系统筛选项")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/sourceSystems/{instanceId}")
    public CommonResult<List<MasterSourceSystemRespVO>> sourceSystems(@PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId,
            @RequestParam(required = false) @Positive(message = "模型ID必须为正数") Long modelId) {
        return CommonResult.success(masterDataService.getSourceSystems(instanceId, modelId));
    }

    @Operation(summary = "按当前筛选条件导出主数据")
    @PreAuthorize("@ss.hasPermi('master:instance:export')")
    @Log(title = "主数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export/{instanceId}")
    public void export(@PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId, @Valid MasterDataPageReqVO reqVO,
                       HttpServletResponse response) throws IOException {
        exportService.export(response, instanceId, reqVO);
    }

    @Operation(summary = "查询单条正式主数据详情")
    @PreAuthorize("@ss.hasAnyPermi('master:instance:query,master:instance:add,master:instance:edit,master:instance:import,master:instance:status')")
    @GetMapping("/{instanceId}/{masterDataId}")
    public CommonResult<MasterDataDetailRespVO> dataDetail(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId,
            @PathVariable("masterDataId") @Positive(message = "主数据ID必须为正数") Long masterDataId,
            @RequestParam @Positive(message = "模型ID必须为正数") Long modelId) {
        return CommonResult.success(masterDataService.getDetail(instanceId, masterDataId, modelId));
    }

    @Operation(summary = "查询单条主数据的子模型数据")
    @PreAuthorize("@ss.hasAnyPermi('master:instance:query,master:instance:add,master:instance:edit,master:instance:import')")
    @GetMapping("/submodels/{instanceId}/{masterDataId}")
    public CommonResult<MasterSubmodelDataRespVO> submodels(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId, @PathVariable("masterDataId") @Positive(message = "主数据ID必须为正数") Long masterDataId,
            @Valid MasterSubmodelPageReqVO reqVO) {
        return CommonResult.success(submodelService.getData(
                instanceId, masterDataId, reqVO));
    }

    @Operation(summary = "查询主子数据维护所需的子模型定义")
    @PreAuthorize("@ss.hasAnyPermi('master:instance:query,master:instance:add,master:instance:edit,master:instance:import')")
    @GetMapping("/submodelDefinitions/{instanceId}")
    public CommonResult<MasterSubmodelDataRespVO> submodelDefinitions(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId, @RequestParam(required = false) @Positive(message = "子模型ID必须为正数") Long submodelId) {
        return CommonResult.success(submodelService.getDefinitions(instanceId, submodelId));
    }

    @Operation(summary = "查询单条子模型记录详情")
    @PreAuthorize("@ss.hasAnyPermi('master:instance:query,master:instance:add,master:instance:edit,master:instance:import')")
    @GetMapping("/submodels/{instanceId}/{masterDataId}/{submodelId}/{subRecordId}")
    public CommonResult<MasterSubmodelRecordRespVO> submodelRecord(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId, @PathVariable("masterDataId") @Positive(message = "主数据ID必须为正数") Long masterDataId,
            @PathVariable("submodelId") @Positive(message = "子模型ID必须为正数") Long submodelId, @PathVariable("subRecordId") @Positive(message = "子记录ID必须为正数") Long subRecordId) {
        return CommonResult.success(submodelService.getRecord(
                instanceId, masterDataId, submodelId, subRecordId));
    }

    @Operation(summary = "查询分类模型的分类树")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/tree/{instanceId}")
    public CommonResult<List<MasterTreeNodeRespVO>> tree(@PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId) {
        return CommonResult.success(masterDataService.getTree(instanceId));
    }
}
