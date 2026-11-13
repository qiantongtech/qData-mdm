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

package tech.qiantong.qdata.mdm.model.controller.admin.base;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelInstanceHistoryRespVO;
import tech.qiantong.qdata.mdm.model.service.base.IModelInstanceService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 模型发布实例Controller，与模型版本查询分别维护。
 */
@Tag(name = "模型发布实例")
@RestController
@RequestMapping("/model/base/instance")
@Validated
public class ModelInstanceController extends BaseController {

    /**
     * 读取当前模型的真实物化发布记录，包含成功、失败和进行中的记录。
     */
    @Resource
    private IModelInstanceService modelInstanceService;

    /**
     * 按模型ID查询发布历史，沿用模型详情查询权限，列表在前端分页。
     */
    @Operation(summary = "查询模型物化发布历史")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping("/list")
    public CommonResult<List<ModelInstanceHistoryRespVO>> list(@RequestParam("modelId") Long modelId) {
        return CommonResult.success(BeanUtils.toBean(modelInstanceService.getModelInstanceHistory(modelId), ModelInstanceHistoryRespVO.class));
    }
}
