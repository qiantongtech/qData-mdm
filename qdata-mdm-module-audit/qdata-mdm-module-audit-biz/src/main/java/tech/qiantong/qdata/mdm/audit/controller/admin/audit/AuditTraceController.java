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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.audit.controller.admin.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditTracePageRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditChangeTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditChangeTraceListRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditVersionTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceTraceRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditSourceTraceListRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditModelOptionRespVO;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditOperationTraceService;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditChangeTraceService;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditVersionTraceService;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditSourceTraceService;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditTraceModelsService;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/** 审计追溯四页的只读接口，不提供修改历史事实的入口。 */
@Tag(name = "审计追溯")
@RestController
@RequestMapping("/audit/trace")
public class AuditTraceController {
    @Resource private IAuditOperationTraceService operationService;
    @Resource private IAuditChangeTraceService changeService;
    @Resource private IAuditVersionTraceService versionService;
    @Resource private IAuditSourceTraceService sourceService;
    @Resource private IAuditTraceModelsService modelsService;

    @Operation(summary = "查询操作审计列表")
    @GetMapping("/operations")
    @PreAuthorize("@ss.hasPermi('audit:log:list')")
    public CommonResult<AuditTracePageRespVO<AuditOperationTraceRespVO>> operations(@RequestParam Map<String, String> filters) {
        return CommonResult.success(operationService.operations(filters));
    }

    @Operation(summary = "查询操作审计详情")
    @GetMapping("/operations/{id}")
    @PreAuthorize("@ss.hasPermi('audit:log:query')")
    public CommonResult<AuditOperationTraceRespVO> operation(@PathVariable long id,
            @RequestParam(defaultValue = "log") String source) {
        return CommonResult.success(operationService.operation(id, source));
    }

    @Operation(summary = "查询变更历史列表")
    @GetMapping("/changes")
    @PreAuthorize("@ss.hasPermi('audit:change:list')")
    public CommonResult<AuditTracePageRespVO<AuditChangeTraceListRespVO>> changes(@RequestParam Map<String, String> filters) {
        return CommonResult.success(changeService.changes(filters));
    }

    @Operation(summary = "查询变更历史详情")
    @GetMapping("/changes/{id}")
    @PreAuthorize("@ss.hasPermi('audit:change:list')")
    public CommonResult<AuditChangeTraceRespVO> change(@PathVariable String id) {
        return CommonResult.success(changeService.change(id));
    }

    @Operation(summary = "查询数据版本列表")
    @GetMapping("/versions")
    @PreAuthorize("@ss.hasPermi('audit:version:list')")
    public CommonResult<AuditTracePageRespVO<AuditVersionTraceRespVO>> versions(@RequestParam Map<String, String> filters) {
        return CommonResult.success(versionService.versions(filters));
    }

    @Operation(summary = "查询主数据记录的版本列表")
    @GetMapping("/versions/object")
    @PreAuthorize("@ss.hasPermi('audit:version:list')")
    public CommonResult<List<AuditVersionTraceRespVO>> objectVersions(@RequestParam long modelId, @RequestParam long masterRecordId) {
        return CommonResult.success(versionService.objectVersions(modelId, masterRecordId));
    }

    @Operation(summary = "查询数据版本详情")
    @GetMapping("/versions/{id}")
    @PreAuthorize("@ss.hasPermi('audit:version:list')")
    public CommonResult<AuditVersionTraceRespVO> version(@PathVariable long id) {
        return CommonResult.success(versionService.version(id));
    }

    @Operation(summary = "查询来源追溯列表")
    @GetMapping("/sources")
    @PreAuthorize("@ss.hasPermi('audit:source:list')")
    public CommonResult<AuditTracePageRespVO<AuditSourceTraceListRespVO>> sources(@RequestParam Map<String, String> filters) {
        return CommonResult.success(sourceService.sources(filters));
    }

    @Operation(summary = "查询主数据记录的来源追溯详情")
    @GetMapping("/sources/{modelId}/{masterRecordId}")
    @PreAuthorize("@ss.hasPermi('audit:source:list')")
    public CommonResult<AuditSourceTraceRespVO> source(@PathVariable long modelId, @PathVariable long masterRecordId) {
        return CommonResult.success(sourceService.source(modelId, masterRecordId));
    }

    @Operation(summary = "查询审计追溯的模型选项")
    @GetMapping("/models")
    @PreAuthorize("@ss.hasAnyPermi('audit:log:list,audit:change:list,audit:version:list,audit:source:list')")
    public CommonResult<List<AuditModelOptionRespVO>> models() {
        return CommonResult.success(modelsService.models());
    }
}
