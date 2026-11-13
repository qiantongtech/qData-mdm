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

package tech.qiantong.qdata.mdm.master.controller.admin.instance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterInstanceRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.instance.vo.MasterInstancePageReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.instance.vo.MasterInstanceRespVO;
import tech.qiantong.qdata.mdm.master.service.instance.IMasterInstanceService;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 可用主数据模型实例入口查询。
 */
@Tag(name = "主数据模型实例")
@RestController
@RequestMapping("/master/instance")
@Validated
public class MasterInstanceController extends BaseController {

    @Resource
    private IMasterInstanceService masterInstanceService;

    @Operation(summary = "查询主数据模型实例入口列表")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<MasterInstanceRespVO>> list(
            @Valid MasterInstancePageReqVO pageReqVO) {
        PageResult<MasterInstanceRespDTO> page = masterInstanceService.getInstancePage(pageReqVO);
        return CommonResult.success(BeanUtils.toBean(page, MasterInstanceRespVO.class));
    }

}
