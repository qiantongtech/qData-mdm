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

package tech.qiantong.qdata.mdm.master.controller.admin.source;

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
import tech.qiantong.qdata.mdm.master.controller.admin.source.vo.MasterSourceDetailRespVO;
import tech.qiantong.qdata.mdm.master.service.source.IMasterSourceRelService;

import javax.annotation.Resource;

/**
 * MASTER_SOURCE_REL 来源记录与 MASTER_FIELD_SOURCE_REL 当前版本字段来源的只读详情入口。
 */
@Tag(name = "主数据来源映射")
@RestController
@RequestMapping("/master/source/rel")
@Validated
public class MasterSourceRelController extends BaseController {

    @Resource
    private IMasterSourceRelService masterSourceRelService;

    /**
     * 查询主数据的有效来源记录及当前版本的字段取值来源。
     *
     * @param instanceId 可用模型实例 ID
     * @param masterDataId 权威主数据记录 ID
     * @return 全部有效来源记录及当前版本字段来源；无当前版本时字段来源为空列表
     */
    @Operation(summary = "查询单条主数据的来源记录与字段取值来源")
    @PreAuthorize("@ss.hasPermi('master:instance:source:list')")
    @GetMapping("/records/{instanceId}/{masterDataId}")
    public CommonResult<MasterSourceDetailRespVO> sourceDetail(
            @PathVariable Long instanceId, @PathVariable Long masterDataId) {
        return CommonResult.success(masterSourceRelService.getDetail(
                instanceId, masterDataId));
    }
}
