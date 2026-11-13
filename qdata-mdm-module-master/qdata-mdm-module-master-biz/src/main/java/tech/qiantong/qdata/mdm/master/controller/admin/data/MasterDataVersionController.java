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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionPageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionSnapshotRespVO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataVersionService;

import javax.annotation.Resource;

/**
 * MASTER_DATA_VERSION 历史版本列表与快照只读入口；不提供版本维护或导入导出。
 */
@Tag(name = "主数据版本")
@RestController
@RequestMapping("/master/data/version")
@Validated
public class MasterDataVersionController extends BaseController {

    @Resource
    private IMasterDataVersionService masterDataVersionService;

    @Operation(summary = "查询单条主数据的正式数据版本记录")
    @PreAuthorize("@ss.hasPermi('master:instance:version:list')")
    @GetMapping("/list/{instanceId}/{masterDataId}")
    public CommonResult<PageResult<MasterDataVersionRespVO>> list(
            @PathVariable Long instanceId, @PathVariable Long masterDataId,
            MasterDataVersionPageReqVO reqVO) {
        return CommonResult.success(masterDataVersionService.getPage(
                instanceId, masterDataId, reqVO));
    }

    @Operation(summary = "查询单条主数据的历史版本快照")
    @PreAuthorize("@ss.hasPermi('master:instance:version:list')")
    @GetMapping("/snapshot/{instanceId}/{masterDataId}/{versionId}")
    public CommonResult<MasterDataVersionSnapshotRespVO> snapshot(
            @PathVariable Long instanceId, @PathVariable Long masterDataId,
            @PathVariable Long versionId) {
        return CommonResult.success(masterDataVersionService.getSnapshot(instanceId, masterDataId, versionId));
    }
}
