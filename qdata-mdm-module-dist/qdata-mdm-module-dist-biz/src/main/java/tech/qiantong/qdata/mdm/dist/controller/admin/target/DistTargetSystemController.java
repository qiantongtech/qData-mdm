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

package tech.qiantong.qdata.mdm.dist.controller.admin.target;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemSaveReqVO;
import tech.qiantong.qdata.mdm.dist.service.target.IDistTargetSystemService;

/**
 * 分发目标系统Controller
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
@Tag(name = "分发目标系统")
@RestController
@RequestMapping("/dist/target")
@Validated
public class DistTargetSystemController extends BaseController {
    @Resource
    private IDistTargetSystemService distTargetSystemService;

    /**
     * 查询目标系统分页列表，并补充创建人、负责人及关联分发任务数量。
     *
     * @param distTargetSystem 列表查询和分页条件
     * @return 目标系统分页数据
     */
    @Operation(summary = "查询分发目标系统列表")
    @PreAuthorize("@ss.hasPermi('dist:target:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<DistTargetSystemRespVO>> list(DistTargetSystemPageReqVO distTargetSystem) {
        return CommonResult.success(distTargetSystemService.getDistTargetSystemPageDetail(distTargetSystem));
    }

    /**
     * 查询目标系统详情，包含创建人和负责人的资料。
     *
     * @param id 目标系统 ID
     * @return 目标系统详情
     */
    @Operation(summary = "获取分发目标系统详细信息")
    @PreAuthorize("@ss.hasPermi('dist:target:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<DistTargetSystemRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(distTargetSystemService.getDistTargetSystemDetail(id));
    }

    /**
     * 创建目标系统，并记录当前登录人为创建人。
     *
     * @param distTargetSystem 目标系统保存信息
     * @return 新建目标系统的 ID
     */
    @Operation(summary = "新增分发目标系统")
    @PreAuthorize("@ss.hasPermi('dist:target:add')")
    @Log(title = "分发目标系统", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody DistTargetSystemSaveReqVO distTargetSystem) {
        // 创建人从当前登录账号自动记录，负责人另存于 RESP_USER_ID。
        distTargetSystem.setCreatorId(getUserId());
        distTargetSystem.setCreateBy(getNickName());
        distTargetSystem.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(distTargetSystemService.createDistTargetSystem(distTargetSystem));
    }

    /**
     * 更新目标系统，并记录当前登录人为修改人。
     *
     * @param distTargetSystem 目标系统保存信息
     * @return 受影响的记录数
     */
    @Operation(summary = "修改分发目标系统")
    @PreAuthorize("@ss.hasPermi('dist:target:edit')")
    @Log(title = "分发目标系统", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody DistTargetSystemSaveReqVO distTargetSystem) {
        distTargetSystem.setUpdatorId(getUserId());
        distTargetSystem.setUpdateBy(getNickName());
        distTargetSystem.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(distTargetSystemService.updateDistTargetSystem(distTargetSystem));
    }

    /**
     * 批量删除符合删除条件的目标系统。
     *
     * @param ids 待删除的目标系统 ID
     * @return 受影响的记录数
     */
    @Operation(summary = "删除分发目标系统")
    @PreAuthorize("@ss.hasPermi('dist:target:remove')")
    @Log(title = "分发目标系统", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(distTargetSystemService.removeDistTargetSystem(Arrays.asList(ids)));
    }

}
