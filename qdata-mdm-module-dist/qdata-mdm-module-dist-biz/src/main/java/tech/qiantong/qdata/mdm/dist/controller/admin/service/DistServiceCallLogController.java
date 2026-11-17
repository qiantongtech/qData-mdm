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

package tech.qiantong.qdata.mdm.dist.controller.admin.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogSummaryRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceCallLogDO;
import tech.qiantong.qdata.mdm.dist.service.service.IDistServiceCallLogService;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Positive;

/**
 * 数据服务调用记录的分页、统计和详情查询入口。
 *
 * @author chentangwen
 */
@Tag(name = "数据服务调用记录")
@RestController
@RequestMapping("/dist/service-call-logs")
@Validated
public class DistServiceCallLogController {
    @Resource
    private IDistServiceCallLogService service;

    /**
     * 按指定服务分页查询调用记录，供服务详情页展示。
     */
    @Operation(summary = "分页查询数据服务调用记录")
    @PreAuthorize("@ss.hasPermi('dist:service:query')")
    @GetMapping("/list")
    public CommonResult<PageResult<DistServiceCallLogRespVO>> list(
            @RequestParam @Positive Long serviceId, @Valid DistServiceCallLogPageReqVO request) {
        request.setServiceId(serviceId);
        return CommonResult.success(BeanUtils.toBean(service.getPage(request), DistServiceCallLogRespVO.class));
    }

    /**
     * 返回近七天调用次数及历史最近一次调用摘要。
     */
    @Operation(summary = "查询数据服务调用统计")
    @PreAuthorize("@ss.hasPermi('dist:service:query')")
    @GetMapping("/summary")
    public CommonResult<DistServiceCallLogSummaryRespVO> summary(@RequestParam @Positive Long serviceId) {
        return CommonResult.success(service.getSummary(serviceId));
    }

    /**
     * 根据日志主键查询单次调用快照。
     */
    @Operation(summary = "查询数据服务调用记录详情")
    @PreAuthorize("@ss.hasPermi('dist:service:query')")
    @GetMapping("/{id}")
    public CommonResult<DistServiceCallLogRespVO> getInfo(@PathVariable @Positive Long id) {
        DistServiceCallLogDO callLog = service.getById(id);
        return CommonResult.success(BeanUtils.toBean(callLog, DistServiceCallLogRespVO.class));
    }
}
