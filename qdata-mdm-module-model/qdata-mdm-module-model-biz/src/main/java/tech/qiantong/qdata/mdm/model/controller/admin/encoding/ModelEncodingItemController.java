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

package tech.qiantong.qdata.mdm.model.controller.admin.encoding;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.exception.enums.GlobalErrorCodeConstants;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemBatchSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingItemService;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingVersionService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 编码方案规则段Controller
 *
 * @author qdata
 * @date 2026-09-17
 */
@Tag(name = "编码方案规则段")
@RestController
@RequestMapping("/model/encoding/item")
@Validated
public class ModelEncodingItemController extends BaseController {
    @Resource
    private IModelEncodingItemService modelEncodingItemService;
    @Resource
    private IModelEncodingVersionService modelEncodingVersionService;

    @Operation(summary = "查询编码方案规则段列表")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelEncodingItemRespVO>> list(ModelEncodingItemPageReqVO modelEncodingItem) {
        PageResult<ModelEncodingItemDO> page = modelEncodingItemService.getModelEncodingItemPage(modelEncodingItem);
        return CommonResult.success(BeanUtils.toBean(page, ModelEncodingItemRespVO.class));
    }

    /** 查询有效编码规则段，参数结构与普通列表一致，仅固定过滤 validFlag=1。 */
    @Operation(summary = "查询有效编码规则段列表")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:list')")
    @GetMapping("/valid-list")
    public CommonResult<List<ModelEncodingItemRespVO>> validList(ModelEncodingItemPageReqVO modelEncodingItem) {
        modelEncodingItem.setPageSize(PageParam.PAGE_SIZE_NONE);
        // 复用规则段列表的分页、版本过滤和排序，仅覆盖有效状态，供参考数据映射下拉框使用。
        modelEncodingItem.setValidFlag(Boolean.TRUE);
        PageResult<ModelEncodingItemDO> page = modelEncodingItemService.getModelEncodingItemPage(modelEncodingItem);
        return CommonResult.success(BeanUtils.toBean(page.getRows(), ModelEncodingItemRespVO.class));
    }

    @Operation(summary = "获取编码方案规则段详细信息")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelEncodingItemRespVO> getInfo(@PathVariable("id") Long id) {
        ModelEncodingItemDO modelEncodingItemDO = modelEncodingItemService.getModelEncodingItemById(id);
        return CommonResult.success(BeanUtils.toBean(modelEncodingItemDO, ModelEncodingItemRespVO.class));
    }

    @Operation(summary = "新增编码方案规则段")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:add')")
    @Log(title = "编码方案规则段", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelEncodingItemSaveReqVO modelEncodingItem) {
        if (modelEncodingItem.getEncodingVersionId() == null
                || modelEncodingVersionService.getModelEncodingVersionById(modelEncodingItem.getEncodingVersionId()) == null) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "新增编码方案规则段失败，编码方案版本不存在");
        }
        modelEncodingItem.setCreatorId(getUserId());
        modelEncodingItem.setCreateBy(getNickName());
        modelEncodingItem.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(modelEncodingItemService.createModelEncodingItem(modelEncodingItem));
    }

    @Operation(summary = "修改编码方案规则段")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:edit')")
    @Log(title = "编码方案规则段", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelEncodingItemSaveReqVO modelEncodingItem) {
        if (modelEncodingItem.getEncodingVersionId() == null
                || modelEncodingVersionService.getModelEncodingVersionById(modelEncodingItem.getEncodingVersionId()) == null) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(), "修改编码方案规则段失败，编码方案版本不存在");
        }
        modelEncodingItem.setUpdatorId(getUserId());
        modelEncodingItem.setUpdateBy(getNickName());
        modelEncodingItem.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(modelEncodingItemService.updateModelEncodingItem(modelEncodingItem));
    }

    @Operation(summary = "批量保存编码方案规则段")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:edit')")
    @Log(title = "编码方案规则段", businessType = BusinessType.UPDATE)
    @PutMapping("/batch")
    public CommonResult<Boolean> saveBatch(@Valid @RequestBody ModelEncodingItemBatchSaveReqVO saveReqVO) {
        // 版本ID以批量请求外层参数为准，防止规则项携带其他版本ID导致跨版本写入。
        saveReqVO.getItems().forEach(item -> {
            item.setEncodingVersionId(saveReqVO.getEncodingVersionId());
            // 无ID表示新增，有ID表示更新，分别补齐对应的创建或修改审计信息。
            if (item.getId() == null) {
                item.setCreatorId(getUserId());
                item.setCreateBy(getNickName());
                item.setCreateTime(DateUtil.date());
            } else {
                item.setUpdatorId(getUserId());
                item.setUpdateBy(getNickName());
                item.setUpdateTime(DateUtil.date());
            }
        });
        // 整套规则交由Service在同一事务内完成新增、更新、删除及顺序同步。
        modelEncodingItemService.saveModelEncodingItems(
                saveReqVO.getEncodingVersionId(), saveReqVO.getItems());
        return CommonResult.success(true);
    }

    @Operation(summary = "删除编码方案规则段")
    @PreAuthorize("@ss.hasPermi('model:encoding:item:remove')")
    @Log(title = "编码方案规则段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            ModelEncodingItemDO modelEncodingItem = modelEncodingItemService.getModelEncodingItemById(id);
            if (modelEncodingItem != null && Boolean.TRUE.equals(modelEncodingItem.getValidFlag())) {
                return CommonResult.error(
                        GlobalErrorCodeConstants.ERROR.getCode(),
                        "删除编码方案规则段'" + id + "'失败,请先停用!"
                );
            }
        }
        return CommonResult.toAjax(modelEncodingItemService.removeModelEncodingItem(Arrays.asList(ids)));
    }

}
