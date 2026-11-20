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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstancePageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceDO;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskInstanceService;

/**
 * 分发任务执行记录查询接口。
 * 提供执行记录分页列表和单次执行详情。
 *
 * @author qdata
 * @date 2026-10-13
 */
@Tag(name = "分发任务执行实例")
@RestController
@RequestMapping("/dist/task/instance")
@Validated
public class DistTaskInstanceController extends BaseController {
    @Resource
    private IDistTaskInstanceService distTaskInstanceService;

    /**
     * 分页查询分发任务的执行记录。
     *
     * @param distTaskInstance 查询和分页条件
     * @return 执行记录分页结果
     */
    @Operation(summary = "查询分发任务执行实例列表")
    @PreAuthorize("@ss.hasAnyPermi('dist:task:query,dist:task:instance:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<DistTaskInstanceRespVO>> list(DistTaskInstancePageReqVO distTaskInstance) {
        PageResult<DistTaskInstanceDO> page = distTaskInstanceService.getDistTaskInstancePage(distTaskInstance);
        return CommonResult.success(BeanUtils.toBean(page, DistTaskInstanceRespVO.class));
    }

    /**
     * 根据执行记录 ID 查询单次执行详情。
     *
     * @param id 执行记录 ID
     * @return 执行记录详情
     */
    @Operation(summary = "获取分发任务执行实例详细信息")
    @PreAuthorize("@ss.hasAnyPermi('dist:task:query,dist:task:instance:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<DistTaskInstanceRespVO> getInfo(@PathVariable("id") Long id) {
        DistTaskInstanceDO distTaskInstanceDO = distTaskInstanceService.getDistTaskInstanceById(id);
        return CommonResult.success(BeanUtils.toBean(distTaskInstanceDO, DistTaskInstanceRespVO.class));
    }

}
