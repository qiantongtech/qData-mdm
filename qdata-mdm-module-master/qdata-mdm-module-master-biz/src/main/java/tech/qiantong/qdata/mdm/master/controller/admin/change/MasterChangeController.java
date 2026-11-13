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

package tech.qiantong.qdata.mdm.master.controller.admin.change;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeSnapshotRespVO;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeService;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * MASTER_CHANGE 已生效业务变更历史的只读入口；不提供维护或导入导出。
 */
@Tag(name = "主数据变更历史")
@RestController
@RequestMapping("/master/change")
@Validated
public class MasterChangeController extends BaseController {

    @Resource
    private IMasterChangeService masterChangeService;

    @Operation(summary = "查询单条主数据已生效的业务变更记录")
    @PreAuthorize("@ss.hasPermi('master:instance:change:list')")
    @GetMapping("/list/{instanceId}/{masterDataId}")
    public CommonResult<PageResult<MasterChangeRespVO>> list(
            @PathVariable Long instanceId, @PathVariable Long masterDataId,
            @Valid MasterChangePageReqVO reqVO) {
        return CommonResult.success(masterChangeService.getPage(instanceId, masterDataId, reqVO));
    }

    @Operation(summary = "查询单条主数据已生效变更的前后快照")
    @PreAuthorize("@ss.hasPermi('master:instance:change:list')")
    @GetMapping("/snapshot/{instanceId}/{masterDataId}/{changeId}")
    public CommonResult<MasterChangeSnapshotRespVO> snapshot(
            @PathVariable Long instanceId, @PathVariable Long masterDataId,
            @PathVariable Long changeId) {
        return CommonResult.success(
                masterChangeService.getSnapshot(instanceId, masterDataId, changeId));
    }
}
