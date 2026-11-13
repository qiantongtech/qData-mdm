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
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.*;
import tech.qiantong.qdata.mdm.model.service.base.IModelPublishService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 已发布模型公共查询，不增加发布操作入口。
 */
@Tag(name = "已发布模型查询")
@RestController
@RequestMapping("/model/published")
@Validated
public class ModelPublishedController extends BaseController {

    /** 读取成功发布的模型、字段、规则和物化信息。 */
    @Resource
    private IModelPublishService modelPublishService;

    /**
     * 按成功模型内容筛选，返回基本信息和发布版本，不附带字段或物化信息。
     */
    @Operation(summary = "查询已发布模型列表")
    @PreAuthorize("@ss.hasPermi('model:base:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelPublishedRespVO>> list(ModelPublishedPageReqVO request) {
        return CommonResult.success(BeanUtils.toBean(modelPublishService.getPublishedModelPage(request), ModelPublishedRespVO.class));
    }

    @Operation(summary = "查询已发布模型树")
    @PreAuthorize("@ss.hasPermi('model:base:list')")
    @GetMapping("/tree")
    public CommonResult<List<ModelTreeRespVO>> tree() {
        return CommonResult.success(modelPublishService.getPublishedModelTree());
    }

    /**
     * 模型正在修订或发布失败时，仍返回最近一次成功发布的信息。
     */
    @Operation(summary = "查询已发布模型基础信息")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping("/{id}")
    public CommonResult<ModelPublishedRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(BeanUtils.toBean(modelPublishService.getPublishedModel(id), ModelPublishedRespVO.class));
    }

    /**
     * 一次返回成功版本的主子字段和引用信息，避免调用方拼接不同版本配置。
     */
    @Operation(summary = "查询已发布模型字段及子模型")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping("/{id}/structure")
    public CommonResult<ModelPublishedStructureRespVO> getStructure(@PathVariable("id") Long id) {
        return CommonResult.success(BeanUtils.toBean(modelPublishService.getPublishedModelStructure(id), ModelPublishedStructureRespVO.class));
    }

    /**
     * 返回成功实例保存的物化信息，不触发建表或业务库探查。
     */
    @Operation(summary = "查询已发布模型物化信息")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping("/{id}/materialization")
    public CommonResult<ModelPublishedMaterializationRespVO> getMaterialization(@PathVariable("id") Long id) {
        return CommonResult.success(BeanUtils.toBean(modelPublishService.getPublishedModelMaterialization(id), ModelPublishedMaterializationRespVO.class));
    }

    /**
     * ruleCategory选择三类规则，具体策略仍在返回的ruleType中。
     */
    @Operation(summary = "查询已发布模型关联规则")
    @PreAuthorize("@ss.hasPermi('model:base:query')")
    @GetMapping("/{id}/rules")
    public CommonResult<ModelPublishedRulesRespVO> getRules(@PathVariable("id") Long id,
                                                            @RequestParam("ruleCategory") String ruleCategory) {
        return CommonResult.success(BeanUtils.toBean(modelPublishService.getPublishedModelRules(id, ruleCategory), ModelPublishedRulesRespVO.class));
    }
}
