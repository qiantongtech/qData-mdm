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

package tech.qiantong.qdata.mdm.dist.controller.admin.task;

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
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskSaveReqVO;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskService;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskExecutionService;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceRespVO;

/**
 * 分发任务管理接口。
 * 负责接收请求、校验访问权限，并将任务查询、保存和执行交给业务服务。
 *
 * @author qdata
 * @date 2026-10-13
 */
@Tag(name = "分发任务")
@RestController
@RequestMapping("/dist/task")
@Validated
public class DistTaskController extends BaseController {
    @Resource
    private IDistTaskService distTaskService;

    @Resource
    private IDistTaskExecutionService executionService;

    /**
     * 使用当前登录人身份人工执行一次分发任务。
     *
     * @param id 分发任务 ID
     * @return 本次执行结果
     */
    @Operation(summary = "人工执行分发任务一次")
    @PreAuthorize("@ss.hasPermi('dist:task:edit')")
    @PostMapping("/{id}/run")
    public CommonResult<DistTaskInstanceRespVO> run(@PathVariable Long id) {
        return CommonResult.success(executionService.execute(id, getUserId(), getNickName()));
    }

    /**
     * 克隆任务定义及其条件和字段映射。
     *
     * @param id 原任务 ID
     * @return 新任务 ID
     */
    @Operation(summary = "克隆分发任务")
    @PreAuthorize("@ss.hasPermi('dist:task:add')")
    @Log(title = "分发任务", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/clone")
    public CommonResult<Long> clone(@PathVariable Long id) {
        return CommonResult.success(distTaskService.cloneDistTask(id, getUserId(), getNickName()));
    }

    /**
     * 分页查询分发任务及其展示信息。
     *
     * @param distTask 查询和分页条件
     * @return 分发任务分页结果
     */
    @Operation(summary = "查询分发任务列表")
    @PreAuthorize("@ss.hasPermi('dist:task:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<DistTaskRespVO>> list(DistTaskPageReqVO distTask) {
        return CommonResult.success(distTaskService.getDistTaskPageDetail(distTask));
    }

    /**
     * 查询任务详情，按需加载筛选条件和字段映射。
     *
     * @param id 分发任务 ID
     * @param includeFilters 是否加载筛选条件
     * @param includeFieldMappings 是否加载字段映射
     * @return 分发任务详情
     */
    @Operation(summary = "获取分发任务详细信息")
    @PreAuthorize("@ss.hasPermi('dist:task:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<DistTaskRespVO> getInfo(@PathVariable("id") Long id,
                                                @RequestParam(value = "includeFilters", defaultValue = "false") boolean includeFilters,
                                                @RequestParam(value = "includeFieldMappings", defaultValue = "false") boolean includeFieldMappings) {
        return CommonResult.success(distTaskService.getDistTaskDetail(id, includeFilters, includeFieldMappings));
    }

    /**
     * 创建分发任务；未指定创建人时使用当前登录人。
     *
     * @param distTask 分发任务保存信息
     * @return 新任务 ID
     */
    @Operation(summary = "新增分发任务")
    @PreAuthorize("@ss.hasPermi('dist:task:add')")
    @Log(title = "分发任务", businessType = BusinessType.INSERT,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody DistTaskSaveReqVO distTask) {
        if (distTask.getCreatorId() == null) {
            distTask.setCreatorId(getUserId());
        }
        distTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(distTaskService.createDistTask(distTask));
    }

    /**
     * 修改分发任务，并记录当前登录人为修改人。
     *
     * @param distTask 分发任务保存信息
     * @return 受影响的记录数
     */
    @Operation(summary = "修改分发任务")
    @PreAuthorize("@ss.hasPermi('dist:task:edit')")
    @Log(title = "分发任务", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody DistTaskSaveReqVO distTask) {
        distTask.setUpdatorId(getUserId());
        distTask.setUpdateBy(getNickName());
        distTask.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(distTaskService.updateDistTask(distTask));
    }

    /**
     * 批量删除分发任务。
     *
     * @param ids 待删除任务的 ID
     * @return 受影响的记录数
     */
    @Operation(summary = "删除分发任务")
    @PreAuthorize("@ss.hasPermi('dist:task:remove')")
    @Log(title = "分发任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(distTaskService.removeDistTask(Arrays.asList(ids)));
    }

}
